package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Both wires of one wearer, computed and drawn every frame as continuous glowing ribbons.
 *
 * <p>Each wire runs from its housing's nozzle along the inside of the hand to the grip on the
 * front of the fist, then on: at rest to the point between both fists, where it meets the other
 * wire (built from the real grips every frame, so it holds whatever poses the arms), and while
 * lashing along the path its free end has just travelled ({@link NanowireMoves}). Fast moves also
 * leave fading copies of the wire from a moment before, like the game's light trails.
 *
 * <p>All points are in render space (camera-relative, the camera at the origin), captured from
 * the pose the housing was drawn with, so the same code serves both first and third person.
 */
public final class NanowireWire {
    /** Anchor points in the pack's per-arm export space (pixels), right arm then left arm. */
    private static final float[][] NOZZLE = {{-2.61F, 0.0F, -6.74F}, {2.61F, 0.0F, -6.74F}};
    private static final float[][] GUIDE = {{-2.61F, 0.0F, -11.85F}, {2.61F, 0.0F, -11.85F}};
    private static final float[][] GRIP = {{-1.15F, 0.0F, -11.57F}, {1.15F, 0.0F, -11.57F}};
    /** Points along the free part of a wire. */
    private static final int POINTS = 72;
    /** Fading copies of a moving wire, and how far back in move time each one is. */
    private static final int TRAILS = 8;
    private static final float TRAIL_STEP = 0.018F;
    /** The wire's own red. */
    private static final int RED = 0xFFFF0000;
    /** Centre of the atlas' pure red cell, which is also fully emissive in the mask. */
    private static final float U = 40.0F / 64.0F;
    private static final float V = 24.0F / 64.0F;

    /** Where a wire's lead runs, captured while its housing is drawn, in render space. */
    static final class Anchors {
        final Vector3f nozzle = new Vector3f();
        final Vector3f guide = new Vector3f();
        final Vector3f grip = new Vector3f();
        boolean captured;

        void capture(PoseStack.Pose pose, boolean right) {
            int side = right ? 0 : 1;
            transform(pose.pose(), NOZZLE[side], nozzle);
            transform(pose.pose(), GUIDE[side], guide);
            transform(pose.pose(), GRIP[side], grip);
            captured = true;
        }

        private static void transform(Matrix4f matrix, float[] pixels, Vector3f out) {
            matrix.transformPosition(pixels[0] / 16.0F, pixels[1] / 16.0F, pixels[2] / 16.0F, out);
        }
    }

    /** The wearer's frame in render space: origin, right, up and forward (unit vectors, blocks). */
    public record Frame(Vector3f origin, Vector3f right, Vector3f up, Vector3f forward) {
        public static Frame of(PoseStack.Pose pose, boolean modelSpace) {
            Matrix3f normal = pose.normal();
            Vector3f origin = pose.pose().transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
            // The player model's space has +X to the wearer's left and +Y down; view space has
            // +X right and +Y up. Forward is -Z in both.
            Vector3f right = normal.transform(modelSpace ? new Vector3f(-1, 0, 0) : new Vector3f(1, 0, 0)).normalize();
            Vector3f up = normal.transform(modelSpace ? new Vector3f(0, -1, 0) : new Vector3f(0, 1, 0)).normalize();
            Vector3f forward = normal.transform(new Vector3f(0, 0, -1)).normalize();
            return new Frame(origin, right, up, forward);
        }

        Vector3f toRender(Vector3f p) {
            return new Vector3f(origin).fma(p.x, right).fma(p.y, up).fma(p.z, forward);
        }

        Vector3f toFrame(Vector3f p) {
            Vector3f d = new Vector3f(p).sub(origin);
            return new Vector3f(d.dot(right), d.dot(up), d.dot(forward));
        }
    }

    private NanowireWire() {
    }

