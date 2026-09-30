package dev.apogee.nanowire;
public final class NanowireInputStateTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        var s = new NanowireInputState();
        check(!s.press(NanowireInputState.Button.LEFT, 0), "inactive blocks clicks");
        s.setActive(true, 0);
        check(!s.press(NanowireInputState.Button.LEFT, .5), "deploy blocks attack");
        check(s.press(NanowireInputState.Button.LEFT, 1.1), "first click accepted");
        check(s.clip() == NanowireInputState.Clip.ATTACK_RIGHT, "first right arm");
        check(!s.press(NanowireInputState.Button.LEFT, 1.2), "busy ignored");
        check(s.press(NanowireInputState.Button.LEFT, 1.82), "second accepted");
        check(s.clip() == NanowireInputState.Clip.ATTACK_LEFT, "second left arm");
        check(s.press(NanowireInputState.Button.RIGHT, 2.54), "secondary accepted");
        check(s.clip() == NanowireInputState.Clip.DOUBLE, "secondary double");
        check(s.press(NanowireInputState.Button.LEFT, 3.69), "third accepted");
        check(s.clip() == NanowireInputState.Clip.ATTACK_RIGHT, "double preserves alternation");
        s.setActive(false, 3.8);
        check(!s.press(NanowireInputState.Button.RIGHT, 4.0), "retraction blocks damage");
        s.tick(4.8);
        check(s.clip() == NanowireInputState.Clip.STOWED, "fully retracted");
        s.setActive(true, 5);
        s.press(NanowireInputState.Button.LEFT, 6.1);
        check(s.clip() == NanowireInputState.Clip.ATTACK_RIGHT, "reactivation resets right");
        s.reset(6.2);
        check(!s.active() && s.clip() == NanowireInputState.Clip.STOWED, "death/reset");
        System.out.println("PASS: inactive, deploy, right-left alternation, busy clicks, double, retract, reset");
    }
}
