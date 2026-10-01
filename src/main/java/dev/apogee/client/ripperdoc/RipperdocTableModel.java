package dev.apogee.client.ripperdoc;

import dev.apogee.Apogee;
import dev.apogee.ripperdoc.RipperdocTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** The surgery couch of Project: Edgerunner's ripperdoc set (see LICENSE_ASSETS.md). */
public final class RipperdocTableModel extends GeoModel<RipperdocTableBlockEntity> {
    private static final ResourceLocation GEO = Apogee.id("geo/ripperdoc_table.geo.json");
    private static final ResourceLocation TEXTURE = Apogee.id("textures/block/ripperdoc_table.png");
    private static final ResourceLocation ANIMATIONS = Apogee.id("animations/ripperdoc_table.animation.json");

    @Override
    public ResourceLocation getModelResource(RipperdocTableBlockEntity animatable) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(RipperdocTableBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RipperdocTableBlockEntity animatable) {
        return ANIMATIONS;
    }
}
