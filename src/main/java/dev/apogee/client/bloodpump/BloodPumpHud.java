package dev.apogee.client.bloodpump;

import dev.apogee.bloodpump.BloodPumpRules;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.client.fx.CyberwareHudSlot;
import dev.apogee.client.fx.SandevistanEffects;
import dev.apogee.client.mantis.ArmCyberwareHud;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * HUD square of the Blood Pump: cyan when ready, magenta with the seconds left while it
 * recharges. It stacks over the Sandevistan's and the arm cyberware's squares when they show.
 */
public final class BloodPumpHud {
    private static float alpha;

    private BloodPumpHud() {
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean shown = player != null && TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(ApogeeItems.BLOOD_PUMP)).orElse(false);
        alpha = Mth.clamp(alpha + (shown ? 0.25F : -0.125F), 0.0F, 1.0F);
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || alpha <= 0.0F || mc.options.hideGui || !ApogeeClientConfig.get().hud) {
            return;
        }
        float left = BloodPumpClient.cooldownLeft(player, partialTick);
        float fill = left / BloodPumpRules.COOLDOWN_TICKS;
        int accent = left > 0.0F ? CyberwareHudSlot.MAGENTA : CyberwareHudSlot.CYAN;
        float step = CyberwareHudSlot.SIZE + 6;
        float lift = (SandevistanEffects.hudAlpha() + ArmCyberwareHud.alpha()) * step;
        int x = 6;
        int y = Math.round(graphics.guiHeight() - CyberwareHudSlot.SIZE - 6 - lift);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, (1.0F - alpha) * 6.0F, 0.0F);
        CyberwareHudSlot.draw(graphics, x, y, alpha, fill, CyberwareHudSlot.MAGENTA, accent, new ItemStack(ApogeeItems.BLOOD_PUMP));
        CyberwareHudSlot.seconds(graphics, x, y, left, 1.0F, accent, alpha);
        graphics.pose().popPose();
    }
}
