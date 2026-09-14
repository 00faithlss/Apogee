package dev.apogee.registry;

import dev.apogee.Apogee;
import dev.apogee.item.SandevistanItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ApogeeItems {
    public static final Item SANDEVISTAN = new SandevistanItem(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant());
    public static final Item OS_CORE_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item OVERDRIVE_KERNEL_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item QUANTUM_SYNC_LATTICE_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));

    private ApogeeItems() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("sandevistan"), SANDEVISTAN);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("os_core_t5"), OS_CORE_T5);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("apogee_overdrive_kernel_t5"), OVERDRIVE_KERNEL_T5);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("quantum_sync_lattice_t5"), QUANTUM_SYNC_LATTICE_T5);

        CreativeModeTab tab = FabricItemGroup.builder()
                .icon(() -> new ItemStack(SANDEVISTAN))
                .title(Component.translatable("itemGroup.apogee"))
                .displayItems((parameters, output) -> {
                    output.accept(SANDEVISTAN);
                    output.accept(OS_CORE_T5);
                    output.accept(OVERDRIVE_KERNEL_T5);
                    output.accept(QUANTUM_SYNC_LATTICE_T5);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Apogee.id("apogee"), tab);
    }
}
