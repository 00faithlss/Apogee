package dev.apogee.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisArmorSleeves;
import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
public abstract class MantisArmorLayerMixin {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"))
    private void apogee$mantisSleeves(HumanoidArmorLayer<?, ?, ?> layer, PoseStack poses, MultiBufferSource buffers,
                                    LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> armor,
                                    Operation<Void> original, PoseStack renderPoses, MultiBufferSource renderBuffers,
                                    int renderLight, LivingEntity renderEntity, float limbSwing, float limbAmount,
                                    float partialTick, float age, float yaw, float pitch) {
        if (slot != EquipmentSlot.CHEST || !(entity instanceof AbstractClientPlayer player)
                || !MantisArmorSleeves.applies(player)) {
            original.call(layer, poses, buffers, entity, slot, light, armor);
            return;
        }
        float progress = MantisClient.deployProgress(player, partialTick);
        if (progress <= 0.0F) {
            original.call(layer, poses, buffers, entity, slot, light, armor);
            return;
        }
        MantisArmorSleeves.Context previous = MantisArmorSleeves.begin(armor, progress);
        try {
            original.call(layer, poses, buffers, entity, slot, light, armor);
        } finally {
            MantisArmorSleeves.restore(previous);
        }
    }
}
