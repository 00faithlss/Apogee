package dev.apogee.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.client.mantis.MantisArmorSleeves;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

public final class MantisArmorProbe {
    public static int clippedCubes;

    private MantisArmorProbe() {}

    public static void verify(Minecraft mc) {
        HumanoidModel<?> model = new HumanoidModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        ModelPart.Cube cube = new ModelPart.Cube(40, 16, -3, -2, -2, 4, 12, 4,
                1, 1, 1, false, 64, 32, EnumSet.allOf(Direction.class));
        for (float progress : new float[]{0.25F, 0.5F, 1.0F, 0.5F, 0.25F}) {
            var old = MantisArmorSleeves.begin(model, progress);
            Capture captured = new Capture();
            try {
                require(MantisArmorSleeves.render(model.rightArm, cube, new PoseStack().last(), captured,
                        0xF000F0, 0, 0xFF123456), "arm clipping active");
                float expected = (11.0F + (MantisArmorSleeves.OPENING_Y - 11.0F) * progress) / 16.0F;
                require(captured.vertices == 24 && Math.abs(captured.bottom - expected) < 0.00001F, "cropped sleeve bounds");
                require(captured.top == -3.0F / 16.0F, "shoulder retained");
                require(!MantisArmorSleeves.render(model.body, cube, new PoseStack().last(), new Capture(), 0, 0, -1),
                        "chest geometry unaffected");
            } finally {
                MantisArmorSleeves.restore(old);
            }
            require(!MantisArmorSleeves.render(model.rightArm, cube, new PoseStack().last(), new Capture(), 0, 0, -1),
                    "scope restored for the next wearer");
        }
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    private static final class Capture implements VertexConsumer {
        int vertices;
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            require(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "finite vertices");
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
            vertices++;
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            require(r == 0x12 && g == 0x34 && b == 0x56 && a == 0xFF, "armor tint retained");
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            require(Float.isFinite(u) && Float.isFinite(v) && u >= 0 && u <= 1 && v >= 0 && v <= 1, "valid texture UVs");
            return this;
        }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
