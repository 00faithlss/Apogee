package dev.apogee.gametest;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
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
 * Client half of the multiplayer Mantis Blades check. Run with -Dapogee.mpRole=observer or
 * wearer and the matching --username: on each "[mp] phase" chat line from the server it checks
 * what this client sees of the wearer and takes a screenshot. The observer looks at the wearer;
 * the wearer stays in first person, where both blades must show once deployed.
 */
public final class MultiplayerSmokeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiplayerSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static String role;
    private static String pendingPhase;
    private static int shotDelay = -1;
    private static int stopDelay = -1;

    @Override
    public void onInitializeClient() {
        role = System.getProperty("apogee.mpRole");
        if (role == null) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("[mp] ")) {
                pendingPhase = text.substring(5);
                shotDelay = 3;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(MultiplayerSmokeClient::tick);
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
        // The observer only needs the view; the wearer keeps the GUI on, since F1 also hides hands.
        mc.options.hideGui = "observer".equals(role);
        if (stopDelay >= 0 && stopDelay-- == 0) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("{}: multiplayer smoke run passed", role);
            } else {
                FAILURES.forEach(failure -> LOGGER.error("{}: multiplayer check failed: {}", role, failure));
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
            boolean deployed = MantisClient.isDeployed(wearer);
            switch (phase) {
                case "folded" -> {
                    check(MantisClient.variant(wearer) == MantisVariant.THERMAL, "thermal Mantis Blades synced to " + role);
                    check(!deployed, "folded for " + role);
                }
                case "open", "attack", "thrust" -> check(deployed, "deployed for " + role + " in " + phase);
                case "retracted" -> check(!deployed, "retracted for " + role);
                default -> {
                }
            }
        }
        if ("done".equals(phase)) {
            stopDelay = 20;
            return;
        }
        Screenshot.grab(mc.gameDirectory, "mp-" + role + "-" + phase + ".png", mc.getMainRenderTarget(),
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
