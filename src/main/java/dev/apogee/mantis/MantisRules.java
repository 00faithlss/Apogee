package dev.apogee.mantis;

/** Fixed balance values for the Mantis Blades. */
public final class MantisRules {
    /**
     * Attack damage of a fully charged cut (a critical hit deals 1.5 times as much, 15). The
     * bonus added to the player's base of 1 makes up for whatever the item left in hand adds.
     */
    public static final double ATTACK_DAMAGE = 10.0;
    /** Attack speed while the blades are out: two full-strength cuts per second (base 4.0). */
    public static final double ATTACK_SPEED = 2.0;
    /** Seconds a target hit by the thermal blades burns. */
    public static final float THERMAL_BURN_SECONDS = 4.0F;
    /** Slowness the electric blades leave on a target: length in ticks and amplifier (2 is Slowness III). */
    public static final int ELECTRIC_STUN_TICKS = 30;
    public static final int ELECTRIC_STUN_AMPLIFIER = 2;
    /** Extra damage of the electric blades against a target standing or swimming in water. */
    public static final float ELECTRIC_WATER_BONUS = 4.0F;
    /** Damage of the two-blade lunge to every entity along its path. */
    public static final float THRUST_DAMAGE = 9.0F;
    /** Reach of the lunge, in blocks. */
    public static final double THRUST_REACH = 4.0;
    /** Horizontal impulse of the lunge. */
    public static final double THRUST_IMPULSE = 1.1;
    public static final int THRUST_COOLDOWN_TICKS = 40;
    /** Client-side length of the lunge animation, in ticks. */
    public static final int THRUST_ANIMATION_TICKS = 8;
    /** Client-side length of a first-person slash, in ticks. */
    public static final int SLASH_ANIMATION_TICKS = 7;
    /**
     * Fewest ticks between two cuts. Vanilla swings every tick while a block is being mined; each
     * cut plays its whole animation before the next one starts.
     */
    public static final int SLASH_INTERVAL_TICKS = SLASH_ANIMATION_TICKS;
    /** Ticks the unfold/fold animation takes (the kit's deploy and retract clips last 1 s). */
    public static final int DEPLOY_TICKS = 20;

    public static final int ARM_RIGHT = 0;
    public static final int ARM_LEFT = 1;
    /** Swing kind sent for the lunge, which uses both arms. */
    public static final int ARM_BOTH = 2;

    private MantisRules() {
    }

    /** Fewest ticks between two attacks of one arm for the given arm cyberware. */
    public static int slashIntervalTicks(MantisVariant variant) {
        return (variant != null && variant.isNanowire()) ? NanowireRules.LASH_TICKS : SLASH_INTERVAL_TICKS;
    }

    /** Client-side length of one arm's attack animation, in ticks. */
    public static int slashAnimationTicks(MantisVariant variant) {
        return (variant != null && variant.isNanowire()) ? NanowireRules.LASH_ANIMATION_TICKS : SLASH_ANIMATION_TICKS;
    }

    /** Cooldown of the two-arm attack (lunge or two-wire lash), in ticks. */
    public static int thrustCooldownTicks(MantisVariant variant) {
        return (variant != null && variant.isNanowire()) ? NanowireRules.HEAVY_COOLDOWN_TICKS : THRUST_COOLDOWN_TICKS;
    }

    /** Client-side length of the two-arm attack animation, in ticks. */
    public static int thrustAnimationTicks(MantisVariant variant) {
        return (variant != null && variant.isNanowire()) ? NanowireRules.HEAVY_TICKS : THRUST_ANIMATION_TICKS;
    }
}
