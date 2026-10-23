package dev.apogee.gametest;

import dev.apogee.armor.MaxtacVisor;
import dev.apogee.client.armor.MaxtacVisorClient;
import dev.apogee.registry.ApogeeItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Prueba real de equipamiento, sincronización del visor y emisión con luz ambiental cero. */
public final class MaxtacClientSmoke implements ClientModInitializer {
    public static PlayerSkin fixtureSkin;
    private static final Logger LOG = LoggerFactory.getLogger("MaxtacSmoke");
    private static final List<String> FAILURES = new ArrayList<>();
    private static boolean requested;
    private static int ticks;
    private static int activationWait;
    private static int deactivationWait;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("apogee.maxtacSmoke")) {
            ClientTickEvents.END_CLIENT_TICK.register(MaxtacClientSmoke::tick);
        }
    }

    private static void tick(Minecraft mc) {
        if (!requested && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.setScreen(new TitleScreen());
        }
        if (!requested && mc.screen instanceof TitleScreen) {
            requested = true;
            LevelSettings settings = new LevelSettings("MaxTacSmoke", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("MaxTacSmoke" + System.currentTimeMillis(), settings,
                    new WorldOptions(2077L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        switch (++ticks) {
            case 20 -> {
                fixtureSkin = skin(PlayerSkin.Model.WIDE);
                for (PlayerModelPart part : PlayerModelPart.values()) mc.options.toggleModelPart(part, true);
                server(mc, player -> {
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ApogeeItems.MAXTAC_HELMET));
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ApogeeItems.MAXTAC_CHESTPLATE));
                    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ApogeeItems.MAXTAC_LEGGINGS));
                    player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ApogeeItems.MAXTAC_BOOTS));
                    player.serverLevel().setDayTime(6000);
                    player.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.getServer());
                });
                mc.options.hideGui = true;
                mc.options.fov().set(45);
                mc.player.setYRot(0);
                mc.player.setXRot(-10);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 50 -> {
                check(mc.player.getSkin().model() == PlayerSkin.Model.WIDE, "Ajuste con brazos clásicos");
                check(mc.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)
                        && mc.player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG), "Capas exteriores de la skin activas");
                check(!MaxtacVisorClient.auxiliaryActive(mc.player), "Dos ópticas por defecto al equipar");
                shot(mc, "01-dia-dos-opticas");
            }
            case 55 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 70 -> shot(mc, "02-espalda");
            case 75 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 80 -> {
                check(ClientPlayNetworking.canSend(MaxtacVisor.Toggle.TYPE), "Canal de solicitud del visor disponible");
                KeyMapping.click(InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_N, 0));
            }
            case 90 -> server(mc, player -> LOG.info("Visor en servidor: activo={}, canal de respuesta={}",
                    MaxtacVisor.auxiliaryActive(player), net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,MaxtacVisor.Status.TYPE)));
            case 105 -> {
                if (!MaxtacVisorClient.auxiliaryActive(mc.player) && activationWait++ < 200) {
                    ticks--;
                    return;
                }
                check(MaxtacVisorClient.auxiliaryActive(mc.player), "La respuesta del servidor activa las seis ópticas");
                shot(mc, "03-dia-seis-opticas");
            }
            case 110 -> server(mc, player -> {
                BlockPos center = player.blockPosition();
                int floor = center.getY()-1;
                for (int x=-7;x<=7;x++) for (int z=-7;z<=7;z++) for (int y=0;y<=8;y++) {
                    boolean wall = Math.abs(x)==7 || Math.abs(z)==7 || y==0 || y==8;
                    player.serverLevel().setBlock(new BlockPos(center.getX()+x,floor+y,center.getZ()+z),
                            wall ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
                }
                player.serverLevel().setDayTime(18000);
            });
            case 155 -> {
                check(mc.level.getBrightness(LightLayer.BLOCK,mc.player.blockPosition())==0, "Cámara sin luz de bloques");
                check(mc.level.getBrightness(LightLayer.SKY,mc.player.blockPosition())==0, "Cámara sin luz del cielo");
                shot(mc, "04-oscuridad-seis-opticas");
            }
            case 160 -> ClientPlayNetworking.send(MaxtacVisor.Toggle.INSTANCE);
            case 185 -> {
                if (MaxtacVisorClient.auxiliaryActive(mc.player) && deactivationWait++ < 200) {
                    ticks--;
                    return;
                }
                check(!MaxtacVisorClient.auxiliaryActive(mc.player), "Desactivar devuelve las dos ópticas principales");
                shot(mc, "05-oscuridad-dos-opticas");
            }
            case 190 -> server(mc, player -> player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY));
            case 210 -> ClientPlayNetworking.send(MaxtacVisor.Toggle.INSTANCE);
            case 230 -> {
                check(!MaxtacVisorClient.auxiliaryActive(mc.player), "El servidor rechaza activar el visor sin casco");
                server(mc, player -> player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ApogeeItems.MAXTAC_HELMET)));
            }
            case 255 -> {
                check(!MaxtacVisorClient.auxiliaryActive(mc.player), "Reequipar vuelve al modo básico");
                shot(mc,"06-casco-reequipado");
            }
            case 275 -> server(mc, player -> {
                BlockPos center = player.blockPosition();
                for (int x=-7;x<=7;x++) for (int z=-7;z<=7;z++) for (int y=0;y<8;y++) {
                    player.serverLevel().setBlock(center.offset(x,y,z), Blocks.AIR.defaultBlockState(),3);
                }
                player.serverLevel().setDayTime(6000);
            });
            case 290 -> fixtureSkin = skin(PlayerSkin.Model.SLIM);
            case 320 -> {
                check(mc.player.getSkin().model() == PlayerSkin.Model.SLIM, "Ajuste con brazos delgados");
                shot(mc,"07-skin-delgada");
            }
            case 325 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 345 -> shot(mc,"08-skin-delgada-espalda");
            case 365 -> {
                LOG.info(FAILURES.isEmpty() ? "MAXTAC_SMOKE_PASSED" : "MAXTAC_SMOKE_FAILED " + FAILURES);
                mc.stop();
            }
            default -> {}
        }
    }

    private static PlayerSkin skin(PlayerSkin.Model model) {
        for (int i=0;i<100;i++) {
            PlayerSkin candidate = DefaultPlayerSkin.get(new UUID(2077L,i));
            if (candidate.model() == model) return candidate;
        }
        throw new IllegalStateException("No se encontró skin de prueba " + model);
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        var server=mc.getSingleplayerServer();
        var id=mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void check(boolean passed,String description) {
        LOG.info("{}: {}",description,passed ? "OK" : "ERROR");
        if (!passed) FAILURES.add(description);
    }

    private static void shot(Minecraft mc,String name) {
        Screenshot.grab(mc.gameDirectory,"maxtac-"+name+".png",mc.getMainRenderTarget(),message -> LOG.info("Captura {}",name));
    }
}
