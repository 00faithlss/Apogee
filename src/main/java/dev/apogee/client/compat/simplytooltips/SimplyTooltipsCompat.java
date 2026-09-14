package dev.apogee.client.compat.simplytooltips;

import dev.apogee.client.tooltip.SandevistanTooltipContent;
import dev.apogee.registry.ApogeeItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

/** Only loaded when Simply Tooltips is present. */
public final class SimplyTooltipsCompat {
    public static final String THEME_KEY = "apogee_sandevistan";

    private SimplyTooltipsCompat() {
    }

    public static void register() {
        TooltipProviderRegistry.register(new SandevistanProvider(), 100);
        ClientTickEvents.END_CLIENT_TICK.register(client -> SandevistanTooltipPanel.releaseTabsIfHidden());
    }

    /**
     * Claims the Sandevistan so Simply Tooltips routes it to its renderer, where
     * TooltipRendererMixin draws the Apogee panel. The model below is what Simply Tooltips uses
     * for its own previews and exports.
     */
    private static final class SandevistanProvider implements TooltipProvider {
        @Override
        public boolean supports(ItemStack stack) {
            return stack.is(ApogeeItems.SANDEVISTAN);
        }

        @Override
        public ModernTooltipModel build(ItemStack stack, List<Component> lines, boolean altDown) {
            List<String> ability = SandevistanTooltipContent.descriptionLines().stream().map(Component::getString).toList();
            List<String> body = new ArrayList<>();
            body.add(ModernTooltipModel.SECTION_MARKER + SandevistanTooltipContent.statsHeader().getString());
            for (SandevistanTooltipContent.Stat stat : SandevistanTooltipContent.stats()) {
                body.add(stat.label().getString() + ": " + stat.value().getString());
            }
            body.add(SandevistanTooltipContent.cooldownNote().getString());
            return new ModernTooltipModel(
                    SandevistanTooltipContent.title().getString(),
                    List.of(SandevistanTooltipContent.badge().getString()),
                    TooltipBorderStyle.IRON,
                    ability,
                    body,
                    List.of(),
                    ThemeRegistry.get(THEME_KEY).colors(),
                    null,
                    "",
                    THEME_KEY,
                    null);
        }
    }
}
