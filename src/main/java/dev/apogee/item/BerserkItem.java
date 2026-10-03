package dev.apogee.item;

import dev.apogee.berserk.BerserkVariant;

/** Reusable Berserk operating system with no durability; equipped in the Trinkets slot cyberware/os. */
public class BerserkItem extends CyberwareItem {
    private final BerserkVariant variant;

    public BerserkItem(Properties properties, BerserkVariant variant) {
        super(properties);
        this.variant = variant;
    }

    public BerserkVariant variant() {
        return variant;
    }
}
