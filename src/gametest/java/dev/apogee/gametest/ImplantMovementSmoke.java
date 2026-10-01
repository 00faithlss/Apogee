package dev.apogee.gametest;

import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public final class ImplantMovementSmoke implements ClientModInitializer {
    private static final boolean ENABLED = Boolean.getBoolean("apogee.movementSmoke");
    private static final boolean PUNCHY = FabricLoader.getInstance().isModLoaded("punchy");
    private static final int ROUND = 355;
    private static final int ROUNDS = Integer.getInteger("apogee.movementRounds", 4);
    private static final Logger LOGGER = LoggerFactory.getLogger("ImplantMovementSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static boolean requested;
    private static int ticks = -1;
    private static int samples;
    private static float idleSpan;
    private static float walkSpan;
    private static float runSpan;
    private static Vector3f previous;

    @Override
    public void onInitializeClient() {
        if (ENABLED) {
            ClientTickEvents.END_CLIENT_TICK.register(ImplantMovementSmoke::tick);
        }
    }

    public static void sample(Matrix4f pose) {
        if (!ENABLED || !PunchyCompat.rendered() || ticks < 40) {
            return;
        }
        check(pose.isFinite(), "finite Punchy arm transform");
        Vector3f position = pose.getTranslation(new Vector3f());
        int t = (ticks - 40) % ROUND;
        if (previous != null) {
            float distance = previous.distance(position);
            if (t >= 60 && t < 75) {
                idleSpan += distance;
            } else if (t >= 90 && t < 115) {
                walkSpan += distance;
            } else if (t >= 130 && t < 155) {
                runSpan += distance;
            }
        }
        previous = position;
        samples++;
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
            var settings = new LevelSettings("ImplantMovement", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ImplantMovement" + System.currentTimeMillis(), settings,
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
        if (round >= ROUNDS) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("Implant movement smoke passed (Punchy={}, rounds={})", PUNCHY, ROUNDS);
            } else {
                LOGGER.error("Implant movement smoke FAILED: {}", FAILURES);
            }
            mc.stop();
            return;
        }
        boolean nano = round % 2 == 1;
        String name = (nano ? "nanowire" : "mantis") + (round < 2 ? "-right-bob-on" : "-left-bob-off");
        if (t == 0) {
            samples = 0;
            idleSpan = walkSpan = runSpan = 0.0F;
            previous = null;
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.bobView().set(round < 2);
            mc.options.mainHand().set(round < 2 ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            MantisMotionStudy.server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.teleportTo(0.5, -60.0, 0.5);
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory().values()
                        .forEach(group -> group.values().forEach(inventory -> inventory.clearContent()));
                check(TrinketItem.equipItem(player, new ItemStack(nano ? ApogeeItems.NANOWIRE : ApogeeItems.MANTIS_BLADES)), "equip " + name);
            });
        }
        if (t == 5) {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
        }
        switch (t) {
            case 28 -> {
                check(MantisClient.variant(mc.player) == (nano ? MantisVariant.NANOWIRE : MantisVariant.STANDARD), "synced " + name);
                check(PunchyCompat.active(mc.player) == PUNCHY, "optional Punchy bridge " + name);
            }
            case 30, 200, 255, 305 -> ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            case 75 -> mc.options.keyUp.setDown(true);
            case 115 -> mc.options.keySprint.setDown(true);
            case 150 -> check(mc.player.isSprinting(), "running " + name);
            case 155 -> {
                mc.options.keyUp.setDown(false);
                mc.options.keySprint.setDown(false);
            }
            case 170 -> {
                mc.player.swing(InteractionHand.MAIN_HAND);
                if (PUNCHY) {
                    try {
                        var tracker = Class.forName("punchy.client.state.AttackActionTracker");
                        check(!(boolean) tracker.getMethod("isTriggered").invoke(null), "implant attack owns the hands " + name);
                    } catch (ReflectiveOperationException exception) {
                        check(false, "Punchy attack probe " + exception);
                    }
                }
            }
            case 230 -> {
                check(!MantisClient.isOut(mc.player), "folded " + name);
                MantisMotionStudy.server(mc, (level, player) -> {
                    player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                    player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
                });
            }
            case 275 -> check(MantisClient.implantLift(mc.player, 0.0F) > 0.99F, "raised after item stow " + name);
            case 340 -> {
                check(!MantisClient.isOut(mc.player), "returned to held items " + name);
                if (PUNCHY) {
                    check(samples > 30, "Punchy frames were rendered " + name);
                    check(walkSpan > 0.02F, "walking moves native poses " + name);
                    check(runSpan > 0.02F, "running moves native poses " + name);
                }
                LOGGER.info("{} samples={} idle={} walking={} running={}", name, samples, idleSpan, walkSpan, runSpan);
            }
            default -> {
            }
        }
        if (t >= 25 && t < 340 && t % 2 == 0) {
            Screenshot.grab(mc.gameDirectory, "movement-" + name + "-" + String.format("%03d", t) + ".png",
                    mc.getMainRenderTarget(), message -> {});
        }
    }

    private static void check(boolean pass, String message) {
        if (!pass && !FAILURES.contains(message)) {
            FAILURES.add(message);
            LOGGER.error("FAILED {}", message);
        }
    }
}
