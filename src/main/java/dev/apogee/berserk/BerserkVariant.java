package dev.apogee.berserk;

import java.util.Locale;

/** The three Berserk operating systems, after the ones in Cyberpunk 2077 2.0. */
public enum BerserkVariant {
    /** Aerial impact: height-scaled superhero landing and healing on impact. */
    ZETATECH,
    /** Sustained combos build attack speed, critical chance and periodic healing. */
    BIODYNE,
    /** Invulnerable rage: starting health fixes damage, kills extend it and bank healing. */
    MILITECH;

    private static final BerserkVariant[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BerserkVariant byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ZETATECH;
    }
}
