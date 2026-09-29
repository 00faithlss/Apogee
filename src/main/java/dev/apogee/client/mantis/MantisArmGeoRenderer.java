package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Draws one {@link MantisArm} in three passes: the mechanism with the variant's texture, the
 * skin panels with the wearer's own skin, and the emissive mask while the blade is out.
 *
 * <p>Skin panels are the cubes painted with the kit atlas' {@code skin} cell. In the skin pass
 * their UVs are replaced by a projection of each vertex's rest position onto the arm's skin
 * layout, so every panel shows the patch of skin it covers when the arm is closed and carries it
 * along as it opens.
 */
public final class MantisArmGeoRenderer extends GeoObjectRenderer<MantisArm> {
    /** The kit's {@code skin} material cell in its 64x64 atlas, as UV bounds. */
    private static final float SKIN_U0 = 48 / 64F;
    private static final float SKIN_V0 = 16 / 64F;
    private static final float SKIN_V1 = 32 / 64F;

    private boolean skinPass;
    private ResourceLocation skinTexture;
    private boolean slim;
    private boolean glow;

    public MantisArmGeoRenderer() {
        super(new MantisArmGeoModel());
        addRenderLayer(new SkinLayer(this));
        addRenderLayer(new GlowLayer(this));
    }

    /** Sets up the next {@link #render} call for a given wearer. */
    public void prepare(ResourceLocation skinTexture, boolean slim, boolean glow) {
        this.skinTexture = skinTexture;
        this.slim = slim;
        this.glow = glow;
    }

    @Override
    public void preRender(PoseStack poseStack, MantisArm animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        // The caller already placed the pose stack on the arm; skip the block-centring offset.
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
    }

    @Override
    public long getInstanceId(MantisArm animatable) {
        return 0L;
    }

    private static boolean isSkin(GeoCube cube) {
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            float u = 0.0F;
            float v = 0.0F;
            for (GeoVertex vertex : quad.vertices()) {
                u += vertex.texU();
                v += vertex.texV();
            }
            u /= quad.vertices().length;
            v /= quad.vertices().length;
            return u > SKIN_U0 && v > SKIN_V0 && v < SKIN_V1;
        }
        return false;
    }

    @Override
    public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        boolean skin = isSkin(cube);
        if (skin != skinPass) {
            return;
        }
        if (!skin) {
            super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
            return;
        }
        RenderUtil.translateToPivotPoint(poseStack, cube);
        RenderUtil.rotateMatrixAroundCube(poseStack, cube);
        RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
        Matrix3f normalState = poseStack.last().normal();
        Matrix4f poseState = new Matrix4f(poseStack.last().pose());
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            Vector3f normal = normalState.transform(new Vector3f(quad.normal()));
            RenderUtil.fixInvertedFlatCube(cube, normal);
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f position = vertex.position();
                Vector4f transformed = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
                float[] uv = skinUv(position, quad.direction());
                buffer.addVertex(transformed.x(), transformed.y(), transformed.z(), colour, uv[0], uv[1],
                        packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
            }
        }
    }

    /**
     * Projects a rest-pose vertex onto the wearer's skin. The kit's arm spans x and y -2..2 and
     * z -6..6 (editor pixels, hand at -Z, the implant opening at +Y facing away from the body);
     * GeckoLib stores x mirrored and in blocks. The opening side reads the arm's outer face, the
     * opposite side its inner face, the X sides its front and back, and the ends its caps.
     */
    private float[] skinUv(Vector3f position, Direction direction) {
        float x = -position.x() * 16.0F;
        float y = position.y() * 16.0F;
        float z = position.z() * 16.0F;
        boolean right = animatable.right();
        int width = slim ? 3 : 4;
        int depth = 4;
        float widthScale = width / 4.0F;
        int baseU = right ? 40 : 32;
        int baseV = right ? 16 : 48;
        float row = Mth.clamp(6.0F - z, 0.0F, 12.0F);
        float across = Mth.clamp(x + 2.0F, 0.0F, 4.0F);
        float thick = Mth.clamp(y + 2.0F, 0.0F, 4.0F);
        int outerU = right ? baseU : baseU + depth + width;
        int innerU = right ? baseU + depth + width : baseU;
        float u;
        float v;
        int stepX = -direction.getStepX();
        if (direction.getStepY() > 0) {
            u = outerU + across;
            v = baseV + depth + row;
        } else if (direction.getStepY() < 0) {
            u = innerU + across;
            v = baseV + depth + row;
        } else if (stepX > 0) {
            u = baseU + depth + thick * widthScale;
            v = baseV + depth + row;
        } else if (stepX < 0) {
            u = baseU + 2 * depth + width + (4.0F - thick) * widthScale;
            v = baseV + depth + row;
        } else if (direction.getStepZ() > 0) {
            u = baseU + depth + thick * widthScale;
            v = baseV + across;
        } else {
            u = baseU + depth + width + thick * widthScale;
            v = baseV + across;
        }
        return new float[]{u / 64.0F, v / 64.0F};
    }

    /** Second pass: only the skin panels, textured with the wearer's skin. */
    private static final class SkinLayer extends GeoRenderLayer<MantisArm> {
        SkinLayer(GeoRenderer<MantisArm> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MantisArm animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight,
                           int packedOverlay) {
            MantisArmGeoRenderer renderer = (MantisArmGeoRenderer) getRenderer();
            if (renderer.skinTexture == null) {
                return;
            }
            RenderType skinType = RenderType.entityCutoutNoCull(renderer.skinTexture);
            renderer.skinPass = true;
            try {
                renderer.reRender(bakedModel, poseStack, bufferSource, animatable, skinType, bufferSource.getBuffer(skinType),
                        partialTick, packedLight, packedOverlay, -1);
            } finally {
                renderer.skinPass = false;
            }
        }
    }

    /**
     * Emissive mask ({@code *_glowmask.png}), drawn only while the implant is open. The standard
     * variant has nothing that glows, and GeckoLib rejects an empty mask, so it is skipped.
     */
    private static final class GlowLayer extends AutoGlowingGeoLayer<MantisArm> {
        GlowLayer(GeoRenderer<MantisArm> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MantisArm animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight,
                           int packedOverlay) {
            if (((MantisArmGeoRenderer) getRenderer()).glow && animatable.variant() != MantisVariant.STANDARD) {
                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        }
    }
}
