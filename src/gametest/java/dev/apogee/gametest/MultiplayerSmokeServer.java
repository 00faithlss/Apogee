package dev.apogee.gametest;

import dev.apogee.mantis.MantisManager;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.registry.ApogeeBlocks;
import dev.apogee.ripperdoc.RipperdocMenu;
import dev.apogee.ripperdoc.RipperdocSeat;
import dev.apogee.ripperdoc.RipperdocTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
 * chat so both clients can take their screenshots. Then the wearer lies on a ripperdoc table and
 * swaps the Mantis Blades for the nanowire with real clicks from their client, while the observer
 * watches them lie on the couch. Disabled unless -Dapogee.mpSmoke=true.
 */
public final class MultiplayerSmokeServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiplayerSmoke");
    private static int ticksTogether = -1;
    private static final java.util.List<String> FAILURES = new java.util.ArrayList<>();
    /** The ripperdoc table's foot; the couch lies across the observer's view, its head to the west. */
    private static BlockPos tableFoot;

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
            case 270 -> {
                ServerLevel level = server.overworld();
                tableFoot = BlockPos.containing(1.5, wearer.getY(), 1.5);
                BlockState table = ApogeeBlocks.RIPPERDOC_TABLE.defaultBlockState().setValue(RipperdocTableBlock.FACING, Direction.WEST);
                level.setBlock(tableFoot, table, 3);
                level.setBlock(tableFoot.west(), table.setValue(RipperdocTableBlock.PART, BedPart.HEAD), 3);
                wearer.getInventory().add(new ItemStack(ApogeeItems.NANOWIRE));
            }
            case 280 -> {
                // As the wearer's right click on the couch would.
                BlockPos head = tableFoot.west();
                ServerLevel level = server.overworld();
                level.getBlockState(head).useWithoutItem(level, wearer, new BlockHitResult(Vec3.atCenterOf(head), Direction.UP, head, false));
                check(wearer.getVehicle() instanceof RipperdocSeat, "the wearer lies on the couch");
                check(wearer.containerMenu instanceof RipperdocMenu, "the ripperdoc menu is open on the server");
            }
            case 284 -> phase(server, "couch");
            // The wearer's client swaps the implants and switches to x-ray meanwhile.
            case 340 -> {
                check(MantisManager.equippedVariant(wearer) == MantisVariant.NANOWIRE, "the server fitted the nanowire: " + MantisManager.equippedVariant(wearer));
                check(wearer.getHealth() < wearer.getMaxHealth(), "surgery hurt the wearer: " + wearer.getHealth());
                check(wearer.hasPose(Pose.SLEEPING), "the wearer is in the sleeping pose");
                phase(server, "fitted");
            }
            case 360 -> wearer.closeContainer();
            case 366 -> {
                check(!(wearer.getVehicle() instanceof RipperdocSeat), "closing the screen gets the wearer up");
                phase(server, "getup");
            }
            case 380 -> {
                if (FAILURES.isEmpty()) {
                    LOGGER.info("server: multiplayer smoke run passed");
                } else {
                    FAILURES.forEach(failure -> LOGGER.error("server: multiplayer check failed: {}", failure));
                }
                phase(server, "done");
            }
            case 500 -> server.halt(false);
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
        server.getPlayerList().broadcastSystemMessage(Component.literal("[mp] " + name), false);
    }
}
