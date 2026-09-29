package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apogee.Apogee;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
import software.bernie.geckolib.cache.object.GeoBone;
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
 *
 * <p>The thermal blade also gets a fake bloom (larger, dimmer additive copies of the blade) and
 * glitch ghosts, and every blade's position is recorded for {@link MantisBladeFx}.
 */
public final class MantisArmGeoRenderer extends GeoObjectRenderer<MantisArm> {
    /** The kit's {@code skin} material cell in its 64x64 atlas, as UV bounds. */
    private static final float SKIN_U0 = 48 / 64F;
    private static final float SKIN_V0 = 16 / 64F;
    private static final float SKIN_V1 = 32 / 64F;
    /** Additive, fullbright: each bloom shell adds light instead of covering what is behind it. */
    private static final RenderType THERMAL_BLOOM = RenderType.eyes(Apogee.id("textures/mantis/mantis_thermal_glowmask.png"));

    private boolean skinPass;
    private ResourceLocation skinTexture;
    private boolean slim;
    private boolean glow;
    private boolean firstPerson;
    /** Depth inside the blade's bones ({@code blade_slider} and its child {@code cutting_edge}). */
    private int bladeDepth;
    /** While set, only the blade's cubes are drawn. */
    private boolean bladeOnly;
    /** Extra thickness of the blade's cubes in a bloom pass, as a fraction of their size. */
    private float inflate;
    /** Set during the main pass, while the blade's cubes are recorded for its effects. */
    private boolean recording;

    public MantisArmGeoRenderer() {
        super(new MantisArmGeoModel());
        addRenderLayer(new SkinLayer(this));
        addRenderLayer(new GlowLayer(this));
    }

