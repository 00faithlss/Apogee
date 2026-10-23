package dev.apogee.client.armor;

import dev.apogee.Apogee;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Muestra cada pieza como modelo 3D en inventario, mano y suelo, con la misma textura y brillo que al vestirla. */
public final class ArmorItemRenderer<T extends ArmorItem & GeoItem> extends GeoItemRenderer<T> {
    private final boolean translucent;

    public ArmorItemRenderer(Function<T, String> geometry, ResourceLocation texture,
                             ResourceLocation animations, boolean translucent) {
        super(new GeoModel<T>() {
            @Override
            public ResourceLocation getModelResource(T item) {
                return Apogee.id("geo/item/" + geometry.apply(item) + ".geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(T item) {
                return texture;
            }

            @Override
            public ResourceLocation getAnimationResource(T item) {
                return animations;
            }
        });
        this.translucent = translucent;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(T item, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
        return translucent ? RenderType.entityTranslucent(texture) : super.getRenderType(item, texture, buffers, partialTick);
    }
}
