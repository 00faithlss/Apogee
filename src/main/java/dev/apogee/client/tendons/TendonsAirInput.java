package dev.apogee.client.tendons;

/**
 * When the Reinforced Tendons may fire, from the key states each tick. A press only counts once the
 * player is in the air and it is a new press: the press that left the ground, or one held from
 * before, must never double as the second jump. Kept free of Minecraft types so it can be tested.
 */
public final class TendonsAirInput {
    private boolean jumpCharged = true;
    private boolean dashCharged = true;
    private boolean jumpHeld;
    private boolean dashHeld;
    private boolean wasGrounded = true;

    /** Which of the two the tendons fire this tick. */
    public record Fire(boolean jump, boolean dash) {
        public static final Fire NONE = new Fire(false, false);
    }

    /** Test hook: counts as already well into the flight, with nothing held. */
    public void settle() {
        wasGrounded = false;
        jumpHeld = false;
        dashHeld = false;
    }

    /**
     * @param grounded    on the ground, in a fluid or on a climbable: both are recharged
     * @param pressedJump jump went down this tick
     * @param pressedDash the dash key went down this tick
     * @param usable      the implant is worn and nothing (flight, a mount, spectating) forbids it
     */
    public Fire step(boolean grounded, boolean jumpDown, boolean pressedJump, boolean dashDown, boolean pressedDash, boolean usable) {
        if (grounded) {
            jumpCharged = true;
            dashCharged = true;
            jumpHeld = jumpDown;
            dashHeld = dashDown;
            wasGrounded = true;
            return Fire.NONE;
        }
        if (wasGrounded) {
            // The tick the feet left the ground: the press that did it is still down and is not a second jump.
            wasGrounded = false;
            jumpHeld = jumpDown;
            dashHeld = dashDown;
            return Fire.NONE;
        }
        if (!jumpDown) {
            jumpHeld = false;
        }
        if (!dashDown) {
            dashHeld = false;
        }
        boolean jump = usable && pressedJump && jumpCharged && !jumpHeld;
        boolean dash = usable && pressedDash && dashCharged && !dashHeld;
        if (jump) {
            jumpCharged = false;
        }
        if (dash) {
            dashCharged = false;
        }
        return jump || dash ? new Fire(jump, dash) : Fire.NONE;
    }
}
