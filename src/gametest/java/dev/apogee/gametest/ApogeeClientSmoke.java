package dev.apogee.gametest;

import dev.apogee.client.ApogeeClientState;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplytooltips.client.render.TabState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run ({@code gradlew runSmokeClient}): creates a flat world, equips
 * the implant, captures the tooltip views, an activation, the HUD and afterimages to
 * screenshots, checks client-side slow flags, then quits. Disabled unless -Dapogee.smoke=true.
 */
public final class ApogeeClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ApogeeSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static boolean drawTooltip;
    private static int zombieId = -1;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.smoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(ApogeeClientSmoke::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (drawTooltip) {
                graphics.renderTooltip(Minecraft.getInstance().font, new ItemStack(ApogeeItems.SANDEVISTAN), 24, 40);
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("ApogeeSmoke", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ApogeeSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        switch (worldTicks) {
            case 20 -> server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "equip implant");
                player.getInventory().setItem(0, new ItemStack(ApogeeItems.SANDEVISTAN));
                Vec3 look = player.position().add(0.0, 0.0, 5.0);
                Zombie zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(look.x, player.getY(), look.z, 180.0F, 0.0F);
                zombie.setNoAi(true);
                zombie.setPersistenceRequired();
                level.addFreshEntity(zombie);
                zombie.setTarget(player);
                zombieId = zombie.getId();
            });
            case 40 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 70 -> shot(mc, "01-implant-back");
            case 75 -> drawTooltip = true;
            case 85 -> shot(mc, "02-tooltip-description");
            case 86 -> TabState.cycleNext();
            case 95 -> shot(mc, "03-tooltip-stats");
            case 96 -> {
                drawTooltip = false;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 110 -> ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            case 114 -> shot(mc, "04-activation-radial");
            case 120 -> {
                check(ApogeeClientState.status().active(), "client received active status");
                check(ApogeeClientState.isActiveWearer(mc.player), "local player listed as wearer");
                check(zombieId >= 0 && mc.level.getEntity(zombieId) != null && ApogeeClientState.isSlowed(mc.level.getEntity(zombieId)),
                        "targeting zombie slowed on the client");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 160 -> shot(mc, "05-active-tint-hud-afterimages");
            case 200 -> ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            case 206 -> {
                check(!ApogeeClientState.status().active(), "manual end reached the client");
                check(!ApogeeClientState.isSlowed(mc.level.getEntity(zombieId)), "slow flag cleared on the client");
            }
            case 230 -> shot(mc, "06-cooldown-hud");
            case 234 -> server(mc, (level, player) -> {
                var leggings = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immersive_armors", "steampunk_leggings"));
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(leggings));
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(player.getX(), player.getY() + 12.0, player.getZ());
            });
            case 240 -> {
                check(!mc.player.onGround() && !mc.player.getAbilities().flying, "player airborne for the dash");
                check(dev.apogee.compat.immersivearmors.SteamDash.wearsSteampunkLeggings(mc.player), "steampunk leggings equipped on the client");
                // No real keyboard in the scripted run: inject a fresh sprint-key press.
                dev.apogee.client.compat.immersivearmors.SteamDashClient.simulateSprintPressForTests();
            }
            case 242 -> {
                double horizontal = mc.player.getDeltaMovement().horizontalDistance();
                LOGGER.info("steam dash horizontal speed {}", horizontal);
                check(horizontal > 0.6, "steam dash launches the player forward");
            }
            case 244 -> server(mc, (level, player) -> {
                net.minecraft.advancements.CriteriaTriggers.RECIPE_CRAFTED.trigger(player, dev.apogee.Apogee.id("sandevistan"), List.of());
                ItemStack crafted = new ItemStack(ApogeeItems.SANDEVISTAN);
                crafted.onCraftedBy(level, player, 1); // plays Adam Smasher's voice line to everyone
            });
            case 262 -> shot(mc, "07-challenge-toast");
            case 270 -> {
                LOGGER.info(FAILURES.isEmpty() ? "APOGEE_SMOKE_PASSED" : "APOGEE_SMOKE_FAILED " + FAILURES);
                mc.stop();
            }
            default -> {
            }
        }
        if (worldTicks > 120 && worldTicks < 200 && mc.player != null) {
            // Glide sideways so afterimages are recorded.
            double offset = Math.sin(worldTicks * 0.15) * 0.35;
            mc.player.setPos(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ());
        }
    }

    private interface ServerAction {
        void run(ServerLevel level, ServerPlayer player);
    }

    private static void server(Minecraft mc, ServerAction action) {
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            action.run(player.serverLevel(), player);
        });
    }

    private static void check(boolean condition, String what) {
        LOGGER.info("{} {}", condition ? "OK  " : "FAIL", what);
        if (!condition) {
            FAILURES.add(what);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "apogee-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
