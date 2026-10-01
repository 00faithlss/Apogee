package dev.apogee.registry;

import dev.apogee.Apogee;
import dev.apogee.ripperdoc.RipperdocMenu;
import dev.apogee.ripperdoc.RipperdocSeat;
import dev.apogee.ripperdoc.RipperdocTableBlock;
import dev.apogee.ripperdoc.RipperdocTableBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** The ripperdoc table: its block, item, block entity, seat and screen. */
public final class ApogeeBlocks {
    public static final RipperdocTableBlock RIPPERDOC_TABLE = new RipperdocTableBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY));
    public static final Item RIPPERDOC_TABLE_ITEM = new BlockItem(RIPPERDOC_TABLE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final BlockEntityType<RipperdocTableBlockEntity> RIPPERDOC_TABLE_ENTITY =
            BlockEntityType.Builder.of(RipperdocTableBlockEntity::new, RIPPERDOC_TABLE).build(null);
    public static final EntityType<RipperdocSeat> RIPPERDOC_SEAT = EntityType.Builder.<RipperdocSeat>of(RipperdocSeat::new, MobCategory.MISC)
            .sized(0.01F, 0.01F)
            .noSummon()
            .clientTrackingRange(10)
            .build("apogee:ripperdoc_seat");
    public static final MenuType<RipperdocMenu> RIPPERDOC_MENU = new MenuType<>(RipperdocMenu::new, FeatureFlags.VANILLA_SET);

    private ApogeeBlocks() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, Apogee.id("ripperdoc_table"), RIPPERDOC_TABLE);
        Registry.register(BuiltInRegistries.ITEM, Apogee.id("ripperdoc_table"), RIPPERDOC_TABLE_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Apogee.id("ripperdoc_table"), RIPPERDOC_TABLE_ENTITY);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Apogee.id("ripperdoc_seat"), RIPPERDOC_SEAT);
        Registry.register(BuiltInRegistries.MENU, Apogee.id("ripperdoc"), RIPPERDOC_MENU);
    }
}
