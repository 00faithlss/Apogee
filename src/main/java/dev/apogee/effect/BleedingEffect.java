package dev.apogee.effect;

import dev.apogee.Apogee;
import dev.apogee.mantis.NanowireRules;
import dev.apogee.mixin.LivingEntityDamageAccessor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/** Server-owned damage over time, with a sparse red particle trail at each pulse. */
public final class BleedingEffect extends MobEffect {
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Apogee.id("bleeding"));
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.65F, 0.015F, 0.025F), 0.65F);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xA61928);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity victim, int amplifier) {
        // Entity time keeps the pulse steady even when another lash refreshes the duration.
        // Retracting the implant does not remove a wound already inflicted on a target.
        if (victim.level() instanceof ServerLevel level && victim.isAlive()
                && victim.tickCount % NanowireRules.BLEED_INTERVAL_TICKS == 0) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DAMAGE), victim.getLastHurtByMob());
            LivingEntityDamageAccessor damage = (LivingEntityDamageAccessor) victim;
            int immunity = victim.invulnerableTime;
            float lastDamage = damage.apogee$getLastHurt();
            boolean hurt;
            try {
                hurt = victim.hurt(source, NanowireRules.BLEED_DAMAGE);
            } finally {
                // A dot must neither create a new immunity window nor reduce the previous
                // hit remembered by vanilla (which would change the next slash's damage).
                victim.invulnerableTime = immunity;
                damage.apogee$setLastHurt(lastDamage);
            }
            if (hurt) {
                level.sendParticles(BLOOD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.45,
                        victim.getZ(), 5, victim.getBbWidth() * 0.3, victim.getBbHeight() * 0.15,
                        victim.getBbWidth() * 0.3, 0.0);
            }
        }
        return true;
    }
}
