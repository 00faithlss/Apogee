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
 * A patient on the ripperdoc couch sits in the riding pose leaned back against the raised head
 * end, as if lying on it. The model in the ripperdoc screen's view stands upright instead.
 */
@Mixin(PlayerRenderer.class)
public abstract class RipperdocPatientMixin {
    /** How far the patient leans back, and the height (blocks) the lean turns about: the hips. */
    private static final float LEAN = 50.0F;
    private static final float HIPS = 0.75F;

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void apogee$leanOnCouch(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale,
                                   CallbackInfo ci) {
        if (RipperdocScreen.renderingPatient || !(player.getVehicle() instanceof RipperdocSeat)) {
            return;
        }
        poseStack.translate(0.0F, HIPS, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-LEAN));
        poseStack.translate(0.0F, -HIPS, 0.0F);
    }
}
