package dev.apogee.gametest;

import dev.apogee.registry.ApogeeItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.render.TabState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client half of the combined develop check. Run with -Dapogee.devMpRole=observer or wearer. The
 * wearer keeps its HUD and looks at itself from the front; the observer hides the HUD and looks at
 * the wearer. On "[dv] tooltips" the wearer films every tooltip, description then stats (G).
 */
public final class DevelopMultiplayerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("DevelopMultiplayer");
    private static final Item[] TOOLTIPS = {ApogeeItems.SANDEVISTAN, ApogeeItems.BERSERK_ZETATECH, ApogeeItems.BLOOD_PUMP,
            ApogeeItems.REINFORCED_TENDONS, ApogeeItems.MANTIS_BLADES, ApogeeItems.NANOWIRE_WHITE};
    private static final int PER_ITEM = 24;
    private static String role;
    private static String pendingPhase;
    private static int shotDelay = -1;
    private static int stopDelay = -1;
    private static int tooltipTick = -1;
    private static Item tooltipItem;
    private static final List<String> FAILURES = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        role = System.getProperty("apogee.devMpRole");
        if (role == null) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("[dv] ")) {
                pendingPhase = text.substring(5);
                shotDelay = 3;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(DevelopMultiplayerClient::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (tooltipItem != null) {
                graphics.renderTooltip(Minecraft.getInstance().font, new ItemStack(tooltipItem), 24, 40);
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (mc.player == null) {
            return;
        }
        boolean wearer = "wearer".equals(role);
        CameraType wanted = wearer ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON;
        if (mc.options.getCameraType() != wanted && tooltipTick < 0) {
            mc.options.setCameraType(wanted);
        }
        mc.options.hideGui = !wearer;
        // The announcements would sit on top of the HUD squares being checked.
        mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
        mc.getToasts().clear();
        if (stopDelay >= 0 && stopDelay-- == 0) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("{}: develop multiplayer run passed", role);
            } else {
                FAILURES.forEach(failure -> LOGGER.error("{}: develop multiplayer check failed: {}", role, failure));
            }
            mc.stop();
            return;
        }
        if (tooltipTick >= 0) {
            tooltipTick(mc);
        }
        if (shotDelay < 0 || shotDelay-- > 0) {
            return;
        }
        String phase = pendingPhase;
        if ("done".equals(phase)) {
            stopDelay = 20;
            return;
        }
        if ("tooltips".equals(phase)) {
            if (wearer) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                tooltipTick = 0;
            }
            return;
        }
        shot(mc, phase);
    }

    private static void tooltipTick(Minecraft mc) {
        int index = tooltipTick / PER_ITEM;
        int t = tooltipTick % PER_ITEM;
        if (index >= TOOLTIPS.length) {
            tooltipItem = null;
            tooltipTick = -1;
            return;
        }
        String name = BuiltInRegistries.ITEM.getKey(TOOLTIPS[index]).getPath();
        switch (t) {
            case 0 -> tooltipItem = TOOLTIPS[index];
            case 8 -> shot(mc, "tooltip-" + name + "-description");
            case 9 -> TabState.cycleNext();
            case 16 -> shot(mc, "tooltip-" + name + "-stats");
            default -> {
            }
        }
        tooltipTick++;
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "dv-" + role + "-" + name + ".png", mc.getMainRenderTarget(),
                message -> LOGGER.info("{} screenshot {}", role, name));
    }
}
