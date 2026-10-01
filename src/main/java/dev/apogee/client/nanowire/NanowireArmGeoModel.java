package dev.apogee.client.nanowire;

import dev.apogee.Apogee;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** The housing export for each forearm, centred on its own arm. */
public final class NanowireArmGeoModel extends GeoModel<NanowireArm> {
    public static final ResourceLocation TEXTURE = Apogee.id("textures/nanowire/nanowire_red.png");
    public static final ResourceLocation GLOWMASK = Apogee.id("textures/nanowire/nanowire_red_glowmask.png");
    public static final ResourceLocation WHITE_TEXTURE = Apogee.id("textures/nanowire/nanowire_white.png");
    public static final ResourceLocation WHITE_GLOWMASK = Apogee.id("textures/nanowire/nanowire_white_glowmask.png");

    public static ResourceLocation texture(MantisVariant variant) {
        return variant == MantisVariant.NANOWIRE_WHITE ? WHITE_TEXTURE : TEXTURE;
    }

    public static ResourceLocation glowmask(MantisVariant variant) {
        return variant == MantisVariant.NANOWIRE_WHITE ? WHITE_GLOWMASK : GLOWMASK;
    }

    private static final ResourceLocation RIGHT_MODEL = Apogee.id("geo/nanowire_right_attachment.geo.json");
    private static final ResourceLocation LEFT_MODEL = Apogee.id("geo/nanowire_left_attachment.geo.json");
    private static final ResourceLocation RIGHT_ANIMATIONS = Apogee.id("animations/nanowire_right_attachment.animation.json");
    private static final ResourceLocation LEFT_ANIMATIONS = Apogee.id("animations/nanowire_left_attachment.animation.json");

    @Override
    public ResourceLocation getModelResource(NanowireArm arm) {
        return arm.right() ? RIGHT_MODEL : LEFT_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NanowireArm arm) {
        return texture(arm.variant());
    }

    @Override
    public ResourceLocation getAnimationResource(NanowireArm arm) {
        return arm.right() ? RIGHT_ANIMATIONS : LEFT_ANIMATIONS;
    }

    @Override
    public RenderType getRenderType(NanowireArm arm, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
