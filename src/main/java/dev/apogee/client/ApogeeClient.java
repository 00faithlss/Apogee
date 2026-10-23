package dev.apogee.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apogee.Apogee;
import dev.apogee.client.bloodpump.BloodPumpArmRenderer;
import dev.apogee.client.bloodpump.BloodPumpClient;
import dev.apogee.client.bloodpump.BloodPumpHud;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.client.berserk.BerserkImplantRenderer;
import dev.apogee.client.fx.AfterimageRenderer;
import dev.apogee.client.fx.BerserkEffects;
import dev.apogee.client.tooltip.BerserkTooltipContent;
import dev.apogee.item.BerserkItem;
import dev.apogee.network.ApogeeNetworking.BerserkRulesSync;
import dev.apogee.network.ApogeeNetworking.BerserkStatus;
import dev.apogee.client.fx.SandevistanEffects;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.model.SandevistanModel;
import dev.apogee.client.render.MantisArmsLayer;
import dev.apogee.client.render.SandevistanImplantRenderer;
import dev.apogee.client.tooltip.BloodPumpTooltipContent;
import dev.apogee.client.tooltip.MantisTooltipContent;
import dev.apogee.client.tooltip.SandevistanTooltipContent;
import dev.apogee.item.ArmCyberwareItem;
import dev.apogee.network.ApogeeNetworking.BloodPumpUse;
import dev.apogee.network.ApogeeNetworking.MantisSwing;
import dev.apogee.network.ApogeeNetworking.MantisSync;
import dev.apogee.network.ApogeeNetworking.RulesSync;
import dev.apogee.network.ApogeeNetworking.TimeFieldSync;
import dev.apogee.network.ApogeeNetworking.ToggleBloodPump;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.network.ApogeeNetworking.ToggleSandevistan;
import dev.apogee.network.ApogeeNetworking.WearerStatus;
import dev.apogee.registry.ApogeeBlocks;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.client.ripperdoc.RipperdocScreen;
import dev.apogee.client.ripperdoc.RipperdocTableItemRenderer;
import dev.apogee.client.ripperdoc.RipperdocTableRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
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

    public static final KeyMapping ACTIVATE_BLOOD_PUMP = new KeyMapping(
            "key.apogee.activate_blood_pump", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.apogee");

    private static long clientTicks;

    @Override
    public void onInitializeClient() {
        ApogeeClientConfig.load();
        dev.apogee.client.armor.MaxtacVisorClient.register();
        dev.apogee.client.armor.TraumaHelmetClient.register();
        KeyBindingHelper.registerKeyBinding(ACTIVATE_SANDEVISTAN);
        KeyBindingHelper.registerKeyBinding(TOGGLE_MANTIS);
        dev.apogee.client.tendons.TendonsClient.register();
        KeyBindingHelper.registerKeyBinding(ACTIVATE_BLOOD_PUMP);

        EntityModelLayerRegistry.registerModelLayer(SandevistanModel.LAYER, SandevistanModel::createLayer);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                helper.register(new MantisArmsLayer(playerRenderer));
            }
        });
        TrinketRendererRegistry.registerRenderer(ApogeeItems.SANDEVISTAN, SandevistanImplantRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(ApogeeItems.BLOOD_PUMP, BloodPumpArmRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(ApogeeItems.BERSERK_ZETATECH, BerserkImplantRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(ApogeeItems.BERSERK_BIODYNE, BerserkImplantRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(ApogeeItems.BERSERK_MILITECH, BerserkImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.SANDEVISTAN, SandevistanImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.BERSERK_ZETATECH, BerserkImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.BERSERK_BIODYNE, BerserkImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.BERSERK_MILITECH, BerserkImplantRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.NANOWIRE, dev.apogee.client.nanowire.NanowireItemRenderer.INSTANCE);
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeItems.NANOWIRE_WHITE, dev.apogee.client.nanowire.NanowireItemRenderer.INSTANCE);
        BlockEntityRenderers.register(ApogeeBlocks.RIPPERDOC_TABLE_ENTITY, context -> new RipperdocTableRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ApogeeBlocks.RIPPERDOC_TABLE_ITEM, RipperdocTableItemRenderer.INSTANCE);
        EntityRendererRegistry.register(ApogeeBlocks.RIPPERDOC_SEAT, NoopRenderer::new);
        MenuScreens.register(ApogeeBlocks.RIPPERDOC_MENU, RipperdocScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(RulesSync.TYPE, (payload, context) -> ApogeeClientState.acceptRules(payload.rules()));
        ClientPlayNetworking.registerGlobalReceiver(WearerStatus.TYPE, (payload, context) -> {
            WearerStatus previous = ApogeeClientState.status();
            ApogeeClientState.acceptStatus(payload);
            SandevistanEffects.onStatus(previous, payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(BloodPumpUse.TYPE, (payload, context) -> BloodPumpClient.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(BerserkRulesSync.TYPE, (payload, context) -> ApogeeClientState.acceptBerserkRules(payload.rules()));
        ClientPlayNetworking.registerGlobalReceiver(BerserkStatus.TYPE, (payload, context) -> {
            BerserkStatus previous = ApogeeClientState.berserkStatus();
            ApogeeClientState.acceptBerserkStatus(payload);
            BerserkEffects.onStatus(previous, payload);
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
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            SandevistanEffects.renderHud(graphics, partialTick);
            BerserkEffects.renderHud(graphics, partialTick);
            dev.apogee.client.mantis.ArmCyberwareHud.render(graphics, partialTick);
            BloodPumpHud.render(graphics, partialTick);
        });
        ClientTickEvents.END_CLIENT_TICK.register(dev.apogee.client.mantis.ArmCyberwareHud::tick);
        ClientTickEvents.END_CLIENT_TICK.register(BloodPumpHud::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> AfterimageRenderer.render(context, clientTicks));

        if (FabricLoader.getInstance().isModLoaded("simplytooltips")) {
            dev.apogee.client.compat.simplytooltips.SimplyTooltipsCompat.register();
        } else {
            Apogee.LOGGER.warn("Simply Tooltips is not installed; the Sandevistan and the arm cyberware use plain tooltips.");
            ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
                if (stack.getItem() instanceof ArmCyberwareItem cyberware) {
                    lines.add(MantisTooltipContent.badge().copy().withStyle(ChatFormatting.YELLOW));
                    MantisTooltipContent.plainLines(cyberware.variant()).forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                    return;
                }
                if (stack.is(ApogeeItems.BLOOD_PUMP)) {
                    lines.add(BloodPumpTooltipContent.badge().copy().withStyle(ChatFormatting.RED));
                    BloodPumpTooltipContent.descriptionLines().forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                    BloodPumpTooltipContent.plainLines().forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.DARK_GRAY)));
                    return;
                }
                if (stack.getItem() instanceof BerserkItem berserk) {
                    lines.add(BerserkTooltipContent.badge(berserk.variant()).copy().withStyle(ChatFormatting.RED));
                    BerserkTooltipContent.descriptionLines(berserk.variant()).forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                    for (BerserkTooltipContent.Stat stat : BerserkTooltipContent.stats(berserk.variant())) {
                        lines.add(Component.empty().append(stat.label().copy().withStyle(ChatFormatting.DARK_GRAY))
                                .append(": ").append(stat.value().copy().withStyle(ChatFormatting.RED)));
                    }
                    lines.add(BerserkTooltipContent.cooldownNote().copy().withStyle(ChatFormatting.DARK_GRAY));
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
        BerserkEffects.reset();
        AfterimageRenderer.reset();
        BloodPumpClient.reset();
        BloodPumpArmRenderer.clear();
    }

    private static void onEndTick(Minecraft client) {
        clientTicks++;
        while (ACTIVATE_SANDEVISTAN.consumeClick()) {
            if (client.player != null && client.screen == null && ClientPlayNetworking.canSend(ToggleSandevistan.TYPE)) {
                ClientPlayNetworking.send(ToggleSandevistan.INSTANCE);
            }
        }
        while (ACTIVATE_BLOOD_PUMP.consumeClick()) {
            if (client.player != null && client.screen == null && ClientPlayNetworking.canSend(ToggleBloodPump.TYPE)) {
                ClientPlayNetworking.send(ToggleBloodPump.INSTANCE);
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
            BerserkEffects.tick();
            AfterimageRenderer.tick(clientTicks);
        }
    }
}
