package dev.apogee.mantis;

/** Fixed balance values for the Mantis Blades. */
public final class MantisRules {
    /** Added to the wearer's attack damage while the blades are out. */
    public static final double DAMAGE_BONUS = 6.0;
    /** Added to the wearer's attack speed while the blades are out (base 4.0). */
    public static final double ATTACK_SPEED_BONUS = 1.5;
    /** Damage of the two-blade lunge to every entity along its path. */
    public static final float THRUST_DAMAGE = 9.0F;
    /** Reach of the lunge, in blocks. */
    public static final double THRUST_REACH = 4.0;
    /** Horizontal impulse of the lunge. */
    public static final double THRUST_IMPULSE = 1.1;
    public static final int THRUST_COOLDOWN_TICKS = 40;
    /** Client-side length of the lunge animation, in ticks. */
    public static final int THRUST_ANIMATION_TICKS = 8;
    /** Ticks the unfold/fold animation takes. */
    public static final int DEPLOY_TICKS = 8;

    public static final int ARM_RIGHT = 0;
    public static final int ARM_LEFT = 1;
    /** Swing kind sent for the lunge, which uses both arms. */
    public static final int ARM_BOTH = 2;

    private MantisRules() {
    }
}
