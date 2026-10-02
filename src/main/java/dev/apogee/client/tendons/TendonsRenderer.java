package dev.apogee.client.tendons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.Apogee;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Draws the two exhausts on the back of the wearer's ankles, following each leg. The cubes and
 * their per-face UVs come from {@code art/tendons/reinforced_tendons_model.geo.json}, in its
 * Bedrock coordinates (y up, leg pivot at y 12): the model is a plain pair of boxes, so they are
 * emitted directly instead of going through a GeckoLib renderer, which has no use for one
 * static bone per leg.
 */
public final class TendonsRenderer implements TrinketRenderer {
    public static final TendonsRenderer INSTANCE = new TendonsRenderer();
    private static final ResourceLocation TEXTURE = Apogee.id("textures/entity/reinforced_tendons.png");
    private static final float TEX = 8.0F;
    private static final float PIVOT_X = 1.9F;
    private static final float PIVOT_Y = 12.0F;

    /** One box: Bedrock origin and size, then the UV of each face as {u, v, w, h} (north, east, south, west, up, down). */
    private record Box(float x, float y, float z, float w, float h, float d, float[][] uv) {
    }

    private static final float[][] PIPE_UV = {{2, 4, 2, 1}, {2, 5, 1, 1}, {4, 2, 2, 1}, {3, 5, 1, 1}, {4, 3, 2, 1}, {4, 5, 2, 1}};
    private static final float[][] BLOCK_UV = {{0, 0, 2, 2}, {0, 2, 2, 2}, {2, 0, 2, 2}, {2, 2, 2, 2}, {0, 4, 2, 2}, {4, 2, 2, 2}};

    private TendonsRenderer() {
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(contextModel instanceof HumanoidModel humanoid)) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        draw(poseStack, buffer, humanoid.rightLeg, -PIVOT_X, -3.0F, light);
        draw(poseStack, buffer, humanoid.leftLeg, PIVOT_X, 1.0F, light);
    }

    /**
     * Both exhausts as a free-standing piece, centered on the origin and one block wide, for the
     * tooltip's spinning model (model space: y points down, like the leg's).
     */
    public static void renderPreview(PoseStack poseStack, MultiBufferSource buffers, int light) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        // Middle of the pair: x between the legs, y and z at the middle of the boxes.
        poseStack.translate(0.0F, -(PIVOT_Y - 1.5F) / 16.0F, -3.0F / 16.0F);
        for (float side : new float[]{-1.0F, 1.0F}) {
            poseStack.pushPose();
            poseStack.translate(side * PIVOT_X / 16.0F, 0.0F, 0.0F);
            drawBoxes(poseStack, buffer, side * PIVOT_X, side < 0 ? -3.0F : 1.0F, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void drawBoxes(PoseStack poseStack, VertexConsumer buffer, float pivotX, float originX, int light) {
        box(poseStack, buffer, new Box(originX, 2, 2, 2, 1, 1, PIPE_UV), pivotX, light);
        box(poseStack, buffer, new Box(originX, 0, 2, 2, 2, 2, BLOCK_UV), pivotX, light);
    }

    private static void draw(PoseStack poseStack, VertexConsumer buffer, net.minecraft.client.model.geom.ModelPart leg,
                             float pivotX, float originX, int light) {
        poseStack.pushPose();
        leg.translateAndRotate(poseStack);
        for (Box box : new Box[]{
                new Box(originX, 2, 2, 2, 1, 1, PIPE_UV),
                new Box(originX, 0, 2, 2, 2, 2, BLOCK_UV)}) {
            box(poseStack, buffer, box, pivotX, light);
        }
        poseStack.popPose();
    }

    /** Bedrock point to the leg's model space: x relative to the pivot, y flipped to point down. */
    private static float[] p(float x, float y, float z, float pivotX) {
        return new float[]{(x - pivotX) / 16.0F, (PIVOT_Y - y) / 16.0F, z / 16.0F};
    }

    private static void box(PoseStack poseStack, VertexConsumer buffer, Box b, float pivotX, int light) {
        float x0 = b.x, x1 = b.x + b.w, y0 = b.y, y1 = b.y + b.h, z0 = b.z, z1 = b.z + b.d;
        // Corners of each face seen from outside, as top-left, top-right, bottom-right, bottom-left.
        float[][][] faces = {
                {{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}},
                {{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}},
                {{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}},
                {{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}},
                {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
                {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
        };
        // Model space has y down, so up and down swap.
        float[][] normals = {{0, 0, -1}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, -1, 0}, {0, 1, 0}};
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        for (int i = 0; i < 6; i++) {
            float[] uv = b.uv[i];
            float u0 = uv[0] / TEX, v0 = uv[1] / TEX, u1 = (uv[0] + uv[2]) / TEX, v1 = (uv[1] + uv[3]) / TEX;
            float[][] us = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            for (int c = 0; c < 4; c++) {
                float[] pos = p(faces[i][c][0], faces[i][c][1], faces[i][c][2], pivotX);
                buffer.addVertex(matrix, pos[0], pos[1], pos[2])
                        .setColor(-1)
                        .setUv(us[c][0], us[c][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, normals[i][0], normals[i][1], normals[i][2]);
            }
        }
    }
}
