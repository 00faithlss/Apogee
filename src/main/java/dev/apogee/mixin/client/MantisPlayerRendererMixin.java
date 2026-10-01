package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireRig;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person hands: while Mantis Blades are worn the implant model is the arm, so it is drawn
 * in place of the vanilla arm and sleeve, with the same pose vanilla would give them.
 */
@Mixin(PlayerRenderer.class)
public abstract class MantisPlayerRendererMixin {
    @Shadow
    protected abstract void setModelProperties(AbstractClientPlayer player);

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void apogee$mantisHand(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                   ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        MantisVariant variant = MantisClient.variant(player);
        if (variant == null || !variant.isBlades()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        setModelProperties(player);
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        MantisClient.firstPersonRendering = true;
        try {
            model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        } finally {
            MantisClient.firstPersonRendering = false;
        }
        arm.xRot = 0.0F;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        MantisRig.firstPersonPass = true;
        try {
            MantisRig.drawOnArm(poseStack, buffers, light, player, arm, arm == model.rightArm, partialTick);
        } finally {
            MantisRig.firstPersonPass = false;
        }
        ci.cancel();
    }

    /**
     * The nanowire housing rides on the arm vanilla (or the mod posing the hands) has just drawn,
     * with the pose it was drawn with.
     */
    @Inject(method = "renderHand", at = @At("TAIL"))
    private void apogee$nanowireHand(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (MantisClient.variant(player) == null || !MantisClient.variant(player).isNanowire()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        NanowireRig.drawOnArm(poseStack, buffers, light, player, arm, arm == model.rightArm, partialTick);
    }
}
