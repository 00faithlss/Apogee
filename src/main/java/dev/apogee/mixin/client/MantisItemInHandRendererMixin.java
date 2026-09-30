package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the blades are out or unfold or fold away, the first-person view shows both implants
 * and no held item; the other arm rises into view as they unfold and sinks away as they fold.
 * Each slash swings the arm that cuts (they alternate), and a lunge brings both arms forward
 * together. The arms breathe, swing with each step and trail the view as it turns, as hands
 * holding items do.
 * Low priority so this runs before other first-person animation mods and takes over the view.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class MantisItemInHandRendererMixin {
    /** Extra sideways offset of each forearm while the blades are out, in screen units. */
    private static final float ARM_SPREAD = 0.22F;
    /** How far below its place the other forearm starts and ends while unfolding or folding. */
    private static final float OFF_ARM_DROP = 0.9F;
    /** Idle breathing: radians per tick, rise in screen units and tilt in degrees. */
    private static final float BREATH_SPEED = 0.07F;
    private static final float BREATH_HEIGHT = 0.012F;
    private static final float BREATH_TILT = 1.2F;
    /** Walking swing per unit of the player's bob (at most 0.1): sideways, down and roll. */
    private static final float STEP_SIDE = 0.35F;
    private static final float STEP_HEIGHT = 0.5F;
    private static final float STEP_ROLL = 40.0F;

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int light, float equippedProgress,
                                 float swingProgress, HumanoidArm arm) {
        throw new AssertionError();
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void apogee$mantisHands(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                    LocalPlayer player, int light, CallbackInfo ci) {
        if (!MantisClient.isOut(player) || player.isScoping()) {
            return;
        }
        ci.cancel();
        poseStack.pushPose();
        // Vanilla's lag as the view turns: the hands trail a little behind the camera.
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
        float thrust = MantisClient.thrustProgress(player, partialTick);
        for (HumanoidArm arm : new HumanoidArm[]{HumanoidArm.RIGHT, HumanoidArm.LEFT}) {
            boolean right = arm == HumanoidArm.RIGHT;
            float side = right ? 1.0F : -1.0F;
            poseStack.pushPose();
            sway(poseStack, player, partialTick, right);
            // Spread the forearms out towards the screen edges, level with where vanilla holds
            // an item in each hand; the implants' blades then rise clear of the view's centre.
            float guard = MantisClient.deployProgress(player, partialTick);
            poseStack.translate(side * ARM_SPREAD * guard, 0.0F, 0.0F);
            if (arm != player.getMainArm()) {
                // The other arm rises into view as the blades unfold and sinks out as they fold,
                // so folded only the main hand is left, as in vanilla.
                poseStack.translate(0.0F, -OFF_ARM_DROP * (1.0F - guard), 0.0F);
            }
            if (!right && MantisClient.variant(player) == MantisVariant.THERMAL) {
                // More of the left implant faces the camera, without resizing the skin hand.
                poseStack.translate(0.10F * guard, 0.12F * guard, 0.10F * guard);
                poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F * guard));
                poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F * guard));
            }
            if (thrust >= 0.0F) {
                float lunge = Mth.sin(thrust * Mth.PI);
                poseStack.translate(-side * 0.30F * lunge, 0.12F * lunge, -0.60F * lunge);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 16.0F * lunge));
            }
            float slash = thrust >= 0.0F ? -1.0F
                    : MantisClient.slashProgress(player, right ? MantisRules.ARM_RIGHT : MantisRules.ARM_LEFT, partialTick);
            if (slash >= 0.0F) {
                // A diagonal cut: the forearm sweeps in from its side of the view and down across it.
                float reach = Mth.sin(Mth.sqrt(slash) * Mth.PI);
                poseStack.translate(-side * 0.22F * reach, -0.08F * reach, -0.18F * reach);
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 28.0F * reach));
            }
            float swing = Math.max(slash, 0.0F);
            // The vanilla swing, on whichever hand cuts; the implant's attack clip adds the blade's arc.
            renderPlayerArm(poseStack, buffers, light, 0.0F, swing, arm);
            poseStack.popPose();
        }
        poseStack.popPose();
        buffers.endBatch();
    }

    /**
     * Keeps the arms alive the way hands holding items are: a slow breath while standing, and a
     * step-by-step swing while walking, each arm half a step behind the other.
     */
    private static void sway(PoseStack poseStack, LocalPlayer player, float partialTick, boolean right) {
        float side = right ? 1.0F : -1.0F;
        float time = (player.tickCount + partialTick) * BREATH_SPEED + (right ? 0.0F : 1.3F);
        poseStack.translate(0.0F, Mth.sin(time) * BREATH_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time - 0.6F) * BREATH_TILT));
        float step = -(player.walkDist + (player.walkDist - player.walkDistO) * partialTick) * Mth.PI + (right ? 0.0F : Mth.PI);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        poseStack.translate(Mth.sin(step) * bob * STEP_SIDE, -Math.abs(Mth.cos(step)) * bob * STEP_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(step) * bob * STEP_ROLL));
    }
}
