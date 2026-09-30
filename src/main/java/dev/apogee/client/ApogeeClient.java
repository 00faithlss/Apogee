package dev.apogee.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.Apogee;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.client.fx.AfterimageRenderer;
import dev.apogee.client.fx.SandevistanEffects;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.model.SandevistanModel;
import dev.apogee.client.render.MantisArmsLayer;
import dev.apogee.client.render.SandevistanImplantRenderer;
import dev.apogee.client.tooltip.MantisTooltipContent;
import dev.apogee.client.tooltip.SandevistanTooltipContent;
import dev.apogee.item.MantisBladesItem;
import dev.apogee.network.ApogeeNetworking.MantisSwing;
import dev.apogee.network.ApogeeNetworking.MantisSync;
import dev.apogee.network.ApogeeNetworking.RulesSync;
import dev.apogee.network.ApogeeNetworking.TimeFieldSync;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.network.ApogeeNetworking.WearerStatus;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ApogeeClient implements ClientModInitializer {
    public static final KeyMapping ACTIVATE_SANDEVISTAN = new KeyMapping(
            "key.apogee.activate_sandevistan", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.apogee");

    public static final KeyMapping TOGGLE_MANTIS = new KeyMapping(
            "key.apogee.toggle_mantis", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.apogee");

    private static long clientTicks;

    @Override
    public void onInitializeClient() {
        ApogeeClientConfig.load();
        KeyBindingHelper.registerKeyBinding(ACTIVATE_SANDEVISTAN);
        KeyBindingHelper.registerKeyBinding(TOGGLE_MANTIS);
        dev.apogee.client.compat.immersivearmors.SteamDashClient.register();

        EntityModelLayerRegistry.registerModelLayer(SandevistanModel.LAYER, SandevistanModel::createLayer);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                helper.register(new MantisArmsLayer(playerRenderer));
            }
        });
        TrinketRendererRegistry.registerRenderer(ApogeeItems.SANDEVISTAN, SandevistanImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.SANDEVISTAN, SandevistanImplantRenderer.INSTANCE);

        ClientPlayNetworking.registerGlobalReceiver(RulesSync.TYPE, (payload, context) -> ApogeeClientState.acceptRules(payload.rules()));
        ClientPlayNetworking.registerGlobalReceiver(WearerStatus.TYPE, (payload, context) -> {
            WearerStatus previous = ApogeeClientState.status();
            ApogeeClientState.acceptStatus(payload);
            SandevistanEffects.onStatus(previous, payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(MantisSync.TYPE, (payload, context) -> MantisClient.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(MantisSwing.TYPE, (payload, context) -> MantisClient.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(TimeFieldSync.TYPE, (payload, context) -> {
            IntSet previous = ApogeeClientState.acceptField(payload);
            SandevistanEffects.onWearers(previous, ApogeeClientState.currentWearers());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ApogeeClient::resetSession));

        ClientTickEvents.START_CLIENT_TICK.register(MantisClient::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ApogeeClient::onEndTick);
        HudRenderCallback.EVENT.register((graphics, deltaTracker) ->
                SandevistanEffects.renderHud(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> AfterimageRenderer.render(context, clientTicks));

        if (FabricLoader.getInstance().isModLoaded("simplytooltips")) {
            dev.apogee.client.compat.simplytooltips.SimplyTooltipsCompat.register();
        } else {
            Apogee.LOGGER.warn("Simply Tooltips is not installed; the Sandevistan and the Mantis Blades use plain tooltips.");
            ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
                if (stack.getItem() instanceof MantisBladesItem blades) {
                    lines.add(MantisTooltipContent.badge().copy().withStyle(ChatFormatting.YELLOW));
                    MantisTooltipContent.plainLines(blades.variant()).forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                    return;
                }
                if (!stack.is(ApogeeItems.SANDEVISTAN)) {
                    return;
                }
                lines.add(SandevistanTooltipContent.badge().copy().withStyle(ChatFormatting.AQUA));
                SandevistanTooltipContent.descriptionLines().forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                for (SandevistanTooltipContent.Stat stat : SandevistanTooltipContent.stats()) {
                    lines.add(Component.empty().append(stat.label().copy().withStyle(ChatFormatting.DARK_GRAY))
                            .append(": ").append(stat.value().copy().withStyle(ChatFormatting.AQUA)));
                }
                lines.add(SandevistanTooltipContent.cooldownNote().copy().withStyle(ChatFormatting.DARK_GRAY));
            });
        }
    }

    private static void resetSession() {
        ApogeeClientState.reset();
        MantisClient.reset();
        SandevistanEffects.reset();
        AfterimageRenderer.reset();
    }

    private static void onEndTick(Minecraft client) {
        clientTicks++;
        while (ACTIVATE_SANDEVISTAN.consumeClick()) {
            if (client.player != null && client.screen == null && ClientPlayNetworking.canSend(ToggleSandevistan.TYPE)) {
                ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            }
        }
        while (TOGGLE_MANTIS.consumeClick()) {
            if (client.player != null && client.screen == null && ClientPlayNetworking.canSend(ToggleMantis.TYPE)) {
                ClientPlayNetworking.send(ToggleMantis.INSTANCE);
            }
        }
        if (client.level == null) {
            return;
        }
        if (!client.isPaused()) {
            SandevistanEffects.tick();
            AfterimageRenderer.tick(clientTicks);
        }
    }
}
