package dev.apogee.client.armor;

import dev.apogee.item.MaxtacArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class MaxtacArmorRenderer extends GeoArmorRenderer<MaxtacArmorItem> {
    public MaxtacArmorRenderer() {
        super(new MaxtacArmorModel());
        // GeckoLib separa los píxeles emisivos de la base para evitar que la armadura los tape.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private boolean auxiliaryActive() {
        return MaxtacVisorClient.auxiliaryActive(getCurrentEntity());
    }

    @Override
    public ResourceLocation getTextureLocation(MaxtacArmorItem item) {
        return auxiliaryActive() ? MaxtacArmorModel.ACTIVE : MaxtacArmorModel.STANDBY;
    }
}
