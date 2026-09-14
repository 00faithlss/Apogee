package dev.apogee.mixin.client.compat;

import dev.apogee.client.compat.simplytooltips.SandevistanTooltipPanel;
import dev.apogee.registry.ApogeeItems;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.client.render.TooltipRenderState;
import net.sweenus.simplytooltips.client.render.TooltipRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Apogee panel for the Sandevistan in place of Simply Tooltips' default layout. Only
 * applied when Simply Tooltips is loaded (see ApogeeMixinPlugin). Theme Studio previews and
 * exports (non-null render state) keep Simply Tooltips' renderer and the registered provider.
 * Simply Tooltips ships intermediary names, while development runs remap it to Mojang names,
 * so both descriptors are listed.
 */
@Mixin(value = TooltipRenderer.class, remap = false)
public abstract class SimplyTooltipsRendererMixin {
    @Inject(
            method = {
                    "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Lnet/sweenus/simplytooltips/api/TooltipProvider;Ljava/util/List;IIII)V",
                    "render(Lnet/minecraft/class_332;Lnet/minecraft/class_327;Lnet/minecraft/class_1799;Ljava/util/List;Lnet/sweenus/simplytooltips/api/TooltipProvider;Ljava/util/List;IIII)V"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void apogee$renderSandevistan(GuiGraphics graphics, Font font, ItemStack stack, List<Component> rawLines,
                                                 TooltipProvider provider, List<ClientTooltipComponent> nativeComponents,
                                                 int x, int y, int screenW, int screenH, CallbackInfo ci) {
        if (stack.is(ApogeeItems.SANDEVISTAN) && TooltipRenderState.current() == null) {
            SandevistanTooltipPanel.render(graphics, font, stack, x, y, screenW, screenH);
            ci.cancel();
        }
    }
}
