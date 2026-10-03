package dev.apogee.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.apogee.berserk.BerserkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class BerserkPlayerMixin {
    /**
     * The Berserk's health floor applies to the health a hit leaves, after armor, enchantments and
     * absorption, so it holds whatever the damage was. Damage that ignores invulnerability
     * (/kill, the void) is not stopped.
     */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void apogee$berserkHealthFloor(Player self, float health, Operation<Void> original, DamageSource source, float amount) {
        if (self instanceof ServerPlayer player) {
            health = BerserkManager.floorHealth(player, source, health);
        }
        original.call(self, health);
    }

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void apogee$berserkNoExhaustion(float exhaustion, CallbackInfo ci) {
        if (BerserkManager.blocksExhaustion((Player) (Object) this)) {
            ci.cancel();
        }
    }
}
