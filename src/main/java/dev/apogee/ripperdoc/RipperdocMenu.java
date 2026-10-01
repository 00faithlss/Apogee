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
 * The ripperdoc screen's menu: the places of every body system on the screen, backed straight by
 * the player's Trinkets inventories, and the player's own inventory below, laid
 * out like a chest: the ripperdoc panel on top, the usual inventory under it.
 */
public class RipperdocMenu extends AbstractContainerMenu {
    /**
     * The body systems on the screen, after the author's design: each is a Trinkets slot in the
     * cyberware group with as many places as the design shows, in two columns either side of the
     * patient view. Implants go in by the Trinkets tag {@code trinkets:cyberware/<slot>}; systems
     * with no implants yet take nothing until one is added to their tag.
     */
    public enum Implant {
        CORTEX("cortex", 3, 0, 0),
        ARMS("arms", 1, 0, 1),
        SKELETON("skeleton", 2, 0, 2),
        NERVES("nerves", 2, 0, 3),
        INTEGUMENTARY("integumentary", 3, 0, 4),
        OS("os", 1, 1, 0),
        OPTICS("optics", 1, 1, 1),
        HANDS("hands", 2, 1, 2),
        CIRCULATORY("circulatory", 3, 1, 3),
        LEGS("legs", 1, 1, 4);

        public final String slot;
        public final int count;
        /** Left (0) or right (1) column, and the row in it. */
        public final int column;
        public final int row;
        private final TagKey<Item> tag;

        Implant(String slot, int count, int column, int row) {
            this.slot = slot;
            this.count = count;
            this.column = column;
            this.row = row;
            this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", "cyberware/" + slot));
        }

        /** The Trinkets tag of the items that fit this body system. */
        public TagKey<Item> tag() {
            return tag;
        }

        /** Left edge of the column, where its label and first slot frame start. */
        public int columnX() {
            return column == 0 ? 9 : 113;
        }

        /** Top of the row: the label, with the slot frames under it. */
        public int rowY() {
            return 18 + row * 24;
        }

        /** Where the item of place {@code index} sits on the screen. */
        public int itemX(int index) {
            return columnX() + 1 + index * 18;
        }

        public int itemY() {
            return rowY() + 6;
        }
    }

    /** Height of the ripperdoc panel above the inventory; the inventory part is vanilla's. */
    public static final int PANEL_HEIGHT = 142;
    public static final int WIDTH = 176;
    public static final int HEIGHT = PANEL_HEIGHT + 96;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = PANEL_HEIGHT + 14;
    private static final int HOTBAR_Y = PANEL_HEIGHT + 72;

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
            Container container = container(player, implant);
            for (int i = 0; i < implant.count; i++) {
                addSlot(new ImplantSlot(container, i, implant, player));
            }
        }
        implantSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    /** Menu index of place {@code index} of a body system. */
    public static int slotIndex(Implant implant, int index) {
        int first = 0;
        for (Implant other : Implant.values()) {
            if (other == implant) {
                return first + index;
            }
            first += other.count;
        }
        throw new IllegalArgumentException(implant.name());
    }

    /** The player's Trinkets inventory for a body system, or an empty stand-in if it is missing. */
    private static Container container(Player player, Implant implant) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.getInventory().get("cyberware"))
                .map(group -> (Container) group.get(implant.slot))
                .filter(inventory -> inventory != null && inventory.getContainerSize() >= implant.count)
                .orElseGet(() -> new SimpleContainer(implant.count));
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
        Implant implant();
    }

    /** An implant slot: only its own kind of implant, one at a time; fitting one is surgery. */
    static final class ImplantSlot extends Slot implements ImplantView {
        final Implant implant;
        private final Player patient;

        ImplantSlot(Container container, int index, Implant implant, Player patient) {
            super(container, index, implant.itemX(index), implant.itemY());
            this.implant = implant;
            this.patient = patient;
        }

        @Override
        public Implant implant() {
            return implant;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(implant.tag());
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
