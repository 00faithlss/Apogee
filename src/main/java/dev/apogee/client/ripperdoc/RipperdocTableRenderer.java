package dev.apogee.client.ripperdoc;

import dev.apogee.ripperdoc.RipperdocTableBlockEntity;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the couch from its foot half, the raised back over the head half. The model's back points
 * the opposite way to GeckoLib's facing, so it is turned round; without it the back hung past the
 * foot, outside the table's two blocks.
 */
public final class RipperdocTableRenderer extends GeoBlockRenderer<RipperdocTableBlockEntity> {
    public RipperdocTableRenderer() {
        super(new RipperdocTableModel());
    }

    @Override
    protected Direction getFacing(RipperdocTableBlockEntity table) {
        return super.getFacing(table).getOpposite();
    }
}
