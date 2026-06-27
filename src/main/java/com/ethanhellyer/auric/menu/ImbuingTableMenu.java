package com.ethanhellyer.auric.menu;

import com.ethanhellyer.auric.blockentity.ImbuingTableBlockEntity;
import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.imbue.ImbuingLogic;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModDataComponents;
import com.ethanhellyer.auric.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ImbuingTableMenu extends AbstractContainerMenu {
    private static final int TARGET_SLOT = 0;
    private static final int POTION_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int PLAYER_INVENTORY_START = 3;
    private static final int PLAYER_INVENTORY_END = 30;
    private static final int HOTBAR_START = 30;
    private static final int HOTBAR_END = 39;

    private final Container input;
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;

    public ImbuingTableMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, data.readBlockPos());
    }

    private ImbuingTableMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, containerAt(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ImbuingTableMenu(int containerId, Inventory playerInventory, Container input, ContainerLevelAccess access) {
        super(ModMenuTypes.IMBUING_TABLE.get(), containerId);
        checkContainerSize(input, 2);
        this.input = input;
        this.access = access;

        addSlot(new Slot(input, ImbuingTableBlockEntity.TARGET_SLOT, 27, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ImbuingLogic.isEligibleTarget(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(input, ImbuingTableBlockEntity.POTION_SLOT, 76, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ImbuingLogic.isPotionCandidate(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(result, 0, 134, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !getItem().isEmpty() && canPay(player);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                pay(player);
                input.removeItem(TARGET_SLOT, 1);
                input.removeItem(POTION_SLOT, 1);
                result.setItem(0, ItemStack.EMPTY);
                input.setChanged();
                slotsChanged(input);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        updateResult();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResult();
    }

    @Override
    public void broadcastChanges() {
        updateResult();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.IMBUING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index == RESULT_SLOT) {
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index == TARGET_SLOT || index == POTION_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ImbuingLogic.isEligibleTarget(stack)) {
            if (!moveItemStackTo(stack, TARGET_SLOT, TARGET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ImbuingLogic.isPotionCandidate(stack)) {
            if (!moveItemStackTo(stack, POTION_SLOT, POTION_SLOT + 1, false)) {
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

    public boolean canPay(Player player) {
        return player.getAbilities().instabuild || player.experienceLevel >= AuricConfig.IMBUING_XP_LEVEL_COST.get();
    }

    private void pay(Player player) {
        int cost = AuricConfig.IMBUING_XP_LEVEL_COST.get();
        if (cost > 0 && !player.getAbilities().instabuild) {
            player.giveExperienceLevels(-cost);
        }
    }

    private void updateResult() {
        result.setItem(0, ImbuingLogic.createResult(input.getItem(TARGET_SLOT), input.getItem(POTION_SLOT)));
    }

    private static Container containerAt(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            return container;
        }
        return new SimpleContainer(2);
    }
}
