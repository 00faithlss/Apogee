package dev.apogee.item;

import dev.apogee.mantis.MantisVariant;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Reusable arm implant with no durability; equipped in the Trinkets slot cyberware/arms. Its
 * tooltip is drawn on the client (MantisTooltipPanel, or plain lines without Simply Tooltips).
 */
public class MantisBladesItem extends CyberwareItem implements ArmCyberwareItem {
    private final MantisVariant variant;

    public MantisBladesItem(Item.Properties properties, MantisVariant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public MantisVariant variant() {
        return variant;
    }

    /** Neither the enchanting table nor books on an anvil (nor /enchant) take the implant. */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        return false;
    }
}
