package dev.apogee.gametest;

import dev.apogee.network.ApogeeNetworking.ToggleBloodPump;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.sweenus.simplytooltips.client.render.TabState;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run for the Blood Pump: worn on the forearm in third person, fired at
 * low health, filmed through the injection and refill, and the heal checked. Disabled unless
 * -Dapogee.bloodPumpSmoke=true.
 */
public final class BloodPumpClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BloodPumpSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static float healthBefore;
    private static boolean drawTooltip;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.bloodPumpSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(BloodPumpClientSmoke::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (drawTooltip) {
                graphics.renderTooltip(Minecraft.getInstance().font, new ItemStack(ApogeeItems.BLOOD_PUMP), 24, 40);
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("BloodPumpSmoke", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("BloodPumpSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        int t = worldTicks;
        if (t == 10) {
            mc.options.fov().set(18);
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.BLOOD_PUMP)), "equip the blood pump");
                player.setHealth(6.0F);
            });
        }
        if (t == 14) {
            healthBefore = mc.player.getHealth();
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = true;
        }
        // Body straight to the camera.
        mc.player.setYBodyRot(mc.player.getYRot());
        mc.player.yBodyRotO = mc.player.yBodyRot;
        if (t == 20) {
            shot(mc, "00-idle-front");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 24) {
            shot(mc, "01-idle-back");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (t == 28) {
            ClientPlayNetworking.send(ToggleBloodPump.INSTANCE);
        }
        if (t == 34) {
            check(mc.player.getHealth() > healthBefore + 6.0F, "instant heal applied");
        }
        for (int shot : new int[] {32, 60, 100, 140, 260, 500, 640}) {
            if (t == shot) {
                shot(mc, String.format("02-after-use-%03d", shot - 28));
            }
        }
        if (t == 150) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (t == 153) {
            shot(mc, "03-hud-recharging");
        }
        if (t == 150 + 1) {
            check(mc.player.getHealth() > healthBefore + 12.0F, "regeneration added health");
        }
        // With the Mantis Blades fitted, then deployed, and an iron chestplate on top.
        if (t == 650) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            server(mc, (level, player) -> check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.MANTIS_BLADES)), "equip the mantis blades"));
        }
        if (t == 660) {
            shot(mc, "04-mantis-stowed-front");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 664) {
            shot(mc, "05-mantis-stowed-back");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            ClientPlayNetworking.send(ToggleMantis.INSTANCE);
        }
        if (t == 700) {
            shot(mc, "06-mantis-deployed-front");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 704) {
            shot(mc, "07-mantis-deployed-back");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            server(mc, (level, player) -> player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
        }
        if (t == 710) {
            shot(mc, "08-mantis-deployed-armor-front");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 714) {
            shot(mc, "09-mantis-deployed-armor-back");
        }
        // The tooltip: description tab, then the stats tab with G (Simply Tooltips' cycle key).
        if (t == 725) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            drawTooltip = true;
        }
        if (t == 729) {
            shot(mc, "10-tooltip-description-a");
        }
        if (t == 735) {
            shot(mc, "10-tooltip-description");
            TabState.cycleNext();
        }
        if (t == 740) {
            shot(mc, "11-tooltip-stats-a");
        }
        if (t == 745) {
            shot(mc, "11-tooltip-stats");
            drawTooltip = false;
        }
        if (t == 760) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("Blood Pump smoke run passed");
            } else {
                FAILURES.forEach(failure -> LOGGER.error("Blood Pump smoke check failed: {}", failure));
            }
            mc.stop();
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
        }
        LOGGER.info("check {}: {}", what, ok ? "ok" : "FAILED");
    }

    @FunctionalInterface
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

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "bloodpump-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
