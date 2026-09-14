package dev.apogee.mixin;

import dev.apogee.time.TimeScaledEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityTimeMixin implements TimeScaledEntity {
    @Unique
    private boolean apogee$slowed;
    @Unique
    private float apogee$accumulator;

    @Override
    public boolean apogee$isSlowed() {
        return apogee$slowed;
    }

    @Override
    public void apogee$setSlowed(boolean slowed) {
        if (apogee$slowed != slowed) {
            apogee$slowed = slowed;
            apogee$accumulator = 0.0F;
        }
    }

    @Override
    public float apogee$timeAccumulator() {
        return apogee$accumulator;
    }

    @Override
    public boolean apogee$advanceTime(float scale) {
        apogee$accumulator += scale;
        if (apogee$accumulator >= 1.0F) {
            apogee$accumulator -= 1.0F;
            return true;
        }
        return false;
    }
}
