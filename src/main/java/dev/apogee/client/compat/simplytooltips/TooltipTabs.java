package dev.apogee.client.compat.simplytooltips;

import dev.apogee.Apogee;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.TabState;

/**
 * Description / Stats views of the Apogee tooltips, switched with Simply Tooltips' own cycle key
 * (G by default). Shared by every Apogee panel.
 */
final class TooltipTabs {
    private static final ResourceLocation RESET_KEY = Apogee.id("tooltip_left");
    private static final List<TabState.Tab> TABS = List.of(TabState.Tab.LORE, TabState.Tab.STATS);
    private static final long HIDDEN_AFTER_MS = 150L;
    private static long lastRenderMs;
    private static ResourceLocation lastItem;
    private static boolean ownsTabs;

    /** Which parts of a panel to draw this frame. */
    record View(boolean tabsEnabled, boolean statsView) {
        boolean showDescription() {
            return !tabsEnabled || !statsView;
        }

        boolean showStats() {
            return !tabsEnabled || statsView;
        }
    }

    private TooltipTabs() {
    }

    static View update(ItemStack stack) {
        boolean tabsEnabled = TooltipNavigationConfig.tooltipTabs();
        long now = Util.getMillis();
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (tabsEnabled) {
            if (now - lastRenderMs > HIDDEN_AFTER_MS || !item.equals(lastItem)) {
                // Hovering away and back, or onto another item, always opens on the description.
                TabState.notifyItem(RESET_KEY, TABS);
            }
            TabState.notifyItem(item, TABS);
            ownsTabs = true;
        }
        lastRenderMs = now;
        lastItem = item;
        return new View(tabsEnabled, tabsEnabled && TabState.activeTab() == TabState.Tab.STATS);
    }

    /** Clears the shared tab list once an Apogee tooltip is gone so G is not swallowed elsewhere. */
    static void releaseIfHidden() {
        if (ownsTabs && Util.getMillis() - lastRenderMs > HIDDEN_AFTER_MS) {
            TabState.notifyItem(RESET_KEY, List.of());
            ownsTabs = false;
        }
    }
}
