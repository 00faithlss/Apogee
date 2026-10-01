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
 * and without implants in skin and x-ray mode, turning the patient view, the surgery damage, the
 * pose on the couch from three sides, and the inventory without cyberware slots. Disabled unless
 * -Dapogee.ripperdocSmoke=true.
 */
public final class RipperdocClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("RipperdocSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static int worldTicks = -1;
    private static boolean worldRequested;
    private static BlockPos foot;

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
        if (t >= 160 && t < 250) {
            Direction facing = new Direction[]{Direction.NORTH, Direction.EAST, Direction.WEST}[(t - 160) / 30];
            int step = (t - 160) % 30;
            if (step == 0) {
                server(mc, (level, player) -> {
                    player.stopRiding();
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        level.setBlock(foot.relative(direction), Blocks.AIR.defaultBlockState(), 3);
                    }
                    BlockState state = ApogeeBlocks.RIPPERDOC_TABLE.defaultBlockState().setValue(RipperdocTableBlock.FACING, facing);
                    level.setBlock(foot, state, 3);
                    level.setBlock(foot.relative(facing), state.setValue(RipperdocTableBlock.PART, BedPart.HEAD), 3);
                    RipperdocSeat.layDown(level, foot, state, player);
                });
            } else if (step == 12) {
                mc.player.setYRot(facing.getOpposite().toYRot() + 55.0F);
                mc.player.setXRot(20.0F);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else if (step == 24) {
                check(mc.player.getVehicle() instanceof RipperdocSeat, facing + " patient mounted");
                var renderer = (net.minecraft.client.renderer.entity.player.PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
                var model = renderer.getModel();
                check(Math.abs(model.body.xRot) < 0.01F && Math.abs(model.rightArm.xRot) < 0.01F
                        && Math.abs(model.leftArm.xRot) < 0.01F && Math.abs(model.rightLeg.xRot - model.leftLeg.xRot) < 0.01F,
                        facing + " patient rests symmetrically with deployed blades");
                shot(mc, "12-patient-" + facing.getName());
            } else if (step == 28) {
                server(mc, (level, player) -> player.stopRiding());
            }
            return;
        }
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
                    // Markers beyond each end: gold past the foot, diamond past the head.
                    level.setBlock(at.north().east(), net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                    level.setBlock(at.south(2).east(), net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
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
                // Vanilla's inventory menu has 46 slots; Trinkets would add the cyberware ones.
                check(mc.player.inventoryMenu.slots.size() == 46, "no cyberware slots in the inventory: " + mc.player.inventoryMenu.slots.size());
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
                check(mc.player.hasPose(net.minecraft.world.entity.Pose.SLEEPING), "the patient takes the sleeping pose");
                check(mc.screen instanceof RipperdocScreen, "the ripperdoc screen is open");
                shot(mc, "04-screen-empty");
                click(mc, XRAY);
            }
            case 54 -> {
                check(RipperdocScreen.xray(), "the x-ray button turns x-ray on");
                shot(mc, "04b-xray-empty");
                click(mc, SKIN);
            }
            case 55 -> server(mc, (level, player) -> {
                // Fit the Mantis Blades and the Sandevistan as clicks on their slots would.
                float healthBefore = player.getHealth();
                RipperdocMenu menu = (RipperdocMenu) player.containerMenu;
                menu.getSlot(dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.ARMS, 0)).setByPlayer(player.getInventory().removeItem(0, 1));
                menu.getSlot(dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.OS, 0)).setByPlayer(player.getInventory().removeItem(1, 1));
                menu.broadcastChanges();
                check(player.getHealth() < healthBefore, "surgery hurts immediately on the server");
            });
            case 64 -> {
                check(MantisManager.equippedVariant(mc.player) != null, "the Mantis Blades are fitted");
                check(!RipperdocScreen.xray(), "the skin button turns x-ray off");
                shot(mc, "05-screen-fitted");
                click(mc, XRAY);
            }
            case 66 -> {
                check(RipperdocScreen.xray(), "the x-ray button is on");
                shot(mc, "05b-xray-fitted");
                // Drag the patient view to the right, round to the back.
                RipperdocScreen screen = screen(mc);
                float yawBefore = screen.yaw();
                double x = left(mc) + 88;
                double y = top(mc) + 60;
                screen.mouseClicked(x, y, 0);
                screen.mouseDragged(x + 60, y, 0, 60, 0);
                screen.mouseReleased(x + 60, y, 0);
                // Turning the model to the right shows its left side: its yaw goes down.
                check(screen.yaw() < yawBefore, "dragging right turns the patient right: " + yawBefore + " -> " + screen.yaw());
            }
            case 70 -> {
                shot(mc, "06-xray-turned");
                click(mc, SKIN);
            }
            case 72 -> shot(mc, "06b-screen-turned");
            case 73 -> mc.player.closeContainer();
            case 78 -> {
                check(!(mc.player.getVehicle() instanceof RipperdocSeat), "closing the screen gets up from the couch");
                // Lie down again without the screen to film the pose.
                server(mc, (level, player) -> {
                    level.setBlock(foot.north().east(), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(foot.south(2).east(), Blocks.AIR.defaultBlockState(), 3);
                    RipperdocSeat.layDown(level, foot, level.getBlockState(foot), player);
                });
            }
            case 82 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 83 -> {
                mc.player.setYRot(mc.player.getVehicle().getYRot() + 55.0F);
                mc.player.setXRot(20.0F);
            }
            case 88 -> shot(mc, "07-patient-front");
            case 89 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 94 -> shot(mc, "08-patient-back");
            case 95 -> {
                // Turn the head aside so the camera behind it sees the patient from the side.
                mc.player.setYRot(mc.player.getYRot() + 80.0F);
                mc.player.setXRot(25.0F);
            }
            case 100 -> {
                shot(mc, "09-patient-side");
                // Straight down from over the head: the face and the shirt's front must show.
                mc.player.setXRot(90.0F);
            }
            case 105 -> shot(mc, "10-patient-top");
            case 106 -> server(mc, (level, player) -> player.stopRiding());
            case 112 -> server(mc, (level, player) -> {
                MantisManager.toggle(player);
                RipperdocSeat.layDown(level, foot, level.getBlockState(foot), player);
            });
            case 140 -> {
                check(dev.apogee.client.mantis.MantisClient.isDeployed(mc.player), "blades remain deployed on the couch");
                shot(mc, "11-patient-deployed");
            }
            case 145 -> server(mc, (level, player) -> player.stopRiding());
            case 260 -> {
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

    private static RipperdocScreen screen(Minecraft mc) {
        return (RipperdocScreen) mc.screen;
    }

    /** Centres of the x-ray and skin buttons, from the screen's top left corner. */
    private static final int[] XRAY = {65 + 23, 114 + 5};
    private static final int[] SKIN = {65 + 23, 127 + 5};

    private static int left(Minecraft mc) {
        return (mc.getWindow().getGuiScaledWidth() - RipperdocMenu.WIDTH) / 2;
    }

    private static int top(Minecraft mc) {
        return (mc.getWindow().getGuiScaledHeight() - RipperdocMenu.HEIGHT) / 2;
    }

    /** A real left click on a button of the screen, at its place on the screen. */
    private static void click(Minecraft mc, int[] button) {
        double x = left(mc) + button[0];
        double y = top(mc) + button[1];
        screen(mc).mouseClicked(x, y, 0);
        screen(mc).mouseReleased(x, y, 0);
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
