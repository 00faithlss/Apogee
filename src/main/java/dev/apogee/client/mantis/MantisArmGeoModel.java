package dev.apogee.client.mantis;

import dev.apogee.Apogee;
import java.util.Locale;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Picks the geometry for the arm's variant and side; each variant has its own hardware. */
public final class MantisArmGeoModel extends GeoModel<MantisArm> {
    private static final ResourceLocation RIGHT_ANIMATIONS = Apogee.id("animations/mantis_right.animation.json");
    private static final ResourceLocation LEFT_ANIMATIONS = Apogee.id("animations/mantis_left.animation.json");

    private static String variantName(MantisArm arm) {
        return switch (arm.variant()) {
            case THERMAL -> "thermal";
            case ELECTRIC -> "electric";
            default -> "normal";
        };
    }

    @Override
    public ResourceLocation getModelResource(MantisArm arm) {
        return Apogee.id(String.format(Locale.ROOT, "geo/mantis_%s_%s.geo.json", variantName(arm), arm.right() ? "right" : "left"));
    }

    @Override
    public ResourceLocation getTextureResource(MantisArm arm) {
        return Apogee.id("textures/mantis/mantis_" + variantName(arm) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MantisArm arm) {
        return arm.right() ? RIGHT_ANIMATIONS : LEFT_ANIMATIONS;
    }

    @Override
    public RenderType getRenderType(MantisArm arm, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
