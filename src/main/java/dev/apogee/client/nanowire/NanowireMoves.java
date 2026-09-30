package dev.apogee.client.nanowire;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Paths of the wire's free end for each move, in a frame around the wearer: +X to their right,
 * +Y up, +Z forward, in blocks from the frame's origin (the eye in first person, the neck in third
 * person). The wire is a cord of a set length sliding along the path its end has travelled, the
 * way a lashed cord lies, so loops in the path become loops in the wire.
 *
 * <p>Shaped after a Cyberpunk 2077 monowire clip, frame by frame at 30 fps: the lash's arm crosses
 * the view in about 0.13 s and the wire closes a wide loop around the target right after; the
 * two-wire lash reaches out in about 0.2 s, spins in wide stacked loops for another 0.2 s and fades
 * over about 0.4 s.
 *
 * <p>The end travels its path by distance, following one smooth speed curve per move, so it never
 * speeds up and slows down again between the path's keys. As the wire is reeled in it is pulled
 * taut, so it is already straight when its end is caught between the fists.
 */
final class NanowireMoves {
    /** Keys of an end path, in frame coordinates for the right arm; the left arm mirrors X. */
    private static final float[][] LASH = {
            {0.55F, -0.08F, 0.40F},
            {1.45F, -0.05F, 1.20F},
            {1.10F, -0.05F, 2.60F},
            {-0.50F, -0.08F, 2.90F},
            {-1.50F, -0.12F, 1.60F},
            {-0.90F, -0.16F, 0.80F},
            {0.60F, -0.16F, 1.00F},
            {0.50F, -0.20F, 2.10F},
            {0.00F, -0.20F, 1.00F},
    };

    /** The right wire of the two-wire lash: a rising spiral of two wide loops in front. */
    private static final float[][] HEAVY = {
            {0.45F, -0.05F, 0.60F},
            {1.50F, 0.05F, 1.80F},
            {0.30F, 0.15F, 3.40F},
            {-1.45F, 0.25F, 2.50F},
            {-0.50F, 0.30F, 1.20F},
            {1.35F, 0.40F, 1.90F},
            {0.50F, 0.45F, 3.30F},
            {-1.30F, 0.40F, 2.60F},
            {-0.40F, 0.30F, 1.40F},
            {0.00F, 0.10F, 0.90F},
    };

    /** The left wire of the two-wire lash turns the other way and lower: stacked loops. */
    private static final float HEAVY_LEFT_DROP = 0.45F;
    /** Longest the free wire gets, in blocks. */
    private static final float LASH_LENGTH = 4.2F;
    private static final float HEAVY_LENGTH = 5.0F;
    /** Samples per stretch between two keys when a path is built. */
    private static final int SAMPLES_PER_KEY = 24;
    /** Radius of the loop the end turns in when it is let go and heads back out, and its samples. */
    private static final float TURN_RADIUS = 0.07F;
    private static final int TURN_SAMPLES = 12;
    /** Progress along the path (0..1) against move time, from each move's speed curve. */
    private static final float[] LASH_PROGRESS = progress(false);
    private static final float[] HEAVY_PROGRESS = progress(true);

    private NanowireMoves() {
    }

    /**
     * One wire's path for a move, sampled densely: from the grip along the held line to the point
     * between the fists, out through the move's keys and back to that point.
     */
    static final class Path {
        private final float[] xs;
        private final float[] ys;
        private final float[] zs;
        private final float[] along;
        private final int count;
        private final float heldLength;
        private final boolean heavy;

        private Path(int capacity, boolean heavy) {
            xs = new float[capacity];
            ys = new float[capacity];
            zs = new float[capacity];
            along = new float[capacity];
            this.heavy = heavy;
            count = 0;
            heldLength = 0.0F;
        }

        private Path(Path sampled, int count, float heldLength) {
            xs = sampled.xs;
            ys = sampled.ys;
            zs = sampled.zs;
            along = sampled.along;
            heavy = sampled.heavy;
            this.count = count;
            this.heldLength = heldLength;
        }

        float total() {
            return along[count - 1];
        }

        /** Distance along the path of the free end at move time {@code u}. */
        float end(float u) {
            float progress = sample(heavy ? HEAVY_PROGRESS : LASH_PROGRESS, u);
            return heldLength + (total() - heldLength) * progress;
        }

