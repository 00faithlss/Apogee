package dev.apogee.gametest;

import dev.apogee.bloodpump.BloodPumpManager;
import dev.apogee.mantis.MantisManager;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server half of the multiplayer Blood Pump check (tools/bloodpump_tests.sh): the players
 * "Observer" and "Wearer" face each other, the wearer fits the pump, fires it, then fits the Mantis
 * Blades and an iron chestplate, and every step is announced in chat as "[bp] phase" so both
 * clients can check what they see and take a screenshot. Disabled unless -Dapogee.bpMpSmoke=true.
 */
public final class BloodPumpMultiplayerServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BloodPumpMultiplayer");
    private static final java.util.List<String> FAILURES = new java.util.ArrayList<>();
    private static int ticksTogether = -1;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("apogee.bpMpSmoke")) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(BloodPumpMultiplayerServer::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerPlayer observer = server.getPlayerList().getPlayerByName("Observer");
        ServerPlayer wearer = server.getPlayerList().getPlayerByName("Wearer");
        if (ticksTogether < 0 && (observer == null || wearer == null)) {
            return;
        }
        ticksTogether++;
        if (observer == null || wearer == null) {
            if (ticksTogether > 20) {
                LOGGER.info("players left, stopping");
                server.halt(false);
            }
            return;
        }
        switch (ticksTogether) {
            case 60 -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                observer.setGameMode(GameType.CREATIVE);
                observer.teleportTo(level, 0.5, y, -1.5, 0.0F, 10.0F);
                wearer.setGameMode(GameType.SURVIVAL);
                wearer.teleportTo(level, 0.5, y, 3.5, 180.0F, 10.0F);
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.BLOOD_PUMP)), "equip the blood pump");
                wearer.setHealth(6.0F);
            }
            case 100 -> phase(server, "idle");
            case 110 -> {
                BloodPumpManager.use(wearer);
                check(wearer.getHealth() >= 6.0F + 8.0F, "the server healed the wearer: " + wearer.getHealth());
            }
            case 114 -> phase(server, "fired");
            case 160 -> phase(server, "refilling");
            case 170 -> {
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.MANTIS_BLADES)), "equip the mantis blades");
                MantisManager.toggle(wearer);
            }
            case 215 -> phase(server, "mantis");
            case 220 -> wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            case 230 -> phase(server, "mantis-armor");
            case 250 -> {
                if (FAILURES.isEmpty()) {
                    LOGGER.info("server: blood pump multiplayer run passed");
                } else {
                    FAILURES.forEach(failure -> LOGGER.error("server: blood pump multiplayer check failed: {}", failure));
                }
                phase(server, "done");
            }
            case 400 -> server.halt(false);
            default -> {
            }
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
        }
        LOGGER.info("check {}: {}", what, ok ? "ok" : "FAILED");
    }

    private static void phase(MinecraftServer server, String name) {
        LOGGER.info("phase {}", name);
        server.getPlayerList().broadcastSystemMessage(Component.literal("[bp] " + name), false);
    }
}
