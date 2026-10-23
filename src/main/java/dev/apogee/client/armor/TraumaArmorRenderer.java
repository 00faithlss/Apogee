package dev.apogee.client.armor;

import dev.apogee.item.TraumaArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class TraumaArmorRenderer extends GeoArmorRenderer<TraumaArmorItem> {
    public TraumaArmorRenderer() {
        super(new TraumaArmorModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TraumaArmorItem item) {
        return TraumaHelmetClient.indicatorLit(getCurrentEntity()) ? TraumaArmorModel.LIT : TraumaArmorModel.UNLIT;
    }

    @Override
    public RenderType getRenderType(TraumaArmorItem item, ResourceLocation texture,
                                   @Nullable MultiBufferSource buffers, float partialTick) {
        // El visor interno conserva su transparencia mientras las placas permanecen opacas.
        return RenderType.entityTranslucent(texture);
    }
}
