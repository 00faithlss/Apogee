package dev.apogee.gametest;

import dev.apogee.armor.TraumaHelmet;
import dev.apogee.registry.ApogeeItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TraumaGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_trauma")
    public void helmetRequiresTraumaItemAndStartsClosed(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        TraumaHelmet.toggle(player);
        helper.assertFalse(TraumaHelmet.isOpen(player), "Un casco distinto no activa las Ã³pticas MaxTac");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ApogeeItems.TRAUMA_TEAM_HELMET));
        helper.assertFalse(TraumaHelmet.isOpen(player), "El casco se equipa cerrado");
        TraumaHelmet.toggle(player);
        helper.assertTrue(TraumaHelmet.isOpen(player), "El casco puede activar las cuatro Ã³pticas auxiliares sin el conjunto completo");
        helper.runAfterDelay(5, () -> {
            TraumaHelmet.toggle(player);
            helper.assertFalse(TraumaHelmet.isOpen(player), "La segunda pulsaciÃ³n vuelve al par principal");
            player.getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_trauma")
    public void openingResetsWhenSameHelmetIsReequipped(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack helmet = new ItemStack(ApogeeItems.TRAUMA_TEAM_HELMET);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        TraumaHelmet.toggle(player);
        helper.assertTrue(TraumaHelmet.isOpen(player), "Ã“pticas auxiliares encendidas");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.runAfterDelay(2, () -> {
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            helper.assertFalse(TraumaHelmet.isOpen(player), "Volver a equipar el mismo casco inicia el modo de dos Ã³pticas");
            player.getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_trauma")
    public void armorUsesFourIndependentVanillaSlots(GameTestHelper helper) {
        helper.assertTrue(((ArmorItem)ApogeeItems.TRAUMA_TEAM_HELMET).getEquipmentSlot() == EquipmentSlot.HEAD, "Ranura del casco");
        helper.assertTrue(((ArmorItem)ApogeeItems.TRAUMA_TEAM_CHESTPLATE).getEquipmentSlot() == EquipmentSlot.CHEST, "Ranura del chaleco");
        helper.assertTrue(((ArmorItem)ApogeeItems.TRAUMA_TEAM_LEGGINGS).getEquipmentSlot() == EquipmentSlot.LEGS, "Ranura del pantalÃ³n");
        helper.assertTrue(((ArmorItem)ApogeeItems.TRAUMA_TEAM_BOOTS).getEquipmentSlot() == EquipmentSlot.FEET, "Ranura de las botas");
        helper.succeed();
    }
}
