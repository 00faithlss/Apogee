package dev.apogee.gametest;

import dev.apogee.berserk.BerserkManager;
import dev.apogee.client.ApogeeClientState;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketsApi;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.sweenus.simplytooltips.client.render.TabState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run ({@code gradlew runBerserkSmoke}): installs each Berserk, films
 * the implant on the back, the tooltips, an activation with its vignette and HUD, and a
 * superhero landing among zombies, then quits. Disabled unless -Dapogee.berserkSmoke=true.
 */
public final class BerserkClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BerserkSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final Item[] BERSERKS = {ApogeeItems.BERSERK_ZETATECH, ApogeeItems.BERSERK_BIODYNE, ApogeeItems.BERSERK_MILITECH};
    private static final Item[] HOTBAR = {ApogeeItems.SUBDERMAL_ARMOR, ApogeeItems.ADRENALINE_BOOSTER, ApogeeItems.BIOMONITOR,
            ApogeeItems.FORTIFIED_ANKLES, ApogeeItems.BIONIC_JOINTS, ApogeeItems.SYNAPTIC_ACCELERATOR,
            ApogeeItems.BERSERK_ZETATECH, ApogeeItems.BERSERK_BIODYNE, ApogeeItems.BERSERK_MILITECH};
    /** Each variant: install at 0, back view at 20, tooltip description at 30, stats at 40. */
    private static final int VARIANT_TICKS = 50;
    private static final int ACTION = 40 + VARIANT_TICKS * 3;
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static Item tooltipItem;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.berserkSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(BerserkClientSmoke::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (tooltipItem != null) {
                graphics.renderTooltip(Minecraft.getInstance().font, new ItemStack(tooltipItem), 24, 30);
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("BerserkSmoke", GameType.SURVIVAL, false, Difficulty.EASY, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("BerserkSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        if (worldTicks == 20) {
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                for (int slot = 0; slot < HOTBAR.length; slot++) {
                    player.getInventory().setItem(slot, new ItemStack(HOTBAR[slot]));
                }
                player.getInventory().selected = 8;
            });
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (worldTicks == 30) {
            var missing = mc.getModelManager().getMissingModel();
            for (Item item : HOTBAR) {
                check(mc.getItemRenderer().getItemModelShaper().getItemModel(item) != missing, item + " has an item model");
            }
            shot(mc, "01-hotbar-icons");
        }
        int variantStep = worldTicks - 40;
        if (variantStep >= 0 && variantStep < VARIANT_TICKS * 3) {
            variant(mc, BERSERKS[variantStep / VARIANT_TICKS], variantStep % VARIANT_TICKS);
        }
        int t = worldTicks - ACTION;
        switch (t) {
            case 0 -> {
                install(mc, ApogeeItems.BERSERK_ZETATECH);
                server(mc, (level, player) -> {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.PI * 2.0 * i / 6.0;
                        Zombie zombie = EntityType.ZOMBIE.create(level);
                        zombie.moveTo(player.getX() + Math.cos(angle) * 2.5, player.getY(), player.getZ() + 4.0 + Math.sin(angle) * 2.5, 180.0F, 0.0F);
                        zombie.setNoAi(true);
                        zombie.setPersistenceRequired();
                        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                        level.addFreshEntity(zombie);
                    }
                });
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case 10 -> ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            case 13 -> shot(mc, "04-activation-flash");
            case 18 -> check(ApogeeClientState.berserkStatus().active(), "client sees the Berserk active");
            case 30 -> shot(mc, "05-active-vignette-hud");
            case 32 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 40 -> shot(mc, "06-active-third-person");
            case 42 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                server(mc, (level, player) -> player.teleportTo(player.getX(), player.getY() + 8.0, player.getZ() + 4.0));
            }
            case 45 -> mc.options.keyShift.setDown(true);
            case 47 -> shot(mc, "07-dive");
            case 49 -> server(mc, (level, player) -> check(BerserkManager.isDiving(player), "sneaking in the air starts the dive"));
            default -> {
            }
        }
        if (t >= 46 && t <= 80 && landedAt < 0 && mc.player.onGround()) {
            landedAt = t;
            mc.options.keyShift.setDown(false);
        }
        if (landedAt >= 0) {
            int since = t - landedAt;
            if (since == 1) {
                shot(mc, "08-slam-impact");
            } else if (since == 6) {
                shot(mc, "09-slam-after");
                server(mc, (level, player) -> check(player.getHealth() == player.getMaxHealth(), "no damage from the landing"));
            } else if (since == 10) {
                ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            } else if (since == 30) {
                shot(mc, "10-cooldown-hud");
            } else if (since == 34) {
                LOGGER.info(FAILURES.isEmpty() ? "BERSERK_SMOKE_PASSED" : "BERSERK_SMOKE_FAILED " + FAILURES);
                mc.stop();
            }
        } else if (t == 90) {
            check(false, "landed after the dive");
            LOGGER.info("BERSERK_SMOKE_FAILED " + FAILURES);
            mc.stop();
        }
    }

    private static int landedAt = -1;

    private static void variant(Minecraft mc, Item item, int t) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        switch (t) {
            case 0 -> {
                install(mc, item);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 20 -> shot(mc, "02-" + name + "-back");
            case 22 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                tooltipItem = item;
            }
            case 30 -> shot(mc, "03-" + name + "-tooltip-description");
            case 31 -> TabState.cycleNext();
            case 40 -> shot(mc, "03-" + name + "-tooltip-stats");
            case 41 -> {
                TabState.cycleNext();
                tooltipItem = null;
            }
            default -> {
            }
        }
    }

    private static void install(Minecraft mc, Item item) {
        server(mc, (level, player) -> TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
            component.getInventory().get("cyberware").get("os").setItem(0, new ItemStack(item));
            check(BerserkManager.isEquipped(player), item + " installed");
        }));
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
        Screenshot.grab(mc.gameDirectory, "berserk-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
