package dev.apogee.item;

import dev.apogee.Apogee;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Entre diamante y netherita: misma defensa total 20, dureza 2,5, retroceso 0,05 y durabilidad intermedia. */
public final class TraumaArmorMaterial {
    public static final int DURABILITY_MULTIPLIER = 35;

    public static final Holder<ArmorMaterial> MATERIAL = register();

    private TraumaArmorMaterial() {}

    private static Holder<ArmorMaterial> register() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        ArmorMaterial material = new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(Items.NETHERITE_INGOT),
                List.of(new ArmorMaterial.Layer(Apogee.id("trauma_team"))), 2.5F, 0.05F);
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Apogee.id("trauma_team"), material);
    }
}
