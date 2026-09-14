package dev.apogee.client;

import dev.apogee.config.SandevistanRules;
import dev.apogee.network.ApogeeNetworking.TimeFieldSync;
import dev.apogee.network.ApogeeNetworking.WearerStatus;
import dev.apogee.time.TimeScaledEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/** What this client knows from the host: rules, its own status and the time fields around it. */
public final class ApogeeClientState {
    private static SandevistanRules rules = SandevistanRules.DEFAULTS;
    private static WearerStatus status = WearerStatus.IDLE;
    private static IntSet slowed = new IntOpenHashSet();
    private static IntSet wearers = new IntOpenHashSet();
    private static ClientLevel fieldLevel;

    private ApogeeClientState() {
    }

    public static SandevistanRules rules() {
        return rules;
    }

    public static WearerStatus status() {
        return status;
    }

    static void acceptRules(SandevistanRules synced) {
        rules = synced.sanitized();
    }

    static void acceptStatus(WearerStatus synced) {
        status = synced;
    }

    /** Returns the previous wearer set so effects can react to joins and leaves. */
    static IntSet acceptField(TimeFieldSync sync) {
        IntSet previous = currentWearers();
        slowed = new IntOpenHashSet(sync.slowedEntities());
        wearers = new IntOpenHashSet(sync.activeWearers());
        fieldLevel = Minecraft.getInstance().level;
        return previous;
    }

    static void reset() {
        rules = SandevistanRules.DEFAULTS;
        status = WearerStatus.IDLE;
        slowed = new IntOpenHashSet();
        wearers = new IntOpenHashSet();
        fieldLevel = null;
    }

    public static IntSet currentWearers() {
        return fieldLevel != null && fieldLevel == Minecraft.getInstance().level ? wearers : IntSet.of();
    }

    public static boolean isSlowed(Entity entity) {
        return fieldLevel != null && entity.level() == fieldLevel && slowed.contains(entity.getId());
    }

    public static boolean isActiveWearer(Entity entity) {
        return fieldLevel != null && entity.level() == fieldLevel && wearers.contains(entity.getId());
    }

    /**
     * Interpolation factor for rendering an entity. Slowed entities only advance a real tick
     * every few frames, so their partial tick is their own accumulator progress instead of the
     * global one, which keeps their motion smooth rather than stepping.
     */
    public static float partialTick(Entity entity, float partialTick) {
        if (!isSlowed(entity)) {
            return partialTick;
        }
        float progress = TimeScaledEntity.of(entity).apogee$timeAccumulator() + rules.timeScale() * partialTick;
        return Math.min(1.0F, progress);
    }

    public static long clientGameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }
}
