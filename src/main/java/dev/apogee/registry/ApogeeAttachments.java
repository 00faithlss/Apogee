package dev.apogee.registry;

import com.mojang.serialization.Codec;
import dev.apogee.Apogee;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ApogeeAttachments {
    /**
     * Server game time at which the player's Sandevistan cooldown ends. Game time only advances
     * while the world ticks, so a paused singleplayer world pauses the cooldown too, and it is
     * saved with the player so reconnecting or swapping items cannot reset it.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<Long> COOLDOWN_END = AttachmentRegistry.<Long>builder()
            .persistent(Codec.LONG)
            .copyOnDeath()
            .buildAndRegister(Apogee.id("sandevistan_cooldown_end"));

    /** Set on projectiles launched by an active wearer; they keep the damage bonus until impact. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<Boolean> BOOSTED_PROJECTILE = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .buildAndRegister(Apogee.id("sandevistan_boosted"));

    private ApogeeAttachments() {
    }

    public static void register() {
        // Loaded for the static initializers.
    }
}
