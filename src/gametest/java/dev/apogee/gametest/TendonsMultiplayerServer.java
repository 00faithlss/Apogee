package dev.apogee.gametest;

import dev.apogee.registry.ApogeeItems;
import dev.apogee.tendons.ReinforcedTendons;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server half of the multiplayer Reinforced Tendons check (tools/tendons_tests.sh):
 * once "Observer" and "Wearer" have joined, the wearer stands with their back to the observer
 * (and then facing them), then drops from 8 blocks five times: a double jump and an air dash
 * forward, left, back and right. The wearer's client makes each move once it is falling; the
 * server detects it from the wearer's movement, announces the phases in chat so both clients can
 * take their screenshots and checks the landing. Disabled unless -Dapogee.tendonsMp=true.
 */
public final class TendonsMultiplayerServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TendonsMultiplayer");
    private static final String[] FLIGHTS = {"jump", "forward", "left", "back", "right"};
    private static final List<String> FAILURES = new ArrayList<>();
    private static final double DROP = 8.0;

    private static int ticks = -1;
    private static double groundY;
    private static int flight = -1;
    /** 0 before the next flight, 1 waiting for the move, 2 filming it, 3 waiting for the landing, 4 after the landing. */
    private static int stage;
    private static int stageTick;
    private static Vec3 lastPos = Vec3.ZERO;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("apogee.tendonsMp")) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(TendonsMultiplayerServer::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerPlayer observer = server.getPlayerList().getPlayerByName("Observer");
        ServerPlayer wearer = server.getPlayerList().getPlayerByName("Wearer");
        if (ticks < 0 && (observer == null || wearer == null)) {
            return;
        }
        ticks++;
        if (observer == null || wearer == null) {
            if (ticks > 20) {
                LOGGER.info("players left, stopping");
                server.halt(false);
            }
            return;
        }
        switch (ticks) {
            case 60 -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                // Creative, so nothing here can hurt the observer; they stand north of the wearer, looking south.
                observer.setGameMode(GameType.CREATIVE);
                observer.teleportTo(level, 0.5, groundY, -2.5, 0.0F, 10.0F);
                wearer.setGameMode(GameType.SURVIVAL);
                wearer.teleportTo(level, 0.5, groundY, 3.5, 0.0F, 10.0F);
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "the tendons equip into the legs slot");
            }
            case 100 -> phase(server, "standing-back");
            case 110 -> wearer.teleportTo(server.overworld(), 0.5, groundY, 3.5, 180.0F, 10.0F);
            case 118 -> phase(server, "standing-front");
            default -> {
            }
        }
        if (ticks < 130) {
            return;
        }
        flights(server, wearer);
    }

    private static void flights(MinecraftServer server, ServerPlayer wearer) {
        stageTick++;
        switch (stage) {
            case 0 -> {
                flight++;
                if (flight >= FLIGHTS.length) {
                    finish(server);
                    return;
                }
                wearer.teleportTo(server.overworld(), 0.5, groundY + DROP, 3.5, 0.0F, 10.0F);
                wearer.setHealth(20.0F);
                wearer.fallDistance = 0.0F;
                lastPos = wearer.position();
                phase(server, "flight:" + FLIGHTS[flight]);
                stage = 1;
                stageTick = 0;
            }
            case 1 -> {
                Vec3 pos = wearer.position();
                boolean moved = FLIGHTS[flight].equals("jump")
                        ? ReinforcedTendons.fallMargin(wearer) > 0.0F
                        : Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z) > 0.6;
                lastPos = pos;
                if (stageTick > 10 && moved) {
                    phase(server, "moved:" + FLIGHTS[flight]);
                    stage = 2;
                    stageTick = 0;
                } else if (stageTick > 200) {
                    check(false, FLIGHTS[flight] + ": the server never saw the move");
                    stage = 0;
                }
            }
            case 2 -> {
                switch (stageTick) {
                    case 2 -> phase(server, "shot-a:" + FLIGHTS[flight]);
                    case 5 -> phase(server, "shot-b:" + FLIGHTS[flight]);
                    case 9 -> phase(server, "shot-c:" + FLIGHTS[flight]);
                    case 10 -> {
                        stage = 3;
                        stageTick = 0;
                    }
                    default -> {
                    }
                }
            }
            case 3 -> {
                if (wearer.onGround() && stageTick > 3) {
                    phase(server, "landed:" + FLIGHTS[flight]);
                    stage = 4;
                    stageTick = 0;
                } else if (stageTick > 300) {
                    check(false, FLIGHTS[flight] + ": the wearer never landed");
                    stage = 0;
                }
            }
            case 4 -> {
                if (stageTick == 8) {
                    // From 8 blocks the landing costs 7 without the margin; only the double jump softens it.
                    if (FLIGHTS[flight].equals("jump")) {
                        check(wearer.getHealth() >= 15.0F, "jump: softer landing, health " + wearer.getHealth() + " (7 damage without the margin)");
                    } else {
                        check(wearer.getHealth() < 20.0F, FLIGHTS[flight] + ": a dash does not soften the fall, health " + wearer.getHealth());
                    }
                    stage = 0;
                }
            }
            default -> {
            }
        }
    }

    private static void finish(MinecraftServer server) {
        if (FAILURES.isEmpty()) {
            LOGGER.info("server: tendons multiplayer run passed");
        } else {
            FAILURES.forEach(failure -> LOGGER.error("server: tendons multiplayer check failed: {}", failure));
        }
        phase(server, "done");
        stage = 5;
        new Thread(() -> {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            server.execute(() -> server.halt(false));
        }).start();
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
        }
        LOGGER.info("check {}: {}", what, ok ? "ok" : "FAILED");
    }

    private static void phase(MinecraftServer server, String name) {
        LOGGER.info("phase {}", name);
        server.getPlayerList().broadcastSystemMessage(Component.literal("[tmp] " + name), false);
    }
}
