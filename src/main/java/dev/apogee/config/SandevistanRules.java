package dev.apogee.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Gameplay values owned by the host. The server uses them directly and sends them to every
 * client so tooltips and HUD show exactly what the mechanic applies.
 */
public record SandevistanRules(
        int slowdownPercent,
        double fieldRadius,
        int durationTicks,
        int movementBonusPercent,
        int damageBonusPercent,
        int killExtensionTicks,
        int maxDurationTicks,
        int cooldownTicks,
        int combatMemoryTicks
) {
    public static final SandevistanRules DEFAULTS = new SandevistanRules(85, 10.0, 200, 50, 25, 40, 400, 600, 200);

    public static final StreamCodec<ByteBuf, SandevistanRules> STREAM_CODEC = StreamCodec.of(
            (buf, rules) -> {
                ByteBufCodecs.VAR_INT.encode(buf, rules.slowdownPercent);
                ByteBufCodecs.DOUBLE.encode(buf, rules.fieldRadius);
                ByteBufCodecs.VAR_INT.encode(buf, rules.durationTicks);
                ByteBufCodecs.VAR_INT.encode(buf, rules.movementBonusPercent);
                ByteBufCodecs.VAR_INT.encode(buf, rules.damageBonusPercent);
                ByteBufCodecs.VAR_INT.encode(buf, rules.killExtensionTicks);
                ByteBufCodecs.VAR_INT.encode(buf, rules.maxDurationTicks);
                ByteBufCodecs.VAR_INT.encode(buf, rules.cooldownTicks);
                ByteBufCodecs.VAR_INT.encode(buf, rules.combatMemoryTicks);
            },
            buf -> new SandevistanRules(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            ).sanitized()
    );

    /** Fraction of normal speed that affected entities keep, e.g. 0.15 for an 85% slowdown. */
    public float timeScale() {
        return 1.0F - slowdownPercent / 100.0F;
    }

    public double movementMultiplier() {
        return movementBonusPercent / 100.0;
    }

    public float damageMultiplier() {
        return 1.0F + damageBonusPercent / 100.0F;
    }

    public SandevistanRules sanitized() {
        int slowdown = clamp(slowdownPercent, 0, 95);
        double radius = Math.max(1.0, Math.min(64.0, fieldRadius));
        int duration = clamp(durationTicks, 20, 20 * 600);
        int maxDuration = clamp(maxDurationTicks, duration, 20 * 1200);
        return new SandevistanRules(
                slowdown,
                radius,
                duration,
                clamp(movementBonusPercent, 0, 500),
                clamp(damageBonusPercent, 0, 500),
                clamp(killExtensionTicks, 0, 20 * 60),
                maxDuration,
                clamp(cooldownTicks, 0, 20 * 3600),
                clamp(combatMemoryTicks, 0, 20 * 120)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
