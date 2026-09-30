package dev.apogee.mantis;

/**
 * Fixed balance values for the nanowire. The timings follow its animation clips: a one-arm lash
 * lasts 0.72 s and the two-wire lash 1.15 s (see art/nanowire/animation-events.json).
 */
public final class NanowireRules {
    /** Attack damage of a fully charged lash (a critical hit deals 1.5 times as much). */
    public static final double ATTACK_DAMAGE = 8.0;
    /** Ticks a one-arm lash takes; the next one starts once it is over. */
    public static final int LASH_TICKS = 15;
    /** Attack speed matching one full-strength lash per clip. */
    public static final double ATTACK_SPEED = 20.0 / LASH_TICKS;
    /** Extra reach of every attack while the wire is out: 3 blocks become 5. */
    public static final double REACH_BONUS = 2.0;
    /** Damage of the two-wire lash to every target in front within reach. */
    public static final float HEAVY_DAMAGE = 12.0F;
    /** Half-angle, in degrees, of the arc the two-wire lash sweeps in front of the wearer. */
    public static final double HEAVY_HALF_ANGLE = 70.0;
    /** Ticks from the two-wire lash starting to the wires crossing the targets (0.3 s). */
    public static final int HEAVY_HIT_DELAY_TICKS = 6;
    /** Ticks the two-wire lash animation takes. */
    public static final int HEAVY_TICKS = 23;
    public static final int HEAVY_COOLDOWN_TICKS = 60;

    private NanowireRules() {
    }
}
