package dev.apogee.gametest;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.ripperdoc.RipperdocMenu;
import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.ClickType;
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
 * the wearer stays in first person, where both blades must show once deployed. On the ripperdoc
 * table the wearer swaps the Mantis Blades for the nanowire with real clicks and turns x-ray on;
 * the observer checks that they lie on the couch, in the sleeping pose.
 */
public final class MultiplayerSmokeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiplayerSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static String role;
    private static String pendingPhase;
    private static int shotDelay = -1;
    private static int stopDelay = -1;
    /** Ticks since the wearer was laid on the couch, while the wearer works the ripperdoc screen. */
    private static int surgeryTicks = -1;

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
        if (surgeryTicks >= 0) {
            surgery(mc, surgeryTicks++);
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
                case "couch" -> {
                    check(wearer.getVehicle() instanceof RipperdocSeat, "the wearer lies on the couch for " + role);
                    check(wearer.hasPose(Pose.SLEEPING), "the wearer is in the sleeping pose for " + role);
                    if ("wearer".equals(role)) {
                        check(mc.screen instanceof RipperdocScreen, "the ripperdoc screen opened for the wearer");
                        surgeryTicks = 0;
                    }
                }
                case "fitted" -> check(MantisClient.variant(wearer) == MantisVariant.NANOWIRE,
                        "the nanowire is fitted for " + role + ": " + MantisClient.variant(wearer));
                case "getup" -> {
                    check(!(wearer.getVehicle() instanceof RipperdocSeat), "the wearer got up for " + role);
                    check(!wearer.hasPose(Pose.SLEEPING), "the wearer stands again for " + role);
                }
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

    /** The wearer's own clicks on the ripperdoc screen, sent to the server like a player's. */
    private static void surgery(Minecraft mc, int t) {
        if (!(mc.screen instanceof RipperdocScreen screen)) {
            if (t == 5) {
                check(false, "the ripperdoc screen stays open for the wearer");
            }
            return;
        }
        RipperdocMenu menu = screen.getMenu();
        int arms = RipperdocMenu.slotIndex(RipperdocMenu.Implant.ARMS, 0);
        switch (t) {
            // Shift-click the Mantis Blades out of the arms slot, then the nanowire in.
            case 5 -> mc.gameMode.handleInventoryMouseClick(menu.containerId, arms, 0, ClickType.QUICK_MOVE, mc.player);
            case 15 -> {
                check(!menu.getSlot(arms).hasItem(), "the Mantis Blades came out of the arms slot");
                for (int i = menu.slots.size() - 36; i < menu.slots.size(); i++) {
                    if (menu.getSlot(i).getItem().is(ApogeeItems.NANOWIRE)) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                        break;
                    }
                }
            }
            case 25 -> {
                // The x-ray button, clicked where it is on the screen.
                double x = (mc.getWindow().getGuiScaledWidth() - RipperdocMenu.WIDTH) / 2.0 + 88;
                double y = (mc.getWindow().getGuiScaledHeight() - RipperdocMenu.HEIGHT) / 2.0 + 119;
                screen.mouseClicked(x, y, 0);
                screen.mouseReleased(x, y, 0);
            }
            case 30 -> {
                check(menu.getSlot(arms).getItem().is(ApogeeItems.NANOWIRE), "the nanowire sits in the arms slot");
                check(RipperdocScreen.xray(), "the x-ray button works for the wearer");
                Screenshot.grab(mc.gameDirectory, "mp-wearer-xray.png", mc.getMainRenderTarget(), message -> LOGGER.info("wearer screenshot xray"));
            }
            case 31 -> surgeryTicks = -1;
            default -> {
            }
        }
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
