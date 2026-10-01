package dev.apogee.client.nanowire;

import dev.apogee.mantis.NanowireRules;
import org.joml.Vector3f;

/** Numerical regressions for the procedural wire, independent of a running world. */
public final class NanowireMotionChecks {
    public static void main(String[] args) {
        for (Vector3f[] grips : new Vector3f[][]{
                {new Vector3f(0.45F, -0.4F, 0.7F), new Vector3f(-0.45F, -0.4F, 0.7F)},
                {new Vector3f(0.6F, -0.3F, 0.9F), new Vector3f(-0.3F, -0.7F, 0.6F)},
                {new Vector3f(-0.3F, -0.6F, 0.8F), new Vector3f(0.45F, -0.4F, 0.7F)},
                {new Vector3f(0.0F, -0.4F, 0.7F), new Vector3f(0.0F, -0.4F, 0.7F)}}) {
            checkHeld(grips[0], grips[1]);
        }
        // Include crossed fists and coincident grips: the X attack passes through both poses.
        for (Vector3f grip : new Vector3f[]{new Vector3f(0.45F, -0.4F, 0.7F),
                new Vector3f(-0.3F, -0.6F, 0.8F), new Vector3f(0.0F, -0.4F, 0.7F)}) {
            Vector3f held = new Vector3f(0.0F, -0.4F, 0.7F);
            checkPath(NanowireMoves.lash(1.0F, grip, held), grip, held);
            for (int style = 0; style < NanowireMoves.HEAVY_STYLES; style++) {
                checkWhip(1.0F, style, grip, held);
                checkWhip(-1.0F, style, grip, held);
            }
        }
        Vector3f right = new Vector3f(0.45F, -0.4F, 0.7F);
        Vector3f left = new Vector3f(-0.45F, -0.4F, 0.7F);
        Vector3f middle = new Vector3f(0.0F, -0.4F, 0.7F);
        NanowireMoves.Path r = NanowireMoves.lash(1.0F, right, middle);
        NanowireMoves.Path l = NanowireMoves.lash(-1.0F, left, middle);
        for (int i = 0; i <= 1000; i++) {
            float u = i / 1000.0F;
            Vector3f a = r.at(r.end(u));
            Vector3f b = l.at(l.end(u));
            close(new Vector3f(-a.x, a.y, a.z), b, "opposite single attacks must mirror");
        }
        for (boolean heavy : new boolean[]{false, true}) {
            for (float u : new float[]{0.0F, 1.0F}) {
                NanowireMoves.Motion m = NanowireMoves.motion(u, heavy);
                require(m.wind() == 0.0F && m.strike() == 0.0F && m.follow() == 0.0F
                        && m.catchWire() == 0.0F, "arm pose must meet idle at both ends");
                require(NanowireMoves.release(u, heavy) == 0.0F, "wire must meet idle at both ends");
                require(NanowireMoves.trail(u, heavy) == 0.0F, "idle cannot have attack trails");
            }
        }
        for (int style = 0; style < NanowireMoves.HEAVY_STYLES; style++) {
            require(NanowireMoves.heavyTime(-1.0F, style, 0.0F) == 0.0F, "left wire starts at rest");
            require(NanowireMoves.heavyTime(-1.0F, style, 1.0F) == 1.0F, "left wire finishes at rest");
        }
        float reach = (float) (3.0 + NanowireRules.REACH_BONUS);
        float furthest = 0.0F;
        for (int i = 0; i <= 1000; i++) {
            float u = i / 1000.0F;
            furthest = Math.max(furthest, r.at(r.end(u)).z);
        }
        require(furthest >= reach, "single lash must visibly reach the nominal hit range");
        float hit = NanowireRules.HEAVY_HIT_DELAY_TICKS / (float) NanowireRules.HEAVY_TICKS;
        for (int style = 0; style < NanowireMoves.HEAVY_STYLES; style++) {
            for (float side : new float[]{1.0F, -1.0F}) {
                Vector3f grip = side > 0 ? right : left;
                var wire = NanowireMoves.heavyWire(side, style, NanowireMoves.heavyTime(side, style, hit), grip, middle, 72);
                require(wire.getLast().z >= reach, "both heavy wires must reach the hit range at impact");
            }
        }
        System.out.println("Nanowire motion checks passed: idle joins, mirrored sweeps, finite paths, continuous tips, bounded feed and downward whip propagation.");
    }

