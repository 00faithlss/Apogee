package dev.apogee.mixin.client;

import dev.apogee.client.mantis.MantisClient;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel cannot move the hotbar selection while the blades are out. */
@Mixin(Inventory.class)
public abstract class MantisInventoryMixin {
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void apogee$lockScroll(double direction, CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;
        if (self.player.level().isClientSide && MantisClient.isDeployed(self.player)) {
            ci.cancel();
        }
    }
}
