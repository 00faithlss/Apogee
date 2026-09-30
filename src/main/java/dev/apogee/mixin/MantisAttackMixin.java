package dev.apogee.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.apogee.mantis.MantisManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With the Mantis Blades out the blades are the weapon: the item left in the locked hotbar slot
 * (hidden on screen) lends no enchantments, sweep, special bonus or durability loss to the cuts.
 * Its attribute modifiers are cancelled in {@link MantisManager}.
 */
@Mixin(Player.class)
public abstract class MantisAttackMixin {
    @Inject(method = "getWeaponItem", at = @At("HEAD"), cancellable = true)
    private void apogee$bladesAreTheWeapon(CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ServerPlayer player && MantisManager.isDeployed(player)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    /** The sweep only happens with a sword in the main hand. */
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack apogee$noSweepWithBlades(ItemStack held) {
        return (Object) this instanceof ServerPlayer player && MantisManager.isDeployed(player) ? ItemStack.EMPTY : held;
    }

    /** Variant bonus on top of the cut, added like an enchantment's (scaled by the attack charge, not by crits). */
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float apogee$variantBonus(float damage, Entity target) {
        return (Object) this instanceof ServerPlayer player ? damage + MantisManager.variantBonus(player, target) : damage;
    }
}
