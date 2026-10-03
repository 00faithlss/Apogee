package dev.apogee.mixin;

import dev.apogee.Apogee;
import dev.apogee.berserk.BerserkManager;
import dev.apogee.sandevistan.SandevistanManager;
import dev.apogee.time.TimeScaledEntity;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float apogee$sandevistanDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        return BerserkManager.modifyDamage(self, source, SandevistanManager.modifyDamage(self, source, amount));
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

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void apogee$removeUnserializableMobEffects(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Map<Holder<MobEffect>, MobEffectInstance> activeEffects = self.getActiveEffectsMap();
        if (activeEffects.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Holder<MobEffect>, MobEffectInstance>> iterator = activeEffects.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Holder<MobEffect>, MobEffectInstance> entry = iterator.next();
            MobEffectInstance instance = entry.getValue();
            if (apogee$canSerialize(instance)) {
                continue;
            }

            iterator.remove();
            Apogee.LOGGER.warn("Removed unserializable mob effect {} from {} to prevent an NBT save crash",
                    apogee$effectName(instance.getEffect()), self.getName().getString());
        }
    }

    private static boolean apogee$canSerialize(MobEffectInstance instance) {
        if (!instance.getEffect().isBound()) {
            return false;
        }

        try {
            instance.save();
            return true;
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("Unregistered holder")) {
                return false;
            }
            throw exception;
        }
    }

    private static String apogee$effectName(Holder<MobEffect> effect) {
        return effect.unwrapKey()
                .map(key -> key.location().toString())
                .orElse(effect.getRegisteredName());
    }
}
