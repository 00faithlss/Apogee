package dev.apogee.armor;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class TraumaHelmet {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private record State(ItemStack helmet, boolean active, int lastToggle) {}

    private TraumaHelmet() {}

    public static boolean isOpen(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.active && state.helmet == player.getItemBySlot(EquipmentSlot.HEAD)
                && state.helmet.is(ApogeeItems.TRAUMA_TEAM_HELMET) && player.isAlive();
    }

    public static void toggle(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(ApogeeItems.TRAUMA_TEAM_HELMET) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        State previous = STATES.get(player.getUUID());
        if (previous != null && previous.helmet == helmet && player.tickCount - previous.lastToggle < 4) {
            return;
        }
        boolean active = !isOpen(player);
        STATES.put(player.getUUID(), new State(helmet, active, player.tickCount));
        broadcast(player, active);
        player.displayClientMessage(Component.translatable(active ? "message.apogee.trauma_helmet_open" : "message.apogee.trauma_helmet_closed"), true);
    }

    private static void send(ServerPlayer target, Status status) {
        if (ServerPlayNetworking.canSend(target, Status.TYPE)) {
            ServerPlayNetworking.send(target, status);
        }
    }

    private static void broadcast(ServerPlayer player, boolean active) {
        Status status = new Status(player.getUUID(), active);
        send(player, status);
        for (ServerPlayer observer : PlayerLookup.tracking(player)) {
            send(observer, status);
        }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(Toggle.TYPE, Toggle.CODEC);
        PayloadTypeRegistry.playS2C().register(Status.TYPE, Status.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Toggle.TYPE, (payload, context) ->
                context.server().execute(() -> toggle(context.player())));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            STATES.remove(handler.player.getUUID());
            send(handler.player, new Status(handler.player.getUUID(), false));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUUID()));
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> {
            if (entity instanceof ServerPlayer wearer) {
                send(observer, new Status(wearer.getUUID(), isOpen(wearer)));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var iterator = STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                ServerPlayer wearer = server.getPlayerList().getPlayer(entry.getKey());
                State state = entry.getValue();
                if (wearer == null || !wearer.isAlive() || state.helmet != wearer.getItemBySlot(EquipmentSlot.HEAD)
                        || !state.helmet.is(ApogeeItems.TRAUMA_TEAM_HELMET)) {
                    iterator.remove();
                    if (wearer != null) {
                        broadcast(wearer, false);
                    }
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
    }

    public record Toggle() implements CustomPacketPayload {
        public static final Toggle INSTANCE = new Toggle();
        public static final Type<Toggle> TYPE = new Type<>(Apogee.id("trauma_helmet_toggle"));
        public static final StreamCodec<FriendlyByteBuf, Toggle> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Status(UUID playerId, boolean active) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Apogee.id("trauma_helmet_status"));
        public static final StreamCodec<FriendlyByteBuf, Status> CODEC = StreamCodec.of(
                (buffer, value) -> { buffer.writeUUID(value.playerId); buffer.writeBoolean(value.active); },
                buffer -> new Status(buffer.readUUID(), buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
