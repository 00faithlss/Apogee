package dev.apogee.gametest;

import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With Punchy, measures where its first-person right arm is and how much it moves, with empty
 * hands and no implant, with the Mantis Blades stowed and with the Nanowire stowed. The three must
 * match: a stowed implant must give the hands back to Punchy untouched.
 * Run with {@code -Dapogee.stowedSmoke=true} and Punchy installed.
 */
public final class PunchyStowedSmoke implements ClientModInitializer {
    private static final boolean ENABLED = Boolean.getBoolean("apogee.stowedSmoke");
    private static final Logger LOGGER = LoggerFactory.getLogger("PunchyStowedSmoke");
    private static final String[] NAMES = {"none", "mantis", "nanowire", "nanowire-white"};
    private static final int ROUND = 260;
    private static boolean requested;
    private static int ticks = -1;
    private static Vector3f previous;
    private static final float[][] span = new float[NAMES.length][3];
    private static final float[][][] position = new float[NAMES.length][3][3];
    private static final int[][] count = new int[NAMES.length][3];
    private static int phase;

    @Override
    public void onInitializeClient() {
        if (ENABLED) {
            ClientTickEvents.END_CLIENT_TICK.register(PunchyStowedSmoke::tick);
            PunchyCompat.probe = (left, pose) -> {
                if (left || ticks < 40) {
                    return;
                }
                int round = (ticks - 40) / ROUND;
                if (round >= NAMES.length) {
                    return;
                }
                Vector3f at = pose.getTranslation(new Vector3f());
                if (previous != null) {
                    span[round][phase] += previous.distance(at);
                }
                previous = at;
                for (int i = 0; i < 3; i++) {
                    position[round][phase][i] += at.get(i);
                }
                count[round][phase]++;
            };
        }
    }

    private static void tick(Minecraft mc) {
        if (!requested && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!requested && mc.screen instanceof TitleScreen) {
            requested = true;
            var settings = new LevelSettings("Stowed", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("Stowed" + System.currentTimeMillis(), settings,
                    new WorldOptions(1L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        ticks++;
        if (ticks < 40) {
            return;
        }
        int round = (ticks - 40) / ROUND;
        int t = (ticks - 40) % ROUND;
        if (round >= NAMES.length) {
            for (int r = 0; r < NAMES.length; r++) {
                StringBuilder line = new StringBuilder(NAMES[r]);
                String[] phases = {"idle", "walk", "run"};
                for (int p = 0; p < 3; p++) {
                    int n = Math.max(1, count[r][p]);
                    line.append(String.format(" | %s span=%.3f at=(%.3f,%.3f,%.3f)", phases[p], span[r][p],
                            position[r][p][0] / n, position[r][p][1] / n, position[r][p][2] / n));
                }
                LOGGER.info(line.toString());
            }
            LOGGER.info("Punchy stowed smoke done");
            mc.stop();
            return;
        }
        if (t == 0) {
            previous = null;
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.bobView().set(true);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            MantisMotionStudy.server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.teleportTo(0.5, -60.0, 0.5);
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory().values()
                        .forEach(group -> group.values().forEach(inventory -> inventory.clearContent()));
                if (round == 1) {
                    TrinketItem.equipItem(player, new ItemStack(ApogeeItems.MANTIS_BLADES));
                } else if (round == 2) {
                    TrinketItem.equipItem(player, new ItemStack(ApogeeItems.NANOWIRE));
                } else if (round == 3) {
                    TrinketItem.equipItem(player, new ItemStack(ApogeeItems.NANOWIRE_WHITE));
                }
            });
        }
        if (t == 5) {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
        }
        if (t == 20 && round > 0) {
            LOGGER.info("{} variant={} out={}", NAMES[round], MantisClient.variant(mc.player), MantisClient.isOut(mc.player));
        }
        // idle 60..120, walk 130..190, run 200..255
        phase = t < 125 ? 0 : t < 195 ? 1 : 2;
        if (t == 125) {
            mc.options.keyUp.setDown(true);
            previous = null;
        }
        if (t == 195) {
            mc.options.keySprint.setDown(true);
            previous = null;
        }
        if (t == 255) {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
        }
        if (t < 60) {
            previous = null;
        }
    }
}
