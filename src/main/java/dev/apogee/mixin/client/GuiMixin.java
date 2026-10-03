package dev.apogee.mixin.client;

import dev.apogee.client.fx.BerserkEffects;
import dev.apogee.client.fx.SandevistanEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void apogee$screenOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        SandevistanEffects.renderScreenOverlay(graphics, partialTick);
        BerserkEffects.renderScreenOverlay(graphics, partialTick);
    }
}