    private static void checkHeld(Vector3f right, Vector3f left) {
        float tension = Math.min(1.0F, right.distance(left) / 0.5F);
        Vector3f held = new Vector3f(right).add(left).mul(0.5F).add(0.0F, -0.010F * tension, 0.0F);
        var r = new java.util.ArrayList<Vector3f>();
        var l = new java.util.ArrayList<Vector3f>();
        for (int i = 0; i <= 72; i++) {
            float s = i / 72.0F;
            r.add(NanowireMoves.heldCurve(right, left, held, s));
            l.add(NanowireMoves.heldCurve(left, right, held, s));
            require(r.getLast().isFinite() && l.getLast().isFinite(), "finite held cable");
        }
        close(r.getFirst(), right, "right held cable starts in its fist");
        close(l.getFirst(), left, "left held cable starts in its fist");
        close(r.getLast(), l.getLast(), "held cable meets exactly in the middle");
        Vector3f incoming = new Vector3f(held).sub(NanowireMoves.heldCurve(right, left, held, 0.999F));
        Vector3f outgoing = NanowireMoves.heldCurve(left, right, held, 0.999F).sub(held);
        close(incoming, outgoing, "both halves have the same tangent at the join");
        var joined = NanowireMoves.joinHeld(r, l);
        close(joined.getFirst(), right, "joined cable starts in the right fist");
        close(joined.getLast(), left, "joined cable ends in the left fist");
        long middlePoints = joined.stream().filter(point -> point.distanceSquared(held) < 1.0E-12F).count();
        require(middlePoints == 1L, "middle must be drawn once without overlapping glow");
        for (int i = 1; i < joined.size(); i++) {
            require(joined.get(i).distanceSquared(joined.get(i - 1)) > 1.0E-12F, "joined cable has no zero-length segments");
        }
    }

    private static void checkWhip(float side, int style, Vector3f grip, Vector3f held) {
        for (float u : new float[]{0.0F, 1.0F}) {
            var wire = NanowireMoves.heavyWire(side, style, u, grip, held, 72);
            close(wire.getFirst(), grip, "heavy root must stay in the hand");
            close(wire.getLast(), held, "heavy starts and finishes held");
        }
        var previous = NanowireMoves.heavyWire(side, style, 0.0F, grip, held, 72);
        for (int i = 1; i <= 1000; i++) {
            float u = i / 1000.0F;
            var wire = NanowireMoves.heavyWire(side, style, u, grip, held, 72);
            close(wire.getFirst(), grip, "heavy root remains anchored");
            float length = 0.0F;
            for (int k = 1; k < wire.size(); k++) {
                require(wire.get(k).isFinite(), "finite heavy cable");
                require(wire.get(k).distance(previous.get(k)) < 0.15F, "heavy cable moves continuously");
                length += wire.get(k).distance(wire.get(k - 1));
            }
            float maximumLength = 1.3F * (float) (3.0 + NanowireRules.REACH_BONUS);
            require(length <= Math.max(maximumLength, grip.distance(held)) + 1.0E-4F, "heavy cable cannot stretch beyond its length");
            previous = wire;
        }
        var raised = NanowireMoves.heavyWire(side, style, 0.16F, grip, held, 72);
        var falling = NanowireMoves.heavyWire(side, style, 0.40F, grip, held, 72);
        require(raised.getLast().y > grip.y + 4.0F, "heavy prepares overhead, not in front");
        require(falling.getLast().y < grip.y - 0.80F, "heavy must strike from above downwards");
        var travelling = NanowireMoves.heavyWire(side, style, 0.27F, grip, held, 72);
        require(travelling.get(2).y < travelling.get(1).y, "root follows the downward pull first");
        require(travelling.get(72).y > travelling.get(71).y, "tip lags above the root during the snap");
    }

    private static void checkPath(NanowireMoves.Path path, Vector3f grip, Vector3f held) {
        close(path.at(0.0F), grip, "path starts at grip");
        close(path.at(path.end(0.0F)), held, "release must start at the actual held point");
        close(path.at(path.end(1.0F)), held, "reel-in must finish at the actual held point");
        float previous = path.end(0.0F);
        Vector3f last = path.at(previous);
        for (int i = 0; i <= 1000; i++) {
            float u = i / 1000.0F;
            float end = path.end(u);
            float length = path.length(u);
            Vector3f tip = path.at(end);
            require(tip.isFinite(), "no NaN in sampled tip");
            require(end >= previous, "tip must not reverse along its path");
            require(length >= 0.0F && length <= end + 1.0E-5F, "feed cannot stack at the grip");
            require(tip.distance(last) < 0.15F, "tip must not jump between subframes");
            float s = 0.0F;
            while (s <= 1.0F) {
                require(path.at(end - (1.0F - s) * length).isFinite(), "no NaN in wire samples");
                s += 0.1F;
            }
            previous = end;
            last = tip;
        }
        close(path.at(path.end(NanowireMoves.arrival(false))), held, "tip arrives before release ends");
    }

    private static void close(Vector3f a, Vector3f b, String message) {
        require(a.distance(b) < 1.0E-4F, message + ": " + a + " vs " + b);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
