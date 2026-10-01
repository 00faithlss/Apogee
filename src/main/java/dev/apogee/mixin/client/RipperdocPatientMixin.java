package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A patient on the ripperdoc couch lies on their back: the hips on the hinge of the cushion, the
 * body tipped back along the raised head end and the legs flat along the foot end (see
 * {@link RipperdocPatientModelMixin}). The model in the ripperdoc screen's view stands upright.
 */
@Mixin(PlayerRenderer.class)
public abstract class RipperdocPatientMixin {
    /** Height of the hips over the feet once the player model is scaled (12 px at 0.9375). */
    private static final float HIPS = 0.75F * 0.9375F;

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void apogee$lieOnCouch(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale,
                                   CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient || !(player.getVehicle() instanceof RipperdocSeat seat)) {
            return;
        }
        // Here +Y is up and +Z is in front of the patient, towards the foot end of the couch.
        float hipsY = (float) (seat.cushionY() + RipperdocSeat.HIPS_OVER_CUSHION - player.getY());
        poseStack.translate(0.0F, hipsY, -RipperdocSeat.HINGE_BEHIND);
        poseStack.mulPose(Axis.XP.rotationDegrees(RipperdocSeat.BACKREST - 90.0F));
        poseStack.translate(0.0F, -HIPS, 0.0F);
    }
}
