package dev.apogee.sandevistan;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;

/** Remembers when a mob and a player last exchanged damage, in either direction. */
final class CombatMemory {
    private static final long MAX_RETENTION_TICKS = 20L * 120L;

    private record Pair(UUID mob, UUID player) {
    }

    private final Object2LongOpenHashMap<Pair> lastExchange = new Object2LongOpenHashMap<>();

    void record(UUID mob, UUID player, long gameTime) {
        lastExchange.put(new Pair(mob, player), gameTime);
    }

    boolean recent(UUID mob, UUID player, long gameTime, int memoryTicks) {
        Pair key = new Pair(mob, player);
        if (!lastExchange.containsKey(key)) {
            return false;
        }
        return gameTime - lastExchange.getLong(key) <= memoryTicks;
    }

    void prune(long gameTime) {
        lastExchange.object2LongEntrySet().removeIf((Object2LongMap.Entry<Pair> entry) ->
                gameTime - entry.getLongValue() > MAX_RETENTION_TICKS || entry.getLongValue() > gameTime);
    }

    void clear() {
        lastExchange.clear();
    }
}
