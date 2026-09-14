package dev.apogee.sandevistan;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One running activation. Times are server game times. */
final class ActiveSession {
    final UUID wearer;
    final long startTime;
    final long maxEndTime;
    long endTime;
    /** Movement bonus currently applied while it eases in; NaN before the first update. */
    double appliedSpeedBonus = Double.NaN;
    /** Living entities that were inside this field as valid targets at some point. */
    final Set<UUID> affected = new HashSet<>();

    ActiveSession(UUID wearer, long startTime, long endTime, long maxEndTime) {
        this.wearer = wearer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxEndTime = maxEndTime;
    }
}
