package dev.apogee.mantis;

/**
 * The arm cyberware worn in the {@code cyberware/arms} slot: the three Mantis Blades models
 * (plain steel, heated thermal and charged electric) and the nanowire. They share deployment,
 * the hotbar lock and the alternating attacks; only the blades replace the arm model.
 */
public enum MantisVariant {
    STANDARD,
    THERMAL,
    ELECTRIC,
    NANOWIRE,
    NANOWIRE_WHITE;

    public static MantisVariant byId(int id) {
        MantisVariant[] all = values();
        return id >= 0 && id < all.length ? all[id] : STANDARD;
    }

    public boolean isNanowire() {
        return this == NANOWIRE || this == NANOWIRE_WHITE;
    }

    /** Mantis Blades, as opposed to the nanowire. */
    public boolean isBlades() {
        return !isNanowire();
    }
}
