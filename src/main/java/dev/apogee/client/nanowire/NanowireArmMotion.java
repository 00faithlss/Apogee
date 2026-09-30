package dev.apogee.client.nanowire;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The arm choreography of the pack's two-arm rig ({@code nanowire/arm_motion.json}, the
 * {@code left_arm} and {@code right_arm} tracks of its seven clips), sampled to pose the player's
 * own arms in first person. Coordinates are the rig's editor space in pixels: +X to the right,
 * +Y up, the arms reaching forward along -Z from their pivots at (±6, 0, 7).
 */
final class NanowireArmMotion {
    private static final ResourceLocation FILE = Apogee.id("nanowire/arm_motion.json");
    private static final float[] RIGHT_PIVOT = {6.0F, 0.0F, 7.0F};
    private static final float[] LEFT_PIVOT = {-6.0F, 0.0F, 7.0F};

    /** Keyframes of one channel: times in seconds and xyz values. */
    private record Track(float[] times, float[][] values) {
        float[] sample(float t) {
            if (times.length == 1 || t <= times[0]) {
                return values[0];
            }
            int last = times.length - 1;
            if (t >= times[last]) {
                return values[last];
            }
            int lo = 0;
            int hi = last;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (times[mid] <= t) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            float k = (t - times[lo]) / (times[hi] - times[lo]);
            float[] a = values[lo];
            float[] b = values[hi];
            return new float[]{Mth.lerp(k, a[0], b[0]), Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2])};
        }
    }

    private record Clip(float length, Track rightRotation, Track rightPosition, Track leftRotation, Track leftPosition) {
    }

    private static Map<String, Clip> clips;

    private NanowireArmMotion() {
    }

    /** Clip lengths in seconds, for mapping progress to time. */
    static float length(String clip) {
        Clip c = load().get(clip);
        return c == null ? 1.0F : c.length();
    }

    /**
     * Applies one arm's pose in {@code clip} at {@code seconds} to the pose stack, in blocks: the
     * rig's bone transform about the arm's pivot.
     */
    static void apply(PoseStack poseStack, String clip, float seconds, boolean right) {
        Clip c = load().get(clip);
        if (c == null) {
            return;
        }
        float[] pivot = right ? RIGHT_PIVOT : LEFT_PIVOT;
        float[] rotation = (right ? c.rightRotation() : c.leftRotation()).sample(seconds);
        float[] position = (right ? c.rightPosition() : c.leftPosition()).sample(seconds);
        poseStack.translate((pivot[0] + position[0]) / 16.0F, (pivot[1] + position[1]) / 16.0F, (pivot[2] + position[2]) / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]));
        poseStack.translate(-pivot[0] / 16.0F, -pivot[1] / 16.0F, -pivot[2] / 16.0F);
    }

    private static Map<String, Clip> load() {
        if (clips != null) {
            return clips;
        }
        clips = new HashMap<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
        if (resource.isEmpty()) {
            Apogee.LOGGER.error("Missing {}", FILE);
            return clips;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                JsonObject clip = entry.getValue().getAsJsonObject();
                JsonObject bones = clip.getAsJsonObject("bones");
                JsonObject right = bones.getAsJsonObject("right_arm");
                JsonObject left = bones.getAsJsonObject("left_arm");
                String name = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
                clips.put(name, new Clip(clip.get("animation_length").getAsFloat(),
                        track(right.get("rotation")), track(right.get("position")),
                        track(left.get("rotation")), track(left.get("position"))));
            }
        } catch (Exception e) {
            Apogee.LOGGER.error("Could not read {}", FILE, e);
        }
        return clips;
    }

    private static Track track(JsonElement element) {
        if (element == null) {
            return new Track(new float[]{0.0F}, new float[][]{{0.0F, 0.0F, 0.0F}});
        }
        if (element.isJsonArray()) {
            return new Track(new float[]{0.0F}, new float[][]{vector(element)});
        }
        var keys = element.getAsJsonObject().entrySet().stream()
                .sorted((a, b) -> Float.compare(Float.parseFloat(a.getKey()), Float.parseFloat(b.getKey())))
                .toList();
        float[] times = new float[keys.size()];
        float[][] values = new float[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            times[i] = Float.parseFloat(keys.get(i).getKey());
            JsonElement value = keys.get(i).getValue();
            values[i] = vector(value.isJsonObject() ? value.getAsJsonObject().get("post") : value);
        }
        return new Track(times, values);
    }

    private static float[] vector(JsonElement element) {
        var array = element.getAsJsonArray();
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }
}
