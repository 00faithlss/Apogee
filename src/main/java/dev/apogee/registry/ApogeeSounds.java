package dev.apogee.registry;

import dev.apogee.Apogee;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ApogeeSounds {
    public static final SoundEvent SANDEVISTAN_ACTIVATE = SoundEvent.createVariableRangeEvent(Apogee.id("sandevistan_activate"));
    public static final SoundEvent SANDEVISTAN_DEACTIVATE = SoundEvent.createVariableRangeEvent(Apogee.id("sandevistan_deactivate"));
    public static final SoundEvent RUDIMENTARY_IMPLANT = SoundEvent.createVariableRangeEvent(Apogee.id("rudimentary_implant"));
    public static final SoundEvent MANTIS_DEPLOY = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_deploy"));
    public static final SoundEvent MANTIS_RETRACT = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_retract"));
    public static final SoundEvent MANTIS_SWING = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_swing"));
    public static final SoundEvent MANTIS_HIT = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_hit"));
    public static final SoundEvent MANTIS_THRUST = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_thrust"));

    private ApogeeSounds() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, SANDEVISTAN_ACTIVATE.getLocation(), SANDEVISTAN_ACTIVATE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SANDEVISTAN_DEACTIVATE.getLocation(), SANDEVISTAN_DEACTIVATE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, RUDIMENTARY_IMPLANT.getLocation(), RUDIMENTARY_IMPLANT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_DEPLOY.getLocation(), MANTIS_DEPLOY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_RETRACT.getLocation(), MANTIS_RETRACT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_SWING.getLocation(), MANTIS_SWING);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_HIT.getLocation(), MANTIS_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_THRUST.getLocation(), MANTIS_THRUST);
    }
}
