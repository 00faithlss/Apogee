package dev.apogee.item;

import dev.apogee.mantis.MantisVariant;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Reusable arm implant with no durability; equipped in the Trinkets slot cyberware/arms. A
 * housing on the inner side of each forearm feeds a red monofilament wire held between the
 * hands, lashed out one arm at a time or with both at once.
 */
public class NanowireItem extends CyberwareItem implements ArmCyberwareItem {
    public NanowireItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public MantisVariant variant() {
        return MantisVariant.NANOWIRE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        return false;
    }
}
