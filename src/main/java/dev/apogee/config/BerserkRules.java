package dev.apogee.config;

import dev.apogee.berserk.BerserkVariant;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Berserk values owned by the host, one set per variant plus Militech's kill healing and
 * Zetatech's landing values. The server
 * uses them directly and sends them to every client for the tooltip and the HUD.
 */
public record BerserkRules(
        Stats zetatech,
        Stats biodyne,
        Stats militech,
        int healPerKillPercent,
        double slamRadius,
        float slamDamage,
        double slamMinHeight
) {
    /**
     * What one variant does while active. Percentages are whole numbers; a variant leaves at 0
     * whatever it does not have.
     */
    public record Stats(
            int durationTicks,
            int cooldownTicks,
            int damageReductionPercent,
            int healthFloorPercent,
            boolean invulnerable,
            int attackSpeedPercent,
            int fallDamageReductionPercent,
            int critChancePercent,
            int critDamagePercent,
            int movementPercent,
            int lowHealthDamagePercent
    ) {
        public static final StreamCodec<ByteBuf, Stats> STREAM_CODEC = StreamCodec.of(
                (buf, stats) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, stats.durationTicks);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.cooldownTicks);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.damageReductionPercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.healthFloorPercent);
                    ByteBufCodecs.BOOL.encode(buf, stats.invulnerable);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.attackSpeedPercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.fallDamageReductionPercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.critChancePercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.critDamagePercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.movementPercent);
                    ByteBufCodecs.VAR_INT.encode(buf, stats.lowHealthDamagePercent);
                },
                buf -> new Stats(
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf)
                )
        );

        public Stats sanitized() {
            return new Stats(
                    clamp(durationTicks, 20, 20 * 600),
                    clamp(cooldownTicks, 0, 20 * 3600),
                    clamp(damageReductionPercent, 0, 95),
                    clamp(healthFloorPercent, 0, 100),
                    invulnerable,
                    clamp(attackSpeedPercent, 0, 500),
                    clamp(fallDamageReductionPercent, 0, 100),
                    clamp(critChancePercent, 0, 100),
                    clamp(critDamagePercent, 0, 500),
                    clamp(movementPercent, 0, 500),
                    clamp(lowHealthDamagePercent, 0, 500)
            );
        }
    }

    public static final Stats ZETATECH_DEFAULTS = new Stats(200, 800, 40, 25, false, 15, 50, 0, 0, 0, 0);
    public static final Stats BIODYNE_DEFAULTS = new Stats(180, 900, 35, 25, false, 25, 0, 20, 50, 0, 0);
    public static final Stats MILITECH_DEFAULTS = new Stats(240, 1200, 0, 0, true, 25, 0, 0, 0, 20, 50);
    public static final BerserkRules DEFAULTS = new BerserkRules(ZETATECH_DEFAULTS, BIODYNE_DEFAULTS, MILITECH_DEFAULTS, 25, 4.0, 8.0F, 3.0);

    /** Health fraction at which the Militech's low-health bonus reaches its maximum. */
    public static final float LOW_HEALTH_FULL_BONUS = 0.2F;
    public static final int COMBO_HITS = 5;
    public static final int COMBO_WINDOW_TICKS = 40;
    public static final int COMBO_HEAL_PERCENT = 10;
    public static final int LANDING_HEAL_PERCENT = 5;
    public static final int LANDING_HEAL_CAP_PERCENT = 25;
    public static final int KILL_EXTENSION_TICKS = 40;

    /** Height adds 25% per block above the dive threshold, capped at triple impact. */
    public static float landingPower(double dropped, double minimum) {
        return (float) Math.max(1.0, Math.min(3.0, 1.0 + (dropped - minimum) * 0.25));
    }

    public static final StreamCodec<ByteBuf, BerserkRules> STREAM_CODEC = StreamCodec.of(
            (buf, rules) -> {
                Stats.STREAM_CODEC.encode(buf, rules.zetatech);
                Stats.STREAM_CODEC.encode(buf, rules.biodyne);
                Stats.STREAM_CODEC.encode(buf, rules.militech);
                ByteBufCodecs.VAR_INT.encode(buf, rules.healPerKillPercent);
                ByteBufCodecs.DOUBLE.encode(buf, rules.slamRadius);
                ByteBufCodecs.FLOAT.encode(buf, rules.slamDamage);
                ByteBufCodecs.DOUBLE.encode(buf, rules.slamMinHeight);
            },
            buf -> new BerserkRules(
                    Stats.STREAM_CODEC.decode(buf),
                    Stats.STREAM_CODEC.decode(buf),
                    Stats.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
            ).sanitized()
    );

    public Stats stats(BerserkVariant variant) {
        return switch (variant) {
            case ZETATECH -> zetatech;
            case BIODYNE -> biodyne;
            case MILITECH -> militech;
        };
    }

    public BerserkRules sanitized() {
        return new BerserkRules(
                zetatech.sanitized(),
                biodyne.sanitized(),
                militech.sanitized(),
                clamp(healPerKillPercent, 0, 100),
                Math.max(0.0, Math.min(16.0, slamRadius)),
                Math.max(0.0F, Math.min(100.0F, slamDamage)),
                Math.max(1.0, Math.min(32.0, slamMinHeight))
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
