package dev.apogee.registry;

import dev.apogee.Apogee;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ApogeeSounds {
    public static final SoundEvent SANDEVISTAN_ACTIVATE = SoundEvent.createVariableRangeEvent(Apogee.id("sandevistan_activate"));
    public static final SoundEvent SANDEVISTAN_DEACTIVATE = SoundEvent.createVariableRangeEvent(Apogee.id("sandevistan_deactivate"));

    private ApogeeSounds() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, SANDEVISTAN_ACTIVATE.getLocation(), SANDEVISTAN_ACTIVATE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SANDEVISTAN_DEACTIVATE.getLocation(), SANDEVISTAN_DEACTIVATE);
    }
}
