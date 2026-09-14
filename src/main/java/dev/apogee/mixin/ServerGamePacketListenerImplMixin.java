package dev.apogee.mixin;

import dev.apogee.time.TimeScaledEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;
    @Shadow
    private boolean clientIsFloating;
    @Shadow
    private int aboveGroundTickCount;
    @Shadow
    private boolean clientVehicleIsFloating;
    @Shadow
    private int aboveGroundVehicleTickCount;

    /**
     * A slowed player's jump lasts several times longer in real ticks; freeze the anti-fly
     * counters while slowed so they are not kicked for "flying".
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void apogee$holdFloatingCounters(CallbackInfo ci) {
        if (player != null && TimeScaledEntity.of(player).apogee$isSlowed()) {
            if (clientIsFloating) {
                aboveGroundTickCount--;
            }
            if (clientVehicleIsFloating) {
                aboveGroundVehicleTickCount--;
            }
        }
    }
}
