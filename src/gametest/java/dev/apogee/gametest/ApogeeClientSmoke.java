package dev.apogee.gametest;

import dev.apogee.client.ApogeeClientState;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplytooltips.client.render.TabState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run ({@code gradlew runSmokeClient}): creates a flat world, equips
 * the implant, captures the tooltip views, an activation, the HUD and afterimages to
 * screenshots, checks client-side slow flags, then quits. Disabled unless -Dapogee.smoke=true.
 */
public final class ApogeeClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ApogeeSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static boolean drawTooltip;
    private static net.minecraft.world.item.Item tooltipItem = ApogeeItems.SANDEVISTAN;
    /** The Mantis Blades tooltips, filmed in both views after the Sandevistan run. */
    private static final net.minecraft.world.item.Item[] MANTIS = {
            ApogeeItems.MANTIS_BLADES, ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC};
    private static final int MANTIS_TOOLTIPS = 272;
    private static final net.minecraft.world.item.Item[] PARTS = {
            ApogeeItems.NEURAL_CABLE, ApogeeItems.CONTROL_CHIP, ApogeeItems.BLADE_ACTUATOR, ApogeeItems.DEPLOY_RAIL,
            ApogeeItems.MANTIS_BLADE, ApogeeItems.THERMAL_BLADE, ApogeeItems.ELECTRIC_BLADE};
    private static int zombieId = -1;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.smoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(ApogeeClientSmoke::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (drawTooltip) {
                graphics.renderTooltip(Minecraft.getInstance().font, new ItemStack(tooltipItem), 24, 40);
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("ApogeeSmoke", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ApogeeSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        worldTicks++;
        switch (worldTicks) {
            case 20 -> server(mc, (level, player) -> {
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                check(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "equip implant");
                player.getInventory().setItem(0, new ItemStack(ApogeeItems.SANDEVISTAN));
                Vec3 look = player.position().add(0.0, 0.0, 5.0);
                Zombie zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(look.x, player.getY(), look.z, 180.0F, 0.0F);
                zombie.setNoAi(true);
                zombie.setPersistenceRequired();
                level.addFreshEntity(zombie);
                zombie.setTarget(player);
                zombieId = zombie.getId();
            });
            case 40 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 70 -> shot(mc, "01-implant-back");
            case 75 -> drawTooltip = true;
            case 85 -> shot(mc, "02-tooltip-description");
            case 86 -> TabState.cycleNext();
            case 95 -> shot(mc, "03-tooltip-stats");
            case 96 -> {
                drawTooltip = false;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 110 -> ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            case 114 -> shot(mc, "04-activation-radial");
            case 120 -> {
                check(ApogeeClientState.status().active(), "client received active status");
                check(ApogeeClientState.isActiveWearer(mc.player), "local player listed as wearer");
                check(zombieId >= 0 && mc.level.getEntity(zombieId) != null && ApogeeClientState.isSlowed(mc.level.getEntity(zombieId)),
                        "targeting zombie slowed on the client");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 150 -> {
                var speed = mc.player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                var bonus = speed.getModifier(dev.apogee.sandevistan.SandevistanManager.SPEED_MODIFIER);
                check(bonus != null && bonus.amount() > 0.4, "speed bonus synced to the client");
                double withoutBonus = speed.getValue() / (1.0 + (bonus == null ? 0.0 : bonus.amount()));
                float expected = (mc.player.getAbilities().flying ? 1.1F : 1.0F)
                        * ((float) withoutBonus / mc.player.getAbilities().getWalkingSpeed() + 1.0F) / 2.0F;
                expected = net.minecraft.util.Mth.lerp(mc.options.fovEffectScale().get().floatValue(), 1.0F, expected);
                LOGGER.info("fov modifier {} expected without bonus {}", mc.player.getFieldOfViewModifier(), expected);
                check(Math.abs(mc.player.getFieldOfViewModifier() - expected) < 1.0E-4F, "Sandevistan speed does not widen the FOV");
            }
            case 160 -> shot(mc, "05-active-tint-hud-afterimages");
            case 200 -> ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            case 206 -> {
                check(!ApogeeClientState.status().active(), "manual end reached the client");
                check(!ApogeeClientState.isSlowed(mc.level.getEntity(zombieId)), "slow flag cleared on the client");
            }
            case 230 -> shot(mc, "06-cooldown-hud");
            case 234 -> server(mc, (level, player) -> {
                var leggings = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immersive_armors", "steampunk_leggings"));
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(leggings));
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(player.getX(), player.getY() + 12.0, player.getZ());
            });
            case 240 -> {
                check(!mc.player.onGround() && !mc.player.getAbilities().flying, "player airborne for the dash");
                check(dev.apogee.compat.immersivearmors.SteamDash.wearsSteampunkLeggings(mc.player), "steampunk leggings equipped on the client");
                // No real keyboard in the scripted run: inject a fresh sprint-key press.
                dev.apogee.client.compat.immersivearmors.SteamDashClient.simulateSprintPressForTests();
            }
            case 242 -> {
                double horizontal = mc.player.getDeltaMovement().horizontalDistance();
                LOGGER.info("steam dash horizontal speed {}", horizontal);
                check(horizontal > 0.6, "steam dash launches the player forward");
            }
            case 244 -> server(mc, (level, player) -> {
                // Getting the implant completes the advancement, which plays Adam Smasher's voice line to everyone.
                player.getInventory().add(new ItemStack(ApogeeItems.SANDEVISTAN));
                player.inventoryMenu.broadcastChanges();
            });
            case 262 -> shot(mc, "07-challenge-toast");
            case MANTIS_TOOLTIPS + 20 * 3 + 1 -> server(mc, (level, player) -> {
                // The Mantis Blades parts in the hotbar, in crafting order, then the implant.
                for (int slot = 0; slot < PARTS.length; slot++) {
                    player.getInventory().setItem(slot, new ItemStack(PARTS[slot]));
                }
                player.getInventory().setItem(PARTS.length, new ItemStack(ApogeeItems.MANTIS_BLADES));
            });
            case MANTIS_TOOLTIPS + 20 * 3 + 6 -> {
                var missing = mc.getModelManager().getMissingModel();
                for (net.minecraft.world.item.Item part : PARTS) {
                    check(mc.getItemRenderer().getItemModelShaper().getItemModel(part) != missing, part + " has an item model");
                }
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case MANTIS_TOOLTIPS + 20 * 3 + 12 -> shot(mc, "09-mantis-parts");
            case MANTIS_TOOLTIPS + 20 * 3 + 16 -> {
                LOGGER.info(FAILURES.isEmpty() ? "APOGEE_SMOKE_PASSED" : "APOGEE_SMOKE_FAILED " + FAILURES);
                mc.stop();
            }
            default -> {
                int step = worldTicks - MANTIS_TOOLTIPS;
                if (step >= 0 && step < 20 * MANTIS.length) {
                    mantisTooltip(mc, MANTIS[step / 20], step % 20);
                }
            }
        }
        if (worldTicks > 120 && worldTicks < 200 && mc.player != null) {
            // Glide sideways so afterimages are recorded.
            double offset = Math.sin(worldTicks * 0.15) * 0.35;
            mc.player.setPos(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ());
        }
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

    /** Description view, then stats view, of one Mantis Blades tooltip. */
    private static void mantisTooltip(Minecraft mc, net.minecraft.world.item.Item item, int t) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        switch (t) {
            case 0 -> {
                tooltipItem = item;
                drawTooltip = true;
                if (item == MANTIS[MANTIS.length - 1]) {
                    // The tooltip names the deploy key as currently bound in the controls menu.
                    dev.apogee.client.ApogeeClient.TOGGLE_MANTIS.setKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.k"));
                    net.minecraft.client.KeyMapping.resetMapping();
                    String shown = dev.apogee.client.tooltip.MantisTooltipContent.sections(dev.apogee.mantis.MantisVariant.ELECTRIC)
                            .get(0).header().getString();
                    check(shown.equals("K"), "tooltip follows the rebound deploy key, shows " + shown);
                }
            }
            case 6 -> shot(mc, "08-" + name + "-description");
            case 7 -> TabState.cycleNext();
            case 13 -> shot(mc, "08-" + name + "-stats");
            case 14 -> drawTooltip = false;
            default -> {
            }
        }
    }

    private static void check(boolean condition, String what) {
        LOGGER.info("{} {}", condition ? "OK  " : "FAIL", what);
        if (!condition) {
            FAILURES.add(what);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "apogee-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
