package dev.apogee.registry;

import dev.apogee.Apogee;
import dev.apogee.effect.BleedingEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public final class ApogeeEffects {
    public static final Holder<MobEffect> BLEEDING = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, Apogee.id("bleeding"), new BleedingEffect());

    private ApogeeEffects() {
    }

    public static void register() {
    }
}
