package dev.apogee.bloodpump;

import dev.apogee.network.ApogeeNetworking.BloodPumpUse;
import dev.apogee.network.ApogeeNetworking.ToggleBloodPump;
import dev.apogee.registry.ApogeeAttachments;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.registry.ApogeeSounds;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Server authority for the Blood Pump: validates the key press, applies the instant heal and the
 * regeneration window, keeps the recharge and tells clients when a pump fired.
 */
public final class BloodPumpManager {
    /** Regeneration left, in ticks, per player. Dropped when the player dies or leaves. */
    private static final Map<UUID, Integer> REGEN = new HashMap<>();

    private BloodPumpManager() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ToggleBloodPump.TYPE, (payload, context) -> use(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(BloodPumpManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> REGEN.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> REGEN.remove(handler.getPlayer().getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> REGEN.remove(newPlayer.getUUID()));
        // A player coming into view of another sees the pump as it is now.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer(), handler.getPlayer()));
    }

    public static boolean isEquipped(ServerPlayer player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(ApogeeItems.BLOOD_PUMP))
                .orElse(false);
    }

    public static long cooldownEnd(ServerPlayer player) {
        return player.getAttachedOrElse(ApogeeAttachments.BLOOD_PUMP_COOLDOWN_END, 0L);
    }

    public static void use(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !isEquipped(player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < cooldownEnd(player)) {
            return;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            player.displayClientMessage(Component.translatable("hud.apogee.blood_pump.full_health"), true);
            return;
        }
        player.heal(BloodPumpRules.INSTANT_HEAL);
        REGEN.put(player.getUUID(), BloodPumpRules.REGEN_TICKS);
        player.setAttached(ApogeeAttachments.BLOOD_PUMP_COOLDOWN_END, now + BloodPumpRules.COOLDOWN_TICKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ApogeeSounds.BLOOD_PUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player, player);
    }

    private static void tick(MinecraftServer server) {
        if (REGEN.isEmpty()) {
            return;
        }
        float perTick = BloodPumpRules.REGEN_HEAL / BloodPumpRules.REGEN_TICKS;
        REGEN.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                return true;
            }
            player.heal(perTick);
            int left = entry.getValue() - 1;
            if (left <= 0) {
                return true;
            }
            entry.setValue(left);
            return false;
        });
    }

    /** Tells {@code receiver} and everyone tracking {@code player} when the pump last fired. */
    private static void sync(ServerPlayer player, ServerPlayer receiver) {
        long end = cooldownEnd(player);
        long start = end > 0L ? end - BloodPumpRules.COOLDOWN_TICKS : 0L;
        BloodPumpUse payload = new BloodPumpUse(player.getId(), start);
        ServerPlayNetworking.send(receiver, payload);
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracker, payload);
        }
    }
}
