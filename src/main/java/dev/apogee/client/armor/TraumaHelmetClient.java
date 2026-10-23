package dev.apogee.client.armor;

import dev.apogee.armor.TraumaHelmet;
import dev.apogee.registry.ApogeeItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class TraumaHelmetClient {
    private record Motion(float from, boolean open, double since) {
        float amount(double now) {
            float t = Mth.clamp((float)((now - since) / 10.0), 0, 1);
            float smooth = t * t * (3 - 2 * t);
            return Mth.lerp(smooth, from, open ? 1 : 0);
        }
    }

    private static final Map<UUID, Motion> MOTIONS = new HashMap<>();

    private TraumaHelmetClient() {}

    public static boolean isOpen(@Nullable Entity entity) {
        Motion motion = entity == null ? null : MOTIONS.get(entity.getUUID());
        return wearingHelmet(entity) && motion != null && motion.open;
    }

    private static boolean wearingHelmet(@Nullable Entity entity) {
        return entity instanceof LivingEntity wearer
                && wearer.getItemBySlot(EquipmentSlot.HEAD).is(ApogeeItems.TRAUMA_TEAM_HELMET);
    }

    public static float openness(@Nullable Entity entity, float partialTick) {
        if (!wearingHelmet(entity)) return 0;
        Motion motion = MOTIONS.get(entity.getUUID());
        return motion == null ? 0 : motion.amount(entity.level().getGameTime() + partialTick);
    }

    public static boolean indicatorLit(@Nullable Entity entity) {
        return entity != null && Math.floorMod(entity.level().getGameTime(), 20) < 8;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TraumaHelmet.Status.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft client = context.client();
            if (client.level == null) return;
            double now = client.level.getGameTime();
            Motion previous = MOTIONS.get(payload.playerId());
            float from = previous == null ? 0 : previous.amount(now);
            MOTIONS.put(payload.playerId(), new Motion(from, payload.active(), now));
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MOTIONS.clear());
    }
}