        /** Length of free wire out at move time {@code u}: the held stretch at first, growing as it is fed out, then reeled in. */
        float length(float u) {
            float out = ease((u - 0.03F) / 0.15F);
            float in = ease((u - (heavy ? 0.62F : 0.55F)) / 0.30F);
            float full = heavy ? HEAVY_LENGTH : LASH_LENGTH;
            return heldLength + (full - heldLength) * out * (1.0F - in);
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
        return build(LASH, side, 0.0F, grip, held, false);
    }

    /** One wire's path of the two-wire lash. */
    static Path heavy(float side, Vector3f grip, Vector3f held) {
        return build(HEAVY, side, side < 0.0F ? -HEAVY_LEFT_DROP : 0.0F, grip, held, true);
    }

    private static Path build(float[][] keys, float side, float drop, Vector3f grip, Vector3f held, boolean heavy) {
        // Held point, the keys, and the held point again, after a straight lead-in from the grip.
        Vector3f[] points = new Vector3f[keys.length + 2];
        points[0] = new Vector3f(held);
        for (int i = 0; i < keys.length; i++) {
            points[i + 1] = new Vector3f(side * keys[i][0], keys[i][1] + drop, keys[i][2]);
        }
        points[points.length - 1] = new Vector3f(held);
        int stretches = points.length - 1;
        Path path = new Path(2 + TURN_SAMPLES + stretches * SAMPLES_PER_KEY, heavy);
        int n = add(path, 0, grip);
        n = add(path, n, held);
        int first = 0;
        Vector3f inward = new Vector3f(held).sub(grip);
        if (inward.lengthSquared() > 1.0E-8F) {
            // Let go, the end heads back out: it turns in a small round loop instead of a corner,
            // then runs into the first key along the path that follows it.
            inward.normalize();
            Vector3f out = new Vector3f(points[1]).sub(held);
            Vector3f normal = new Vector3f(out).fma(-out.dot(inward), inward);
            if (normal.lengthSquared() < 1.0E-8F) {
                normal.set(0.0F, 1.0F, 0.0F).fma(-inward.y, inward);
            }
            normal.normalize();
            Vector3f centre = new Vector3f(held).fma(TURN_RADIUS, normal);
            for (int j = 1; j <= TURN_SAMPLES; j++) {
                float angle = Mth.PI * j / TURN_SAMPLES;
                n = add(path, n, new Vector3f(centre).fma(-TURN_RADIUS * Mth.cos(angle), normal).fma(TURN_RADIUS * Mth.sin(angle), inward));
            }
            Vector3f from = centre.fma(TURN_RADIUS, normal);
            Vector3f p3 = points[Math.min(stretches, 3)];
            Vector3f next = catmullRom(points[0], points[1], points[2], p3, 1.0E-3F).sub(points[1]).normalize();
            float reach = from.distance(points[1]) / 3.0F;
            Vector3f leave = new Vector3f(from).fma(-reach, inward);
            Vector3f arrive = new Vector3f(points[1]).fma(-reach, next);
            for (int j = 1; j <= SAMPLES_PER_KEY; j++) {
                n = add(path, n, bezier(from, leave, arrive, points[1], j / (float) SAMPLES_PER_KEY));
            }
            first = 1;
        }
        for (int i = first; i < stretches; i++) {
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

    private static Vector3f bezier(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float t) {
        float s = 1.0F - t;
        return new Vector3f(a).mul(s * s * s).fma(3.0F * s * s * t, b).fma(3.0F * s * t * t, c).fma(t * t * t, d);
    }

    /**
     * Cumulative progress for a move's speed curve, as a table over move time: quick to get going,
     * fastest through the sweep, then slowing through the loop and the reel-in, until the end is
     * back between the fists at {@link #arrival}.
     */
    private static float[] progress(boolean heavy) {
        int steps = 256;
        float[] table = new float[steps + 1];
        float end = arrival(heavy);
        float sum = 0.0F;
        for (int i = 1; i <= steps; i++) {
            float u = (i - 0.5F) / steps;
            float speed = u >= end ? 0.0F : ease(u / 0.14F) * (float) Math.pow(1.0F - ease((u - 0.24F) / (end - 0.24F)), 1.4);
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
