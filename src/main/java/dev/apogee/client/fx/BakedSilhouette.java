package dev.apogee.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * One afterimage frozen as plain vertices: the player model and every render layer (armor,
 * held items, implants) are drawn once, when the silhouette is left behind, into this recorder.
 * Each frame then only replays the stored vertices with the fade and color of the moment, which
 * skips posing the model, running every layer and animating GeckoLib models again for every
 * silhouette on every frame.
 */
final class BakedSilhouette implements MultiBufferSource {
    /** Light value recorded when no layer forced its own; replaced by the light of the moment. */
    static final int SCENE_LIGHT = 0;
    /** Layer models with more vertices than this are thinned out (the player model has about 1,300). */
    private static final int DENSE_VERTICES = 4000;
    /** A quarter of a square model pixel, in square blocks. */
    private static final double SPECK_AREA = 0.25 / 256.0;

    private final Map<RenderType, Group> groups = new LinkedHashMap<>();
    private boolean layers;

    /** Vertices drawn from now on belong to equipment layers, which fade out before the body. */
    void startLayers() {
        layers = true;
    }

    Map<RenderType, Group> groups() {
        return groups;
    }

    /** Thins out dense layer models once recording is over; see {@link Group#dropSpecks}. */
    void finish() {
        for (Group group : groups.values()) {
            if (group.layer && group.vertices() > DENSE_VERTICES && group.vertices() % 4 == 0) {
                group.dropSpecks(SPECK_AREA);
            }
        }
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        Group group = groups.computeIfAbsent(renderType, type -> new Group(layers));
        return group;
    }

    /** Vertices of one render type, in model space relative to the silhouette's feet. */
    static final class Group implements VertexConsumer {
        private static final int FLOATS = 8;
        private static final int INTS = 3;
        final boolean layer;
        private float[] floats = new float[FLOATS * 64];
        private int[] ints = new int[INTS * 64];
        private int count = -1;

        Group(boolean layer) {
            this.layer = layer;
        }

        /**
         * Drops the quads smaller than {@code minArea} (in square blocks). Dense models such as
         * the Mantis Blades are mostly tiny detail faces a translucent silhouette never shows.
         */
        void dropSpecks(double minArea) {
            int kept = 0;
            for (int q = 0; q + 3 <= count; q += 4) {
                int o = q * FLOATS;
                float ax = floats[o + FLOATS] - floats[o];
                float ay = floats[o + FLOATS + 1] - floats[o + 1];
                float az = floats[o + FLOATS + 2] - floats[o + 2];
                float bx = floats[o + 3 * FLOATS] - floats[o];
                float by = floats[o + 3 * FLOATS + 1] - floats[o + 1];
                float bz = floats[o + 3 * FLOATS + 2] - floats[o + 2];
                double cx = ay * bz - az * by;
                double cy = az * bx - ax * bz;
                double cz = ax * by - ay * bx;
                if (cx * cx + cy * cy + cz * cz < minArea * minArea) {
                    continue;
                }
                System.arraycopy(floats, o, floats, kept * FLOATS, 4 * FLOATS);
                System.arraycopy(ints, q * INTS, ints, kept * INTS, 4 * INTS);
                kept += 4;
            }
            count = kept - 1;
        }

        int vertices() {
            return count + 1;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            count++;
            if ((count + 1) * FLOATS > floats.length) {
                floats = Arrays.copyOf(floats, floats.length * 2);
                ints = Arrays.copyOf(ints, ints.length * 2);
            }
            int f = count * FLOATS;
            floats[f] = x;
            floats[f + 1] = y;
            floats[f + 2] = z;
            int i = count * INTS;
            ints[i] = -1;
            ints[i + 1] = 0;
            ints[i + 2] = SCENE_LIGHT;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            ints[count * INTS] = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            floats[count * FLOATS + 3] = u;
            floats[count * FLOATS + 4] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            ints[count * INTS + 1] = v << 16 | u & 0xFFFF;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            ints[count * INTS + 2] = v << 16 | u & 0xFFFF;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            floats[count * FLOATS + 5] = x;
            floats[count * FLOATS + 6] = y;
            floats[count * FLOATS + 7] = z;
            return this;
        }

        /** Replays the vertices at {@code pose}, multiplied by {@code tint} and lit with {@code light}. */
        void replay(VertexConsumer out, PoseStack.Pose pose, int tint, int light) {
            Matrix4f matrix = pose.pose();
            int tintA = tint >>> 24 & 0xFF;
            int tintR = tint >>> 16 & 0xFF;
            int tintG = tint >>> 8 & 0xFF;
            int tintB = tint & 0xFF;
            for (int v = 0; v <= count; v++) {
                int f = v * FLOATS;
                int i = v * INTS;
                int color = ints[i];
                int recordedLight = ints[i + 2];
                out.addVertex(matrix, floats[f], floats[f + 1], floats[f + 2])
                        .setColor((color >>> 16 & 0xFF) * tintR / 255, (color >>> 8 & 0xFF) * tintG / 255,
                                (color & 0xFF) * tintB / 255, (color >>> 24 & 0xFF) * tintA / 255)
                        .setUv(floats[f + 3], floats[f + 4])
                        .setOverlay(ints[i + 1])
                        .setLight(recordedLight == SCENE_LIGHT ? light : recordedLight)
                        .setNormal(pose, floats[f + 5], floats[f + 6], floats[f + 7]);
            }
        }
    }
}