    /** Sets up the next {@link #render} call for a given wearer. */
    public void prepare(ResourceLocation skinTexture, boolean slim, boolean glow, boolean firstPerson) {
        this.skinTexture = skinTexture;
        this.slim = slim;
        this.glow = glow;
        this.firstPerson = firstPerson;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, MantisArm animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        recording = !isReRender;
        if (recording) {
            animatable.bladeSegmentCount = 0;
            animatable.bladeTick = MantisClient.clientTicks();
            animatable.bladeFirstPerson = firstPerson;
        }
        try {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                    packedOverlay, colour);
        } finally {
            recording = false;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MantisArm animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        boolean blade = bone.getName().equals("blade_slider");
        if (blade) {
            bladeDepth++;
        }
        try {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                    packedOverlay, colour);
        } finally {
            if (blade) {
                bladeDepth--;
            }
        }
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
        if (skin != skinPass || (bladeOnly && bladeDepth == 0)) {
            return;
        }
        if (!skin) {
            if (recording && bladeDepth > 0) {
                recordBlade(poseStack, cube);
            }
            if (inflate > 0.0F) {
                Vector3f min = new Vector3f(Float.MAX_VALUE);
                Vector3f max = new Vector3f(-Float.MAX_VALUE);
                bounds(cube, min, max);
                Vector3f centre = min.add(max).mul(0.5F);
                poseStack.pushPose();
                poseStack.translate(centre.x(), centre.y(), centre.z());
                // The blade runs along Z: thicken it all round, lengthen it only a little.
                poseStack.scale(1.0F + inflate, 1.0F + inflate, 1.0F + inflate * 0.06F);
                poseStack.translate(-centre.x(), -centre.y(), -centre.z());
                super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
                poseStack.popPose();
            } else {
                super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
            }
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

    private static void bounds(GeoCube cube, Vector3f min, Vector3f max) {
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                min.min(vertex.position());
                max.max(vertex.position());
            }
        }
    }

    /** Stores a blade cube's two ends, along its length, in world space. */
    private void recordBlade(PoseStack poseStack, GeoCube cube) {
        MantisArm arm = animatable;
        if (arm.bladeSegmentCount >= MantisArm.MAX_BLADE_SEGMENTS) {
            return;
        }
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        bounds(cube, min, max);
        float x = (min.x() + max.x()) * 0.5F;
        float y = (min.y() + max.y()) * 0.5F;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f pose = poseStack.last().pose();
        int at = arm.bladeSegmentCount * 6;
        for (int end = 0; end < 2; end++) {
            Vector3f point = pose.transformPosition(new Vector3f(x, y, end == 0 ? min.z() : max.z()));
            if (firstPerson) {
                // The hands are drawn in view space; the level's pose stack is only camera-relative.
                camera.rotation().transform(point);
            }
            arm.bladeSegments[at + end * 3] = camera.getPosition().x + point.x();
            arm.bladeSegments[at + end * 3 + 1] = camera.getPosition().y + point.y();
            arm.bladeSegments[at + end * 3 + 2] = camera.getPosition().z + point.z();
        }
        arm.bladeSegmentCount++;
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
     *
     * <p>The thermal blade glows hot: additive shells around it, each larger and dimmer, stand in
     * for bloom, and now and then a displaced copy flickers beside it like signal noise.
     */
    private static final class GlowLayer extends AutoGlowingGeoLayer<MantisArm> {
        GlowLayer(GeoRenderer<MantisArm> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MantisArm animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight,
                           int packedOverlay) {
            MantisArmGeoRenderer renderer = (MantisArmGeoRenderer) getRenderer();
            if (!renderer.glow || animatable.variant() == MantisVariant.STANDARD) {
                return;
            }
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            if (animatable.variant() == MantisVariant.THERMAL) {
                heat(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay);
            }
        }

        private void heat(PoseStack poseStack, MantisArm animatable, BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                          float partialTick, int packedOverlay) {
            MantisArmGeoRenderer renderer = (MantisArmGeoRenderer) getRenderer();
            RenderType additive = THERMAL_BLOOM;
            long tick = MantisClient.clientTicks();
            // Slow shimmer of the whole halo.
            float pulse = 0.85F + 0.15F * Mth.sin((tick + partialTick) * 0.7F + (animatable.right() ? 0.0F : 2.1F));
            renderer.bladeOnly = true;
            try {
                shell(renderer, poseStack, animatable, bakedModel, bufferSource, additive, partialTick, packedOverlay, 0.8F, 0.50F * pulse);
                shell(renderer, poseStack, animatable, bakedModel, bufferSource, additive, partialTick, packedOverlay, 1.8F, 0.26F * pulse);
                shell(renderer, poseStack, animatable, bakedModel, bufferSource, additive, partialTick, packedOverlay, 3.2F, 0.11F * pulse);
                long hash = mix(tick / 2 * 0x9E3779B97F4A7C15L + System.identityHashCode(animatable));
                if ((hash & 0xFF) < 90) {
                    // Glitch: a copy of the blade jumps sideways for a moment.
                    float dx = (((hash >>> 8) & 0xFF) / 255.0F - 0.5F) * 0.10F;
                    float dy = (((hash >>> 16) & 0xFF) / 255.0F - 0.5F) * 0.05F;
                    poseStack.pushPose();
                    poseStack.translate(dx, dy, 0.0F);
                    shell(renderer, poseStack, animatable, bakedModel, bufferSource, additive, partialTick, packedOverlay, 0.0F, 0.7F);
                    poseStack.popPose();
                }
            } finally {
                renderer.bladeOnly = false;
                renderer.inflate = 0.0F;
            }
        }

        private static void shell(MantisArmGeoRenderer renderer, PoseStack poseStack, MantisArm animatable, BakedGeoModel bakedModel,
                                  MultiBufferSource bufferSource, RenderType additive, float partialTick, int packedOverlay,
                                  float inflate, float strength) {
            renderer.inflate = inflate;
            int level = Mth.clamp((int) (strength * 255.0F), 0, 255);
            int colour = 0xFF000000 | level << 16 | level << 8 | level;
            renderer.reRender(bakedModel, poseStack, bufferSource, animatable, additive, bufferSource.getBuffer(additive), partialTick,
                    0xF000F0, packedOverlay, colour);
        }

        private static long mix(long h) {
            h ^= h >>> 33;
            h *= 0xFF51AFD7ED558CCDL;
            h ^= h >>> 33;
            return h;
        }
    }
}
