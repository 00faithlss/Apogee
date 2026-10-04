package dev.apogee.gametest;

import dev.apogee.Apogee;
import dev.apogee.berserk.BerserkManager;
import dev.apogee.berserk.BerserkVariant;
import dev.apogee.config.BerserkRules;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.ripperdoc.RipperdocMenu;
import dev.apogee.sandevistan.SandevistanManager;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Server-side checks of the Berserk rules with mock players. Run with {@code gradlew runGametest}. */
public final class BerserkGameTests implements FabricGameTest {
    /** A freshly joined player ignores most damage for its first 60 ticks. */
    private static final int SPAWN_PROTECTION_TICKS = 61;

    private static ServerPlayer wearer(GameTestHelper helper, Item implant) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.teleportTo(helper.getLevel(), pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(implant)), "Berserk should equip into cyberware/os");
        return player;
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            BerserkManager.end(player, false);
            player.getServer().getPlayerList().remove(player);
        }
        helper.killAllEntities();
    }

    private static BerserkRules.Stats stats(BerserkVariant variant) {
        return BerserkManager.rules().stats(variant);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_recipes")
    public void berserkRecipesProduceOneItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> recipes = List.of("berserk_zetatech", "berserk_biodyne", "berserk_militech", "subdermal_armor",
                "adrenaline_booster", "biomonitor", "fortified_ankles", "bionic_joints", "synaptic_accelerator",
                "microelectric_module", "compact_reactor");
        for (String name : recipes) {
            Optional<RecipeHolder<?>> recipe = level.getRecipeManager().byKey(Apogee.id(name));
            helper.assertTrue(recipe.isPresent(), "Missing recipe apogee:" + name);
            helper.assertTrue(recipe.get().value().getResultItem(level.registryAccess()).getCount() == 1, name + " should yield one item");
        }
        helper.assertTrue(new ItemStack(ApogeeItems.BERSERK_MILITECH).getMaxStackSize() == 1, "Implant must not stack");
        helper.assertTrue(level.getServer().getAdvancements().get(Apogee.id("craft_berserk")) != null, "Missing advancement apogee:craft_berserk");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_slot")
    public void berserkSharesTheOperatingSystemSlot(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_ZETATECH);
        helper.assertTrue(!TrinketItem.equipItem(player, new ItemStack(ApogeeItems.SANDEVISTAN)), "Sandevistan has no slot left");
        helper.assertTrue(BerserkManager.installed(player).orElse(null) == BerserkVariant.ZETATECH, "Zetatech installed");
        SandevistanManager.toggle(player);
        helper.assertTrue(!SandevistanManager.isActive(player), "Sandevistan cannot run without its implant");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_toggle")
    public void activationAppliesBonusesAndCooldown(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_ZETATECH);
        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        BerserkManager.toggle(player);
        helper.assertTrue(BerserkManager.isActive(player), "Berserk activates");
        double boosted = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double expected = attackSpeed * (1.0 + stats(BerserkVariant.ZETATECH).attackSpeedPercent() / 100.0);
        helper.assertTrue(Math.abs(boosted - expected) < 1.0E-6, "attack speed " + boosted + " expected " + expected);
        helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) < 1.0, "falls hurt less");
        float food = player.getFoodData().getExhaustionLevel();
        player.causeFoodExhaustion(4.0F);
        helper.assertTrue(player.getFoodData().getExhaustionLevel() == food, "no hunger cost while active");

        BerserkManager.toggle(player);
        helper.assertTrue(!BerserkManager.isActive(player), "pressing again ends it");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - attackSpeed) < 1.0E-6, "bonus removed");
        BerserkManager.toggle(player);
        helper.assertTrue(!BerserkManager.isActive(player), "cooldown blocks a new activation");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_reduction", timeoutTicks = SPAWN_PROTECTION_TICKS + 40)
    public void zetatechReducesDamageAndKeepsAQuarterOfHealth(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_ZETATECH);
        helper.runAfterDelay(SPAWN_PROTECTION_TICKS, () -> reducesDamage(helper, player));
    }

    private static void reducesDamage(GameTestHelper helper, ServerPlayer player) {
        BerserkManager.toggle(player);
        player.hurt(player.damageSources().generic(), 10.0F);
        float expected = 20.0F - 10.0F * (1.0F - stats(BerserkVariant.ZETATECH).damageReductionPercent() / 100.0F);
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.01F, "health " + player.getHealth() + " expected " + expected);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), 1000.0F);
        float floor = player.getMaxHealth() * stats(BerserkVariant.ZETATECH).healthFloorPercent() / 100.0F;
        helper.assertTrue(player.isAlive() && Math.abs(player.getHealth() - floor) < 0.01F, "health floor " + player.getHealth());
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_militech", timeoutTicks = SPAWN_PROTECTION_TICKS + 40)
    public void militechIsInvulnerableExceptToTheVoid(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_MILITECH);
        helper.runAfterDelay(SPAWN_PROTECTION_TICKS, () -> invulnerable(helper, player));
    }

    private static void invulnerable(GameTestHelper helper, ServerPlayer player) {
        player.hurt(player.damageSources().generic(), 1.0F);
        helper.assertTrue(player.getHealth() == 19.0F, "spawn protection is over");
        player.setHealth(20.0F);
        player.invulnerableTime = 0;
        BerserkManager.toggle(player);
        player.hurt(player.damageSources().generic(), 15.0F);
        helper.assertTrue(player.getHealth() == 20.0F, "no damage while invulnerable");
        player.hurt(player.damageSources().fellOutOfWorld(), 4.0F);
        helper.assertTrue(player.getHealth() == 16.0F, "the void still hurts");
        helper.assertTrue(Math.abs(BerserkManager.lowHealthBonus(0.2F, 50) - 0.5F) < 1.0E-6, "full bonus at 20% health");
        helper.assertTrue(BerserkManager.lowHealthBonus(1.0F, 50) == 0.0F, "no bonus at full health");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_heal")
    public void killsHealWhenItEnds(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_MILITECH);
        BerserkManager.toggle(player);
        player.setHealth(4.0F);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2.0, 1.5));
        zombie.setNoAi(true);
        zombie.hurt(player.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(BerserkManager.kills(player) == 1, "kill counted");
        BerserkManager.toggle(player);
        float expected = 4.0F + player.getMaxHealth() * BerserkManager.rules().healPerKillPercent() / 100.0F;
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.01F, "health " + player.getHealth() + " expected " + expected);
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_slam")
    public void superheroLandingHurtsAndPushesNearbyMobs(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_ZETATECH);
        BerserkManager.toggle(player);
        Zombie near = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
        Zombie far = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5, 2.0, 1.5 + BerserkManager.rules().slamRadius() + 3.0));
        near.setNoAi(true);
        far.setNoAi(true);
        float nearHealth = near.getHealth();
        float farHealth = far.getHealth();
        BerserkManager.slam(player, BerserkManager.rules(), 6.0);
        helper.assertTrue(near.getHealth() < nearHealth, "mob in range takes damage");
        helper.assertTrue(near.getDeltaMovement().x > 0.0, "mob is pushed away");
        helper.assertTrue(far.getHealth() == farHealth, "mob out of range is untouched");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the wearer is not hurt");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_identity")
    public void militechLocksStartingHealthAndCapsKillExtension(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_MILITECH);
        player.setHealth(4.0F);
        BerserkManager.toggle(player);
        long originalEnd = BerserkManager.activeEndTime(player);
        Zombie victim = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
        float before = BerserkManager.modifyDamage(victim, player.damageSources().playerAttack(player), 10.0F);
        player.setHealth(player.getMaxHealth());
        float after = BerserkManager.modifyDamage(victim, player.damageSources().playerAttack(player), 10.0F);
        helper.assertTrue(before > 10.0F && before == after, "healing must not change the starting-health damage bonus");
        for (int i = 0; i < 15; i++) {
            Zombie target = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
            target.hurt(player.damageSources().playerAttack(player), 1000.0F);
            if (i == 0) {
                helper.assertTrue(BerserkManager.activeEndTime(player) == originalEnd + BerserkRules.KILL_EXTENSION_TICKS,
                        "a kill extends Militech");
            }
        }
        helper.assertTrue(BerserkManager.activeEndTime(player) == originalEnd + stats(BerserkVariant.MILITECH).durationTicks(),
                "extensions are capped at double the initial duration");
        cleanup(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_identity", timeoutTicks = 100)
    public void biodyneBuildsComboHealsAndLosesRhythm(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, ApogeeItems.BERSERK_BIODYNE);
        player.setHealth(10.0F);
        player.getFoodData().setFoodLevel(17);
        double base = player.getAttributeValue(Attributes.ATTACK_SPEED);
        BerserkManager.toggle(player);
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_SPEED) == base, "Biodyne starts without combo speed");
        Zombie victim = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
        victim.setNoAi(true);
        Zombie other = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
        other.setNoAi(true);
        for (int i = 1; i <= BerserkRules.COMBO_HITS; i++) {
            final int hit = i;
            helper.runAfterDelay(i * 3, () -> {
                victim.invulnerableTime = 0;
                victim.hurt(player.damageSources().playerAttack(player), 0.2F);
                helper.assertTrue(BerserkManager.comboHits(player) == hit, "successful hit advances one combo step");
                double expected = base * (1.0 + stats(BerserkVariant.BIODYNE).attackSpeedPercent() / 100.0 * hit / BerserkRules.COMBO_HITS);
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - expected) < 1.0E-6,
                        "speed scales with the combo");
                other.invulnerableTime = 0;
                other.hurt(player.damageSources().playerAttack(player), 0.2F);
                helper.assertTrue(BerserkManager.comboHits(player) == hit, "AoE damage in the same tick must not add another step");
                if (hit == BerserkRules.COMBO_HITS) {
                    helper.assertTrue(Math.abs(player.getHealth() - 12.0F) < 0.01F, "five hits heal ten percent");
                    helper.assertTrue(dev.apogee.mantis.MantisRules.berserkTicks(15, player) < 15, "combo speed also accelerates nanowire");
                }
            });
        }
        helper.runAfterDelay(15 + BerserkRules.COMBO_WINDOW_TICKS + 2, () -> {
            helper.assertTrue(BerserkManager.comboHits(player) == 0, "combo expires after losing rhythm");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - base) < 1.0E-6, "expired combo removes speed");
            cleanup(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_identity")
    public void landingIsExclusiveAndOnlyHealsOnSuccessfulImpact(GameTestHelper helper) {
        ServerPlayer biodyne = wearer(helper, ApogeeItems.BERSERK_BIODYNE);
        BerserkManager.toggle(biodyne);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
        target.setNoAi(true);
        float initial = target.getHealth();
        BerserkManager.slam(biodyne, BerserkManager.rules(), 12.0);
        helper.assertTrue(target.getHealth() == initial, "Biodyne cannot slam");
        biodyne.teleportTo(biodyne.getX() + 20.0, biodyne.getY(), biodyne.getZ());
        ServerPlayer zetatech = wearer(helper, ApogeeItems.BERSERK_ZETATECH);
        BerserkManager.toggle(zetatech);
        zetatech.setHealth(10.0F);
        BerserkManager.slam(zetatech, BerserkManager.rules(), 3.0);
        helper.assertTrue(Math.abs(zetatech.getHealth() - 11.0F) < 0.01F, "one enemy hit heals five percent");
        float lowerImpact = initial - target.getHealth();
        target.invulnerableTime = 0;
        target.setHealth(initial);
        BerserkManager.slam(zetatech, BerserkManager.rules(), 7.0);
        helper.assertTrue(initial - target.getHealth() > lowerImpact, "higher landing deals more damage");
        float healed = zetatech.getHealth();
        // The weaker immediate hit is rejected by the target's damage immunity window.
        BerserkManager.slam(zetatech, BerserkManager.rules(), 3.0);
        helper.assertTrue(zetatech.getHealth() == healed, "rejected damage does not grant landing healing");
        helper.assertTrue(BerserkRules.landingPower(1000.0, 3.0) == 3.0F, "height power is capped");
        BerserkManager.end(zetatech, true);
        helper.assertTrue(zetatech.getHealth() == healed, "Zetatech does not bank kill healing");
        cleanup(helper, biodyne, zetatech);
        helper.succeed();
    }
    /** Implants go in only at the ripperdoc table: using one does not equip it, and no inventory screen offers its slot. */
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_ripperdoc")
    public void berserkEquipsOnlyAtTheRipperdoc(GameTestHelper helper) {
        for (Item implant : List.of(ApogeeItems.BERSERK_ZETATECH, ApogeeItems.BERSERK_BIODYNE, ApogeeItems.BERSERK_MILITECH)) {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(implant));
            implant.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!TrinketsApi.getTrinketComponent(player).map(component -> component.isEquipped(implant)).orElse(false),
                    "using the Berserk must not equip it");
            helper.assertTrue(TrinketsApi.getPlayerSlots(player).get("cyberware") == null, "the inventory screens must not offer cyberware slots");
            helper.assertTrue(player.inventoryMenu.slots.stream().noneMatch(slot -> slot.container.getClass().getName().contains("Trinket")),
                    "the inventory menu must have no trinket slots");

            // The ripperdoc table is the way in: shift-click puts it in the operating system slot and nowhere else.
            RipperdocMenu menu = new RipperdocMenu(0, player.getInventory(), null);
            int menuIndex = menu.slots.size() - 9;
            player.getInventory().setItem(0, new ItemStack(implant));
            menu.quickMoveStack(player, menuIndex);
            helper.assertTrue(menu.getSlot(RipperdocMenu.slotIndex(RipperdocMenu.Implant.OS, 0)).getItem().is(implant),
                    "the ripperdoc must take the Berserk in the operating system slot");
            helper.assertTrue(TrinketsApi.getTrinketComponent(player).map(component -> component.isEquipped(implant)).orElse(false),
                    "the Berserk is equipped once fitted at the ripperdoc");
            player.getServer().getPlayerList().remove(player);
        }
        helper.killAllEntities();
        helper.succeed();
    }

    /** Each Berserk has its own 3x3 recipe: it crafts from the raw parts and never needs another Berserk. */
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_berserk_recipes")
    public void eachBerserkCraftsFromItsOwnRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item subdermal = ApogeeItems.SUBDERMAL_ARMOR;
        Item core = BuiltInRegistries.ITEM.get(Apogee.id("os_core_t5"));
        Item kernel = BuiltInRegistries.ITEM.get(Apogee.id("apogee_overdrive_kernel_t5"));
        Item[][] grids = {
                {subdermal, core, subdermal, ApogeeItems.ADRENALINE_BOOSTER, Items.NETHERITE_INGOT, ApogeeItems.BIOMONITOR,
                        ApogeeItems.BIONIC_JOINTS, kernel, ApogeeItems.FORTIFIED_ANKLES},
                {subdermal, core, subdermal, ApogeeItems.ADRENALINE_BOOSTER, ApogeeItems.SYNAPTIC_ACCELERATOR, ApogeeItems.MICROELECTRIC_MODULE,
                        ApogeeItems.BIONIC_JOINTS, kernel, Items.GOLDEN_APPLE},
                {subdermal, core, subdermal, ApogeeItems.ADRENALINE_BOOSTER, Items.NETHER_STAR, ApogeeItems.COMPACT_REACTOR,
                        ApogeeItems.BIONIC_JOINTS, kernel, Items.TOTEM_OF_UNDYING}};
        Item[] expected = {ApogeeItems.BERSERK_ZETATECH, ApogeeItems.BERSERK_BIODYNE, ApogeeItems.BERSERK_MILITECH};
        for (int i = 0; i < grids.length; i++) {
            List<ItemStack> stacks = java.util.Arrays.stream(grids[i]).map(ItemStack::new).toList();
            CraftingInput input = CraftingInput.of(3, 3, stacks);
            Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipe =
                    level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            helper.assertTrue(recipe.isPresent(), "grid " + i + " crafts something");
            ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
            helper.assertTrue(result.is(expected[i]), "grid " + i + " crafts " + expected[i] + " but gave " + result.getItem());
            for (Item ingredient : grids[i]) {
                helper.assertTrue(!(ingredient instanceof dev.apogee.item.BerserkItem), "recipe " + i + " must not use a Berserk");
            }
        }
        helper.succeed();
    }
}
