package dev.apogee.gametest;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.function.BiConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
 * Dev-only measurement run for the first-person arm motion ({@code tools/mantis_tests.sh motion}):
 * on foot, it captures every tick of standing, walking and folding, with and without view
 * bobbing and with and without open Mantis Blades, so the hands can be tracked frame by frame
 * (for instance against another first-person animation mod). Disabled unless
 * -Dapogee.mantisMotion=true. Screenshots land in the game directory's screenshots folder.
 */
public final class MantisMotionStudy implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MantisMotion");
    /** Ticks of each phase of a scenario. */
    private static final int SETTLE = 30;
    private static final int IDLE = 100;
    private static final int WALK = 60;
    private static final int STOP = 30;
    private static final int SCENARIO = SETTLE + IDLE + WALK + STOP;
    /** Folding with the blades out, filmed from the moment the key is pressed. */
    private static final int FOLD = 60;
    /** Fold tick on which vanilla's re-equip dip is triggered, to see how the hands answer it. */
    private static final int EQUIP_DIP = 35;
    /**
     * Empty hands; then the folded implant held on screen by the implant view (its motion on the
     * same arm shape and place as the empty hand's, for a direct comparison); then open blades.
     */
    private static final String[] SCENARIOS = {"empty-bob-off", "empty-bob-on", "held-bob-off", "held-bob-on", "mantis-bob-off",
            "mantis-bob-on"};
    /** With -Dapogee.motionSprint=true: the empty hand and the held implant, sprinting instead of walking. */
    private static final boolean SPRINT = Boolean.getBoolean("apogee.motionSprint");
    private static final String[] SPRINT_SCENARIOS = {"empty-sprint-bob-off", "held-sprint-bob-off"};
    private static boolean equipped;
    private static final int START = 40;
    private static int worldTicks = -1;
    private static boolean worldRequested;

    private static final boolean CALIBRATE = Boolean.getBoolean("apogee.mantisCalibrate");

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.mantisMotion") && !CALIBRATE) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(MantisMotionStudy::tick);
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
            LevelSettings settings = new LevelSettings("MantisMotion", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("MantisMotion" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        if (worldTicks == 10) {
            // Hiding the HUD would hide the hands too; the hotbar is left out of the tracking.
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            });
        }
        if (worldTicks < START) {
            return;
        }
        int step = worldTicks - START;
        if (CALIBRATE) {
            MantisFoldCalibration.tick(mc, step);
            return;
        }
        int scenario = step / SCENARIO;
        if (SPRINT) {
            if (scenario < SPRINT_SCENARIOS.length) {
                run(mc, SPRINT_SCENARIOS[scenario], scenario == 1, false, false, step % SCENARIO);
                return;
            }
            LOGGER.info("Mantis motion study done");
            mc.stop();
            return;
        }
        if (scenario < SCENARIOS.length) {
            run(mc, SCENARIOS[scenario], scenario >= 2, scenario >= 4, scenario % 2 == 1, step % SCENARIO);
            return;
        }
        int fold = step - SCENARIOS.length * SCENARIO;
        if (fold < SETTLE + FOLD) {
            foldAway(mc, fold);
            return;
        }
        LOGGER.info("Mantis motion study done");
        mc.stop();
    }

    private static void run(Minecraft mc, String name, boolean implant, boolean open, boolean bob, int t) {
        if (t == 0) {
            MantisHandPose.holdFolded = implant && !open;
            mc.options.bobView().set(bob);
            mc.options.keyUp.setDown(false);
            server(mc, (level, player) -> {
                player.teleportTo(0.5, -60.0, 0.5);
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                player.setYHeadRot(0.0F);
                if (implant && !equipped) {
                    TrinketItem.equipItem(player, new ItemStack(ApogeeItems.MANTIS_BLADES));
                }
            });
        }
        if (t == 1 && implant) {
            equipped = true;
        }
        if (t == 5) {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
            if (open && !MantisClient.isDeployed(mc.player)) {
                ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            }
        }
        if (t >= SETTLE && t < SETTLE + IDLE) {
            // The idle drift is slow: every other tick is enough.
            if ((t - SETTLE) % 2 == 0) {
                shot(mc, name + "-idle-" + String.format("%03d", t - SETTLE));
            }
        } else if (t >= SETTLE + IDLE && t < SETTLE + IDLE + WALK) {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(SPRINT);
            shot(mc, name + "-walk-" + String.format("%03d", t - SETTLE - IDLE));
        } else if (t >= SETTLE + IDLE + WALK) {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            shot(mc, name + "-stop-" + String.format("%03d", t - SETTLE - IDLE - WALK));
        }
    }

    /** With the blades out and view bobbing off, folds them and films every tick. */
    private static void foldAway(Minecraft mc, int t) {
        if (t == 0) {
            MantisHandPose.holdFolded = false;
            mc.options.bobView().set(false);
            server(mc, (level, player) -> {
                player.teleportTo(0.5, -60.0, 0.5);
                player.setYRot(0.0F);
                player.setXRot(10.0F);
            });
        }
        if (t == SETTLE) {
            ClientPlayNetworking.send(ToggleMantis.INSTANCE);
        }
        if (t == SETTLE + EQUIP_DIP) {
            mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
        }
        if (t >= SETTLE) {
            shot(mc, "fold-" + String.format("%03d", t - SETTLE));
        }
    }

    static void server(Minecraft mc, BiConsumer<ServerLevel, ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                action.accept(player.serverLevel(), player);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        // The walk cycle's phase and the bob amplitude of this frame, to line the tracks up with.
        LOGGER.info("frame {} walkDist={} bob={}", name, mc.player.walkDist, mc.player.bob);
        Screenshot.grab(mc.gameDirectory, "motion-" + name + ".png", mc.getMainRenderTarget(), message -> {
        });
    }
}
