package dev.apogee.ripperdoc;

import dev.apogee.Apogee;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

/** Fitting an implant hurts: three hearts, through armour, but never enough to kill. */
public final class RipperdocSurgery {
    public static final ResourceKey<DamageType> SURGERY = ResourceKey.create(Registries.DAMAGE_TYPE, Apogee.id("surgery"));
    /** Health a fitted implant costs. */
    public static final float DAMAGE = 6.0F;

    private RipperdocSurgery() {
    }

    static void operate(Player patient) {
        float damage = Math.min(DAMAGE, patient.getHealth() - 1.0F);
        if (damage <= 0.0F) {
            return;
        }
        // Each implant is its own operation: the hurt cooldown from the last one must not swallow it.
        patient.invulnerableTime = 0;
        DamageSource source = new DamageSource(patient.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SURGERY));
        patient.hurt(source, damage);
    }
}
