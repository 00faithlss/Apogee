package dev.apogee.gametest;

import dev.apogee.berserk.BerserkManager;
import dev.apogee.client.ApogeeClientState;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client half of the multiplayer Berserk check. Run with -Dapogee.mpBerserk=true, -Dapogee.mpRole=observer
 * or wearer and the matching --username: on each "[mp] phase" chat line from the server it checks
 * what this client sees of the wearer (the implant on their back, for the observer; the Berserk
 * state and HUD, for the wearer) and takes a screenshot. The wearer holds sneak while airborne,
 * like a player starting the superhero landing.
 */
public final class BerserkMultiplayerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BerserkMultiplayer");
    private static final List<String> FAILURES = new ArrayList<>();
    private static String role;
    private static String pendingPhase;
    private static int shotDelay = -1;
    private static int stopDelay = -1;

    @Override
    public void onInitializeClient() {
        role = System.getProperty("apogee.mpRole");
        if (role == null || !Boolean.getBoolean("apogee.mpBerserk")) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("[mp] ")) {
                pendingPhase = text.substring(5);
                shotDelay = "slam".equals(pendingPhase) ? 2 : 3;
                if ("wearer".equals(role)) {
                    Minecraft mc = Minecraft.getInstance();
                    if ("airborne".equals(pendingPhase)) {
                        mc.options.keyShift.setDown(true);
                    } else if ("slam".equals(pendingPhase)) {
                        mc.options.keyShift.setDown(false);
                    }
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(BerserkMultiplayerClient::tick);
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
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        // The observer only needs the view; the wearer keeps the GUI on for the Berserk's HUD.
        mc.options.hideGui = "observer".equals(role);
        if (stopDelay >= 0 && stopDelay-- == 0) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("{}: berserk multiplayer run passed", role);
            } else {
                FAILURES.forEach(failure -> LOGGER.error("{}: berserk multiplayer check failed: {}", role, failure));
            }
            mc.stop();
            return;
        }
        if (shotDelay < 0 || shotDelay-- > 0) {
            return;
        }
        String phase = pendingPhase;
        Player wearer = "wearer".equals(role) ? mc.player : findWearer(mc);
        check(wearer != null, "wearer visible in " + phase);
        if (wearer != null) {
            boolean installed = TrinketsApi.getTrinketComponent(wearer)
                    .map(component -> component.isEquipped(stack -> stack.getItem() instanceof dev.apogee.item.BerserkItem))
                    .orElse(false);
            switch (phase) {
                case "front", "back" -> check(installed, "the Berserk implant is synced to " + role);
                case "active" -> {
                    if ("wearer".equals(role)) {
                        check(ApogeeClientState.berserkStatus().active(), "the wearer's client sees the Berserk active");
                    }
                }
                case "slam" -> {
                    if ("wearer".equals(role)) {
                        check(!mc.options.keyShift.isDown(), "the wearer let go of sneak");
                    }
                }
                case "cooldown" -> {
                    if ("wearer".equals(role)) {
                        check(!ApogeeClientState.berserkStatus().active(), "the wearer's client sees the Berserk ended");
                        check(ApogeeClientState.berserkStatus().cooldownEnd() > ApogeeClientState.clientGameTime(), "the wearer's client sees the cooldown");
                    }
                }
                default -> {
                }
            }
        }
        if ("done".equals(phase)) {
            stopDelay = 20;
            return;
        }
        Screenshot.grab(mc.gameDirectory, "bmp-" + role + "-" + phase + ".png", mc.getMainRenderTarget(),
                message -> LOGGER.info("{} screenshot {}", role, phase));
    }

    private static Player findWearer(Minecraft mc) {
        for (Player player : mc.level.players()) {
            if ("Wearer".equals(player.getGameProfile().getName())) {
                return player;
            }
        }
        return null;
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
        }
        LOGGER.info("check {}: {}", what, ok ? "ok" : "FAILED");
    }
}
