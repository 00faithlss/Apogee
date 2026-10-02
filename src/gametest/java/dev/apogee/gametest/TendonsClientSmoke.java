package dev.apogee.gametest;

import dev.apogee.client.tendons.TendonsClient;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run for the Reinforced Tendons: the exhausts on the legs from the back
 * and the side, then a real double jump from a fall, checking the launch and that the landing
 * does no damage. Disabled unless -Dapogee.tendonsSmoke=true.
 */
public final class TendonsClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TendonsSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static double peakWithoutJump;
    private static double peakAfterJump;
    private static int jumpTick = -1;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.tendonsSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(TendonsClientSmoke::tick);
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
            LevelSettings settings = new LevelSettings("TendonsSmoke", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("TendonsSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        int t = ++worldTicks;
        if (t == 10) {
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "equip the tendons");
            });
            mc.options.hideGui = true;
            mc.options.fov().set(30);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 40) {
            shot(mc, "01-back");
            mc.player.setYBodyRot(mc.player.getYRot() + 90.0F);
            mc.player.yBodyRotO = mc.player.yBodyRot;
        }
        if (t == 42) {
            shot(mc, "02-side");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (t == 46) {
            shot(mc, "03-front");
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            // Drop from 8 blocks up; the double jump softens the landing.
            server(mc, (level, player) -> player.teleportTo(level, player.getX(), player.getY() + 8.0, player.getZ(), player.getYRot(), 0.0F));
        }
        if (t > 46 && jumpTick < 0) {
            if (!mc.player.onGround() && mc.player.getDeltaMovement().y < -0.3) {
                jumpTick = t;
                peakWithoutJump = mc.player.getDeltaMovement().y;
                TendonsClient.simulateJumpPressForTests();
            }
        }
        if (jumpTick >= 0 && t > jumpTick && t <= jumpTick + 3) {
            peakAfterJump = Math.max(peakAfterJump, mc.player.getDeltaMovement().y);
        }
        if (jumpTick >= 0 && t == jumpTick + 2) {
            shot(mc, "04-double-jump");
        }
        if (jumpTick >= 0 && t == jumpTick + 4) {
            check(peakAfterJump > 0.5, "the double jump launches upward (peak " + peakAfterJump + ", was " + peakWithoutJump + ")");
        }
        if (jumpTick >= 0 && t > jumpTick + 10 && mc.player.onGround() && t < 1000) {
            check(mc.player.getHealth() >= 20.0F, "no damage from the landing (health " + mc.player.getHealth() + ")");
            shot(mc, "05-landed");
            t = 1000;
            worldTicks = 1000;
        }
        if (t == 1005) {
            check(jumpTick >= 0, "a fall started");
            if (FAILURES.isEmpty()) {
                LOGGER.info("Tendons smoke run passed");
            } else {
                FAILURES.forEach(failure -> LOGGER.error("Tendons smoke check failed: {}", failure));
            }
            mc.stop();
        }
        if (t == 600 && jumpTick < 0) {
            check(false, "the player never fell");
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
        Screenshot.grab(mc.gameDirectory, "tendons-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
