package dev.apogee.bloodpump;

/** Fixed values of the Blood Pump: an instant heal, then a regeneration window, then a recharge. */
public final class BloodPumpRules {
    /** Health restored the moment the pump fires. */
    public static final float INSTANT_HEAL = 8.0F;
    /** Health restored over {@link #REGEN_TICKS} after the instant heal. */
    public static final float REGEN_HEAL = 8.0F;
    public static final int REGEN_TICKS = 6 * 20;
    /** Time from firing until the pump can fire again. */
    public static final int COOLDOWN_TICKS = 30 * 20;

    private BloodPumpRules() {
    }
}
