package dev.apogee.berserk;

import java.util.Locale;

/** The three Berserk operating systems, after the ones in Cyberpunk 2077 2.0. */
public enum BerserkVariant {
    /** The base model: damage reduction, a health floor, faster attacks and softer falls. */
    ZETATECH,
    /** Trades some reduction and duration for faster attacks and critical hits. */
    BIODYNE,
    /** The iconic one: invulnerable, faster, and hits harder the less health is left. */
    MILITECH;

    private static final BerserkVariant[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BerserkVariant byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ZETATECH;
    }
}
