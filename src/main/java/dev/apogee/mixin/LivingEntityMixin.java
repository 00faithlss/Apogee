package dev.apogee.mixin;

import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.time.TimeScaledEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float apogee$sandevistanDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        return SandevistanManager.modifyDamage(self, source, amount);
    }

    /**
     * A slowed target keeps its velocity several times longer in real time, so a normal hit
     * would leave it floating. Its knockback is scaled down while it is inside a field.
     */
    @ModifyVariable(method = "knockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double apogee$slowedKnockback(double strength) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || !TimeScaledEntity.of(self).apogee$isSlowed()) {
            return strength;
        }
        return strength * SandevistanManager.rules().slowedKnockbackMultiplier();
    }
}
