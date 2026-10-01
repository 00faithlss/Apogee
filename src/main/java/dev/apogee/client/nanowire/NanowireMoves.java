package dev.apogee.client.nanowire;

import dev.apogee.mantis.NanowireRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Procedural cord motion in the wearer's frame: +X right, +Y up, +Z forward.
 * Single lashes follow the tip's sampled path; heavy lashes propagate the hand's downward
 * pull along the cord with a delay towards its tip. Both meet the held point at rest.
 */
public final class NanowireMoves {
    static Vector3f heldCurve(Vector3f grip, Vector3f opposite, Vector3f held, float s) {
        Vector3f sag = new Vector3f(held).sub(new Vector3f(grip).add(opposite).mul(0.5F));
        return new Vector3f(grip).lerp(held, s).fma(s * (1.0F - s), sag);
    }

    static List<Vector3f> joinHeld(List<Vector3f> right, List<Vector3f> left) {
        List<Vector3f> joined = new ArrayList<>(right.size() + left.size() - 1);
        for (Vector3f point : right) {
            appendDistinct(joined, point);
        }
        for (int i = left.size() - 2; i >= 0; i--) {
            appendDistinct(joined, left.get(i));
        }
        return joined;
    }

    private static void appendDistinct(List<Vector3f> points, Vector3f point) {
        if (points.isEmpty() || points.getLast().distanceSquared(point) > 1.0E-12F) {
            points.add(point);
        }
    }

    /** Keys of an end path, in frame coordinates for the right arm; the left arm mirrors X. */
    private static final float[][] LASH = {
            {0.85F, -0.12F, 0.75F},
            {2.05F, 0.02F, 1.80F},
            {1.25F, 0.15F, 3.25F},
            {-0.65F, 0.02F, 3.55F},
            {-2.00F, -0.20F, 2.50F},
            {-1.65F, -0.35F, 1.25F},
            {0.10F, -0.30F, 1.35F},
            {0.65F, -0.22F, 2.20F},
            {0.10F, -0.20F, 1.05F},
    };

    /** The two-wire lashes, played in turn: both wires slash down across each other in an X... */
    public static final int HEAVY_X = 0;
    /** ...both arms chop straight down together, the wires falling side by side... */
    public static final int HEAVY_VERTICAL = 1;
    /** ...or a rolling downward lash, with the left wire following the right one. */
    public static final int HEAVY_S = 2;
    public static final int HEAVY_STYLES = 3;

    /** Except in the parallel chop, the left arm starts just after the right one. */
    private static final float HEAVY_LEFT_LAG = 0.05F;
    /** Nominal survival reach. A curved cord needs more length than this straight-line distance. */
    private static final float REACH = (float) (3.0 + NanowireRules.REACH_BONUS);
    private static final float SPREAD = REACH / 5.0F;
    /** The furthest authored forward key is 3.55: scale that point to the actual hit range. */
    private static final float FORWARD = REACH / 3.55F;
    private static final float LIFT = 1.0F + (SPREAD - 1.0F) * 0.5F;
    /** Longest the free wire gets, in blocks. */
    private static final float LASH_LENGTH = 1.2F * REACH;
    private static final float HEAVY_LENGTH = 1.3F * REACH;
    /** Samples per stretch between two keys when a path is built. */
    private static final int SAMPLES_PER_KEY = 24;
    /** Progress along the path (0..1) against move time, from each move's speed curve. */
    private static final float[] LASH_PROGRESS = progress();

    /** Shared accents for the skin poses in both views: pull, snap, follow-through, catch. */
    record Motion(float wind, float strike, float follow, float catchWire) {
    }

    static Motion motion(float u, boolean heavy) {
        float windEnd = heavy ? 0.16F : 0.14F;
        float strikeEnd = heavy ? 0.30F : 0.34F;
        float recovery = heavy ? 0.62F : 0.48F;
        float wind = ease(u / windEnd) * (1.0F - ease((u - windEnd) / (strikeEnd - windEnd)));
        float strike = ease((u - windEnd) / (strikeEnd - windEnd))
                * (1.0F - ease((u - recovery) / (arrival(heavy) - recovery)));
        float follow = ease((u - strikeEnd) / 0.14F)
                * (1.0F - ease((u - recovery) / (arrival(heavy) - recovery)));
        float catchWire = ease((u - 0.68F) / 0.10F) * (1.0F - ease((u - 0.80F) / 0.20F));
        return new Motion(wind, strike, follow, catchWire);
    }

    static float release(float u, boolean heavy) {
        return ease(u / 0.12F) * (1.0F - ease((u - arrival(heavy)) / (1.0F - arrival(heavy))));
    }

