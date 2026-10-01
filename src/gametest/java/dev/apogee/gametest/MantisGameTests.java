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
        Item power = ApogeeItems.ARM_POWER_CORE;
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

        expect(helper, craft(helper, Items.NETHERITE_SCRAP, Items.REDSTONE_BLOCK, Items.NETHERITE_SCRAP, cable, Items.DIAMOND, cable,
                Items.NETHERITE_SCRAP, Items.REDSTONE_BLOCK, Items.NETHERITE_SCRAP), power, 1, "Arm power core");
        Item[][] implants = {
                {ApogeeItems.MANTIS_BLADE, ApogeeItems.MANTIS_BLADES},
                {ApogeeItems.THERMAL_BLADE, ApogeeItems.MANTIS_BLADES_THERMAL},
                {ApogeeItems.ELECTRIC_BLADE, ApogeeItems.MANTIS_BLADES_ELECTRIC}};
        for (Item[] implant : implants) {
            expect(helper, craft(helper, implant[0], power, implant[0], rail, core, rail, actuator, cable, actuator),
                    implant[1], 1, implant[1] + " from its blades");
            helper.assertTrue(craft(helper, implant[0], kernel, implant[0], rail, core, rail, actuator, cable, actuator).isEmpty(),
                    implant[1] + " no longer takes the Overdrive Kernel");
        }
        // The old recipes are gone: no netherite-only implant, no upgrade of the finished implant.
        Item netherite = Items.NETHERITE_INGOT;
        helper.assertTrue(craft(helper, netherite, Items.NETHERITE_SWORD, netherite, core, kernel, core, netherite, Items.NETHERITE_SWORD, netherite).isEmpty(),
                "The pre-parts Mantis Blades recipe no longer works");
        // The nanowire: its monofilament and two housings, then the arm power core and the T5 core.
        Item string = Items.STRING;
        expect(helper, craft(helper, string, Items.DIAMOND, string, string, cable, string, string, Items.DIAMOND, string),
                ApogeeItems.MONOFILAMENT, 1, "Monofilament");
        expect(helper, craft(helper, iron, netherite, iron, iron, actuator, iron, iron, chip, iron),
                ApogeeItems.WIRE_HOUSING, 1, "Nanowire housing");
        Item housing = ApogeeItems.WIRE_HOUSING;
        expect(helper, craft(helper, housing, ApogeeItems.MONOFILAMENT, housing, cable, power, cable, cable, core, cable),
                ApogeeItems.NANOWIRE, 1, "Nanowire");
        helper.assertTrue(craft(helper, cable, chip, cable, netherite, kernel, netherite, cable, core, cable).isEmpty(),
                "The first nanowire recipe no longer works");
        helper.assertTrue(craft(helper, Items.BLAZE_ROD, Items.MAGMA_BLOCK, Items.BLAZE_ROD, Items.MAGMA_BLOCK, ApogeeItems.MANTIS_BLADES,
                Items.MAGMA_BLOCK, Items.BLAZE_ROD, Items.MAGMA_BLOCK, Items.BLAZE_ROD).isEmpty(), "The finished implant is not upgraded any more");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_ripperdoc")
    public void ripperdocFitsImplantsWithSurgery(GameTestHelper helper) {
        Item cable = ApogeeItems.NEURAL_CABLE;
        Item iron = Items.IRON_INGOT;
        Item wool = Items.WHITE_WOOL;
        expect(helper, craft(helper, cable, ApogeeItems.CONTROL_CHIP, cable, wool, wool, wool, iron, Items.IRON_BLOCK, iron),
                dev.apogee.registry.ApogeeBlocks.RIPPERDOC_TABLE_ITEM, 1, "Ripperdoc table");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // A new player is invulnerable for three seconds; operate once that is over.
        helper.runAfterDelay(70, () -> ripperdocSurgery(helper, player));
    }

    private static final int ARMS_SLOT = dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.ARMS, 0);
    private static final int OS_SLOT = dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.OS, 0);

    private static void ripperdocSurgery(GameTestHelper helper, ServerPlayer player) {
        // The inventory screens leave the cyberware slots out; the table's menu has them.
        helper.assertTrue(!dev.emi.trinkets.api.TrinketsApi.getPlayerSlots(player).containsKey("cyberware"),
                "The inventory does not show the cyberware slots");
        ((dev.emi.trinkets.TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(true);
        helper.assertTrue(player.inventoryMenu.slots.size() == 46,
                "The inventory menu has no cyberware slots, slots " + player.inventoryMenu.slots.size());
        dev.apogee.ripperdoc.RipperdocMenu menu = new dev.apogee.ripperdoc.RipperdocMenu(1, player.getInventory(), null);
        // Every body system of the design is a Trinkets slot with its own places.
        var cyberware = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory().get("cyberware");
        for (dev.apogee.ripperdoc.RipperdocMenu.Implant implant : dev.apogee.ripperdoc.RipperdocMenu.Implant.values()) {
            helper.assertTrue(cyberware.containsKey(implant.slot) && cyberware.get(implant.slot).getContainerSize() == implant.count,
                    "The cyberware/" + implant.slot + " slot has " + implant.count + " places");
        }
        helper.assertTrue(menu.slots.size() == 19 + 36, "The table shows the 19 implant places and the inventory, " + menu.slots.size());
        helper.assertTrue(!menu.getSlot(ARMS_SLOT).mayPlace(new ItemStack(ApogeeItems.SANDEVISTAN)), "The arms slot only takes arm cyberware");
        menu.getSlot(ARMS_SLOT).setByPlayer(new ItemStack(ApogeeItems.MANTIS_BLADES));
        helper.assertTrue(MantisManager.equippedVariant(player) != null, "The table fits the Mantis Blades in the Trinkets slot");
        helper.assertTrue(player.getHealth() == player.getMaxHealth() - 6.0F, "Surgery costs three hearts, health " + player.getHealth());
        // A second implant right after is its own operation.
        menu.getSlot(ARMS_SLOT).setByPlayer(ItemStack.EMPTY);
        menu.getSlot(ARMS_SLOT).setByPlayer(new ItemStack(ApogeeItems.NANOWIRE));
        helper.assertTrue(player.getHealth() == player.getMaxHealth() - 12.0F, "Each implant costs three hearts, health " + player.getHealth());
        // Surgery never kills: at two hearts it leaves the patient at half a heart.
        player.setHealth(4.0F);
        menu.getSlot(OS_SLOT).setByPlayer(new ItemStack(ApogeeItems.SANDEVISTAN));
        helper.assertTrue(player.isAlive() && player.getHealth() == 1.0F, "Surgery is never lethal, health " + player.getHealth());
        // The implants do not equip themselves from the hand any more.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ApogeeItems.NANOWIRE));
        ApogeeItems.NANOWIRE.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ApogeeItems.NANOWIRE), "Using an implant does not equip it");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_mantis_enchant")
    public void bladesCannotBeEnchanted(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchantments.getHolderOrThrow(Enchantments.SHARPNESS);
        helper.assertTrue(new ItemStack(Items.IRON_SWORD).canBeEnchantedWith(sharpness, EnchantingContext.ACCEPTABLE),
                "Control: a sword takes Sharpness");
        for (Item blades : new Item[]{ApogeeItems.MANTIS_BLADES, ApogeeItems.MANTIS_BLADES_THERMAL, ApogeeItems.MANTIS_BLADES_ELECTRIC, ApogeeItems.NANOWIRE}) {
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
