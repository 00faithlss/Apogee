package dev.apogee.mixin.client;

import dev.apogee.client.ApogeeClientState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    /** Keeps the camera gliding when the local player is slowed instead of stepping each real tick. */
    @ModifyVariable(method = "setup", at = @At("HEAD"), argsOnly = true)
    private float apogee$slowedCameraPartialTick(float partialTick, BlockGetter level, Entity entity) {
        return ApogeeClientState.partialTick(entity, partialTick);
    }
}
