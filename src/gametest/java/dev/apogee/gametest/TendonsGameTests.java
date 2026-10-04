package dev.apogee.gametest;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.tendons.ReinforcedTendons;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Server-side checks of the Reinforced Tendons double jump. Run with {@code gradlew runGametest}. */
public final class TendonsGameTests implements FabricGameTest {
    private static ServerPlayer airborne(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.0, 6.0, 1.0));
        player.teleportTo(helper.getLevel(), pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(false);
        player.fallDistance = 2.0F;
        return player;
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void doubleJumpNeedsTheImplant(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        ReinforcedTendons.perform(player);
        helper.assertTrue(player.fallDistance == 2.0F, "Without the implant the request must do nothing");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void doubleJumpSoftensTheFallOncePerFlight(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "Tendons should equip into cyberware/legs");
        ReinforcedTendons.perform(player);
        helper.assertTrue(ReinforcedTendons.fallMargin(player) == ReinforcedTendons.FALL_MARGIN, "The jump should leave a fall margin");
        player.fallDistance = 2.5F;
        ReinforcedTendons.absorbFall(player);
        helper.assertTrue(player.fallDistance == 0.0F, "The margin should absorb the fall built up");
        helper.assertTrue(ReinforcedTendons.fallMargin(player) == ReinforcedTendons.FALL_MARGIN - 2.5F, "The margin should be spent");
        player.fallDistance = 6.0F;
        ReinforcedTendons.absorbFall(player);
        helper.assertTrue(player.fallDistance == 6.0F - (ReinforcedTendons.FALL_MARGIN - 2.5F), "Only what the margin covers is absorbed");
        helper.assertTrue(ReinforcedTendons.fallMargin(player) == 0.0F, "A spent margin stays at zero");
        player.fallDistance = 1.0F;
        ReinforcedTendons.perform(player);
        helper.assertTrue(ReinforcedTendons.fallMargin(player) == 0.0F, "A second request in the same flight must be ignored");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void doubleJumpIgnoredOnTheGround(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "Tendons should equip");
        player.setOnGround(true);
        ReinforcedTendons.perform(player);
        helper.assertTrue(player.fallDistance == 2.0F, "No double jump from the ground");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void airDashNeedsTheImplantAndWorksOncePerFlight(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        ReinforcedTendons.performDash(player);
        helper.assertTrue(player.fallDistance == 2.0F, "Without the implant the dash request must do nothing");
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "Tendons should equip");
        ReinforcedTendons.performDash(player);
        helper.assertTrue(player.fallDistance == 0.0F, "The dash drops the fall built up");
        player.fallDistance = 2.0F;
        ReinforcedTendons.performDash(player);
        helper.assertTrue(player.fallDistance == 2.0F, "A second dash in the same flight must be ignored");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void dashKeepsTheDoubleJumpMargin(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        helper.assertTrue(TrinketItem.equipItem(player, new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "Tendons should equip");
        ReinforcedTendons.perform(player);
        ReinforcedTendons.performDash(player);
        helper.assertTrue(ReinforcedTendons.fallMargin(player) == ReinforcedTendons.FALL_MARGIN, "The dash must not erase the double jump margin");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void recipeAndItem(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(Apogee.id("reinforced_tendons"));
        helper.assertTrue(recipe.isPresent(), "Missing recipe apogee:reinforced_tendons");
        helper.assertTrue(new ItemStack(ApogeeItems.REINFORCED_TENDONS).getMaxStackSize() == 1, "Implant must not stack");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void onlyTheRipperdocFitsTheTendons(GameTestHelper helper) {
        ServerPlayer player = airborne(helper);
        player.setGameMode(GameType.SURVIVAL);
        var legs = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory().get("cyberware").get("legs");

        // Not from the hand.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ApogeeItems.REINFORCED_TENDONS));
        ApogeeItems.REINFORCED_TENDONS.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ApogeeItems.REINFORCED_TENDONS) && legs.getItem(0).isEmpty(), "Using the implant must not equip it");

        // Not from the inventory screen: it has no cyberware slots, and shift-click leaves it where it is.
        ((dev.emi.trinkets.TrinketPlayerScreenHandler) player.inventoryMenu).trinkets$updateTrinketSlots(true);
        helper.assertTrue(player.inventoryMenu.slots.size() == 46, "The inventory menu has no cyberware slots, slots " + player.inventoryMenu.slots.size());
        player.getInventory().setItem(10, new ItemStack(ApogeeItems.REINFORCED_TENDONS));
        player.inventoryMenu.quickMoveStack(player, 10 + 0);
        helper.assertTrue(legs.getItem(0).isEmpty(), "Shift-click in the inventory must not equip the implant");

        // At the table, only in the legs system.
        var menu = new dev.apogee.ripperdoc.RipperdocMenu(1, player.getInventory(), null);
        int legsSlot = dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.LEGS, 0);
        int armsSlot = dev.apogee.ripperdoc.RipperdocMenu.slotIndex(dev.apogee.ripperdoc.RipperdocMenu.Implant.ARMS, 0);
        helper.assertTrue(!menu.getSlot(armsSlot).mayPlace(new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "The arms system must not take the tendons");
        helper.assertTrue(!menu.getSlot(legsSlot).mayPlace(new ItemStack(ApogeeItems.SANDEVISTAN)), "The legs system only takes leg cyberware");
        helper.assertTrue(menu.getSlot(legsSlot).mayPlace(new ItemStack(ApogeeItems.REINFORCED_TENDONS)), "The legs system takes the tendons");
        menu.getSlot(legsSlot).setByPlayer(new ItemStack(ApogeeItems.REINFORCED_TENDONS));
        helper.assertTrue(ReinforcedTendons.isEquipped(player), "The table fits the tendons");
        player.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void soundsAreRegisteredAndShort(GameTestHelper helper) {
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(Apogee.id("tendons_double_jump")), "double jump sound registered");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(Apogee.id("tendons_air_dash")), "air dash sound registered");
        double velocity = ReinforcedTendons.JUMP_POWER;
        double height = 0.0;
        while (velocity > 0.0) {
            height += velocity;
            velocity = (velocity - 0.08) * 0.98;
        }
        helper.assertTrue(Math.abs(height - 2.0) < 0.02,
                "The extra jump gains two blocks under vanilla gravity and drag");
        for (String name : new String[]{"double_jump", "air_dash"}) {
            try (var in = ReinforcedTendons.class.getResourceAsStream("/assets/apogee/sounds/tendons/" + name + ".ogg")) {
                helper.assertTrue(in != null, name + ".ogg is packaged");
                helper.assertTrue(in.readAllBytes().length < 40_000, name + ".ogg is a short clip");
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
        helper.succeed();
    }

    /** The press that leaves the ground, or a key held from the ground, must not fire the double jump. */
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void groundJumpPressIsNotTheDoubleJump(GameTestHelper helper) {
        dev.apogee.client.tendons.TendonsAirInput input = new dev.apogee.client.tendons.TendonsAirInput();
        // On the ground, key up.
        helper.assertTrue(!input.step(true, false, false, false, false, true).jump(), "nothing on the ground");
        // The tick the feet leave the ground: jump is down and was just pressed.
        helper.assertTrue(!input.step(false, true, true, false, false, true).jump(), "the take-off press must not fire");
        // Still held in the air.
        helper.assertTrue(!input.step(false, true, false, false, false, true).jump(), "a held key must not fire");
        // Released, then pressed again: the double jump.
        helper.assertTrue(!input.step(false, false, false, false, false, true).jump(), "release does not fire");
        helper.assertTrue(input.step(false, true, true, false, false, true).jump(), "a new press in the air fires");
        helper.assertTrue(!input.step(false, false, false, false, false, true).jump(), "release");
        helper.assertTrue(!input.step(false, true, true, false, false, true).jump(), "only once per flight");
        // Landing recharges it.
        input.step(true, false, false, false, false, true);
        input.step(false, true, true, false, false, true);
        input.step(false, false, false, false, false, true);
        helper.assertTrue(input.step(false, true, true, false, false, true).jump(), "recharged after landing");
        helper.succeed();
    }

    /** A dash needs a new press too, and works independently of the jump. */
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_tendons")
    public void dashIsIndependentFromTheJump(GameTestHelper helper) {
        dev.apogee.client.tendons.TendonsAirInput input = new dev.apogee.client.tendons.TendonsAirInput();
        input.step(true, false, false, false, false, true);
        input.step(false, true, true, false, false, true);
        input.step(false, false, false, false, false, true);
        helper.assertTrue(input.step(false, false, false, true, true, true).dash(), "a dash press in the air fires");
        helper.assertTrue(input.step(false, true, true, true, false, true).jump(), "the jump still works after the dash");
        helper.succeed();
    }
}
