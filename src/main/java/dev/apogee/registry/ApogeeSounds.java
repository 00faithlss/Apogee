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
    public static final SoundEvent MANTIS_ELECTRIC_DEPLOY = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_electric_deploy"));
    public static final SoundEvent MANTIS_ELECTRIC_SWING = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_electric_swing"));
    public static final SoundEvent MANTIS_ELECTRIC_HIT = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_electric_hit"));
    public static final SoundEvent MANTIS_ELECTRIC_THRUST = SoundEvent.createVariableRangeEvent(Apogee.id("mantis_electric_thrust"));
    public static final SoundEvent NANOWIRE_DEPLOY = SoundEvent.createVariableRangeEvent(Apogee.id("nanowire_deploy"));
    public static final SoundEvent NANOWIRE_RETRACT = SoundEvent.createVariableRangeEvent(Apogee.id("nanowire_retract"));
    public static final SoundEvent NANOWIRE_SWING = SoundEvent.createVariableRangeEvent(Apogee.id("nanowire_swing"));
    public static final SoundEvent NANOWIRE_HIT = SoundEvent.createVariableRangeEvent(Apogee.id("nanowire_hit"));
    public static final SoundEvent NANOWIRE_HEAVY = SoundEvent.createVariableRangeEvent(Apogee.id("nanowire_heavy"));
    public static final SoundEvent TENDONS_DOUBLE_JUMP = SoundEvent.createVariableRangeEvent(Apogee.id("tendons_double_jump"));
    public static final SoundEvent TENDONS_AIR_DASH = SoundEvent.createVariableRangeEvent(Apogee.id("tendons_air_dash"));

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
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_ELECTRIC_DEPLOY.getLocation(), MANTIS_ELECTRIC_DEPLOY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_ELECTRIC_SWING.getLocation(), MANTIS_ELECTRIC_SWING);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_ELECTRIC_HIT.getLocation(), MANTIS_ELECTRIC_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MANTIS_ELECTRIC_THRUST.getLocation(), MANTIS_ELECTRIC_THRUST);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NANOWIRE_DEPLOY.getLocation(), NANOWIRE_DEPLOY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NANOWIRE_RETRACT.getLocation(), NANOWIRE_RETRACT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NANOWIRE_SWING.getLocation(), NANOWIRE_SWING);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NANOWIRE_HIT.getLocation(), NANOWIRE_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NANOWIRE_HEAVY.getLocation(), NANOWIRE_HEAVY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TENDONS_DOUBLE_JUMP.getLocation(), TENDONS_DOUBLE_JUMP);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TENDONS_AIR_DASH.getLocation(), TENDONS_AIR_DASH);
    }
}
