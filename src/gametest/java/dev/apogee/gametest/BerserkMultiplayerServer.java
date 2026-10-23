package dev.apogee.gametest;

import dev.apogee.berserk.BerserkManager;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server half of the multiplayer Berserk check ({@code tools/mantis_tests.sh berserk-mp}):
 * once "Observer" and "Wearer" have joined it faces them towards each other, installs a Berserk
 * on the wearer, activates it, drops the wearer on a ring of zombies for the superhero landing
 * (the wearer's own client holds sneak, as a player would) and ends it, announcing each phase in
 * chat so both clients can check what they see and take their screenshots. Disabled unless
 * -Dapogee.mpBerserk=true.
 */
public final class BerserkMultiplayerServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BerserkMultiplayer");
    private static final java.util.List<String> FAILURES = new java.util.ArrayList<>();
    private static final java.util.List<Zombie> ZOMBIES = new java.util.ArrayList<>();
    private static int ticksTogether = -1;
    private static int groundY;
    private static boolean diveSeen;
    private static int landedAt = -1;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("apogee.mpBerserk")) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(BerserkMultiplayerServer::tick);
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
        ServerLevel level = server.overworld();
        switch (ticksTogether) {
            case 60 -> {
                level.setDayTime(6000);
                groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                // Creative and out of reach of the shockwave, so the observer only watches.
                observer.setGameMode(GameType.CREATIVE);
                observer.teleportTo(level, 0.5, groundY, -1.5, 0.0F, 10.0F);
                wearer.setGameMode(GameType.SURVIVAL);
                wearer.teleportTo(level, 0.5, groundY, 3.5, 180.0F, 10.0F);
                LOGGER.info("equip wearer: {}", TrinketItem.equipItem(wearer, new ItemStack(ApogeeItems.BERSERK_ZETATECH)));
            }
            case 100 -> phase(server, "front");
            case 110 -> wearer.teleportTo(level, 0.5, groundY, 3.5, 0.0F, 10.0F);
            case 118 -> phase(server, "back");
            case 130 -> {
                wearer.teleportTo(level, 0.5, groundY, 3.5, 180.0F, 10.0F);
                BerserkManager.toggle(wearer);
                check(BerserkManager.isActive(wearer), "the Berserk is active on the server");
            }
            case 142 -> phase(server, "active");
            case 170 -> {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.PI * 2.0 * i / 5.0;
                    Zombie zombie = EntityType.ZOMBIE.create(level);
                    zombie.moveTo(0.5 + Math.cos(angle) * 2.5, groundY, 3.5 + Math.sin(angle) * 2.5, 0.0F, 0.0F);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    level.addFreshEntity(zombie);
                    ZOMBIES.add(zombie);
                }
            }
            case 200 -> {
                wearer.teleportTo(level, 0.5, groundY + 8, 3.5, 180.0F, 10.0F);
                phase(server, "airborne");
            }
            default -> {
            }
        }
        if (ticksTogether > 200 && ticksTogether < 330) {
            diveSeen |= BerserkManager.isDiving(wearer);
            if (diveSeen && landedAt < 0 && wearer.onGround() && !BerserkManager.isDiving(wearer)) {
                landedAt = ticksTogether;
                check(true, "the dive started and the wearer landed");
                phase(server, "slam");
            }
        }
        switch (ticksTogether) {
            case 330 -> {
                check(landedAt > 0, "the wearer landed from the dive");
                check(ZOMBIES.stream().anyMatch(zombie -> zombie.getHealth() < zombie.getMaxHealth()), "the shockwave hurt the zombies");
                check(wearer.getHealth() == wearer.getMaxHealth(), "the landing did not hurt the wearer");
                BerserkManager.toggle(wearer);
                check(!BerserkManager.isActive(wearer), "the Berserk ended on the server");
            }
            case 342 -> phase(server, "cooldown");
            case 380 -> {
                if (FAILURES.isEmpty()) {
                    LOGGER.info("server: berserk multiplayer run passed");
                } else {
                    FAILURES.forEach(failure -> LOGGER.error("server: berserk multiplayer check failed: {}", failure));
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
