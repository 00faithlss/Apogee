package dev.apogee.gametest;

import dev.apogee.berserk.BerserkManager;
import dev.apogee.bloodpump.BloodPumpManager;
import dev.apogee.mantis.MantisManager;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.sandevistan.SandevistanManager;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server half of the combined develop check (tools/develop_tests.sh): "Wearer" fits the
 * Blood Pump, the Mantis Blades and the Sandevistan, uses them together, then swaps to the Berserk
 * and the Nanowire, and every step is announced as "[dv] phase" so both clients capture what they
 * see: the HUD squares stacked, the implants on the body, then every tooltip. Disabled unless
 * -Dapogee.devMpSmoke=true.
 */
public final class DevelopMultiplayerServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("DevelopMultiplayer");
    private static final java.util.List<String> FAILURES = new java.util.ArrayList<>();
    private static int ticksTogether = -1;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("apogee.devMpSmoke")) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(DevelopMultiplayerServer::tick);
    }

    private static void clear(ServerPlayer player, String slot) {
        TrinketsApi.getTrinketComponent(player)
                .ifPresent(component -> component.getInventory().get("cyberware").get(slot).setItem(0, ItemStack.EMPTY));
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
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.MANTIS_BLADES)), "equip the mantis blades");
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.SANDEVISTAN)), "equip the sandevistan");
                wearer.setHealth(6.0F);
            }
            case 100 -> phase(server, "idle");
            case 110 -> {
                BloodPumpManager.use(wearer);
                MantisManager.toggle(wearer);
                SandevistanManager.toggle(wearer);
                check(SandevistanManager.isActive(wearer), "the sandevistan is active on the server");
            }
            case 140 -> phase(server, "all-sandevistan");
            case 150 -> SandevistanManager.toggle(wearer);
            case 200 -> phase(server, "sandevistan-cooldown");
            case 205 -> {
                clear(wearer, "os");
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.BERSERK_ZETATECH)), "equip the berserk");
            }
            case 215 -> {
                BerserkManager.toggle(wearer);
                check(BerserkManager.isActive(wearer), "the berserk is active on the server");
            }
            case 245 -> phase(server, "all-berserk");
            case 250 -> BerserkManager.toggle(wearer);
            case 260 -> {
                MantisManager.toggle(wearer);
                clear(wearer, "arms");
                check(TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.NANOWIRE_WHITE)), "equip the nanowire");
            }
            case 270 -> {
                MantisManager.toggle(wearer);
                BerserkManager.toggle(wearer);
            }
            case 305 -> phase(server, "all-nanowire");
            case 320 -> phase(server, "tooltips");
            // The wearer's client films the tooltips on its own: 6 items of 24 ticks.
            case 500 -> {
                if (FAILURES.isEmpty()) {
                    LOGGER.info("server: develop multiplayer run passed");
                } else {
                    FAILURES.forEach(failure -> LOGGER.error("server: develop multiplayer check failed: {}", failure));
                }
                phase(server, "done");
            }
            case 650 -> server.halt(false);
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
        server.getPlayerList().broadcastSystemMessage(Component.literal("[dv] " + name), false);
    }
}
