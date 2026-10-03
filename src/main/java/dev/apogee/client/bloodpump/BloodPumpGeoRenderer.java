package dev.apogee.client.bloodpump;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/** Draws a {@link BloodPumpArm} at the pose stack's current transform. */
public final class BloodPumpGeoRenderer extends GeoObjectRenderer<BloodPumpArm> {
    public BloodPumpGeoRenderer() {
        super(new BloodPumpGeoModel());
    }

    public void renderInto(PoseStack poseStack, BloodPumpArm arm, MultiBufferSource buffers, int packedLight, float partialTick) {
        this.animatable = arm;
        defaultRender(poseStack, arm, buffers, null, null, 0.0F, partialTick, packedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, BloodPumpArm animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        // The caller already placed the pose stack on the arm; skip the block-centring offset.
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
    }

    @Override
    public long getInstanceId(BloodPumpArm animatable) {
        return 0L;
    }
}
