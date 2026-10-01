package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A patient on the ripperdoc couch is in the sleeping pose (see
 * {@link dev.apogee.mixin.RipperdocPatientPoseMixin}), but lies along the couch rather than across
 * a bed: on their back, the hips on the hinge of the cushion, the body raised along the head end
 * and the legs flat along the foot end (see {@link RipperdocPatientModelMixin}). The model in the
 * ripperdoc screen's view stands upright.
 */
@Mixin(PlayerRenderer.class)
public abstract class RipperdocPatientMixin {
    /** Height of the hips over the feet once the player model is scaled (12 px at 0.9375). */
    private static final float HIPS = 0.75F * 0.9375F;

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void apogee$lieOnCouch(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale,
                                   CallbackInfo ci) {
        boolean onCouch = player.getVehicle() instanceof RipperdocSeat;
        if (!onCouch && !(RipperdocScreen.renderingPatient && player.hasPose(Pose.SLEEPING))) {
            return;
        }
        if (onCouch && !RipperdocScreen.renderingPatient) {
            RipperdocSeat seat = (RipperdocSeat) player.getVehicle();
            // Turned with the couch, not with the body, which follows where the player looks:
            // facing its foot end, so lying back puts the head on the raised end, face up.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - seat.getYRot()));
            // Here +Y is up and -Z is in front of the patient, towards the foot end of the couch.
            float hipsY = (float) (seat.cushionY() + RipperdocSeat.HIPS_OVER_CUSHION - player.getY());
            poseStack.translate(0.0F, hipsY, RipperdocSeat.HINGE_BEHIND);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - RipperdocSeat.BACKREST));
            poseStack.translate(0.0F, -HIPS, 0.0F);
        } else {
            // Upright and turned like any standing player, instead of across a bed.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
        }
        ci.cancel();
    }
}
