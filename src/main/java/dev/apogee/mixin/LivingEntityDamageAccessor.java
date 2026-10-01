package dev.apogee.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Allows a bleeding pulse to leave the normal melee immunity window intact. */
@Mixin(LivingEntity.class)
public interface LivingEntityDamageAccessor {
    @Accessor("lastHurt")
    float apogee$getLastHurt();

    @Accessor("lastHurt")
    void apogee$setLastHurt(float damage);
}
