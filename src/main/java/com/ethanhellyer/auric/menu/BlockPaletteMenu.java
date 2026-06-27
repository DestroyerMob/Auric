package com.ethanhellyer.auric.menu;

import com.ethanhellyer.auric.item.BlockPaletteItem;
import com.ethanhellyer.auric.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BlockPaletteMenu extends AbstractContainerMenu {
    private static final int PALETTE_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_START = 9;
    private static final int PLAYER_INVENTORY_END = 36;
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 45;

    private final InteractionHand hand;
    private final Container palette;

    public BlockPaletteMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, data.readEnum(InteractionHand.class));
    }

    public BlockPaletteMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(containerId, playerInventory, hand, containerFor(playerInventory, hand));
    }

    private BlockPaletteMenu(int containerId, Inventory playerInventory, InteractionHand hand, Container palette) {
        super(ModMenuTypes.BLOCK_PALETTE.get(), containerId);
        checkContainerSize(palette, PALETTE_SLOT_COUNT);
        this.hand = hand;
        this.palette = palette;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(palette, column + row * 3, 62 + column * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return BlockPaletteItem.isAllowedContent(stack);
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addPlayerSlot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            addPlayerSlot(playerInventory, column, 8 + column * 18, 142);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockPaletteItem.isPalette(player.getItemInHand(hand));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index < PALETTE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (BlockPaletteItem.isAllowedContent(stack)) {
            if (!moveItemStackTo(stack, 0, PALETTE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END && !moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        return copy;
    }

    private void addPlayerSlot(Inventory playerInventory, int inventorySlot, int x, int y) {
        if (hand == InteractionHand.MAIN_HAND && inventorySlot == playerInventory.selected) {
            addSlot(new LockedSlot(playerInventory, inventorySlot, x, y));
        } else {
            addSlot(new Slot(playerInventory, inventorySlot, x, y));
        }
    }

    private static Container containerFor(Inventory playerInventory, InteractionHand hand) {
        ItemStack stack = playerInventory.player.getItemInHand(hand);
        return BlockPaletteItem.isPalette(stack) ? new BlockPaletteContainer(stack) : new SimpleContainer(PALETTE_SLOT_COUNT);
    }

    private static class LockedSlot extends Slot {
        LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
