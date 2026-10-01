package dev.apogee.ripperdoc;

/**
 * Whether the current thread is building the inventory menu's trinket slots, during which the
 * cyberware group is hidden so implants are only fitted at a ripperdoc table.
 */
public final class CyberwareSlots {
    public static final String GROUP = "cyberware";
    private static final ThreadLocal<Boolean> HIDING = new ThreadLocal<>();

    private CyberwareSlots() {
    }

    public static void startHiding() {
        HIDING.set(Boolean.TRUE);
    }

    public static void stopHiding() {
        HIDING.remove();
    }

    public static boolean hiding() {
        return HIDING.get() != null;
    }
}
