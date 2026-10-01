package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

/** Draws one {@link NanowireArm}: the housing and wire, then the red emissive mask on top. */
public final class NanowireArmGeoRenderer extends GeoObjectRenderer<NanowireArm> {
    private NanowireWire.Anchors anchors;
    public NanowireArmGeoRenderer() {
        super(new NanowireArmGeoModel());
        addRenderLayer(new GlowLayer(this));
    }

    /**
     * Draws the housing into {@code buffers}, at the pose stack's current transform. As for the
     * Mantis Blades, {@link GeoObjectRenderer#render} would swap in the level's buffers, which
     * shader packs do not use for the first-person hands.
     */
    public void renderInto(PoseStack poseStack, NanowireArm arm, MultiBufferSource buffers, int packedLight, float partialTick,
                           NanowireWire.Anchors anchors) {
        this.animatable = arm;
        this.anchors = anchors;
        anchors.captured = false;
        try {
            defaultRender(poseStack, arm, buffers, null, null, 0.0F, partialTick, packedLight);
        } finally {
            this.anchors = null;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, NanowireArm arm, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int light, int overlay, int colour) {
        if (!isReRender && anchors != null && bone.getName().endsWith("_wire_outlet")) {
            poseStack.pushPose();
            RenderUtil.prepMatrixForBone(poseStack, bone);
            anchors.capture(poseStack.last(), bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
            poseStack.popPose();
        }
        super.renderRecursively(poseStack, arm, bone, renderType, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    public void preRender(PoseStack poseStack, NanowireArm animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        // The caller already placed the pose stack on the arm; skip the block-centring offset.
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
    }

    @Override
    public long getInstanceId(NanowireArm animatable) {
        return 0L;
    }

    /** The red mask with the vanilla eyes type, as for the Mantis Blades' glow (shader packs bloom it). */
    private static final class GlowLayer extends AutoGlowingGeoLayer<NanowireArm> {
        GlowLayer(GeoRenderer<NanowireArm> renderer) {
            super(renderer);
        }

        @Override
        protected RenderType getRenderType(NanowireArm animatable, @Nullable MultiBufferSource bufferSource) {
            return RenderType.eyes(NanowireArmGeoModel.GLOWMASK);
        }
    }
}
