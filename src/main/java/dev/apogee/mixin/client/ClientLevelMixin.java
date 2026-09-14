package dev.apogee.mixin.client;

import dev.apogee.client.ApogeeClientState;
import dev.apogee.time.TimeScaledEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    /**
     * Mirrors the server's tick skipping for slowed entities, including the local player when
     * they are caught in someone else's field. Mouse look, menus and the connection run outside
     * entity ticks and stay fluid.
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void apogee$scaleEntityTick(Entity entity, CallbackInfo ci) {
        TimeScaledEntity scaled = TimeScaledEntity.of(entity);
        boolean slowed = ApogeeClientState.isSlowed(entity);
        scaled.apogee$setSlowed(slowed);
        if (slowed && !scaled.apogee$advanceTime(ApogeeClientState.rules().timeScale())) {
            ci.cancel();
        }
    }
}
