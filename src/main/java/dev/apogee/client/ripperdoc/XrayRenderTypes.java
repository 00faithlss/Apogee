package dev.apogee.client.ripperdoc;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The patient's skin in the ripperdoc's x-ray view: translucent and tinted, and without writing
 * depth, so the cyberware under it shows through from any side.
 */
public final class XrayRenderTypes extends RenderType {
    /** Tint and opacity of the skin under x-ray (about a third opaque). */
    public static final int SKIN_COLOR = 0x557CE0EF;

    private static final Function<ResourceLocation, RenderType> SKIN = Util.memoize(texture -> create("apogee_xray_skin",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)));

    private XrayRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType skin(ResourceLocation texture) {
        return SKIN.apply(texture);
    }
}
