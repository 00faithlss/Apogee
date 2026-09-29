package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.render.MantisArmRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person hands go through renderRightHand/renderLeftHand; draw the blades after the arm. */
@Mixin(PlayerRenderer.class)
public abstract class MantisPlayerRendererMixin {
    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void apogee$beginRight(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        MantisClient.firstPersonRendering = true;
    }

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void apogee$mantisRight(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        drawBlades(poseStack, buffers, light, player, true);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void apogee$beginLeft(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        MantisClient.firstPersonRendering = true;
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void apogee$mantisLeft(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        drawBlades(poseStack, buffers, light, player, false);
    }

    private void drawBlades(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, boolean right) {
        MantisClient.firstPersonRendering = false;
        float progress = MantisClient.deployProgress(player, 1.0F);
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        poseStack.pushPose();
        (right ? model.rightArm : model.leftArm).translateAndRotate(poseStack);
        MantisArmRenderer.render(poseStack, buffers, light, player, right, progress);
        poseStack.popPose();
    }
}
