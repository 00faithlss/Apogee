package dev.apogee.mixin.client;

import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Right click is the lunge while the blades are out; it must not also use items or place blocks. */
@Mixin(Minecraft.class)
public abstract class MantisMinecraftMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void apogee$noUseWhileDeployed(CallbackInfo ci) {
        if (MantisClient.hotbarLocked()) {
            ci.cancel();
        }
    }
}
