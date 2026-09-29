package dev.apogee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides held items in third person while the blades are out. */
@Mixin(ItemInHandLayer.class)
public abstract class MantisItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void apogee$hideHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                     PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (MantisClient.isDeployed(entity)) {
            ci.cancel();
        }
    }
}
