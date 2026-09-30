package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Draws one {@link NanowireArm}: the housing and wire, then the red emissive mask on top. */
public final class NanowireArmGeoRenderer extends GeoObjectRenderer<NanowireArm> {
    public NanowireArmGeoRenderer() {
        super(new NanowireArmGeoModel());
        addRenderLayer(new GlowLayer(this));
    }

    /**
     * Draws the housing into {@code buffers}, at the pose stack's current transform. As for the
     * Mantis Blades, {@link GeoObjectRenderer#render} would swap in the level's buffers, which
     * shader packs do not use for the first-person hands.
     */
    public void renderInto(PoseStack poseStack, NanowireArm arm, MultiBufferSource buffers, int packedLight, float partialTick) {
        this.animatable = arm;
        defaultRender(poseStack, arm, buffers, null, null, 0.0F, partialTick, packedLight);
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
