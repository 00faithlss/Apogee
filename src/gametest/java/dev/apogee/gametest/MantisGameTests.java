package dev.apogee.gametest;

import dev.apogee.mantis.MantisManager;
import dev.apogee.mantis.MantisRules;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Server-side checks of the Mantis Blades' combat rules with mock players. */
public final class MantisGameTests implements FabricGameTest {
    private static ServerPlayer wearer(GameTestHelper helper, Item blades) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.teleportTo(helper.getLevel(), pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(blades)), "Mantis Blades should equip into cyberware/arms");
        return player;
    }

    /** Puts {@code stack} in the main hand with its attribute modifiers applied, as vanilla does on its next tick. */
    private static void hold(ServerPlayer player, ItemStack stack) {
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            var instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.addOrUpdateTransientModifier(modifier);
            }
        });
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer player) {
        MantisManager.retract(player, false);
        player.getServer().getPlayerList().remove(player);
        helper.killAllEntities();
    }

    private static boolean near(double value, double expected) {
        return Math.abs(value - expected) < 1.0E-6;
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_damage")
    public void bladesHitForTheirOwnDamageWhateverIsHeld(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.MANTIS_BLADES);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        Holder<Enchantment> sharpness = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SHARPNESS);
        sword.enchant(sharpness, 5);
        hold(player, sword);
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 7.0, "The sword adds its damage before deploying");

        MantisManager.toggle(player);
        helper.assertTrue(MantisManager.isDeployed(player), "Blades deploy");
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), MantisRules.ATTACK_DAMAGE),
                "Damage is the blades' own with a sword in hand, got " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_SPEED), MantisRules.ATTACK_SPEED),
                "Attack speed is the blades' own with a sword in hand, got " + player.getAttributeValue(Attributes.ATTACK_SPEED));
        helper.assertTrue(player.getWeaponItem().isEmpty(), "The held sword is not the weapon while the blades are out");
        Zombie target = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2.0, 1.5));
        float enchanted = EnchantmentHelper.modifyDamage(helper.getLevel(), player.getWeaponItem(), target,
                player.damageSources().playerAttack(player), (float) MantisRules.ATTACK_DAMAGE);
        helper.assertTrue(enchanted == (float) MantisRules.ATTACK_DAMAGE, "The sword's Sharpness does not add to the cuts");

        MantisManager.retract(player, false);
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 7.0, "Folding the blades gives the sword back its damage");
        helper.assertTrue(player.getWeaponItem() == sword, "Folding the blades makes the sword the weapon again");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_damage")
    public void emptyHandedBladesHitForTen(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.MANTIS_BLADES_THERMAL);
        MantisManager.toggle(player);
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 10.0), "Damage per hit is 10");
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5, 15.0), "A critical hit deals 15");
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_SPEED), 2.0), "Two full-strength cuts per second");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_water", timeoutTicks = 60)
    public void electricBladesHitHarderInWater(GameTestHelper helper) {
        ServerPlayer electric = wearer(helper, ApogeeItems.MANTIS_BLADES_ELECTRIC);
        MantisManager.toggle(electric);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.WATER);
        Zombie wet = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 1.0, 3.5));
        Zombie dry = helper.spawn(EntityType.ZOMBIE, new Vec3(0.5, 2.0, 3.5));
        wet.setNoAi(true);
        dry.setNoAi(true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(wet.isInWater(), "The wet target stands in water");
            helper.assertTrue(MantisManager.variantBonus(electric, wet) == MantisRules.ELECTRIC_WATER_BONUS,
                    "Electric blades deal +4 in water");
            helper.assertTrue(MantisManager.variantBonus(electric, dry) == 0.0F, "No bonus on dry land");
            MantisManager.retract(electric, false);
            helper.assertTrue(MantisManager.variantBonus(electric, wet) == 0.0F, "No bonus with the blades folded");
            cleanup(helper, electric);
            helper.succeed();
        });
    }

    /** Result of crafting a 3x3 grid (row by row, null for an empty cell), or EMPTY when nothing matches. */
    private static ItemStack craft(GameTestHelper helper, Item... grid) {
        List<ItemStack> cells = new ArrayList<>();
        for (Item item : grid) {
            cells.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        CraftingInput input = CraftingInput.of(3, 3, cells);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static void expect(GameTestHelper helper, ItemStack result, Item item, int count, String what) {
        helper.assertTrue(result.is(item) && result.getCount() == count,
                what + " should give " + count + " " + item + ", got " + result);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_recipes")
    public void mantisBladesAreBuiltFromParts(GameTestHelper helper) {
        Item copper = Items.COPPER_INGOT;
        Item redstone = Items.REDSTONE;
        Item iron = Items.IRON_INGOT;
        Item cable = ApogeeItems.NEURAL_CABLE;
        Item chip = ApogeeItems.CONTROL_CHIP;
        Item actuator = ApogeeItems.BLADE_ACTUATOR;
        Item rail = ApogeeItems.DEPLOY_RAIL;
        Item kernel = ApogeeItems.OVERDRIVE_KERNEL_T5;
        Item core = ApogeeItems.OS_CORE_T5;

        expect(helper, craft(helper, copper, redstone, copper, redstone, Items.GOLD_INGOT, redstone, copper, redstone, copper),
                cable, 4, "Neural cable");
        expect(helper, craft(helper, Items.QUARTZ, redstone, Items.QUARTZ, cable, Items.AMETHYST_SHARD, cable, Items.QUARTZ, redstone, Items.QUARTZ),
                chip, 1, "Control chip");
        expect(helper, craft(helper, iron, cable, iron, cable, Items.REDSTONE_BLOCK, cable, iron, chip, iron),
                actuator, 1, "Blade actuator");
        expect(helper, craft(helper, iron, cable, iron, iron, actuator, iron, iron, cable, iron),
                rail, 1, "Deployment rail");
        // The project author's recipe: iron around a netherite sword, a netherite ingot below.
        expect(helper, craft(helper, null, iron, null, iron, Items.NETHERITE_SWORD, iron, null, Items.NETHERITE_INGOT, null),
                ApogeeItems.MANTIS_BLADE, 1, "Mantis blade");
        expect(helper, craft(helper, Items.BLAZE_POWDER, Items.MAGMA_BLOCK, Items.BLAZE_POWDER, Items.MAGMA_BLOCK, ApogeeItems.MANTIS_BLADE,
                Items.MAGMA_BLOCK, Items.BLAZE_POWDER, cable, Items.BLAZE_POWDER), ApogeeItems.THERMAL_BLADE, 1, "Thermal blade");
        expect(helper, craft(helper, Items.REDSTONE_BLOCK, Items.LIGHTNING_ROD, Items.REDSTONE_BLOCK, copper, ApogeeItems.MANTIS_BLADE, copper,
                Items.REDSTONE_BLOCK, cable, Items.REDSTONE_BLOCK), ApogeeItems.ELECTRIC_BLADE, 1, "Electric blade");

        Item[][] implants = {
                {ApogeeItems.MANTIS_BLADE, ApogeeItems.MANTIS_BLADES},
                {ApogeeItems.THERMAL_BLADE, ApogeeItems.MANTIS_BLADES_THERMAL},
                {ApogeeItems.ELECTRIC_BLADE, ApogeeItems.MANTIS_BLADES_ELECTRIC}};
        for (Item[] implant : implants) {
            expect(helper, craft(helper, implant[0], kernel, implant[0], rail, core, rail, actuator, cable, actuator),
                    implant[1], 1, implant[1] + " from its blades");
        }
        // The old recipes are gone: no netherite-only implant, no upgrade of the finished implant.
        Item netherite = Items.NETHERITE_INGOT;
        helper.assertTrue(craft(helper, netherite, Items.NETHERITE_SWORD, netherite, core, kernel, core, netherite, Items.NETHERITE_SWORD, netherite).isEmpty(),
                "The pre-parts Mantis Blades recipe no longer works");
        helper.assertTrue(craft(helper, Items.BLAZE_ROD, Items.MAGMA_BLOCK, Items.BLAZE_ROD, Items.MAGMA_BLOCK, ApogeeItems.MANTIS_BLADES,
                Items.MAGMA_BLOCK, Items.BLAZE_ROD, Items.MAGMA_BLOCK, Items.BLAZE_ROD).isEmpty(), "The finished implant is not upgraded any more");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_enchant")
    public void bladesCannotBeEnchanted(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchantments.getHolderOrThrow(Enchantments.SHARPNESS);
        helper.assertTrue(new ItemStack(Items.IRON_SWORD).canBeEnchantedWith(sharpness, EnchantingContext.ACCEPTABLE),
                "Control: a sword takes Sharpness");
        for (Item blades : new Item[]{ApogeeItems.MANTIS_BLADES, ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC}) {
            ItemStack stack = new ItemStack(blades);
            helper.assertTrue(!stack.isEnchantable(), blades + " must not go on the enchanting table");
            enchantments.holders().forEach(enchantment -> {
                helper.assertTrue(!stack.canBeEnchantedWith(enchantment, EnchantingContext.ACCEPTABLE)
                                && !stack.canBeEnchantedWith(enchantment, EnchantingContext.PRIMARY),
                        blades + " must not take " + enchantment.getRegisteredName());
            });
        }
        helper.succeed();
    }
}