    static float reel(float u, boolean heavy) {
        return ease((u - (heavy ? 0.64F : 0.58F)) / 0.28F);
    }

    static float trail(float u, boolean heavy) {
        return ease((u - (heavy ? 0.18F : 0.10F)) / 0.12F)
                * (1.0F - ease((u - (heavy ? 0.56F : 0.48F)) / 0.22F));
    }

    private NanowireMoves() {
    }

    /**
     * One wire's path for a move, sampled densely: from the grip out through the move's keys and
     * back to the point between the fists.
     */
    static final class Path {
        private final float[] xs;
        private final float[] ys;
        private final float[] zs;
        private final float[] along;
        private final int count;
        private final float heldLength;

        private Path(int capacity) {
            xs = new float[capacity];
            ys = new float[capacity];
            zs = new float[capacity];
            along = new float[capacity];
            count = 0;
            heldLength = 0.0F;
        }

        private Path(Path sampled, int count, float heldLength) {
            xs = sampled.xs;
            ys = sampled.ys;
            zs = sampled.zs;
            along = sampled.along;
            this.count = count;
            this.heldLength = heldLength;
        }

        float total() {
            return along[count - 1];
        }

        /** Distance along the path of the free end at move time {@code u}. */
        float end(float u) {
            float progress = sample(LASH_PROGRESS, u);
            return heldLength + (total() - heldLength) * progress;
        }

        /** Length of free wire out at move time {@code u}: the held stretch at first, growing as it is fed out, then reeled in. */
        float length(float u) {
            float out = ease((u - 0.04F) / 0.20F);
            float in = reel(u, false);
            // Before the tip has travelled far enough, do not stack samples at the nozzle.
            return Math.min(end(u), heldLength + (LASH_LENGTH - heldLength) * out * (1.0F - in));
        }

        /** The point at distance {@code d} along the path, in frame coordinates. */
        Vector3f at(float d) {
            if (d <= 0.0F) {
                return new Vector3f(xs[0], ys[0], zs[0]);
            }
            int last = count - 1;
            if (d >= along[last]) {
                return new Vector3f(xs[last], ys[last], zs[last]);
            }
            int lo = 0;
            int hi = last;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (along[mid] <= d) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            float k = (d - along[lo]) / Math.max(1.0E-6F, along[hi] - along[lo]);
            return new Vector3f(Mth.lerp(k, xs[lo], xs[hi]), Mth.lerp(k, ys[lo], ys[hi]), Mth.lerp(k, zs[lo], zs[hi]));
        }
    }

    /** Move time (0..1) at which the free end is back between the fists. */
    static float arrival(boolean heavy) {
        return heavy ? 0.92F : 0.86F;
    }

    /** The lash path of the arm on {@code side} (+1 right, -1 left). */
    static Path lash(float side, Vector3f grip, Vector3f held) {
        return build(side, grip, held);
    }

    /**
     * A flexible heavy lash. The hand turns first, then the bend travels towards the tip.
     * Integrating unit tangents keeps a cord of bounded length attached to its grip, instead
     * of sliding a cord along a tip trajectory that reads as throwing it forwards.
     */
    static List<Vector3f> heavyWire(float side, int style, float u, Vector3f grip, Vector3f held, int segments) {
        List<Vector3f> points = new ArrayList<>(segments + 1);
        points.add(new Vector3f(grip));
        float release = release(u, true);
        float length = Mth.lerp(ease(u / 0.16F) * (1.0F - reel(u, true)), grip.distance(held), HEAVY_LENGTH);
        Vector3f current = new Vector3f(grip);
        for (int i = 1; i <= segments; i++) {
            float s = i / (float) segments;
            float phase = u - 0.10F * s;
            float chop = ease((phase - 0.16F) / 0.14F);
            float roll = ease((phase - 0.42F) / 0.24F);
            // Up at preparation, down through impact, then curl back during follow-through.
            float angle = 0.65F + 1.13F * chop + 0.18F * roll;
            float across = switch (style) {
                case HEAVY_X -> side * (0.38F - 0.76F * chop);
                case HEAVY_VERTICAL -> 0.0F;
                default -> side * 0.28F * Mth.cos(angle + s * 0.65F);
            };
            float plane = (float) Math.sqrt(1.0F - across * across);
            current.add(across * length / segments, plane * Mth.cos(angle) * length / segments,
                    plane * Mth.sin(angle) * length / segments);
            Vector3f rest = new Vector3f(grip).lerp(held, s);
            points.add(rest.lerp(current, release));
        }
        return points;
    }

