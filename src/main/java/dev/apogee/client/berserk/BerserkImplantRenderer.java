package dev.apogee.client.berserk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import dev.apogee.berserk.BerserkVariant;
import dev.apogee.client.fx.BerserkEffects;
import dev.apogee.item.BerserkItem;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Draws the Berserk's vertebral plates on the wearer's back, from the GeckoLib export of
 * {@code art/berserk/berserk.bbmodel}, with the variant's texture and its lights glowing. The
 * plates breathe while the local player's Berserk runs.
 */
public final class BerserkImplantRenderer implements TrinketRenderer, BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final BerserkImplantRenderer INSTANCE = new BerserkImplantRenderer();
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("active");

    private final Display display = new Display();
    private final Renderer renderer = new Renderer();

    private BerserkImplantRenderer() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(contextModel instanceof HumanoidModel humanoid) || !(stack.getItem() instanceof BerserkItem berserk) || entity.isInvisible()) {
            return;
        }
        display.variant = berserk.variant();
        display.active = entity == Minecraft.getInstance().player && BerserkEffects.isLocalActive();
        display.entityId = entity.getId();
        poseStack.pushPose();
        humanoid.body.translateAndRotate(poseStack);
        // Model space has y down from the neck; the export has y up from the feet.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -24.0F / 16.0F, 0.0F);
        renderer.renderInto(poseStack, display, buffers, light, tickDelta);
        poseStack.popPose();
    }

    /** The item: the plates alone, centred in the item cube and turned toward the viewer like the Sandevistan's spine. */
    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BerserkItem berserk)) {
            return;
        }
        // Plates span x -3.7..3.7, y 12.2..24.2 and z 1.6..3.8 in the export, 12 pixels tall.
        float scale = 1.35F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            }
            case GROUND -> scale *= 0.55F;
            case FIXED -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0F, 0.1F, 0.05F);
                scale *= 0.5F;
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                scale *= 0.6F;
                poseStack.mulPose(Axis.YP.rotationDegrees(-35.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F));
            }
            case HEAD -> scale *= 0.6F;
            default -> {
            }
        }
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -18.2F / 16.0F, -2.7F / 16.0F);
        display.variant = berserk.variant();
        display.active = false;
        display.entityId = -1;
        renderer.renderInto(poseStack, display, buffers, light, 0.0F);
        poseStack.popPose();
    }

    /** The item's GUI pose, for the tooltip header: the caller scales and centres the unit cube. */
    public void renderForTooltip(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        render(stack, ItemDisplayContext.GUI, poseStack, buffers, light, overlay);
    }

    static ResourceLocation texture(BerserkVariant variant) {
        return Apogee.id("textures/berserk/berserk_" + variant.id() + ".png");
    }

    static ResourceLocation glowmask(BerserkVariant variant) {
        return Apogee.id("textures/berserk/berserk_" + variant.id() + "_glowmask.png");
    }

    private static final class Display implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        BerserkVariant variant = BerserkVariant.ZETATECH;
        boolean active;
        int entityId;

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "plates", 4, state ->
                    active ? state.setAndContinue(ACTIVE) : PlayState.STOP));
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
        private static final ResourceLocation GEO = Apogee.id("geo/berserk.geo.json");
        private static final ResourceLocation ANIMATIONS = Apogee.id("animations/berserk.animation.json");

        @Override
        public ResourceLocation getModelResource(Display animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(Display animatable) {
            return texture(animatable.variant);
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
                    return RenderType.eyes(glowmask(animatable.variant));
                }
            });
        }

        void renderInto(PoseStack poseStack, Display display, MultiBufferSource buffers, int light, float partialTick) {
            this.animatable = display;
            defaultRender(poseStack, display, buffers, null, null, 0.0F, partialTick, light);
        }

        @Override
        public void preRender(PoseStack poseStack, Display animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                              int colour) {
            this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        }

        @Override
        public long getInstanceId(Display animatable) {
            return animatable.entityId;
        }
    }
}
