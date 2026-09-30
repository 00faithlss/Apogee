package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * How the first-person implant arms move while the blades are out, so that they move like the
 * hands that take over once the blades are folded away.
 *
 * <p>Vanilla hands have no motion of their own: only the view bob (when enabled), which moves the
 * whole first-person scene, and the lag as the view turns (applied by the caller). With Punchy the
 * hands breathe, sway with the steps and dip while walking (differently when sprinting), and Punchy keeps the view bob off
 * them, so it is undone here too. Its figures here come from tracking its hands frame by frame
 * ({@code tools/mantis_tests.sh motion} and {@code tools/mantis_motion.py}, 960x540, fov 70, where
 * one unit of hand translation is about 400 pixels); the pose its hand enters with after the
 * blades fold comes from fitting the folded arm onto it in game ({@code -Dapogee.mantisCalibrate}).
 */
public final class MantisHandPose {
    public static final boolean PUNCHY = FabricLoader.getInstance().isModLoaded("punchy");

    /** Punchy's idle drift: a slow loop of about 4 s, rising as it moves out. */
    private static final float IDLE_PERIOD = 80.0F;
    private static final float IDLE_X = 0.0225F;
    private static final float IDLE_Y = 0.015F;
    /** Punchy's walk, per unit of vanilla's bob (0.1 at walking pace), in phase with the steps. */
    private static final float WALK_X = 0.92F;
    private static final float WALK_Y = 0.175F;
    private static final float WALK_DIP = 0.975F;
    /** Punchy's sprint, per unit of bob, from the same tracking. */
    private static final float SPRINT_X = 0.33F;
    private static final float SPRINT_Y = 0.46F;
    private static final float SPRINT_Y_LEAD = 0.22F;
    private static final float SPRINT_DIP = 0.5F;
    /** Eased 0..1 blend from walking to sprinting, this tick and the last. */
    private static float sprint;
    private static float sprintO;

    /**
     * Pose the main arm ends the fold in, relative to vanilla's, as translation (x, y, z) and
     * rotation in degrees (yaw, pitch, roll); mirrored on the left. Zero without Punchy, where the
     * folded implant already matches vanilla's empty hand.
     */
    public static final float[] FOLDED = PUNCHY ? new float[]{0.127F, -0.026F, 0.096F, 3.4F, 1.9F, 8.5F} : new float[6];

    /** Where vanilla's empty hand sits in the first-person view ({@code renderPlayerArm}). */
    private static final float HAND_X = 0.64F;
    private static final float HAND_Y = -0.6F;
    private static final float HAND_Z = -0.72F;

    /** Test hook: keeps the implant view on screen while folded, to fit {@link #FOLDED}. */
    public static boolean holdFolded;

    private MantisHandPose() {
    }

    /** Eases into and out of the sprint motion; once a client tick. */
    public static void tick(LocalPlayer player) {
        sprintO = sprint;
        sprint += ((player != null && player.isSprinting() ? 1.0F : 0.0F) - sprint) * 0.35F;
    }

    /**
     * Applies the motion to one arm, before its own pose. {@code open} is the eased unfold
     * progress: the folded pose is blended out as the blades unfold.
     */
    public static void apply(PoseStack poseStack, LocalPlayer player, float partialTick, boolean right, float open) {
        float side = right ? 1.0F : -1.0F;
        float folded = 1.0F - open;
        poseStack.translate(side * FOLDED[0] * folded, FOLDED[1] * folded, FOLDED[2] * folded);
        // Rotations turn the hand in place, about where vanilla holds it.
        poseStack.translate(side * HAND_X, HAND_Y, HAND_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * FOLDED[3] * folded));
        poseStack.mulPose(Axis.XP.rotationDegrees(FOLDED[4] * folded));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * FOLDED[5] * folded));
        poseStack.translate(-side * HAND_X, -HAND_Y, -HAND_Z);
        if (!PUNCHY) {
            return;
        }
        float time = (player.tickCount + partialTick) / IDLE_PERIOD * Mth.TWO_PI;
        float idle = Mth.sin(time);
        poseStack.translate(side * IDLE_X * idle, IDLE_Y * idle, 0.0F);
        // Vanilla's step phase, a tick ahead of the interpolated one, as measured.
        float phase = (player.walkDistO + (player.walkDist - player.walkDistO) * (partialTick + 1.0F)) * Mth.PI;
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        float run = Mth.lerp(partialTick, sprintO, sprint);
        float sin = Mth.sin(phase);
        // Walking: a wide sway with a slight tilt, the hand dipped. Sprinting: a narrower sway the
        // other way round, a stronger bounce once a cycle and a smaller dip.
        float x = Mth.lerp(run, WALK_X * sin, -SPRINT_X * sin) * bob;
        float y = Mth.lerp(run, WALK_Y * sin - WALK_DIP, -SPRINT_Y * Mth.sin(phase + SPRINT_Y_LEAD) - SPRINT_DIP) * bob;
        poseStack.translate(side * x, y, 0.0F);
    }

    /**
     * With Punchy, undoes vanilla's view bob, which Punchy leaves off its hands. Must come first,
     * right after the view bob itself.
     */
    public static void undoViewBob(PoseStack poseStack, LocalPlayer player, float partialTick) {
        if (!PUNCHY || !Minecraft.getInstance().options.bobView().get()) {
            return;
        }
        float g = player.walkDist - player.walkDistO;
        float h = -(player.walkDist + g * partialTick);
        float i = Mth.lerp(partialTick, player.oBob, player.bob);
        poseStack.mulPose(Axis.XP.rotationDegrees(-Math.abs(Mth.cos(h * Mth.PI - 0.2F) * i) * 5.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(h * Mth.PI) * i * 3.0F));
        poseStack.translate(-Mth.sin(h * Mth.PI) * i * 0.5F, Math.abs(Mth.cos(h * Mth.PI) * i), 0.0F);
    }
}
