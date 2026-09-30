package dev.apogee.gametest;

import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
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
 * Dev-only scripted client run for the nanowire: the item, the housings stowed and deployed in
 * third person, each lash and the two-wire lash filmed tick by tick in third and first person,
 * and the wire retracting. Disabled unless -Dapogee.nanowireSmoke=true.
 */
public final class NanowireClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("NanowireSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    /** First tick of each filmed move and how many ticks it is filmed for. */
    private static final int FILM = 26;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.nanowireSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(NanowireClientSmoke::tick);
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
            LevelSettings settings = new LevelSettings("NanowireSmoke", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("NanowireSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
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
            mc.options.fov().set(70);
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                player.getInventory().setItem(0, new ItemStack(ApogeeItems.NANOWIRE));
                player.getInventory().selected = 0;
            });
        }
        if (t == 12) {
            mc.player.getInventory().selected = 0;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (t == 30) {
            shot(mc, "00-item-held-and-hotbar");
        }
        if (t == 31) {
            server(mc, (level, player) -> {
                player.getInventory().setItem(0, ItemStack.EMPTY);
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.NANOWIRE)), "equip the nanowire");
            });
        }
        if (t == 40) {
            check(MantisClient.variant(mc.player) == MantisVariant.NANOWIRE, "nanowire worn on the client");
            shot(mc, "01-stowed-first-person");
            mc.options.hideGui = true;
            mc.options.fov().set(30);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        // Three-quarter views: the body turned 45 degrees from the head.
        boolean turned = t >= 48 && t <= 52;
        mc.player.setYBodyRot(mc.player.getYRot() + (turned ? 45.0F : 0.0F));
        mc.player.yBodyRotO = mc.player.yBodyRot;
        switch (t) {
            case 46 -> shot(mc, "02-stowed-front");
            case 52 -> shot(mc, "03-stowed-three-quarter");
            case 53 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 56 -> shot(mc, "04-stowed-back");
            case 57 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            }
            case 66 -> shot(mc, "05-deploying-front");
            case 90 -> {
                check(MantisClient.isDeployed(mc.player), "deployed on the client");
                shot(mc, "06-deployed-front");
            }
            case 91 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 95 -> shot(mc, "07-deployed-back");
            default -> {
            }
        }
        film(mc, t, 100, "08-third-lash-right", () -> mc.player.swing(InteractionHand.MAIN_HAND));
        film(mc, t, 100 + FILM, "09-third-lash-left", () -> mc.player.swing(InteractionHand.MAIN_HAND));
        film(mc, t, 100 + 2 * FILM, "10-third-heavy", () -> KeyMapping.click(KeyBindingHelper.getBoundKeyOf(mc.options.keyUse)));
        int firstPerson = 100 + 3 * FILM + 20;
        if (t == firstPerson - 10) {
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (t == firstPerson - 1) {
            shot(mc, "11-first-idle");
        }
        film(mc, t, firstPerson, "12-first-lash-right", () -> mc.player.swing(InteractionHand.MAIN_HAND));
        film(mc, t, firstPerson + FILM, "13-first-lash-left", () -> mc.player.swing(InteractionHand.MAIN_HAND));
        // The two-wire lash has a cooldown of three seconds from the one filmed in third person.
        film(mc, t, firstPerson + 2 * FILM + 20, "14-first-heavy", () -> KeyMapping.click(KeyBindingHelper.getBoundKeyOf(mc.options.keyUse)));
        int retract = firstPerson + 3 * FILM + 30;
        if (t == retract) {
            ClientPlayNetworking.send(ToggleMantis.INSTANCE);
        }
        if (t > retract && t <= retract + 24 && (t - retract) % 3 == 0) {
            shot(mc, String.format("15-first-retract-%02d", t - retract));
        }
        if (t == retract + 40) {
            check(!MantisClient.isDeployed(mc.player), "retracted on the client");
            shot(mc, "16-first-stowed-again");
        }
        if (t == retract + 45) {
            if (FAILURES.isEmpty()) {
                LOGGER.info("Nanowire smoke run passed");
            } else {
                FAILURES.forEach(failure -> LOGGER.error("Nanowire smoke check failed: {}", failure));
            }
            mc.stop();
        }
    }

    /** Starts a move on {@code start} and films every tick of it. */
    private static void film(Minecraft mc, int t, int start, String name, Runnable move) {
        if (t == start) {
            move.run();
        }
        if (t >= start && t < start + FILM) {
            shot(mc, String.format("%s-%02d", name, t - start));
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
        Screenshot.grab(mc.gameDirectory, "nanowire-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
