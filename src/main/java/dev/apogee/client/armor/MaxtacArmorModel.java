package dev.apogee.client.armor;

import dev.apogee.Apogee;
import dev.apogee.item.MaxtacArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MaxtacArmorModel extends GeoModel<MaxtacArmorItem> {
    public static final ResourceLocation ACTIVE = Apogee.id("textures/armor/maxtac.png");
    public static final ResourceLocation STANDBY = Apogee.id("textures/armor/maxtac_standby.png");
    public static final ResourceLocation ACTIVE_GLOW = Apogee.id("textures/armor/maxtac_glowmask.png");
    public static final ResourceLocation STANDBY_GLOW = Apogee.id("textures/armor/maxtac_standby_glowmask.png");

    @Override
    public ResourceLocation getModelResource(MaxtacArmorItem item) {
        return Apogee.id(item.isClosed() ? "geo/maxtac_casco_cerrado.geo.json" : "geo/maxtac.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MaxtacArmorItem item) {
        return STANDBY;
    }

    @Override
    public ResourceLocation getAnimationResource(MaxtacArmorItem item) {
        return Apogee.id("animations/maxtac.animation.json");
    }
}
