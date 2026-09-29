package dev.apogee.gametest;

import dev.apogee.client.mantis.MantisClient;
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
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
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
 * Dev-only scripted client run ({@code gradlew runMantisSmoke}) for the Mantis Blades: equips
 * each variant, captures it folded, unfolding, open, attacking and in first person, then quits.
 * Disabled unless -Dapogee.mantisSmoke=true. Screenshots land in build/mantis-smoke/screenshots.
 */
public final class MantisClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MantisSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final Item[] VARIANTS = {ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC, ApogeeItems.MANTIS_BLADES};
    private static final String[] NAMES = {"thermal", "electric", "normal"};
    /** Ticks each variant's script takes. */
    private static final int ROUND = 200;
    private static int worldTicks = -1;
    private static boolean worldRequested;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.mantisSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(MantisClientSmoke::tick);
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof AccessibilityOnboardingScreen) {
            // First launch of a fresh run directory shows this instead of the title screen.
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("MantisSmoke", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("MantisSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        if (worldTicks == 10) {
            mc.options.fov().set(30);
            mc.options.hideGui = true;
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
        }
        int round = (worldTicks - 20) / ROUND;
        int t = (worldTicks - 20) % ROUND;
        if (worldTicks < 20) {
            return;
        }
        if (round >= VARIANTS.length) {
            finish(mc);
            return;
        }
        String name = NAMES[round];
        Item variant = VARIANTS[round];
        // Three-quarter shots: turn the body 45 degrees from the head (vanilla keeps it within 50).
        boolean turned = (t >= 30 && t <= 45) || (t >= 80 && t <= 95);
        mc.player.setYBodyRot(mc.player.getYRot() + (turned ? 45.0F : 0.0F));
        mc.player.yBodyRotO = mc.player.yBodyRot;
        switch (t) {
            case 0 -> server(mc, (level, player) -> {
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                var trinkets = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).orElseThrow();
                trinkets.getInventory().values().forEach(group -> group.values().forEach(inventory -> inventory.clearContent()));
                check(TrinketItem.equipItem(player, new ItemStack(variant)), "equip " + name);
            });
            case 20 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 35 -> shot(mc, name + "-01b-folded-three-quarter");
            case 46 -> {
                check(MantisClient.variant(mc.player) != null, name + " worn on the client");
                shot(mc, name + "-01-folded-front");
            }
            case 47 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 50 -> shot(mc, name + "-02-folded-back");
            case 52 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            }
            case 61 -> shot(mc, name + "-03-unfolding");
            case 85 -> shot(mc, name + "-04b-open-three-quarter");
            case 96 -> {
                check(MantisClient.isDeployed(mc.player), name + " deployed on the client");
                shot(mc, name + "-04-open-front");
            }
            case 97 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 101 -> shot(mc, name + "-05-open-back");
            case 102 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 107 -> shot(mc, name + "-06-attack-back");
            case 120 -> {
                // F1 would also hide the hands.
                mc.options.hideGui = false;
                mc.options.fov().set(70);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case 130 -> shot(mc, name + "-07-open-first-person");
            case 132 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 137 -> shot(mc, name + "-08-attack-first-person");
            case 160 -> ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            case 190 -> {
                check(!MantisClient.isDeployed(mc.player), name + " retracted on the client");
                shot(mc, name + "-09-folded-first-person");
                mc.options.hideGui = true;
                mc.options.fov().set(30);
            }
            default -> {
            }
        }
    }

    private static void finish(Minecraft mc) {
        if (FAILURES.isEmpty()) {
            LOGGER.info("Mantis smoke run passed");
        } else {
            FAILURES.forEach(failure -> LOGGER.error("Mantis smoke check failed: {}", failure));
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
        Screenshot.grab(mc.gameDirectory, "mantis-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
