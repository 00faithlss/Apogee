package dev.apogee.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.armor.TraumaHelmet;
import dev.apogee.client.armor.TraumaHelmetClient;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
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

/** Prueba el casco abatible, el parpadeo y el ajuste de las cuatro piezas dentro de Minecraft. */
public final class TraumaClientSmoke implements ClientModInitializer {
    private static final Logger LOG=LoggerFactory.getLogger("TraumaSmoke");
    private static final List<String> FAILURES=new ArrayList<>();
    private static boolean requested;
    private static int ticks, waiting;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("apogee.traumaSmoke")) ClientTickEvents.END_CLIENT_TICK.register(TraumaClientSmoke::tick);
    }

    private static void tick(Minecraft mc) {
        if (!requested && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility=false;
            mc.setScreen(new TitleScreen());
        }
        if (!requested && mc.screen instanceof TitleScreen) {
            requested=true;
            mc.options.pauseOnLostFocus=false;
            var settings=new LevelSettings("TraumaSmoke",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("TraumaSmoke"+System.currentTimeMillis(),settings,new WorldOptions(207703L,false,false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
            return;
        }
        if (mc.player==null || mc.getSingleplayerServer()==null || mc.screen!=null) return;
        switch (++ticks) {
            case 20 -> {
                setSkin(PlayerSkin.Model.WIDE);
                for (PlayerModelPart part:PlayerModelPart.values()) mc.options.toggleModelPart(part,true);
                server(mc,p -> {
                    p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ApogeeItems.TRAUMA_TEAM_HELMET));
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ApogeeItems.TRAUMA_TEAM_CHESTPLATE));
                    p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ApogeeItems.TRAUMA_TEAM_LEGGINGS));
                    p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ApogeeItems.TRAUMA_TEAM_BOOTS));
                    p.serverLevel().setDayTime(6000);
                    p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.getServer());
                });
                mc.options.hideGui=true;mc.options.fov().set(50);
                mc.player.setYRot(0);mc.player.setXRot(-10);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.mouseHandler.releaseMouse();
            }
            case 50 -> {
                check(mc.player.getSkin().model()==PlayerSkin.Model.WIDE,"Skin clásica con capas exteriores");
                check(!TraumaHelmetClient.isOpen(mc.player),"Casco cerrado al equipar");
                shot(mc,"01-cerrado");
            }
            case 60 -> KeyMapping.click(InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_N,0));
            case 85 -> {
                if (!TraumaHelmetClient.isOpen(mc.player) && waiting++<200) {ticks--;return;}
                check(TraumaHelmetClient.isOpen(mc.player),"N abre el casco mediante el servidor");
                waiting=0;
            }
            case 110 -> {
                check(TraumaHelmetClient.openness(mc.player,0)>.99f,"La apertura alcanza su posición final");
                shot(mc,"02-abierto-lamina");
            }
            case 115 -> mc.player.setYRot(25);
            case 130 -> shot(mc,"03-abierto-perspectiva");
            case 135 -> KeyMapping.click(InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_N,0));
            case 160 -> {
                if (TraumaHelmetClient.isOpen(mc.player) && waiting++<200) {ticks--;return;}
                check(!TraumaHelmetClient.isOpen(mc.player),"N vuelve a cerrar el casco");
                waiting=0;
            }
            case 185 -> {
                check(TraumaHelmetClient.openness(mc.player,0)<.01f,"El cierre alcanza su posición inicial");
                server(mc,p -> {
                    BlockPos c=p.blockPosition();
                    for (int x=-7;x<=7;x++) for (int z=-7;z<=7;z++) for (int y=-1;y<=7;y++) {
                        boolean wall=Math.abs(x)==7 || Math.abs(z)==7 || y==-1 || y==7;
                        p.serverLevel().setBlock(c.offset(x,y,z),wall?Blocks.BLACK_CONCRETE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                    }
                    p.serverLevel().setDayTime(18000);
                });
                mc.player.setYRot(0);
            }
            case 230 -> {
                if (Math.floorMod(mc.level.getGameTime(),20)!=3) {ticks--;return;}
                check(mc.level.getBrightness(LightLayer.BLOCK,mc.player.blockPosition())==0
                        && mc.level.getBrightness(LightLayer.SKY,mc.player.blockPosition())==0,"Cámara con luz ambiental cero");
                check(TraumaHelmetClient.indicatorLit(mc.player),"Fase roja encendida");
                shot(mc,"04-rojo-encendido-oscuridad");
            }
            case 245 -> {
                if (Math.floorMod(mc.level.getGameTime(),20)!=13) {ticks--;return;}
                check(!TraumaHelmetClient.indicatorLit(mc.player),"Fase roja apagada");
                shot(mc,"05-rojo-apagado-oscuridad");
            }
            case 250 -> server(mc,p -> p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY));
            case 270 -> ClientPlayNetworking.send(TraumaHelmet.Toggle.INSTANCE);
            case 290 -> {
                check(!TraumaHelmetClient.isOpen(mc.player),"Abrir sin el casco se rechaza");
                server(mc,p -> {
                    p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ApogeeItems.TRAUMA_TEAM_HELMET));
                    BlockPos c=p.blockPosition();
                    for (int x=-7;x<=7;x++) for (int z=-7;z<=7;z++) for (int y=0;y<=7;y++) p.serverLevel().setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
                    p.serverLevel().setDayTime(6000);
                });
                setSkin(PlayerSkin.Model.SLIM);
            }
            case 325 -> {
                check(mc.player.getSkin().model()==PlayerSkin.Model.SLIM,"Ajuste con brazos delgados");
                check(!TraumaHelmetClient.isOpen(mc.player),"Reequipar vuelve al casco cerrado");
                shot(mc,"06-skin-delgada");
            }
            case 330 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 350 -> shot(mc,"07-espalda");
            case 370 -> {
                LOG.info(FAILURES.isEmpty()?"TRAUMA_SMOKE_PASSED":"TRAUMA_SMOKE_FAILED "+FAILURES);
                mc.stop();
            }
            default -> {}
        }
    }

    private static void setSkin(PlayerSkin.Model model) {
        for (int i=0;i<100;i++) {
            PlayerSkin skin=DefaultPlayerSkin.get(new UUID(2077L,i));
            if (skin.model()==model) {MaxtacClientSmoke.fixtureSkin=skin;return;}
        }
        throw new IllegalStateException("Skin de prueba no encontrada");
    }

    private static void server(Minecraft mc,Consumer<ServerPlayer> action) {
        var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }
    private static void check(boolean pass,String message) {
        LOG.info("{}: {}",message,pass?"OK":"ERROR");if (!pass) FAILURES.add(message);
    }
    private static void shot(Minecraft mc,String name) {
        Screenshot.grab(mc.gameDirectory,"trauma-"+name+".png",mc.getMainRenderTarget(),message -> LOG.info("Captura {}",name));
    }
}
