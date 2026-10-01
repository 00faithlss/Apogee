package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.mixin.client.ModelPartCubeAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import org.joml.Vector3f;

public final class MantisArmorSleeves {
    // Leave clearance above the opening covers, keeping the shoulder covered.
    public static final float OPENING_Y = -1.25F;
    private static Context context;

    public record Context(ModelPart right, ModelPart left, float progress) {}

    private MantisArmorSleeves() {}

    public static boolean hasVanillaChestplate(AbstractClientPlayer player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof ArmorItem item && item.getEquipmentSlot() == EquipmentSlot.CHEST
                && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft");
    }

    public static boolean applies(AbstractClientPlayer player) {
        MantisVariant variant = MantisClient.variant(player);
        return variant != null && variant.isBlades() && hasVanillaChestplate(player);
    }

    public static Context begin(HumanoidModel<?> armor, float progress) {
        Context previous = context;
        context = new Context(armor.rightArm, armor.leftArm, progress);
        return previous;
    }

    public static void restore(Context previous) {
        context = previous;
    }

    public static boolean render(ModelPart part, ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer buffer,
                                 int light, int overlay, int color) {
        Context active = context;
        if (active == null || (part != active.right && part != active.left)) {
            return false;
        }
        ModelPart.Polygon[] faces = ((ModelPartCubeAccessor) cube).apogee$polygons();
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (ModelPart.Polygon face : faces) {
            for (ModelPart.Vertex vertex : face.vertices) {
                top = Math.min(top, vertex.pos.y);
                bottom = Math.max(bottom, vertex.pos.y);
            }
        }
        float cut = Mth.lerp(active.progress, bottom, OPENING_Y);
        if (cut >= bottom) {
            return false;
        }
        if (cut <= top) {
            return true;
        }
        Vector3f normal = new Vector3f();
        for (ModelPart.Polygon face : faces) {
            pose.transformNormal(face.normal, normal);
            for (ModelPart.Vertex vertex : face.vertices) {
                float y = Math.min(vertex.pos.y, cut);
                float u = vertex.u;
                float v = vertex.v;
                if (vertex.pos.y > cut && Math.abs(face.normal.y) < 0.5F) {
                    for (ModelPart.Vertex upper : face.vertices) {
                        if (upper.pos.y < vertex.pos.y && upper.pos.x == vertex.pos.x && upper.pos.z == vertex.pos.z) {
                            float t = (cut - upper.pos.y) / (vertex.pos.y - upper.pos.y);
                            u = Mth.lerp(t, upper.u, vertex.u);
                            v = Mth.lerp(t, upper.v, vertex.v);
                            break;
                        }
                    }
                }
                buffer.addVertex(pose, vertex.pos.x / 16.0F, y / 16.0F, vertex.pos.z / 16.0F)
                        .setColor(color).setUv(u, v).setOverlay(overlay).setLight(light)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
        return true;
    }
}
