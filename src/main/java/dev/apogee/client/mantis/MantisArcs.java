package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Small electric arcs around an open electric blade: jagged bolts that crawl along it or jump off
 * it, redrawn in a new shape every tick. They are drawn in the blade's own pose, so they follow it
 * in first and third person alike. They use the additive render type of the dragon's death rays:
 * the lightning one draws to the weather target, which the hands cover with Fabulous graphics.
 */
public final class MantisArcs {
    /** Chance per tick of a first arc on a blade, and of a second one. */
    private static final float FIRST_ARC = 0.85F;
    private static final float SECOND_ARC = 0.5F;
    private static final int POINTS = 7;
    /** Sizes in blocks: the blade is about half a block long. */
    private static final float JITTER = 0.03F;
    private static final float CORE = 0.006F;
    private static final float HALO = 0.02F;

    private MantisArcs() {
    }

    /** One blade's cubes as a line each (x, y, z0 to z1) in the pose they were drawn with. */
    public static final class Blade {
        private final Matrix4f[] poses = new Matrix4f[MantisArm.MAX_BLADE_SEGMENTS];
        private final float[] lines = new float[MantisArm.MAX_BLADE_SEGMENTS * 4];
        private int count;

        void clear() {
            count = 0;
        }

        void add(Matrix4f pose, float x, float y, float z0, float z1) {
            if (count >= poses.length) {
                return;
            }
            if (poses[count] == null) {
                poses[count] = new Matrix4f();
            }
            poses[count].set(pose);
            lines[count * 4] = x;
            lines[count * 4 + 1] = y;
            lines[count * 4 + 2] = z0;
            lines[count * 4 + 3] = z1;
            count++;
        }
    }

    public static void draw(Blade blade, MultiBufferSource buffers, long tick, int seed) {
        if (blade.count == 0) {
            return;
        }
        RandomSource random = RandomSource.create(tick * 0x9E3779B97F4A7C15L + seed);
        int arcs = random.nextFloat() < FIRST_ARC ? (random.nextFloat() < SECOND_ARC ? 2 : 1) : 0;
        if (arcs == 0) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.dragonRays());
        for (int i = 0; i < arcs; i++) {
            arc(blade, buffer, random);
        }
    }

    private static void arc(Blade blade, VertexConsumer buffer, RandomSource random) {
        // Pick a cube weighted by its length, so most arcs start on the long blade faces.
        float total = 0.0F;
        for (int i = 0; i < blade.count; i++) {
            total += Math.abs(blade.lines[i * 4 + 3] - blade.lines[i * 4 + 2]);
        }
        float pick = random.nextFloat() * total;
        int cube = 0;
        for (; cube < blade.count - 1; cube++) {
            pick -= Math.abs(blade.lines[cube * 4 + 3] - blade.lines[cube * 4 + 2]);
            if (pick <= 0.0F) {
                break;
            }
        }
        Matrix4f pose = blade.poses[cube];
        float x = blade.lines[cube * 4];
        float y = blade.lines[cube * 4 + 1];
        float z0 = blade.lines[cube * 4 + 2];
        float z1 = blade.lines[cube * 4 + 3];
        float start = z0 + (z1 - z0) * random.nextFloat();
        // Either a bolt that crawls along the blade and lands on it again, or one that leaps off it.
        boolean crawl = random.nextBoolean();
        float length = 0.10F + random.nextFloat() * 0.18F;
        float end = crawl ? start + (random.nextBoolean() ? length : -length) : start + (random.nextFloat() - 0.5F) * 0.06F;
        float outX = (random.nextFloat() - 0.5F) * 2.0F;
        float outY = (random.nextFloat() - 0.5F) * 2.0F;
        float reach = crawl ? 0.04F : 0.09F + random.nextFloat() * 0.12F;
        Vector3f[] points = new Vector3f[POINTS];
        for (int p = 0; p < POINTS; p++) {
            float t = p / (float) (POINTS - 1);
            // Crawling arcs bow away from the blade in the middle; leaping ones end out in the air.
            float away = crawl ? (float) Math.sin(t * Math.PI) * reach : t * reach;
            float jitter = p == 0 || (crawl && p == POINTS - 1) ? 0.0F : JITTER;
            points[p] = new Vector3f(
                    x + outX * away + (random.nextFloat() - 0.5F) * jitter,
                    y + outY * away + (random.nextFloat() - 0.5F) * jitter,
                    start + (end - start) * t + (random.nextFloat() - 0.5F) * jitter);
        }
        for (int p = 0; p < POINTS - 1; p++) {
            segment(buffer, pose, points[p], points[p + 1], HALO, 40, 110, 255, 70);
            segment(buffer, pose, points[p], points[p + 1], CORE, 200, 240, 255, 230);
        }
    }

    /** A segment as two crossed quads, each with both windings, so it shows from any side. */
    private static void segment(VertexConsumer buffer, Matrix4f pose, Vector3f a, Vector3f b, float width,
                                int r, int g, int bl, int alpha) {
        quad(buffer, pose, a, b, width, 0.0F, r, g, bl, alpha);
        quad(buffer, pose, a, b, 0.0F, width, r, g, bl, alpha);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, Vector3f a, Vector3f b, float dx, float dy,
                             int r, int g, int bl, int alpha) {
        float[][] corners = {
                {a.x - dx, a.y - dy, a.z}, {a.x + dx, a.y + dy, a.z}, {b.x + dx, b.y + dy, b.z}, {b.x - dx, b.y - dy, b.z}};
        // Triangles: 0 1 2, 0 2 3, then the same pair wound the other way.
        int[] order = {0, 1, 2, 0, 2, 3, 2, 1, 0, 3, 2, 0};
        for (int i : order) {
            buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2]).setColor(r, g, bl, alpha);
        }
    }
}
