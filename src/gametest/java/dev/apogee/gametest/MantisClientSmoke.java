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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantments;
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
 * each variant, captures it folded, unfolding, open, attacking and in first person (unfolding with
 * an item in hand, a slash with each arm, folding away), then quits.
 * Disabled unless -Dapogee.mantisSmoke=true. Screenshots land in build/mantis-smoke/screenshots.
 */
public final class MantisClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MantisSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final Item[] VARIANTS = {ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC, ApogeeItems.MANTIS_BLADES};
    private static final String[] NAMES = {"thermal", "electric", "normal"};
    private static final boolean ARMOR = Boolean.getBoolean("apogee.mantisArmorSmoke");
    private static final Item[] CHESTPLATES = {Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE,
            Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE};
    /** Ticks each variant's script takes. */
    private static final int ROUND = 280;
    /** Client tick the first variant's round starts on, after the reference shot. */
    private static final int START = 90;
    /** Third-person camera distance override for close-ups, or 0 for vanilla. */
    public static float cameraDistance;
    private static int worldTicks = -1;
    private static boolean worldRequested;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.mantisSmoke") && !ARMOR) {
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
            if (ARMOR) {
                MantisArmorProbe.verify(mc);
                MantisArmorProbe.clippedCubes = 0;
                LOGGER.info("armor smoke skin model: {}", mc.player.getSkin().model());
            }
            mc.options.fov().set(30);
            mc.options.hideGui = true;
            server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
        }
        int round = (worldTicks - START) / ROUND;
        int t = (worldTicks - START) % ROUND;
        // Reference for the first-person hand spacing: a vanilla item in each hand.
        switch (worldTicks) {
            case 12 -> server(mc, (level, player) -> {
                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
            });
            case 13 -> {
                mc.options.hideGui = false;
                mc.options.fov().set(70);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case 45 -> shot(mc, "reference-items-first-person");
            case 46 -> server(mc, (level, player) -> {
                // The three implants' 3D inventory models, in the hotbar and held.
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                player.getInventory().setItem(0, new ItemStack(ApogeeItems.MANTIS_BLADES));
                player.getInventory().setItem(1, new ItemStack(ApogeeItems.MANTIS_BLADES_THERMAL));
                player.getInventory().setItem(2, new ItemStack(ApogeeItems.MANTIS_BLADES_ELECTRIC));
                player.getInventory().selected = 1;
            });
            case 48 -> mc.player.getInventory().selected = 1;
            case 60 -> shot(mc, "reference-inventory-models");
            case 61 -> {
                // Dropped on the ground: one of each, a little ahead, seen from above.
                mc.player.setXRot(40.0F);
                server(mc, (level, player) -> {
                    Item[] items = {ApogeeItems.MANTIS_BLADES, ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC};
                    for (int i = 0; i < items.length; i++) {
                        ItemEntity drop = new ItemEntity(level, player.getX() + (i - 1) * 0.8, player.getY(), player.getZ() + 2.2,
                                new ItemStack(items[i]), 0.0, 0.0, 0.0);
                        drop.setPickUpDelay(400);
                        level.addFreshEntity(drop);
                    }
                });
            }
            case 85 -> shot(mc, "reference-dropped-models");
            case 86 -> {
                mc.options.hideGui = true;
                mc.options.fov().set(30);
                mc.player.setXRot(0.0F);
                server(mc, (level, player) -> {
                    for (int slot = 0; slot < 3; slot++) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    player.getInventory().selected = 0;
                    level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0)).forEach(ItemEntity::discard);
                });
                mc.player.getInventory().selected = 0;
            }
            default -> {
            }
        }
        if (worldTicks < START) {
            return;
        }
        if (round >= (ARMOR ? Math.min(CHESTPLATES.length, Integer.getInteger("apogee.armorRounds", CHESTPLATES.length)) : VARIANTS.length)) {
            finish(mc);
            return;
        }
        String name = NAMES[round % VARIANTS.length] + (ARMOR ? "-" + BuiltInRegistries.ITEM.getKey(CHESTPLATES[round]).getPath() : "");
        Item variant = VARIANTS[round % VARIANTS.length];
        // Three-quarter shots: turn the body 45 degrees from the head (vanilla keeps it within 50).
        boolean turned = (t >= 30 && t <= 45) || (t >= 80 && t <= 95) || (t >= 140 && t <= 147) || (t >= 154 && t <= 157);
        boolean turnedOther = t >= 148 && t <= 153;
        mc.player.setYBodyRot(mc.player.getYRot() + (turned ? 45.0F : turnedOther ? -45.0F : 0.0F));
        mc.player.yBodyRotO = mc.player.yBodyRot;
        // Close-ups: tilt the view so the camera frames the forearms instead of the head.
        if (t >= 139 && t <= 157) {
            mc.player.setXRot(t < 153 ? -35.0F : 35.0F);
            mc.player.xRotO = mc.player.getXRot();
        }
        switch (t) {
            case 0 -> server(mc, (level, player) -> {
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                var trinkets = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).orElseThrow();
                trinkets.getInventory().values().forEach(group -> group.values().forEach(inventory -> inventory.clearContent()));
                check(TrinketItem.equipItem(player, new ItemStack(variant)), "equip " + name);
                if (ARMOR) {
                    ItemStack chest = new ItemStack(CHESTPLATES[round]);
                    if (chest.is(Items.LEATHER_CHESTPLATE)) {
                        chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x2688BB, true));
                    }
                    chest.set(DataComponents.TRIM, new ArmorTrim(
                            level.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.REDSTONE),
                            level.registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SENTRY)));
                    chest.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 1);
                    player.setItemSlot(EquipmentSlot.CHEST, chest);
                }
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
            case 139 -> {
                mc.options.hideGui = true;
                mc.options.fov().set(30);
                cameraDistance = 2.0F;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 146 -> shot(mc, name + "-10-closeup-front");
            case 152 -> shot(mc, name + "-11-closeup-front-other-side");
            case 153 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 157 -> shot(mc, name + "-12-closeup-back");
            case 158 -> {
                cameraDistance = 0.0F;
                mc.options.hideGui = false;
                mc.options.fov().set(70);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case 160 -> ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            case 190 -> {
                check(!MantisClient.isDeployed(mc.player), name + " retracted on the client");
                shot(mc, name + "-09-folded-first-person");
                // Unfold again in first person, holding an item, so the folded view shows no implant arm.
                server(mc, (level, player) -> player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD)));
            }
            case 195 -> ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            case 203 -> shot(mc, name + "-13-unfolding-first-person");
            case 214 -> mc.player.setXRot(70.0F);
            case 219 -> shot(mc, name + "-13b-open-first-person-looking-down");
            case 220 -> mc.player.setXRot(10.0F);
            case 225 -> {
                check(MantisClient.isDeployed(mc.player), name + " deployed again on the client");
                shot(mc, name + "-14-open-first-person-item");
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            case 227 -> shot(mc, name + "-15-slash-right-first-person");
            case 234 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 236 -> shot(mc, name + "-16-slash-left-first-person");
            // Walk forward once the cut's clip is over: the open arms should swing with the steps.
            case 247 -> mc.options.keyUp.setDown(true);
            case 253 -> shot(mc, name + "-16b-walking-first-person-a");
            case 254 -> shot(mc, name + "-16c-walking-first-person-b");
            case 255 -> {
                shot(mc, name + "-16d-walking-first-person-c");
                mc.options.keyUp.setDown(false);
            }
            case 256 -> ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            case 263 -> shot(mc, name + "-17-folding-first-person");
            case 279 -> {
                server(mc, (level, player) -> player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
                mc.options.hideGui = true;
                mc.options.fov().set(30);
                if (ARMOR) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    shot(mc, name + "-18-restored-armor");
                    check(MantisArmorProbe.clippedCubes > 0, name + " armor sleeves rendered with clearance");
                    MantisArmorProbe.clippedCubes = 0;
                }
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
