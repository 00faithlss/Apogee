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

/** Un poco por sobre la netherita: defensa total 24 (casco cerrado incluido), dureza 3,5 y retroceso 0,12. */
public final class MaxtacArmorMaterial {
    public static final int DURABILITY_MULTIPLIER = 42;

    public static final Holder<ArmorMaterial> MATERIAL = register();

    private MaxtacArmorMaterial() {}

    private static Holder<ArmorMaterial> register() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 4);
        defense.put(ArmorItem.Type.CHESTPLATE, 9);
        defense.put(ArmorItem.Type.LEGGINGS, 7);
        defense.put(ArmorItem.Type.BOOTS, 4);
        defense.put(ArmorItem.Type.BODY, 11);
        ArmorMaterial material = new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(Items.NETHERITE_INGOT),
                List.of(new ArmorMaterial.Layer(Apogee.id("maxtac"))), 3.5F, 0.12F);
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Apogee.id("maxtac"), material);
    }
}
