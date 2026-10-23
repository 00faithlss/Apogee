package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.mantis.MantisRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * First-person view with the nanowire out: both of the player's own arms where vanilla holds a
 * hand, a little higher and closer in so the wire held between the fists crosses the lower view,
 * with the housings and wires on them.
 *
 * <p>After a Cyberpunk 2077 monowire clip, frame by frame: at rest both fists hold the wire taut
 * across the view; a lash winds the arm out to its side and sweeps it across the view close to
 * the camera in about 0.13 s, then brings it back; in the two-wire lash both arms throw their
 * own wire.
 */
public final class NanowireHands {
    /** How far below its place the pair of arms starts and ends while the wire deploys or retracts. */
    private static final float RISE = 0.65F;
    /** The guard: each fist this far in from vanilla's hand and up. */
    private static final float GUARD_IN = 0.10F;
    private static final float GUARD_UP = 0.16F;

    private NanowireHands() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, LocalPlayer player, int light, float partialTick) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        poseStack.pushPose();
        MantisHandPose.applyViewLag(poseStack, player, partialTick);
        NanowireWire.Frame frame = NanowireWire.Frame.of(poseStack.last(), false);
        float deploy = MantisClient.deployProgress(player, partialTick);
        for (boolean right : new boolean[]{true, false}) {
            float side = right ? 1.0F : -1.0F;
            poseStack.pushPose();
            MantisHandPose.apply(poseStack, player, partialTick, right, deploy);
            poseStack.translate(0.0F, -MantisClient.HANDS_DROP * (1.0F - MantisClient.implantLift(player, partialTick)), 0.0F);
            if (right != (player.getMainArm() == HumanoidArm.RIGHT) && PunchyCompat.lowerOffArm(player)) {
                // The other arm rises into view as the wire deploys and sinks as it retracts, so
                // with the wire away only the main hand is left, as in vanilla.
                poseStack.translate(0.0F, -RISE * (1.0F - deploy), 0.0F);
            }
            if (!PunchyCompat.rendering()) {
                // Under Punchy the fists stay exactly on its animated pose, as the Mantis arms do.
                poseStack.translate(-side * GUARD_IN * deploy, GUARD_UP * deploy, 0.0F);
                poseStack.translate(0.0F, (right ? -0.025F : 0.065F) * deploy, (right ? 0.015F : -0.035F) * deploy);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F * deploy));
            }
            applyAttack(poseStack, player, right, partialTick);
            arm(poseStack, buffers, light, player, renderer, right, partialTick);
            poseStack.popPose();
        }
        NanowireRig.drawWires(buffers, player, frame, partialTick);
        poseStack.popPose();
    }

    /** Implant attacks layered on the hand renderer's native pose; no idle or locomotion pose. */
    public static void applyAttack(PoseStack poseStack, LocalPlayer player, boolean right, float partialTick) {
        float side = right ? 1.0F : -1.0F;
        float heavy = MantisClient.thrustProgress(player, partialTick);
        int style = MantisClient.heavyStyle(player);
        float own = MantisClient.slashProgress(player, right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT, partialTick);
        float other = MantisClient.slashProgress(player, right ? MantisRules.ARM_LEFT : MantisRules.ARM_RIGHT, partialTick);
        if (heavy >= 0.0F) {
            heavy(poseStack, NanowireMoves.heavyTime(side, style, heavy), side, style);
        } else if (own >= 0.0F) {
            lash(poseStack, own, side);
        } else if (other >= 0.0F) {
            // The other fist lets go of the wire and gives a little.
            float k = NanowireMoves.release(other, false);
            poseStack.translate(side * 0.08F * k, -0.08F * k, 0.04F * k);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 7.0F * k));
        }
    }

    /**
     * One arm's lash: a short wind-up out to its own side, then a sweep across the view close to
     * the camera, easing back to the guard.
     */
    private static void lash(PoseStack poseStack, float u, float side) {
        NanowireMoves.Motion motion = NanowireMoves.motion(u, false);
        float wind = motion.wind();
        float sweep = motion.strike();
        float follow = motion.follow();
        poseStack.translate(side * (0.16F * wind - 0.76F * sweep + 0.10F * follow),
                0.09F * wind + 0.16F * sweep - 0.12F * follow - 0.025F * motion.catchWire(),
                0.06F * wind - 0.30F * sweep + 0.09F * follow);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (-10.0F * wind + 38.0F * sweep - 8.0F * follow)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (18.0F * wind - 26.0F * sweep + 12.0F * follow)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F * wind + 10.0F * follow));
    }

    /** Both hands pull up, then snap down; the cable bend follows the hands towards the tip. */
    private static void heavy(PoseStack poseStack, float u, float side, int style) {
        NanowireMoves.Motion motion = NanowireMoves.motion(u, true);
        float raise = motion.wind();
        float strike = motion.strike();
        float follow = motion.follow();
        float cross = style == NanowireMoves.HEAVY_X ? 0.26F : style == NanowireMoves.HEAVY_S ? 0.14F : 0.0F;
        poseStack.translate(side * (0.06F * raise - cross * strike),
                0.25F * raise - 0.05F * strike - 0.03F * follow,
                0.03F * raise);
        // In view space the hand is at negative Z: positive X rotation raises it.
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F * raise - 4.0F * strike - 2.0F * follow));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * cross * 35.0F * strike));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (12.0F * raise - cross * 55.0F * strike)));
    }

    /** Vanilla's first-person arm placement (with no swing or equip offset), then the arm itself. */
    private static void arm(PoseStack poseStack, MultiBufferSource buffers, int light, LocalPlayer player, PlayerRenderer renderer,
                            boolean right, float partialTick) {
        if (PunchyCompat.renderArm(poseStack, buffers, light, player, right, partialTick)) {
            return;
        }
        float side = right ? 1.0F : -1.0F;
        poseStack.translate(side * 0.64000005F, -0.6F, -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);
        // Keeps the third-person stance off the arm model while it is posed here.
        MantisClient.firstPersonRendering = true;
        try {
            if (right) {
                renderer.renderRightHand(poseStack, buffers, light, player);
            } else {
                renderer.renderLeftHand(poseStack, buffers, light, player);
            }
        } finally {
            MantisClient.firstPersonRendering = false;
        }
    }
}
