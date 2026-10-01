package dev.apogee.gametest;

import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.mantis.MantisManager;
import dev.apogee.registry.ApogeeBlocks;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.ripperdoc.RipperdocMenu;
import dev.apogee.ripperdoc.RipperdocSeat;
import dev.apogee.ripperdoc.RipperdocTableBlock;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only scripted client run for the ripperdoc table: the couch, lying on it, the screen with
 * and without implants, turning the patient view, the surgery damage, and the inventory without
 * cyberware slots. Disabled unless -Dapogee.ripperdocSmoke=true.
 */
public final class RipperdocClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("RipperdocSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static BlockPos foot;
    private static float healthBefore;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("apogee.ripperdocSmoke")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(RipperdocClientSmoke::tick);
    }

    private static void tick(Minecraft mc) {
        if (!worldRequested && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!worldRequested && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("RipperdocSmoke", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("RipperdocSmoke" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        worldTicks++;
        int t = worldTicks;
        switch (t) {
            case 10 -> {
                mc.options.fov().set(70);
                server(mc, (level, player) -> {
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                    player.setYRot(0.0F);
                    player.setXRot(35.0F);
                    // Facing south: the couch's foot two blocks ahead, its head one further on.
                    BlockPos at = player.blockPosition().south(2);
                    foot = at;
                    BlockState state = ApogeeBlocks.RIPPERDOC_TABLE.defaultBlockState().setValue(RipperdocTableBlock.FACING, Direction.SOUTH);
                    level.setBlock(at, state, 3);
                    level.setBlock(at.south(), state.setValue(RipperdocTableBlock.PART, BedPart.HEAD), 3);
                    player.getInventory().setItem(0, new ItemStack(ApogeeItems.MANTIS_BLADES));
                    player.getInventory().setItem(1, new ItemStack(ApogeeItems.SANDEVISTAN));
                    player.getInventory().setItem(2, new ItemStack(ApogeeBlocks.RIPPERDOC_TABLE_ITEM));
                    player.getInventory().setItem(3, new ItemStack(ApogeeItems.NANOWIRE));
                    player.getInventory().selected = 2;
                });
            }
            case 30 -> shot(mc, "01-table-first-person");
            case 31 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 36 -> shot(mc, "02-table-third-person");
            case 37 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                mc.setScreen(new InventoryScreen(mc.player));
            }
            case 42 -> {
                shot(mc, "03-inventory-without-cyberware");
                mc.setScreen(null);
            }
            case 44 -> server(mc, (level, player) -> {
                // Use the table as a click on its head half would.
                BlockPos head = foot.south();
                BlockState state = level.getBlockState(head);
                state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(head), Direction.UP, head, false));
            });
            case 52 -> {
                check(mc.player.getVehicle() instanceof RipperdocSeat, "the patient lies on the couch");
                check(mc.screen instanceof RipperdocScreen, "the ripperdoc screen is open");
                shot(mc, "04-screen-empty");
                healthBefore = mc.player.getHealth();
            }
            case 53 -> server(mc, (level, player) -> {
                // Fit the Mantis Blades and the Sandevistan as clicks on their slots would.
                RipperdocMenu menu = (RipperdocMenu) player.containerMenu;
                menu.getSlot(1).setByPlayer(player.getInventory().removeItem(0, 1));
                menu.getSlot(0).setByPlayer(player.getInventory().removeItem(1, 1));
                menu.broadcastChanges();
            });
            case 62 -> {
                check(MantisManager.equippedVariant(mc.player) != null, "the Mantis Blades are fitted");
                check(mc.player.getHealth() < healthBefore, "surgery hurts: " + healthBefore + " -> " + mc.player.getHealth());
                shot(mc, "05-screen-fitted");
                if (mc.screen instanceof RipperdocScreen screen) {
                    // Drag the patient view round to the back.
                    double x = mc.getWindow().getGuiScaledWidth() / 2.0;
                    double y = mc.getWindow().getGuiScaledHeight() / 2.0 - 30;
                    screen.mouseClicked(x, y, 0);
                    screen.mouseDragged(x + 60, y, 0, 100, 0);
                    screen.mouseReleased(x + 60, y, 0);
                }
            }
            case 66 -> shot(mc, "06-screen-turned");
            case 67 -> mc.player.closeContainer();
            case 72 -> {
                check(!(mc.player.getVehicle() instanceof RipperdocSeat), "closing the screen gets up from the couch");
                // Lie down again without the screen to film the pose.
                server(mc, (level, player) -> RipperdocSeat.layDown(level, foot, level.getBlockState(foot), player));
            }
            case 76 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 82 -> shot(mc, "07-patient-front");
            case 83 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 88 -> shot(mc, "08-patient-back");
            case 89 -> server(mc, (level, player) -> player.stopRiding());
            case 95 -> {
                if (FAILURES.isEmpty()) {
                    LOGGER.info("Ripperdoc smoke run passed");
                } else {
                    FAILURES.forEach(failure -> LOGGER.error("Ripperdoc smoke check failed: {}", failure));
                }
                mc.stop();
            }
            default -> {
            }
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
        Screenshot.grab(mc.gameDirectory, "ripperdoc-" + name + ".png", mc.getMainRenderTarget(), message -> LOGGER.info("screenshot {}", name));
    }
}
