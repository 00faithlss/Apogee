package dev.apogee.gametest;

import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.tendons.ReinforcedTendons;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
}
