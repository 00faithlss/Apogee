package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.client.nanowire.NanowireHands;
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
 * together. The arms trail the view as it turns, as vanilla hands do, and move as the hands
 * that follow them do ({@link MantisHandPose}).
 * Low priority so this runs before other first-person animation mods and takes over the view.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class MantisItemInHandRendererMixin {
    /** Extra sideways offset of each forearm while the blades are out, in screen units. */
    private static final float ARM_SPREAD = 0.22F;
    /** How far below its place the other forearm starts and ends while unfolding or folding. */
    private static final float OFF_ARM_DROP = 0.9F;

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int light, float equippedProgress,
                                 float swingProgress, HumanoidArm arm) {
        throw new AssertionError();
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void apogee$mantisHands(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                    LocalPlayer player, int light, CallbackInfo ci) {
        if (!(MantisClient.implantViewReady(player) || MantisHandPose.holdFolded) || player.isScoping()) {
            // Around the implant view, a held item eases down out of sight and back up, whoever
            // draws the hands; the rest of this frame's hand rendering inherits the offset.
            float drop = MantisClient.handsDrop(player, partialTick);
            if (drop > 0.0F) {
                poseStack.translate(0.0F, -MantisClient.HANDS_DROP * drop, 0.0F);
            }
            return;
        }
        ci.cancel();
        if (MantisClient.variant(player) == MantisVariant.NANOWIRE) {
            NanowireHands.render(poseStack, buffers, player, light, partialTick);
            buffers.endBatch();
            return;
        }
        poseStack.pushPose();
        MantisHandPose.undoViewBob(poseStack, player, partialTick);
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
            float guard = MantisClient.deployProgress(player, partialTick);
            MantisHandPose.apply(poseStack, player, partialTick, right, guard);
            // After a put-away item the arms come up like a swapped-in one, and go down likewise.
            poseStack.translate(0.0F, -MantisClient.HANDS_DROP * (1.0F - MantisClient.implantLift(player, partialTick)), 0.0F);
            // Spread the forearms out towards the screen edges, level with where vanilla holds
            // an item in each hand; the implants' blades then rise clear of the view's centre.
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
}
