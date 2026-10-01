package dev.apogee.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.client.mantis.MantisArmorSleeves;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPart.class)
public abstract class MantisArmorModelPartMixin {
    @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart$Cube;compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void apogee$openArmorSleeve(ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer buffer,
                                       int light, int overlay, int color, Operation<Void> original) {
        if (!MantisArmorSleeves.render((ModelPart) (Object) this, cube, pose, buffer, light, overlay, color)) {
            original.call(cube, pose, buffer, light, overlay, color);
        }
    }
}
