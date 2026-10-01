package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

/**
 * The nanowire as an item: both housings side by side, from the pack's {@code nanowire_item}
 * export, with the wire stretched straight between their ports instead of the pack's coil. It
 * has no animation.
 */
public final class NanowireItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final NanowireItemRenderer INSTANCE = new NanowireItemRenderer();

    private final Display display = new Display();
    private final Renderer renderer = new Renderer();

    private NanowireItemRenderer() {
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        // Item space is a one-block cube; the export lies flat around its origin, about 7 pixels
        // across and 6.5 deep, with the housings' tops 3 pixels up.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (mode) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-30.0F));
                poseStack.scale(2.4F, 2.4F, 2.4F);
            }
            case GROUND -> poseStack.scale(0.9F, 0.9F, 0.9F);
            case FIXED -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.scale(1.8F, 1.8F, 1.8F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // Vanilla puts the hand's item cube low in the corner, nearly off screen; a flat
                // item has to be lifted into view and tipped toward the eye, as handheld models do.
                float side = mode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 1.0F : -1.0F;
                poseStack.translate(side * -0.08F, 0.30F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * -25.0F));
                poseStack.scale(0.6F, 0.6F, 0.6F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 0.12F, 0.05F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            default -> poseStack.scale(0.55F, 0.55F, 0.55F);
        }
        poseStack.translate(0.0F, -3.0F / 16.0F, -3.5F / 16.0F);
        renderer.renderInto(poseStack, display, buffers, light);
        poseStack.popPose();
    }

    private static final class Display implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return RenderUtil.getCurrentTick();
        }
    }

    private static final class Model extends GeoModel<Display> {
        private static final ResourceLocation GEO = Apogee.id("geo/nanowire_item.geo.json");
        private static final ResourceLocation ANIMATIONS = Apogee.id("animations/nanowire_right_attachment.animation.json");

        @Override
        public ResourceLocation getModelResource(Display animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(Display animatable) {
            return NanowireArmGeoModel.TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Display animatable) {
            return ANIMATIONS;
        }

        @Override
        public RenderType getRenderType(Display animatable, ResourceLocation texture) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private static final class Renderer extends GeoObjectRenderer<Display> {
        Renderer() {
            super(new Model());
            addRenderLayer(new AutoGlowingGeoLayer<>(this) {
                @Override
                protected RenderType getRenderType(Display animatable, @Nullable MultiBufferSource bufferSource) {
                    return RenderType.eyes(NanowireArmGeoModel.GLOWMASK);
                }
            });
        }

        void renderInto(PoseStack poseStack, Display display, MultiBufferSource buffers, int light) {
            this.animatable = display;
            defaultRender(poseStack, display, buffers, null, null, 0.0F, 0.0F, light);
        }

        @Override
        public void preRender(PoseStack poseStack, Display animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                              int colour) {
            this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        }

        @Override
        public long getInstanceId(Display animatable) {
            return 0L;
        }
    }
}
