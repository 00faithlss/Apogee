package dev.apogee.gametest.mixin;

import dev.apogee.gametest.MaxtacClientSmoke;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skins vanilla reproducibles para revisar ambos modelos; solo se incluye en el mod de pruebas. */
@Mixin(AbstractClientPlayer.class)
public abstract class MaxtacSkinFixtureMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void apogee$skinFixture(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Boolean.getBoolean("apogee.maxtacSmoke") || Boolean.getBoolean("apogee.traumaSmoke"))
                && MaxtacClientSmoke.fixtureSkin != null) {
            cir.setReturnValue(MaxtacClientSmoke.fixtureSkin);
        }
    }
}
