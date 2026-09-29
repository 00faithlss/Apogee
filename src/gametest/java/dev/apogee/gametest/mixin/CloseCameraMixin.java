package dev.apogee.gametest.mixin;

import dev.apogee.gametest.MantisClientSmoke;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the scripted client runs pull the third-person camera in for close-up screenshots. */
@Mixin(Camera.class)
public abstract class CloseCameraMixin {
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void apogee$closeUp(float maxZoom, CallbackInfoReturnable<Float> cir) {
        if (MantisClientSmoke.cameraDistance > 0.0F) {
            cir.setReturnValue(MantisClientSmoke.cameraDistance);
        }
    }
}
