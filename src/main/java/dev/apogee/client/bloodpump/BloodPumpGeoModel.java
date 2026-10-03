package dev.apogee.client.bloodpump;

import dev.apogee.Apogee;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BloodPumpGeoModel extends GeoModel<BloodPumpArm> {
    @Override
    public ResourceLocation getModelResource(BloodPumpArm arm) {
        return Apogee.id("geo/blood_pump.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloodPumpArm arm) {
        return Apogee.id("textures/blood_pump/blood_pump.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloodPumpArm arm) {
        return Apogee.id("animations/blood_pump.animation.json");
    }

    @Override
    public RenderType getRenderType(BloodPumpArm arm, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
