package dev.apogee.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import dev.apogee.client.fx.SandevistanEffects;
import dev.apogee.client.model.SandevistanModel;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the ported spine on the wearer's back, as an item, and for the tooltip header. */
public final class SandevistanImplantRenderer implements TrinketRenderer, BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final ResourceLocation TEXTURE = Apogee.id("textures/entity/sandevistan.png");
    public static final ResourceLocation OVERDRIVE_TEXTURE = Apogee.id("textures/entity/sandevistan_overdrive.png");
    public static final SandevistanImplantRenderer INSTANCE = new SandevistanImplantRenderer();

    private SandevistanModel model;

    private SandevistanImplantRenderer() {
    }

    public void clearBakedModel() {
        model = null;
    }

    private SandevistanModel model() {
        if (model == null) {
            model = new SandevistanModel(Minecraft.getInstance().getEntityModels().bakeLayer(SandevistanModel.LAYER));
        }
        return model;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(contextModel instanceof HumanoidModel humanoid)) {
            return;
        }
        SandevistanModel spine = model();
        humanoid.copyPropertiesTo(spine);
        boolean overdrive = SandevistanEffects.isOverdriveFlash(entity);
        ModelPart body = spine.body;
        float z = body.z;
        body.xScale = SandevistanModel.SPINE_X_SCALE;
        body.yScale = SandevistanModel.SPINE_Y_SCALE;
        body.zScale = SandevistanModel.SPINE_Z_SCALE;
        body.z = z + SandevistanModel.BACK_OFFSET;
        try {
            body.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(overdrive ? OVERDRIVE_TEXTURE : TEXTURE)),
                    overdrive ? LightTexture.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY);
        } finally {
            body.xScale = 1.0F;
            body.yScale = 1.0F;
            body.zScale = 1.0F;
            body.z = z;
        }
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));
            }
            case GROUND -> poseStack.scale(0.55F, 0.55F, 0.55F);
            case FIXED -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0F, 0.1F, 0.05F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-35.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F));
            }
            case HEAD -> poseStack.scale(0.6F, 0.6F, 0.6F);
            default -> {
            }
        }
        renderCentered(poseStack, buffers, light, overlay, false);
        poseStack.popPose();
    }

    /**
     * Draws the spine centred on the current origin, one block tall, facing the viewer. Items and
     * the tooltip header both go through here so they share geometry and textures with the
     * equipped implant.
     */
    public void renderCentered(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, boolean overdrive) {
        SandevistanModel spine = model();
        ModelPart body = spine.body;
        body.resetPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        // ModelPart already converts cube coordinates from pixels to blocks.
        poseStack.scale(SandevistanModel.SPINE_X_SCALE, SandevistanModel.SPINE_Y_SCALE, SandevistanModel.SPINE_Z_SCALE);
        poseStack.translate(0.0F, -SandevistanModel.CENTER_Y / 16.0F, -SandevistanModel.CENTER_Z / 16.0F);
        body.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(overdrive ? OVERDRIVE_TEXTURE : TEXTURE)), light, overlay);
        poseStack.popPose();
    }
}