    /** Move time of one wire in the two-wire lash: the left one may start a moment after the right. */
    static float heavyTime(float side, int style, float u) {
        return side > 0.0F || style == HEAVY_VERTICAL ? u : Mth.clamp((u - HEAVY_LEFT_LAG) / (1.0F - HEAVY_LEFT_LAG), 0.0F, 1.0F);
    }

    /**
     * The end's path: out of the fist through the keys and back to the point between the fists.
     * Each wire whips from its own hand, so the two only meet again in the middle at the end.
     */
    private static Path build(float side, Vector3f grip, Vector3f held) {
        Vector3f[] points = new Vector3f[LASH.length + 2];
        points[0] = new Vector3f(held);
        for (int i = 0; i < LASH.length; i++) {
            points[i + 1] = new Vector3f(side * LASH[i][0] * SPREAD, LASH[i][1] * LIFT, LASH[i][2] * FORWARD);
        }
        points[points.length - 1] = new Vector3f(held);
        int stretches = points.length - 1;
        Path path = new Path(2 + stretches * SAMPLES_PER_KEY);
        int n = add(path, 0, grip);
        // The held segment is exactly straight. end(0) must be the actual point between the
        // fists, not heldLength metres into a spline that starts off towards the first key.
        n = add(path, n, held);
        for (int i = 0; i < stretches; i++) {
            Vector3f p0 = points[Math.max(0, i - 1)];
            Vector3f p1 = points[i];
            Vector3f p2 = points[i + 1];
            Vector3f p3 = points[Math.min(stretches, i + 2)];
            for (int j = 1; j <= SAMPLES_PER_KEY; j++) {
                n = add(path, n, catmullRom(p0, p1, p2, p3, j / (float) SAMPLES_PER_KEY));
            }
        }
        return new Path(path, n, grip.distance(held));
    }

    /** Appends a sample to a path being built, keeping its running length. */
    private static int add(Path path, int n, Vector3f p) {
        path.xs[n] = p.x;
        path.ys[n] = p.y;
        path.zs[n] = p.z;
        path.along[n] = n == 0 ? 0.0F : path.along[n - 1] + (float) Math.sqrt(Mth.square(p.x - path.xs[n - 1])
                + Mth.square(p.y - path.ys[n - 1]) + Mth.square(p.z - path.zs[n - 1]));
        return n + 1;
    }

    /**
     * Cumulative progress for a move's speed curve, as a table over move time: quick to get going,
     * fastest through the sweep, then slowing through the loop and the reel-in, until the end is
     * back between the fists at {@link #arrival}.
     */
    private static float[] progress() {
        int steps = 256;
        float[] table = new float[steps + 1];
        float end = arrival(false);
        float sum = 0.0F;
        for (int i = 1; i <= steps; i++) {
            float u = (i - 0.5F) / steps;
            float wind = 0.14F;
            float speed = u >= end ? 0.0F : ease(u / wind)
                    * (float) Math.pow(1.0F - ease((u - 0.30F) / (end - 0.30F)), 1.1);
            sum += speed;
            table[i] = sum;
        }
        for (int i = 0; i <= steps; i++) {
            table[i] /= sum;
        }
        return table;
    }

    private static float sample(float[] table, float u) {
        float x = Mth.clamp(u, 0.0F, 1.0F) * (table.length - 1);
        int i = Math.min(table.length - 2, (int) x);
        return Mth.lerp(x - i, table[i], table[i + 1]);
    }

    /** Centripetal Catmull-Rom: passes through p1 and p2 without overshooting sharp turns. */
    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t0 = 0.0F;
        float t1 = t0 + knot(p0, p1);
        float t2 = t1 + knot(p1, p2);
        float t3 = t2 + knot(p2, p3);
        float s = Mth.lerp(t, t1, t2);
        Vector3f a1 = mix(p0, p1, t0, t1, s);
        Vector3f a2 = mix(p1, p2, t1, t2, s);
        Vector3f a3 = mix(p2, p3, t2, t3, s);
        Vector3f b1 = mix(a1, a2, t0, t2, s);
        Vector3f b2 = mix(a2, a3, t1, t3, s);
        return mix(b1, b2, t1, t2, s);
    }

    private static float knot(Vector3f a, Vector3f b) {
        return Math.max(1.0E-4F, (float) Math.sqrt(a.distance(b)));
    }

    private static Vector3f mix(Vector3f a, Vector3f b, float ta, float tb, float t) {
        float k = (t - ta) / (tb - ta);
        return new Vector3f(a).lerp(b, k);
    }

    static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
