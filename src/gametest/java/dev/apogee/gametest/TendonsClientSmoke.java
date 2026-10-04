package dev.apogee.gametest;

import dev.apogee.client.tendons.TendonsClient;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
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
 * Dev-only scripted client run for the Reinforced Tendons: the item in the hand, the legs
 * from the back, the side and the front (the implant is not drawn on the player), then a double jump and an air dash in each
 * direction from a drop, checking the push and that the double jump softens the landing. Disabled unless
 * -Dapogee.tendonsSmoke=true.
 */
public final class TendonsClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TendonsSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static boolean previewModel;

    /** One flight: a drop from a height, then the move triggered once the player is falling. */
    private record Flight(String name, boolean jump, float forward, float left) {
    }

    private static final Flight[] FLIGHTS = {
            new Flight("double-jump", true, 0.0F, 0.0F),
            new Flight("dash-forward", false, 1.0F, 0.0F),
            new Flight("dash-left", false, 0.0F, 1.0F),
            new Flight("dash-back", false, -1.0F, 0.0F),
            new Flight("dash-right", false, 0.0F, -1.0F),
    };
    private static int flight = -1;
    private static int flightStart;
    private static int moveTick = -1;
    /** Height at the move and its apex, to check the extra jump's slightly increased height. */
    private static double moveY;
    private static double apexY;
    private static int landedTick = -1;
    private static double groundY;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.tendonsSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(TendonsClientSmoke::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (!previewModel) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            ItemStack stack = new ItemStack(ApogeeItems.REINFORCED_TENDONS);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF2A2E38);
            // The inventory icon, then the item turned to three sides, on a flat background.
            graphics.pose().pushPose();
            graphics.pose().translate(20, 20, 0);
            graphics.pose().scale(8.0F, 8.0F, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
            float[] yaws = {0.0F, 90.0F, 180.0F, 270.0F};
            for (int i = 0; i < yaws.length; i++) {
                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(160 + (i % 2) * 200, 85 + (i / 2) * 150, 150.0F);
                pose.scale(90.0F, -90.0F, 90.0F);
                pose.mulPose(Axis.XP.rotationDegrees(20.0F));
                pose.mulPose(Axis.YP.rotationDegrees(yaws[i] + 35.0F));
                Lighting.setupForFlatItems();
                mc.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, pose, graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, mc.getItemRenderer().getModel(stack, null, null, 0));
                graphics.flush();
                Lighting.setupFor3DItems();
                pose.popPose();
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
        if (t == 4) {
            previewModel = true;
        }
        if (t == 8) {
            shot(mc, "model-preview");
        }
        if (t == 9) {
            previewModel = false;
        }
        if (t == 10) {
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                player.setYRot(0.0F);
                player.getInventory().setItem(0, new ItemStack(ApogeeItems.REINFORCED_TENDONS));
                player.getInventory().selected = 0;
            });
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (t == 30) {
            shot(mc, "00-hotbar-first-person");
            server(mc, (level, player) -> {
                player.getInventory().setItem(0, ItemStack.EMPTY);
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "equip the tendons");
            });
            mc.options.hideGui = true;
            // Look down a little so the third-person camera rises and the feet are in frame.
            mc.player.setXRot(22.0F);
            mc.options.fov().set(55);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 45) {
            shot(mc, "01-back");
            mc.player.setYBodyRot(mc.player.getYRot() + 90.0F);
            mc.player.yBodyRotO = mc.player.yBodyRot;
        }
        if (t == 47) {
            shot(mc, "02-side");
            mc.player.setYBodyRot(mc.player.getYRot());
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (t == 51) {
            groundY = mc.player.getY();
            mc.player.setXRot(0.0F);
            shot(mc, "03-front");
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            nextFlight(mc, t);
        }
        if (flight < 0) {
            return;
        }
        if (flight >= FLIGHTS.length) {
            finish(mc);
            return;
        }
        Flight f = FLIGHTS[flight];
        if (moveTick >= 0 && t - moveTick < 4) {
            LOGGER.info("t{} {} y={} vy={} ground={} fall={} hp={}", t, f.name, mc.player.getY(), mc.player.getDeltaMovement().y, mc.player.onGround(),
                    mc.player.fallDistance, mc.player.getHealth());
        }
        if (moveTick < 0) {
            if (t > flightStart && !mc.player.onGround() && mc.player.getDeltaMovement().y < -0.3 && mc.player.getY() > groundY + 2.0) {
                moveTick = t;
                moveY = mc.player.getY();
                apexY = moveY;
                if (f.jump) {
                    TendonsClient.simulateJumpPressForTests();
                } else {
                    TendonsClient.simulateDashPressForTests(f.forward, f.left);
                }
            }
            return;
        }
        int dt = t - moveTick;
        apexY = Math.max(apexY, mc.player.getY());
        if (dt == 1) {
            var v = mc.player.getDeltaMovement();
            if (f.jump) {
                check(v.y > 0.44 && v.y < 0.50, f.name + ": a two-block extra jump (vy " + v.y + ")");
            } else {
                // Facing yaw 0 (south): forward is +z and left is +x.
                double along = v.x * f.left + v.z * f.forward;
                check(along > 0.9, f.name + ": pushed the right way (" + along + " of " + v.horizontalDistance() + ")");
            }
        }
        if (f.jump && dt == 24) {
            double rise = apexY - moveY;
            check(rise > 1.8 && rise < 2.2, f.name + ": rises about 2 blocks, " + rise + " blocks");
        }
        if (dt == 2 || dt == 5 || dt == 9) {
            shot(mc, String.format("%02d-%s-%d", 4 + flight, f.name, dt));
        }
        if (dt > 12 && landedTick < 0 && mc.player.onGround()) {
            landedTick = t;
        }
        // The health update trails the landing by a few ticks. Only the double jump softens the fall.
        if (landedTick >= 0 && t == landedTick + 6) {
            if (f.jump) {
                // From this height the landing costs 7 without the margin (the one before the fix).
                check(mc.player.getHealth() >= 15.0F, f.name + ": softer landing, at most 5 damage instead of 7 (health " + mc.player.getHealth() + ")");
            }
            landedTick = -1;
            nextFlight(mc, t);
        }
        if (dt > 200) {
            check(false, f.name + ": never landed");
            finish(mc);
        }
    }

    private static void nextFlight(Minecraft mc, int t) {
        flight++;
        moveTick = -1;
        flightStart = t;
        if (flight >= FLIGHTS.length) {
            return;
        }
        // Drop from 8 blocks up above where the player stands, facing south.
        mc.getSingleplayerServer().execute(() -> {
            ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.teleportTo(player.serverLevel(), player.getX(), groundY + 8.0, player.getZ(), 0.0F, 0.0F);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            player.fallDistance = 0.0F;
            player.setHealth(20.0F);
        });
    }

    private static boolean finished;

    private static void finish(Minecraft mc) {
        if (finished) {
            return;
        }
        finished = true;
        if (FAILURES.isEmpty()) {
            LOGGER.info("Tendons smoke run passed");
        } else {
            FAILURES.forEach(failure -> LOGGER.error("Tendons smoke check failed: {}", failure));
        }
        mc.stop();
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