    /** Draws both wires of {@code wearer} if both leads were captured this pass, then clears them. */
    static void draw(MultiBufferSource buffers, Entity wearer, Anchors[] anchors, Frame frame, float partialTick) {
        if (!anchors[0].captured || !anchors[1].captured) {
            return;
        }
        anchors[0].captured = false;
        anchors[1].captured = false;
        float feed = MantisClient.deployProgress(wearer, partialTick);
        if (feed <= 0.0F) {
            return;
        }
        float heavy = MantisClient.thrustProgress(wearer, partialTick);
        float lashRight = MantisClient.slashProgress(wearer, MantisRules.ARM_RIGHT, partialTick);
        float lashLeft = MantisClient.slashProgress(wearer, MantisRules.ARM_LEFT, partialTick);
        float time = (MantisClient.clientTicks() + partialTick) / 20.0F;

        // The held point between both fists, a hair below the straight line, with a faint hum.
        Vector3f held = new Vector3f(anchors[0].grip).add(anchors[1].grip).mul(0.5F);
        held.fma(-0.010F, frame.up()).fma(0.0015F * Mth.sin(time * 44.0F), frame.up());

        VertexConsumer core = buffers.getBuffer(RenderType.entityTranslucentEmissive(NanowireArmGeoModel.TEXTURE));
        List<List<Vector3f>> glow = new ArrayList<>();
        List<List<Vector3f>> trails = new ArrayList<>();
        List<Float> trailAlpha = new ArrayList<>();
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? 1.0F : -1.0F;
            float own = side == 0 ? lashRight : lashLeft;
            float other = side == 0 ? lashLeft : lashRight;
            Move move = heavy >= 0.0F ? new Move(heavy, true, true)
                    : own >= 0.0F ? new Move(own, false, true)
                    : other >= 0.0F ? new Move(other, false, false)
                    : null;
            // The lashing wire's path depends only on this frame's fists, so its trails share it.
            NanowireMoves.Path path = null;
            if (move != null && move.moving) {
                Vector3f grip = frame.toFrame(anchors[side].grip);
                Vector3f between = frame.toFrame(held);
                path = move.heavy ? NanowireMoves.heavy(sign, grip, between) : NanowireMoves.lash(sign, grip, between);
            }
            List<Vector3f> wire = wire(anchors[side], held, frame, move, path, 0.0F, feed);
            ribbon(core, wire, 1.0F, RED);
            glow.add(wire);
            if (path != null && move.u > 0.03F && move.u < NanowireMoves.arrival(move.heavy)) {
                for (int k = 1; k <= TRAILS; k++) {
                    trails.add(wire(anchors[side], held, frame, move, path, k * TRAIL_STEP, feed));
                    trailAlpha.add(0.5F * (1.0F - k / (TRAILS + 1.0F)));
                }
            }
        }
        VertexConsumer additive = buffers.getBuffer(RenderType.dragonRays());
        for (List<Vector3f> wire : glow) {
            halo(additive, wire, 6.0F, 0.5F);
        }
        for (int i = 0; i < trails.size(); i++) {
            halo(additive, trails.get(i), 2.6F, trailAlpha.get(i));
        }
    }

    /**
     * The current move for one wire: {@code u} is its progress, {@code heavy} whether it is the
     * two-wire lash, {@code moving} whether this wire lashes (else it hangs from its own fist
     * while the other one does).
     */
    private record Move(float u, boolean heavy, boolean moving) {
    }

    /**
     * One wire as a polyline in render space, {@code back} move-time earlier, cut to {@code feed} of
     * its length; {@code path} is its end's path while it lashes, else null.
     */
    private static List<Vector3f> wire(Anchors anchors, Vector3f held, Frame frame, Move move, NanowireMoves.Path path, float back,
                                       float feed) {
        List<Vector3f> points = new ArrayList<>(POINTS + 3);
        points.add(new Vector3f(anchors.nozzle));
        points.add(new Vector3f(anchors.guide));
        Vector3f grip = anchors.grip;
        points.add(new Vector3f(grip));
        float release = 0.0F;
        float taut = 0.0F;
        float u = 0.0F;
        Vector3f hang = null;
        if (move != null) {
            u = move.u - back;
            if (path != null) {
                float arrival = NanowireMoves.arrival(move.heavy);
                release = NanowireMoves.ease((u - 0.01F) / 0.07F) * (1.0F - NanowireMoves.ease((u - arrival) / (1.0F - arrival)));
                // Reeled in, the wire is pulled straight from the fist to its end before the end is caught.
                taut = NanowireMoves.ease((u - (move.heavy ? 0.62F : 0.56F)) / 0.26F);
            } else {
                // The other wire lets go and hangs a little below its own fist until the lash is over.
                float slack = NanowireMoves.ease(move.u / 0.10F) * (1.0F - NanowireMoves.ease((move.u - 0.78F) / 0.12F));
                hang = new Vector3f(grip).fma(0.10F, frame.forward()).fma(-0.07F, frame.up()).lerp(held, 1.0F - slack);
            }
        }
        float tip = path == null ? 0.0F : path.end(u);
        float length = path == null ? 0.0F : path.length(u);
        Vector3f end = path == null ? null : frame.toRender(path.at(tip));
        // The free wire keeps the shape of the path's last stretch, moved to leave from the fist;
        // the move fades out along the first part of the wire, so it bends there without a kink.
        Vector3f offset = path == null ? null : new Vector3f(grip).sub(frame.toRender(path.at(tip - length)));
        for (int k = 1; k <= POINTS; k++) {
            float s = k / (float) POINTS;
            // At rest: a straight line to the held point, sagging a little in its middle.
            Vector3f rest = new Vector3f(grip).lerp(hang != null ? hang : held, s).fma(-0.006F * Mth.sin(Mth.PI * s), frame.up());
            if (release <= 0.0F) {
                points.add(rest);
                continue;
            }
            // The free wire is the last stretch of its end's path, as long as the wire that is out.
            Vector3f lashed = frame.toRender(path.at(tip - (1.0F - s) * length)).fma(1.0F - NanowireMoves.ease(s / 0.4F), offset);
            lashed.lerp(new Vector3f(grip).lerp(end, s), taut);
            points.add(rest.lerp(lashed, release));
        }
        return cut(points, feed);
    }

    /** Keeps the first {@code fraction} of the polyline's length, ending exactly there. */
    private static List<Vector3f> cut(List<Vector3f> points, float fraction) {
        if (fraction >= 1.0F) {
            return points;
        }
        float total = 0.0F;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i).distance(points.get(i - 1));
        }
        float keep = total * fraction;
        List<Vector3f> out = new ArrayList<>();
        out.add(points.get(0));
        float run = 0.0F;
        for (int i = 1; i < points.size(); i++) {
            Vector3f a = points.get(i - 1);
            Vector3f b = points.get(i);
            float length = a.distance(b);
            if (run + length >= keep) {
                float k = length <= 0.0F ? 0.0F : (keep - run) / length;
                out.add(new Vector3f(a).lerp(b, k));
                return out;
            }
            out.add(b);
            run += length;
        }
        return out;
    }

    /**
     * Half the ribbon's width at a point, in blocks: a fine wire up close that never gets
     * thinner than about two pixels on screen further away.
     */
    private static float halfWidth(Vector3f p) {
        return Math.max(0.0028F, 0.0017F * p.length());
    }

    /** Camera-facing sideways offset at point {@code i}, scaled by {@code widen}. */
    private static Vector3f side(List<Vector3f> points, int i, float widen) {
        Vector3f a = points.get(Math.max(0, i - 1));
        Vector3f b = points.get(Math.min(points.size() - 1, i + 1));
        Vector3f tangent = new Vector3f(b).sub(a);
        Vector3f p = points.get(i);
        Vector3f toCamera = new Vector3f(p).negate();
        Vector3f side = tangent.cross(toCamera, new Vector3f());
        if (side.lengthSquared() < 1.0E-12F) {
            return new Vector3f();
        }
        return side.normalize(halfWidth(p) * widen);
    }

    /** The solid red core, as quads of the emissive entity type (lit by nothing, bloomed by shader packs). */
    private static void ribbon(VertexConsumer out, List<Vector3f> points, float widen, int argb) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Vector3f a = points.get(i);
            Vector3f b = points.get(i + 1);
            Vector3f sa = side(points, i, widen);
            Vector3f sb = side(points, i + 1, widen);
            vertex(out, a.x - sa.x, a.y - sa.y, a.z - sa.z, argb);
            vertex(out, a.x + sa.x, a.y + sa.y, a.z + sa.z, argb);
            vertex(out, b.x + sb.x, b.y + sb.y, b.z + sb.z, argb);
            vertex(out, b.x - sb.x, b.y - sb.y, b.z - sb.z, argb);
        }
    }

    private static void vertex(VertexConsumer out, float x, float y, float z, int argb) {
        out.addVertex(x, y, z).setColor(argb).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
    }

    /** A soft additive glow along the wire: triangles of the dragon-rays type, brightest at the centre line. */
    private static void halo(VertexConsumer out, List<Vector3f> points, float widen, float alpha) {
        int red = 255;
        int green = 24;
        int blue = 20;
        int core = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        for (int i = 0; i + 1 < points.size(); i++) {
            Vector3f a = points.get(i);
            Vector3f b = points.get(i + 1);
            Vector3f sa = side(points, i, widen);
            Vector3f sb = side(points, i + 1, widen);
            // Two strips from the centre line out to each edge, fading to nothing at the edge.
            for (float sign : new float[]{1.0F, -1.0F}) {
                float ex = sa.x * sign;
                float ey = sa.y * sign;
                float ez = sa.z * sign;
                float fx = sb.x * sign;
                float fy = sb.y * sign;
                float fz = sb.z * sign;
                out.addVertex(a.x, a.y, a.z).setColor(red, green, blue, core);
                out.addVertex(a.x + ex, a.y + ey, a.z + ez).setColor(red, green, blue, 0);
                out.addVertex(b.x + fx, b.y + fy, b.z + fz).setColor(red, green, blue, 0);
                out.addVertex(a.x, a.y, a.z).setColor(red, green, blue, core);
                out.addVertex(b.x + fx, b.y + fy, b.z + fz).setColor(red, green, blue, 0);
                out.addVertex(b.x, b.y, b.z).setColor(red, green, blue, core);
            }
        }
    }
}
