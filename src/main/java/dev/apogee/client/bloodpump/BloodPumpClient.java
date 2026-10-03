package dev.apogee.client.bloodpump;

import dev.apogee.bloodpump.BloodPumpRules;
import dev.apogee.network.ApogeeNetworking.BloodPumpUse;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** When each player's Blood Pump last fired, as server game times, from {@link BloodPumpUse}. */
public final class BloodPumpClient {
    private static final Int2LongOpenHashMap STARTS = new Int2LongOpenHashMap();

    private BloodPumpClient() {
    }

    public static void accept(BloodPumpUse payload) {
        if (payload.startTime() <= 0L) {
            STARTS.remove(payload.entityId());
        } else {
            STARTS.put(payload.entityId(), payload.startTime());
        }
    }

    public static void reset() {
        STARTS.clear();
    }

    /** Ticks since the pump fired, or a negative number when it has not (or not recently). */
    public static float elapsed(Entity entity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !STARTS.containsKey(entity.getId())) {
            return -1.0F;
        }
        float elapsed = mc.level.getGameTime() + partialTick - STARTS.get(entity.getId());
        return elapsed >= BloodPumpRules.COOLDOWN_TICKS ? -1.0F : elapsed;
    }

    /** Ticks left of the recharge, 0 when ready. */
    public static float cooldownLeft(Entity entity, float partialTick) {
        float elapsed = elapsed(entity, partialTick);
        return elapsed < 0.0F ? 0.0F : BloodPumpRules.COOLDOWN_TICKS - elapsed;
    }
}
