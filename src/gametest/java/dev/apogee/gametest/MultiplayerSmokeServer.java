package dev.apogee.gametest;

import dev.apogee.mantis.MantisManager;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server half of the multiplayer Mantis Blades check (tools/mantis_tests.sh): once the
 * players "Observer" and "Wearer" have joined, it faces them towards each other, equips the
 * wearer and drives deploy, slash, lunge and retract from the server, announcing each phase in
 * chat so both clients can take their screenshots. Disabled unless -Dapogee.mpSmoke=true.
 */
public final class MultiplayerSmokeServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiplayerSmoke");
    private static int ticksTogether = -1;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("apogee.mpSmoke")) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(MultiplayerSmokeServer::tick);
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
                // Creative and out of reach, so the wearer's lunge does not set the observer alight.
                observer.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                observer.teleportTo(level, 0.5, y, -1.5, 0.0F, 10.0F);
                wearer.teleportTo(level, 0.5, y, 3.5, 180.0F, 10.0F);
                LOGGER.info("equip wearer: {}", TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.MANTIS_BLADES_THERMAL)));
            }
            case 100 -> phase(server, "folded");
            case 110 -> MantisManager.toggle(wearer);
            case 114 -> phase(server, "unfolding");
            case 145 -> phase(server, "open");
            case 165 -> wearer.swing(InteractionHand.MAIN_HAND, true);
            case 167 -> phase(server, "attack");
            case 195 -> MantisManager.thrust(wearer);
            case 197 -> phase(server, "thrust");
            case 225 -> MantisManager.toggle(wearer);
            case 260 -> phase(server, "retracted");
            case 280 -> phase(server, "done");
            case 400 -> server.halt(false);
            default -> {
            }
        }
    }

    private static void phase(MinecraftServer server, String name) {
        LOGGER.info("phase {}", name);
        server.getPlayerList().broadcastSystemMessage(Component.literal("[mp] " + name), false);
    }
}
