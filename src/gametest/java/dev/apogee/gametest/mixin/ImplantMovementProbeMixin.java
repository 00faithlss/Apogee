package dev.apogee.gametest.mixin;

import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.gametest.ImplantMovementSmoke;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PunchyCompat.class, remap = false)
public abstract class ImplantMovementProbeMixin {
    @Inject(method = "capture", at = @At("TAIL"))
    private static void apogee$sample(LocalPlayer player, HumanoidArm arm, Matrix4f matrix, boolean visible, CallbackInfo ci) {
        ImplantMovementSmoke.sample(matrix);
    }
}
