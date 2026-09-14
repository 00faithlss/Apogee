package dev.apogee.time;

import net.minecraft.world.entity.Entity;

/**
 * Per-entity time accumulator, mixed into {@link Entity} on both sides. A slowed entity only
 * runs its real tick when the accumulator crosses 1, so everything the tick drives (movement,
 * AI, attack cooldowns, item use, animations) runs at the field's time scale while the rest of
 * the world keeps the normal rhythm.
 */
public interface TimeScaledEntity {
    boolean apogee$isSlowed();

    void apogee$setSlowed(boolean slowed);

    /** Progress towards the next real tick, in [0, 1). */
    float apogee$timeAccumulator();

    /** Adds {@code scale} and returns whether a real tick should run now. */
    boolean apogee$advanceTime(float scale);

    static TimeScaledEntity of(Entity entity) {
        return (TimeScaledEntity) entity;
    }
}
