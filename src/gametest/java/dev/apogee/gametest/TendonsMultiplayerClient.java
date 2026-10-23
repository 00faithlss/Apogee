package dev.apogee.gametest;

import dev.apogee.client.tendons.TendonsClient;
import dev.apogee.tendons.ReinforcedTendons;
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
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client half of the multiplayer Reinforced Tendons check. Run with -Dapogee.tendonsMpRole=observer
 * or wearer and the matching --username. The observer keeps looking at the wearer and, on each
 * "[tmp] phase" chat line, checks what it sees of them (the implant, the push of each move) and
 * takes a screenshot; the wearer stays in first person and makes each move once it is falling,
 * which is where Punchy's hands show. Disabled without that property.
 */
public final class TendonsMultiplayerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TendonsMultiplayer");
    private static final List<String> FAILURES = new ArrayList<>();
    private static String role;
    private static final List<String> PHASES = new ArrayList<>();
    /** The move the wearer is waiting to make, and for how many ticks it has been waiting. */
    private static String pendingMove;
    private static int pendingTicks;
    private static Vec3 movedFrom;
    /** Lowest height the observer saw the wearer at since the move was announced: the double jump is the rise from there. */
    private static double lowestY = Double.MAX_VALUE;
    /** The wearer's own view of the move it just made: its speed on the next ticks. */
    private static String moveWatch;
    private static int moveWatchTicks;
    /** The move the observer is measuring and the most it has seen the wearer pushed that way since it was announced. */
    private static String trackedMove;
    private static double bestPush;
    private static int stopDelay = -1;

    @Override
    public void onInitializeClient() {
        role = System.getProperty("apogee.tendonsMpRole");
        if (role == null) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("[tmp] ")) {
                PHASES.add(text.substring(6));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(TendonsMultiplayerClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        boolean observer = "observer".equals(role);
        mc.options.hideGui = observer;
        Player wearer = observer ? findWearer(mc) : mc.player;
        if (observer && wearer != null) {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, wearer.getEyePosition());
            lowestY = Math.min(lowestY, wearer.getY());
            if (movedFrom != null && trackedMove != null) {
                bestPush = Math.max(bestPush, push(wearer, trackedMove));
            }
        }
        if (stopDelay >= 0 && stopDelay-- == 0) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("{}: tendons multiplayer run passed", role);
            } else {
                FAILURES.forEach(failure -> LOGGER.error("{}: tendons multiplayer check failed: {}", role, failure));
            }
            mc.stop();
            return;
        }
        if (!observer && moveWatch != null && ++moveWatchTicks == 2) {
            Vec3 v = mc.player.getDeltaMovement();
            boolean jump = moveWatch.equals("jump");
            LOGGER.info("wearer {} speed two ticks in: vy={} horizontal={}", moveWatch, v.y, v.horizontalDistance());
            check(jump ? v.y > 0.3 : v.horizontalDistance() > 0.6, "the wearer's own client makes the " + moveWatch + " (vy " + v.y + ", horizontal " + v.horizontalDistance() + ")");
            moveWatch = null;
        }
        if (!observer && pendingMove != null) {
            pendingTicks++;
            if (pendingTicks > 4 && !mc.player.onGround() && mc.player.getDeltaMovement().y < -0.3) {
                String move = pendingMove;
                pendingMove = null;
                moveWatch = move;
                moveWatchTicks = 0;
                if (move.equals("jump")) {
                    TendonsClient.simulateJumpPressForTests();
                } else {
                    TendonsClient.simulateDashPressForTests(
                            move.equals("forward") ? 1.0F : move.equals("back") ? -1.0F : 0.0F,
                            move.equals("left") ? 1.0F : move.equals("right") ? -1.0F : 0.0F);
                }
            }
        }
        if (PHASES.isEmpty()) {
            return;
        }
        String phase = PHASES.remove(0);
        handle(mc, phase, wearer);
    }

    private static void handle(Minecraft mc, String phase, Player wearer) {
        String name = phase.contains(":") ? phase.substring(phase.indexOf(':') + 1) : "";
        String kind = phase.contains(":") ? phase.substring(0, phase.indexOf(':')) : phase;
        check(wearer != null, "wearer visible in " + phase + " for " + role);
        if (wearer == null) {
            return;
        }
        switch (kind) {
            case "standing-back", "standing-front" ->
                    check(ReinforcedTendons.isEquipped(wearer), "the wearer's tendons are synced to " + role + " in " + phase);
            case "flight" -> {
                if ("wearer".equals(role)) {
                    pendingMove = name;
                    pendingTicks = 0;
                }
                return;
            }
            case "moved" -> {
                lowestY = Double.MAX_VALUE;
                movedFrom = wearer.position();
                trackedMove = name;
                bestPush = 0.0;
                return;
            }
            // Judged when it lands, from the most the observer saw the wearer pushed that way: the
            // frames in between depend on how busy the machine is.
            case "landed" -> {
                trackedMove = null;
                if ("observer".equals(role)) {
                    check(bestPush > (name.equals("jump") ? 0.5 : 1.5), name + ": observer sees the wearer move the right way (" + bestPush + ")");
                }
                return;
            }
            default -> {
            }
        }
        if (kind.equals("done")) {
            stopDelay = 20;
            return;
        }
        String file = "tmp-" + role + "-" + kind + (name.isEmpty() ? "" : "-" + name) + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> LOGGER.info("{} screenshot {}", role, file));
    }

    /** How far the wearer has moved the way {@code move} pushes: up for the jump, else along the wearer's heading (yaw 0: forward is +z, left is +x). */
    private static double push(Player wearer, String move) {
        Vec3 moved = wearer.position().subtract(movedFrom);
        return switch (move) {
            case "jump" -> wearer.getY() - lowestY;
            case "forward" -> moved.z;
            case "back" -> -moved.z;
            case "left" -> moved.x;
            default -> -moved.x;
        };
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
