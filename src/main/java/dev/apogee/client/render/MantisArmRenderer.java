package dev.apogee.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.Apogee;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.model.MantisArmModel;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;

/** Draws the Mantis Blade mechanism on a forearm, in both first and third person. */
public final class MantisArmRenderer {
    public static final ResourceLocation TEXTURE = Apogee.id("textures/entity/mantis_blades.png");
    private static final ResourceLocation THERMAL = Apogee.id("textures/entity/mantis_blades_thermal.png");
    private static final ResourceLocation THERMAL_HOT = Apogee.id("textures/entity/mantis_blades_thermal_hot.png");
    private static final ResourceLocation ELECTRIC = Apogee.id("textures/entity/mantis_blades_electric.png");
    private static final ResourceLocation ELECTRIC_HOT = Apogee.id("textures/entity/mantis_blades_electric_hot.png");
    private static final ResourceLocation GLOW = Apogee.id("textures/entity/mantis_blades_glow.png");
    private static final ResourceLocation THERMAL_GLOW = Apogee.id("textures/entity/mantis_blades_thermal_glow.png");
    private static final ResourceLocation ELECTRIC_GLOW = Apogee.id("textures/entity/mantis_blades_electric_glow.png");

    private static MantisArmModel model;

    private MantisArmRenderer() {
    }

    public static void clearBakedModel() {
        model = null;
    }

    private static MantisArmModel model() {
        if (model == null) {
            var models = Minecraft.getInstance().getEntityModels();
            model = new MantisArmModel(models.bakeLayer(MantisArmModel.SKIN_LAYER), models.bakeLayer(MantisArmModel.MECHANISM_LAYER));
        }
        return model;
    }

    /**
     * Renders one arm's blade, folded at progress 0 and open at 1. Nothing is drawn unless the
     * player wears Mantis Blades. The pose stack must already be at the arm's pivot, as left by
     * {@code ModelPart.translateAndRotate} on the player model's arm.
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                              boolean right, float progress) {
        MantisVariant variant = MantisClient.variant(player);
        if (variant == null) {
            return;
        }
        MantisArmModel arm = model();
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        arm.setDeployment(progress);

        boolean hot = MantisClient.glowing(player);
        ResourceLocation texture = switch (variant) {
            case THERMAL -> hot ? THERMAL_HOT : THERMAL;
            case ELECTRIC -> hot ? ELECTRIC_HOT : ELECTRIC;
            default -> TEXTURE;
        };
        ResourceLocation glow = switch (variant) {
            case THERMAL -> THERMAL_GLOW;
            case ELECTRIC -> ELECTRIC_GLOW;
            default -> GLOW;
        };
        poseStack.pushPose();
        poseStack.translate(MantisArmModel.centerX(right, slim) / 16.0F, 0.0F, 0.0F);
        float scale = MantisArmModel.MECHANISM_SCALE;
        poseStack.scale(scale * MantisArmModel.widthScale(slim), scale, scale);
        arm.skinFlaps(right).render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture())),
                light, OverlayTexture.NO_OVERLAY);
        arm.mechanism().render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
                hot ? LightTexture.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY);
        // Emissive pass: the heated edge, the electrodes and the eye stay lit in the dark.
        arm.mechanism().render(poseStack, buffers.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
