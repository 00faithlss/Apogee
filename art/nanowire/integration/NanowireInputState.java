package dev.apogee.nanowire;

/** Pure Java input/clip state. Not registered with Fabric, Punchy or networking.
 * Invoke press only on a new physical press (never once per held-button tick).
 * Time is a monotonic clock in seconds. All methods run on the client thread.
 * An accepted command must still be authorised on the server before causing damage.
 */
public final class NanowireInputState {
    public enum Button { LEFT, RIGHT }
    public enum Clip {
        STOWED("stowed", 0), DEPLOY("deploy", 1.10), IDLE("idle", 0),
        ATTACK_RIGHT("attack_right", .72), ATTACK_LEFT("attack_left", .72),
        DOUBLE("heavy", 1.15), RETRACT("retract", 1.0);
        public final String animation;
        public final double seconds;
        Clip(String name, double seconds) {
            this.animation = "animation.nanowire." + name;
            this.seconds = seconds;
        }
    }
    private Clip clip = Clip.STOWED;
    private double started, lastNow = Double.NEGATIVE_INFINITY;
    private boolean active, nextRight = true;
    private long revision;

    private void time(double now) {
        if (!Double.isFinite(now) || now < lastNow)
            throw new IllegalArgumentException("Use finite monotonic seconds");
        lastNow = now;
    }
    private void enter(Clip next, double now) {
        clip = next; started = now; revision++;
    }
    public void tick(double now) {
        time(now);
        if (clip.seconds > 0 && now - started + 1e-9 >= clip.seconds)
            enter(active ? Clip.IDLE : Clip.STOWED, now);
    }
    /** Equipping/deactivating resets the next single attack to the right arm. */
    public void setActive(boolean value, double now) {
        tick(now);
        if (value == active) return;
        active = value; nextRight = true;
        enter(value ? Clip.DEPLOY : Clip.RETRACT, now);
    }
    /** Returns true only if a new attack started. Busy clicks never advance alternation. */
    public boolean press(Button button, double now) {
        if (button == null) throw new IllegalArgumentException("button");
        tick(now);
        if (!active || clip != Clip.IDLE) return false;
        if (button == Button.RIGHT) enter(Clip.DOUBLE, now);
        else {
            enter(nextRight ? Clip.ATTACK_RIGHT : Clip.ATTACK_LEFT, now);
            nextRight = !nextRight;
        }
        return true;
    }
    /** Call on disconnect/death/removed implant. Does not play an extra attack. */
    public void reset(double now) {
        time(now); active = false; nextRight = true; enter(Clip.STOWED, now);
    }
    public Clip clip() { return clip; }
    public boolean active() { return active; }
    public long revision() { return revision; }
    public double elapsed(double now) { return Math.max(0, now - started); }
}
