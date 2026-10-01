package dev.apogee.client.ripperdoc;

import dev.apogee.ripperdoc.RipperdocTableBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Draws the couch from its foot half, the raised back over the head half. */
public final class RipperdocTableRenderer extends GeoBlockRenderer<RipperdocTableBlockEntity> {
    public RipperdocTableRenderer() {
        super(new RipperdocTableModel());
    }
}
