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
