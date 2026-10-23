package dev.apogee.registry;

import dev.apogee.Apogee;
import dev.apogee.item.BloodPumpItem;
import dev.apogee.berserk.BerserkVariant;
import dev.apogee.item.BerserkItem;
import dev.apogee.item.MantisBladesItem;
import dev.apogee.item.NanowireItem;
import dev.apogee.item.ReinforcedTendonsItem;
import dev.apogee.item.SandevistanItem;
import dev.apogee.mantis.MantisVariant;
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
    public static final Item BERSERK_ZETATECH = berserk(BerserkVariant.ZETATECH, Rarity.EPIC);
    public static final Item BERSERK_BIODYNE = berserk(BerserkVariant.BIODYNE, Rarity.EPIC);
    public static final Item BERSERK_MILITECH = berserk(BerserkVariant.MILITECH, Rarity.EPIC);
    public static final Item MANTIS_BLADES = mantis(MantisVariant.STANDARD);
    public static final Item MANTIS_BLADES_THERMAL = mantis(MantisVariant.THERMAL);
    public static final Item MANTIS_BLADES_ELECTRIC = mantis(MantisVariant.ELECTRIC);
    public static final Item NANOWIRE = new NanowireItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final Item NANOWIRE_WHITE = new NanowireItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), MantisVariant.NANOWIRE_WHITE);
    public static final Item REINFORCED_TENDONS = new ReinforcedTendonsItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item BLOOD_PUMP = new BloodPumpItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
    // Blood Pump parts.
    public static final Item HEMO_CHAMBER = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item BIO_MONITOR = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item OS_CORE_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item OVERDRIVE_KERNEL_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item QUANTUM_SYNC_LATTICE_T5 = new Item(new Item.Properties().rarity(Rarity.RARE));
    // Mantis Blades parts, in crafting order: cables and chips, mechanisms, blades.
    public static final Item NEURAL_CABLE = new Item(new Item.Properties());
    public static final Item CONTROL_CHIP = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item BLADE_ACTUATOR = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item DEPLOY_RAIL = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item MANTIS_BLADE = blade();
    public static final Item THERMAL_BLADE = blade();
    public static final Item ELECTRIC_BLADE = blade();
    // Shared by the arm cyberware, then the nanowire's own parts.
    public static final Item ARM_POWER_CORE = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item MONOFILAMENT = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item WIRE_HOUSING = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant());

    // Berserk parts, after cyberware from Cyberpunk 2077.
    public static final Item SUBDERMAL_ARMOR = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item ADRENALINE_BOOSTER = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item BIOMONITOR = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item FORTIFIED_ANKLES = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item BIONIC_JOINTS = new Item(new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item SYNAPTIC_ACCELERATOR = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item MICROELECTRIC_MODULE = new Item(new Item.Properties().rarity(Rarity.RARE));
    public static final Item COMPACT_REACTOR = new Item(new Item.Properties().rarity(Rarity.RARE));

    private ApogeeItems() {
    }

    /** Netherite-based blade part: does not burn, like the netherite sword it is made from. */
    private static Item blade() {
        return new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant());
    }

    private static Item berserk(BerserkVariant variant, Rarity rarity) {
        return new BerserkItem(new Item.Properties().stacksTo(1).rarity(rarity).fireResistant(), variant);
    }

    private static Item mantis(MantisVariant variant) {
        return new MantisBladesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), variant);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("sandevistan"), SANDEVISTAN);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("berserk_zetatech"), BERSERK_ZETATECH);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("berserk_biodyne"), BERSERK_BIODYNE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("berserk_militech"), BERSERK_MILITECH);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("mantis_blades"), MANTIS_BLADES);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("mantis_blades_thermal"), MANTIS_BLADES_THERMAL);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("mantis_blades_electric"), MANTIS_BLADES_ELECTRIC);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("nanowire"), NANOWIRE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("nanowire_white"), NANOWIRE_WHITE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("reinforced_tendons"), REINFORCED_TENDONS);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("blood_pump"), BLOOD_PUMP);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("hemo_chamber"), HEMO_CHAMBER);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("bio_monitor"), BIO_MONITOR);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("os_core_t5"), OS_CORE_T5);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("apogee_overdrive_kernel_t5"), OVERDRIVE_KERNEL_T5);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("quantum_sync_lattice_t5"), QUANTUM_SYNC_LATTICE_T5);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("neural_cable"), NEURAL_CABLE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("control_chip"), CONTROL_CHIP);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("blade_actuator"), BLADE_ACTUATOR);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("deploy_rail"), DEPLOY_RAIL);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("mantis_blade"), MANTIS_BLADE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("thermal_blade"), THERMAL_BLADE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("electric_blade"), ELECTRIC_BLADE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("arm_power_core"), ARM_POWER_CORE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("monofilament"), MONOFILAMENT);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("wire_housing"), WIRE_HOUSING);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("subdermal_armor"), SUBDERMAL_ARMOR);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("adrenaline_booster"), ADRENALINE_BOOSTER);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("biomonitor"), BIOMONITOR);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("fortified_ankles"), FORTIFIED_ANKLES);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("bionic_joints"), BIONIC_JOINTS);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("synaptic_accelerator"), SYNAPTIC_ACCELERATOR);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("microelectric_module"), MICROELECTRIC_MODULE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("compact_reactor"), COMPACT_REACTOR);

        CreativeModeTab tab = FabricItemGroup.builder()
                .icon(() -> new ItemStack(SANDEVISTAN))
                .title(Component.translatable("itemGroup.apogee"))
                .displayItems((parameters, output) -> {
                    output.accept(ApogeeBlocks.RIPPERDOC_TABLE_ITEM);
                    output.accept(SANDEVISTAN);
                    output.accept(BERSERK_ZETATECH);
                    output.accept(BERSERK_BIODYNE);
                    output.accept(BERSERK_MILITECH);
                    output.accept(MANTIS_BLADES);
                    output.accept(MANTIS_BLADES_THERMAL);
                    output.accept(MANTIS_BLADES_ELECTRIC);
                    output.accept(NANOWIRE);
                    output.accept(NANOWIRE_WHITE);
                    output.accept(REINFORCED_TENDONS);
                    output.accept(BLOOD_PUMP);
                    output.accept(HEMO_CHAMBER);
                    output.accept(BIO_MONITOR);
                    output.accept(OS_CORE_T5);
                    output.accept(OVERDRIVE_KERNEL_T5);
                    output.accept(QUANTUM_SYNC_LATTICE_T5);
                    output.accept(NEURAL_CABLE);
                    output.accept(CONTROL_CHIP);
                    output.accept(BLADE_ACTUATOR);
                    output.accept(DEPLOY_RAIL);
                    output.accept(MANTIS_BLADE);
                    output.accept(THERMAL_BLADE);
                    output.accept(ELECTRIC_BLADE);
                    output.accept(ARM_POWER_CORE);
                    output.accept(MONOFILAMENT);
                    output.accept(WIRE_HOUSING);
                    output.accept(SUBDERMAL_ARMOR);
                    output.accept(ADRENALINE_BOOSTER);
                    output.accept(BIOMONITOR);
                    output.accept(FORTIFIED_ANKLES);
                    output.accept(BIONIC_JOINTS);
                    output.accept(SYNAPTIC_ACCELERATOR);
                    output.accept(MICROELECTRIC_MODULE);
                    output.accept(COMPACT_REACTOR);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Apogee.id("apogee"), tab);
    }
}
