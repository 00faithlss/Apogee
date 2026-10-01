package dev.apogee.mantis;

/**
 * Fixed balance values for the nanowire. The attack rate follows the pack's clips (a one-arm lash
 * lasts 0.72 s, see art/nanowire/animation-events.json); the drawn moves are timed after a
 * Cyberpunk 2077 monowire clip, measured frame by frame, so they are quicker than that.
 */
public final class NanowireRules {
    /** Attack damage of a fully charged lash (a critical hit deals 1.5 times as much). */
    public static final double ATTACK_DAMAGE = 8.0;
    /** Ticks a one-arm lash takes; the next one starts once it is over. */
    public static final int LASH_TICKS = 15;
    /** Ticks the one-arm lash is drawn over: the arm crosses the view by 0.2 s, the loop closes by 0.35 s. */
    public static final int LASH_ANIMATION_TICKS = 11;
    /** Attack speed matching one full-strength lash per clip. */
    public static final double ATTACK_SPEED = 20.0 / LASH_TICKS;
    /** Extra reach of every attack while the wire is out: 3 blocks become 7. */
    public static final double REACH_BONUS = 4.0;
    /** Damage of the two-wire lash to every target in front within reach. */
    public static final float HEAVY_DAMAGE = 12.0F;
    /** Half-angle, in degrees, of the arc the two-wire lash sweeps in front of the wearer. */
    public static final double HEAVY_HALF_ANGLE = 70.0;
    /** Ticks from the two-wire lash starting to the wires crossing the targets (0.3 s). */
    public static final int HEAVY_HIT_DELAY_TICKS = 6;
    /** Ticks the two-wire lash animation takes: out and spinning by 0.5 s, back in the hands by 0.9 s. */
    public static final int HEAVY_TICKS = 18;
    public static final int HEAVY_COOLDOWN_TICKS = 60;

    private NanowireRules() {
    }
}
