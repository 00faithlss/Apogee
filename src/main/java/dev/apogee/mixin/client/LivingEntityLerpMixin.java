package dev.apogee.mixin.client;

import dev.apogee.client.ApogeeClientState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLerpMixin {
    /**
     * Server position updates for a slowed entity arrive once per slowed tick. Reaching the
     * target in a single slowed tick lets the scaled partial tick do the smoothing instead of
     * spreading one update over several already-slow ticks.
     */
    @ModifyVariable(method = "lerpTo", at = @At("HEAD"), argsOnly = true)
    private int apogee$slowedLerpSteps(int steps) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.level().isClientSide() && ApogeeClientState.isSlowed(self) ? 1 : steps;
    }
}
