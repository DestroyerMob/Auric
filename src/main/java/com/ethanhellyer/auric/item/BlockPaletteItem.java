package com.ethanhellyer.auric.item;

import com.ethanhellyer.auric.menu.BlockPaletteMenu;
import com.ethanhellyer.auric.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class BlockPaletteItem extends Item {
    public static final int SLOT_COUNT = 9;
    private static final Component MENU_TITLE = Component.translatable("container.auric.block_palette");

    public BlockPaletteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            openPalette(context.getLevel(), player, context.getHand());
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return placeRandomBlock(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            openPalette(level, player, hand);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemContainerContents contents = itemEntity.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents != null) {
            ItemUtils.onContainerDestroyed(itemEntity, contents.nonEmptyItemsCopy());
        }
    }

    private InteractionResult placeRandomBlock(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack palette = context.getItemInHand();
        NonNullList<ItemStack> contents = getContents(palette);
        List<Integer> blockSlots = filledBlockSlots(contents);
        if (blockSlots.isEmpty()) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        RandomSource random = level.getRandom();
        ItemStack selected = contents.get(blockSlots.get(random.nextInt(blockSlots.size())));
        if (!(selected.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.FAIL;
        }

        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        InteractionResult result = blockItem.place(new BlockPlaceContext(level, context.getPlayer(), context.getHand(), selected, hit));
        if (result.consumesAction()) {
            saveContents(palette, contents);
        }
        return result;
    }

    private static void openPalette(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new BlockPaletteMenu(containerId, inventory, hand),
                MENU_TITLE
        );
        serverPlayer.openMenu(provider, buffer -> buffer.writeEnum(hand));
    }

    public static boolean isPalette(ItemStack stack) {
        return stack.is(ModItems.BLOCK_PALETTE.get());
    }

    public static boolean isAllowedContent(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.getItem().canFitInsideContainerItems();
    }

    public static NonNullList<ItemStack> getContents(ItemStack palette) {
        NonNullList<ItemStack> contents = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        palette.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents;
    }

    public static void saveContents(ItemStack palette, NonNullList<ItemStack> contents) {
        palette.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    private static List<Integer> filledBlockSlots(NonNullList<ItemStack> contents) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            if (isAllowedContent(contents.get(i))) {
                slots.add(i);
            }
        }
        return slots;
    }
}
