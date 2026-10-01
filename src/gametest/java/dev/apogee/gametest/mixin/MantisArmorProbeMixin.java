package dev.apogee.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.client.mantis.MantisArmorSleeves;
import dev.apogee.gametest.MantisArmorProbe;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MantisArmorSleeves.class, remap = false)
public abstract class MantisArmorProbeMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private static void apogee$countClipped(ModelPart part, ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer buffer,
                                           int light, int overlay, int color, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.getBoolean("apogee.mantisArmorSmoke") && cir.getReturnValueZ()) {
            MantisArmorProbe.clippedCubes++;
        }
    }
}
