package dev.apogee.mixin.client;

import dev.apogee.client.ApogeeClientState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyVariable(method = "renderEntity", at = @At("HEAD"), argsOnly = true)
    private float apogee$slowedPartialTick(float partialTick, Entity entity) {
        return ApogeeClientState.partialTick(entity, partialTick);
    }
}
