package dev.apogee.mixin.client;

import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MantisLocalPlayerMixin {
    @Inject(method = "swing", at = @At("HEAD"))
    private void apogee$predictMantisArm(InteractionHand hand, CallbackInfo ci) {
        MantisClient.onLocalSwing();
    }
}
