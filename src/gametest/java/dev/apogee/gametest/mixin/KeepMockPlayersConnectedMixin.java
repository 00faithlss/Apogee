package dev.apogee.gametest.mixin;

import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameTest mock players have no client, so Cardinal Components kicks them when its sync
 * channel is missing. Keep them connected so tests exercise the real session lifecycle.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class KeepMockPlayersConnectedMixin {
    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void apogeeTest$keepMockPlayer(CallbackInfo ci) {
        if ((Object) this instanceof ServerGamePacketListenerImpl game
                && game.player != null
                && game.player.getClass().getName().startsWith("net.minecraft.gametest.")) {
            ci.cancel();
        }
    }
}
