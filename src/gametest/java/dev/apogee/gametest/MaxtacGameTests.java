package dev.apogee.gametest;

import dev.apogee.armor.MaxtacVisor;
import dev.apogee.registry.ApogeeItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MaxtacGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_maxtac")
    public void visorNeedsHelmetAndStartsWithPrimaryOptics(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        MaxtacVisor.toggle(player);
        helper.assertFalse(MaxtacVisor.auxiliaryActive(player), "Un casco distinto no activa las ópticas MaxTac");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ApogeeItems.MAXTAC_HELMET));
        helper.assertFalse(MaxtacVisor.auxiliaryActive(player), "Al equipar el casco solo se enciende el par principal");
        MaxtacVisor.toggle(player);
        helper.assertTrue(MaxtacVisor.auxiliaryActive(player), "El casco puede activar las cuatro ópticas auxiliares sin el conjunto completo");
        helper.runAfterDelay(5, () -> {
            MaxtacVisor.toggle(player);
            helper.assertFalse(MaxtacVisor.auxiliaryActive(player), "La segunda pulsación vuelve al par principal");
            player.getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_maxtac")
    public void visorResetsWhenSameHelmetIsReequipped(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack helmet = new ItemStack(ApogeeItems.MAXTAC_HELMET);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        MaxtacVisor.toggle(player);
        helper.assertTrue(MaxtacVisor.auxiliaryActive(player), "Ópticas auxiliares encendidas");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.runAfterDelay(2, () -> {
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            helper.assertFalse(MaxtacVisor.auxiliaryActive(player), "Volver a equipar el mismo casco inicia el modo de dos ópticas");
            player.getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "apogee_maxtac")
    public void armorUsesFourIndependentVanillaSlots(GameTestHelper helper) {
        helper.assertTrue(((ArmorItem)ApogeeItems.MAXTAC_HELMET).getEquipmentSlot() == EquipmentSlot.HEAD, "Ranura del casco");
        helper.assertTrue(((ArmorItem)ApogeeItems.MAXTAC_CHESTPLATE).getEquipmentSlot() == EquipmentSlot.CHEST, "Ranura del chaleco");
        helper.assertTrue(((ArmorItem)ApogeeItems.MAXTAC_LEGGINGS).getEquipmentSlot() == EquipmentSlot.LEGS, "Ranura del pantalón");
        helper.assertTrue(((ArmorItem)ApogeeItems.MAXTAC_BOOTS).getEquipmentSlot() == EquipmentSlot.FEET, "Ranura de las botas");
        helper.succeed();
    }
}
