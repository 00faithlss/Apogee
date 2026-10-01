package dev.apogee.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.render.MantisArmsLayer;
import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.client.ripperdoc.XrayRenderTypes;
import dev.emi.trinkets.TrinketFeatureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The ripperdoc's x-ray view: the patient's skin turns translucent and only the cyberware layers
 * are drawn (no armour, cape or held items), so what is fitted shows through the body.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class RipperdocXrayMixin<T extends LivingEntity> {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void apogee$xraySkin(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (RipperdocScreen.xrayPatient()) {
            @SuppressWarnings("unchecked")
            ResourceLocation texture = ((LivingEntityRenderer<T, ?>) (Object) this).getTextureLocation(entity);
            cir.setReturnValue(XrayRenderTypes.skin(texture));
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int apogee$xrayTint(int color) {
        return RipperdocScreen.xrayPatient() ? XrayRenderTypes.SKIN_COLOR : color;
    }

    @WrapWithCondition(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    private boolean apogee$onlyCyberware(RenderLayer<?, ?> layer, PoseStack poseStack, MultiBufferSource buffers, int light, Entity entity,
                                         float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                                         float headPitch) {
        return !RipperdocScreen.xrayPatient() || layer instanceof TrinketFeatureRenderer || layer instanceof MantisArmsLayer;
    }
}
