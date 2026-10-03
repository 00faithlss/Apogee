package dev.apogee.gametest;

import dev.apogee.client.bloodpump.BloodPumpClient;
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
 * Client half of the multiplayer Blood Pump check. Run with -Dapogee.bpMpRole=observer or wearer
 * and the matching --username: on each "[bp] phase" chat line it checks what this client knows of
 * the wearer's pump and takes a screenshot. The observer looks at the wearer in first person; the
 * wearer looks at themselves from the front in third person.
 */
public final class BloodPumpMultiplayerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BloodPumpMultiplayer");
    private static final List<String> FAILURES = new ArrayList<>();
    private static String role;
    private static String pendingPhase;
    private static int shotDelay = -1;
    private static int stopDelay = -1;

    @Override
    public void onInitializeClient() {
        role = System.getProperty("apogee.bpMpRole");
        if (role == null) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("[bp] ")) {
                pendingPhase = text.substring(5);
                shotDelay = 3;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(BloodPumpMultiplayerClient::tick);
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
        CameraType wanted = "wearer".equals(role) ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON;
        if (mc.options.getCameraType() != wanted) {
            mc.options.setCameraType(wanted);
        }
        mc.options.hideGui = "observer".equals(role);
        if (stopDelay >= 0 && stopDelay-- == 0) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("{}: blood pump multiplayer run passed", role);
            } else {
                FAILURES.forEach(failure -> LOGGER.error("{}: blood pump multiplayer check failed: {}", role, failure));
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
            float elapsed = BloodPumpClient.elapsed(wearer, 0.0F);
            switch (phase) {
                case "idle" -> check(elapsed < 0.0F, "the pump is ready for " + role + ": " + elapsed);
                case "fired", "refilling", "mantis", "mantis-armor", "nanowire" -> check(elapsed >= 0.0F, "the pump shows as fired for " + role + " in " + phase + ": " + elapsed);
                default -> {
                }
            }
            if ("nanowire".equals(phase)) {
                check(MantisClient.variant(wearer) == MantisVariant.NANOWIRE, "nanowire synced to " + role);
            }
            if ("mantis".equals(phase) || "mantis-armor".equals(phase)) {
                check(MantisClient.variant(wearer) == MantisVariant.STANDARD, "mantis blades synced to " + role);
            }
        }
        if ("done".equals(phase)) {
            stopDelay = 20;
            return;
        }
        Screenshot.grab(mc.gameDirectory, "bp-" + role + "-" + phase + ".png", mc.getMainRenderTarget(),
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
