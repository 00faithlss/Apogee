package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import java.util.function.IntPredicate;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * Both Mantis Blades of every player in view: their animation state and renderers, and the
 * transform that seats the kit's model on a player arm.
 */
public final class MantisRig {
    private static final Int2ObjectOpenHashMap<MantisRig> RIGS = new Int2ObjectOpenHashMap<>();
    /** The kit's combat pose holds each arm 20 degrees out from the torso. */
    public static final float STANCE_ABDUCTION = (float) Math.toRadians(20.0);

    /*
     * Seating, from GeckoLib's model space (the kit's editor axes with X mirrored) to a player arm
     * part (+Y towards the hand, the right arm's outer face at -X): the kit's hand end (-Z) goes
     * down the arm and its opening (+Y) faces away from the body. The left arm's model is already
     * mirrored, so both mappings are proper rotations.
     */
    private static final Quaternionf RIGHT_SEAT = seat(new Matrix3f().setRow(0, 0, -1, 0).setRow(1, 0, 0, -1).setRow(2, 1, 0, 0));
    private static final Quaternionf LEFT_SEAT = seat(new Matrix3f().setRow(0, 0, 1, 0).setRow(1, 0, 0, -1).setRow(2, -1, 0, 0));

    private final MantisArm[] arms = {new MantisArm(true), new MantisArm(false)};
    private final MantisArmGeoRenderer[] renderers = {new MantisArmGeoRenderer(), new MantisArmGeoRenderer()};
    /** Set while the first-person hands are drawn, which use view space instead of the level's. */
    public static boolean firstPersonPass;

    private MantisRig() {
    }

    private static Quaternionf seat(Matrix3f rotation) {
        return new Quaternionf().setFromNormalized(rotation);
    }

    public static MantisRig of(int entityId) {
        return RIGS.computeIfAbsent(entityId, id -> new MantisRig());
    }

    public static MantisRig get(int entityId) {
        return RIGS.get(entityId);
    }

    public static void forget(int entityId) {
        RIGS.remove(entityId);
    }

    public static void clear() {
        RIGS.clear();
    }

    /** Drops the rigs of players no longer in the level. */
    public static void prune(IntPredicate present) {
        RIGS.int2ObjectEntrySet().removeIf(entry -> !present.test(entry.getIntKey()));
    }

    /**
     * Syncs one arm with the wearer's variant and deployed flag. A change first seen once its
     * travel is already over (a player coming into view with the blades out) skips the
     * transition nobody saw instead of replaying it late; one seen while under way plays.
     */
    private void update(int side, MantisVariant variant, boolean deployed, float progress) {
        MantisArm arm = arms[side];
        arm.setVariant(variant);
        arm.setDeployed(deployed, deployed ? progress >= 1.0F : progress <= 0.0F);
    }

    /** Visits both arms of every rig, with the wearer's entity id. */
    public static void forEachArm(ArmVisitor visitor) {
        for (var entry : RIGS.int2ObjectEntrySet()) {
            for (MantisArm arm : entry.getValue().arms) {
                visitor.visit(entry.getIntKey(), arm);
            }
        }
    }

    @FunctionalInterface
    public interface ArmVisitor {
        void visit(int entityId, MantisArm arm);
    }

    /** A slash by one arm: both clips play so the other arm follows through. */
    public void attack(int arm) {
        for (MantisArm each : arms) {
            each.attack(arm);
        }
    }

    /** The two-blade lunge: each arm plays its own attack clip at once. */
    public void thrust() {
        arms[0].attack(MantisRules.ARM_RIGHT);
        arms[1].attack(MantisRules.ARM_LEFT);
    }

    /**
     * Draws the Mantis Blade on one of a player's arms, if they wear one: seats the model on the
     * arm part's current pose and brings its animation state up to date.
     */
    public static void drawOnArm(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 ModelPart armPart, boolean right, float partialTick) {
        drawOnArm(poseStack, buffers, light, player, armPart, right, partialTick, false);
    }

    /**
     * As {@link #drawOnArm(PoseStack, MultiBufferSource, int, AbstractClientPlayer, ModelPart, boolean, float)};
     * with {@code enforceStance} (third person), an open implant also gets the combat stance's
     * 20 degree abduction when the arm does not already have it. Animation packs such as Fresh
     * Animations pose the arms after the vanilla pose code runs, and would otherwise leave the
     * open implant's lower covers inside the torso.
     */
    public static void drawOnArm(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 ModelPart armPart, boolean right, float partialTick, boolean enforceStance) {
        MantisVariant variant = MantisClient.variant(player);
        if (variant == null) {
            return;
        }
        MantisRig rig = of(player.getId());
        float progress = MantisClient.deployProgress(player, partialTick);
        rig.update(right ? 0 : 1, variant, MantisClient.isDeployed(player), progress);
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);
        if (enforceStance && progress > 0.0F) {
            // A positive zRot lifts the right arm away from the body, a negative one the left.
            float target = progress * STANCE_ABDUCTION;
            float missing = right ? Math.max(0.0F, target - armPart.zRot) : Math.min(0.0F, -target - armPart.zRot);
            poseStack.mulPose(Axis.ZP.rotation(missing));
        }
        rig.render(poseStack, buffers, light, player, right, progress > 0.0F, partialTick);
        poseStack.popPose();
    }

    /**
     * Draws one arm's Mantis Blade. The pose stack must be at the player arm's pivot, as left by
     * {@code ModelPart.translateAndRotate}.
     */
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, boolean right,
                       boolean glow, float partialTick) {
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        MantisArm arm = arms[right ? 0 : 1];
        MantisArmGeoRenderer renderer = renderers[right ? 0 : 1];
        // Centre of the arm box relative to its shoulder pivot, in model pixels.
        float centerX = right ? (slim ? -0.5F : -1.0F) : (slim ? 0.5F : 1.0F);
        poseStack.pushPose();
        poseStack.translate(centerX / 16.0F, 4.0F / 16.0F, 0.0F);
        if (slim) {
            poseStack.scale(0.75F, 1.0F, 1.0F);
        }
        poseStack.mulPose(right ? RIGHT_SEAT : LEFT_SEAT);
        renderer.prepare(player.getSkin().texture(), slim, glow, firstPersonPass);
        renderer.render(poseStack, arm, buffers, null, null, light, partialTick);
        poseStack.popPose();
    }
}
