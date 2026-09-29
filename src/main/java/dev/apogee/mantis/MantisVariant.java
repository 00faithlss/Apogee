package dev.apogee.mantis;

/** The three Mantis Blades models: plain steel, heated (thermal) and charged (electric). */
public enum MantisVariant {
    STANDARD,
    THERMAL,
    ELECTRIC;

    public static MantisVariant byId(int id) {
        MantisVariant[] all = values();
        return id >= 0 && id < all.length ? all[id] : STANDARD;
    }
}
