package dev.apogee.client.mantis;

import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.client.fx.CyberwareHudSlot;
import dev.apogee.client.fx.SandevistanEffects;
import dev.apogee.item.ArmCyberwareItem;
import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * HUD square of the arm cyberware's right-click attack (the Mantis Blades' thrust, the nanowire's
 * two-wire lash), like the Sandevistan's: shown while the implant is out, its fill draining in
 * magenta with the seconds left while the attack recharges, cyan once it is ready. It stacks
 * over the Sandevistan's square when both show.
 */
public final class ArmCyberwareHud {
    private static float alpha;

    private ArmCyberwareHud() {
    }

    /** How visible the arm cyberware's square is, 0 to 1. */
    public static float alpha() {
        return alpha;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean shown = player != null && (MantisClient.isDeployed(player) || MantisClient.heavyCooldownLeft(0.0F) > 0.0F)
                && MantisClient.variant(player) != null;
        alpha = Mth.clamp(alpha + (shown ? 0.25F : -0.125F), 0.0F, 1.0F);
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || alpha <= 0.0F || mc.options.hideGui || !ApogeeClientConfig.get().hud) {
            return;
        }
        MantisVariant variant = MantisClient.variant(player);
        ItemStack icon = TrinketsApi.getTrinketComponent(player)
                .flatMap(component -> component.getEquipped(stack -> stack.getItem() instanceof ArmCyberwareItem).stream().findFirst())
                .map(pair -> pair.getB())
                .orElse(ItemStack.EMPTY);
        if (variant == null || icon.isEmpty()) {
            return;
        }
        float left = MantisClient.heavyCooldownLeft(partialTick);
        float fill = left / Math.max(1, MantisRules.thrustCooldownTicks(variant));
        int accent = left > 0.0F ? CyberwareHudSlot.MAGENTA : CyberwareHudSlot.CYAN;
        int x = 6;
        // Over the Sandevistan's square while that one shows.
        float lift = SandevistanEffects.hudAlpha() * (CyberwareHudSlot.SIZE + 6);
        int y = Math.round(graphics.guiHeight() - CyberwareHudSlot.SIZE - 6 - lift);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, (1.0F - alpha) * 6.0F, 0.0F);
        CyberwareHudSlot.draw(graphics, x, y, alpha, fill, CyberwareHudSlot.MAGENTA, accent, icon);
        CyberwareHudSlot.seconds(graphics, x, y, left, 1.0F, accent, alpha);
        graphics.pose().popPose();
    }
}
