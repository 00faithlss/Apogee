package dev.apogee.ripperdoc;

import dev.apogee.registry.ApogeeBlocks;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The ripperdoc screen's menu: one slot per implant slot the mod has (operating system and arms),
 * backed straight by the player's Trinkets inventories, and the player's own inventory. Slot
 * positions follow the screen texture, 480 by 356 pixels.
 */
public class RipperdocMenu extends AbstractContainerMenu {
    /** The implant slots shown on the screen: Trinkets slot, and where its frame is on the texture. */
    public enum Implant {
        OS("os", 326, 65),
        ARMS("arms", 47, 100);

        public final String slot;
        public final int x;
        public final int y;
        final TagKey<Item> tag;

        Implant(String slot, int x, int y) {
            this.slot = slot;
            this.x = x;
            this.y = y;
            this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", "cyberware/" + slot));
        }
    }

    public static final int WIDTH = 480;
    public static final int HEIGHT = 356;
    private static final int INVENTORY_X = 159;
    private static final int INVENTORY_Y = 263;
    private static final int HOTBAR_Y = 322;

    private final ContainerLevelAccess access;
    private final Player player;
    private final int implantSlots;

    /** Client side, opened from the server's packet. */
    public RipperdocMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public RipperdocMenu(int id, Inventory inventory, BlockPos table) {
        super(ApogeeBlocks.RIPPERDOC_MENU, id);
        this.player = inventory.player;
        this.access = table == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(player.level(), table);
        for (Implant implant : Implant.values()) {
            addSlot(new ImplantSlot(container(player, implant), implant, player));
        }
        implantSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + 1 + column * 18, INVENTORY_Y + 1 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + 1 + column * 18, HOTBAR_Y + 1));
        }
    }

    /** The player's Trinkets inventory for an implant slot, or an empty stand-in if the slot is missing. */
    private static Container container(Player player, Implant implant) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.getInventory().get("cyberware"))
                .map(group -> (Container) group.get(implant.slot))
                .filter(inventory -> inventory != null && inventory.getContainerSize() > 0)
                .orElseGet(() -> new SimpleContainer(1));
    }

    @Override
    public boolean stillValid(Player player) {
        return access == ContainerLevelAccess.NULL
                || stillValid(access, player, ApogeeBlocks.RIPPERDOC_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < implantSlots) {
            if (!moveItemStackTo(stack, implantSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < implantSlots && !moved; i++) {
                Slot target = slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player.getVehicle() instanceof RipperdocSeat) {
            player.stopRiding();
        }
    }

    /** Marks the implant slots for the screen. */
    public interface ImplantView {
    }

    /** An implant slot: only its own kind of implant, one at a time; fitting one is surgery. */
    static final class ImplantSlot extends Slot implements ImplantView {
        final Implant implant;
        private final Player patient;

        ImplantSlot(Container container, Implant implant, Player patient) {
            super(container, 0, implant.x + 1, implant.y + 1);
            this.implant = implant;
            this.patient = patient;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(implant.tag);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            super.setByPlayer(stack, previous);
            if (!patient.level().isClientSide && !stack.isEmpty()) {
                RipperdocSurgery.operate(patient);
            }
        }
    }
}
