package com.ethanhellyer.auric.item;

import com.ethanhellyer.auric.camo.CamoLinkedBlocks;
import com.ethanhellyer.auric.camo.CamoNetworking;
import com.ethanhellyer.auric.camo.CamoPasteData;
import com.ethanhellyer.auric.camo.CamouflageHelper;
import com.ethanhellyer.auric.camo.PositionCamoSavedData;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModDataComponents;
import com.ethanhellyer.auric.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class CamoBrushItem extends Item {
    private static final int BAR_COLOR = Mth.color(0.67F, 0.36F, 0.95F);

    public CamoBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useBrush(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useBrush(context);
    }

    private static InteractionResult useBrush(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack brush = context.getItemInHand();
        BlockState clickedState = level.getBlockState(pos);

        if (player != null && player.isSecondaryUseActive()) {
            if (clickedState.is(ModBlocks.CAMOUFLAGED_BLOCK.get())) {
                if (!level.isClientSide && CamouflageHelper.restore(level, pos)) {
                    player.displayClientMessage(Component.translatable("message.auric.camo_restored"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide && removePositionCamouflage(level, pos)) {
                player.displayClientMessage(Component.translatable("message.auric.camo_restored"), true);
                return InteractionResult.sidedSuccess(false);
            }
            if (!level.isClientSide) {
                BlockState disguise = clickedState;
                if (!canUseAsCamouflage(disguise)) {
                    player.displayClientMessage(Component.translatable("message.auric.camo_unsupported"), true);
                    return InteractionResult.FAIL;
                }
                brush.set(ModDataComponents.CAMO_SAMPLE.get(), new CamoPasteData(disguise));
                player.displayClientMessage(Component.translatable("message.auric.camo_sampled", disguise.getBlock().getName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        CamoPasteData sample = brush.get(ModDataComponents.CAMO_SAMPLE.get());
        if (clickedState.is(ModBlocks.CAMOUFLAGED_BLOCK.get()) && sample == null) {
            if (!level.isClientSide && player != null && CamouflageHelper.restore(level, pos)) {
                player.displayClientMessage(Component.translatable("message.auric.camo_restored"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (sample == null) {
            if (!level.isClientSide && player != null) {
                if (removePositionCamouflage(level, pos)) {
                    player.displayClientMessage(Component.translatable("message.auric.camo_restored"), true);
                    return InteractionResult.SUCCESS;
                }
                player.displayClientMessage(Component.translatable("message.auric.camo_needs_sample"), true);
            }
            return level.isClientSide ? InteractionResult.PASS : InteractionResult.FAIL;
        }

        if (!level.isClientSide && player != null && !canUseAsCamouflage(sample.disguise())) {
            player.displayClientMessage(Component.translatable("message.auric.camo_unsupported"), true);
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide && player != null && !canUseAsCamouflage(clickedState)) {
            player.displayClientMessage(Component.translatable("message.auric.camo_unsupported"), true);
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            if (player != null && !player.getAbilities().instabuild && pasteCount(brush) <= 0) {
                player.displayClientMessage(Component.translatable("message.auric.camo_brush_needs_paste"), true);
                return InteractionResult.FAIL;
            }
            if (!applyPositionCamouflage(level, pos, sample.disguise())) {
                return InteractionResult.FAIL;
            }
            if (player != null && !player.getAbilities().instabuild) {
                consumePaste(brush);
            }
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.auric.camo_applied"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack brush, Slot slot, ClickAction action, Player player) {
        if (brush.getCount() != 1 || action != ClickAction.SECONDARY) {
            return false;
        }

        BundleContents contents = brush.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return false;
        }

        ItemStack slotStack = slot.getItem();
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        if (slotStack.isEmpty()) {
            playRemoveOneSound(player);
            ItemStack removed = mutable.removeOne();
            if (removed != null) {
                ItemStack leftover = slot.safeInsert(removed);
                mutable.tryInsert(leftover);
            }
        } else if (canStore(slotStack)) {
            int inserted = mutable.tryTransfer(slot, player);
            if (inserted > 0) {
                playInsertSound(player);
            }
        }

        brush.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack brush, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (brush.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        BundleContents contents = brush.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return false;
        }

        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        if (carried.isEmpty()) {
            ItemStack removed = mutable.removeOne();
            if (removed != null) {
                playRemoveOneSound(player);
                access.set(removed);
            }
        } else if (canStore(carried)) {
            int inserted = mutable.tryInsert(carried);
            if (inserted > 0) {
                playInsertSound(player);
            }
        }

        brush.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack brush = player.getItemInHand(hand);
        if (dropContents(brush, player)) {
            playDropContentsSound(player);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(brush, level.isClientSide());
        }
        return InteractionResultHolder.pass(brush);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return pasteCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = pasteCapacity();
        return Math.min(1 + pasteCount(stack) * 12 / capacity, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (stack.has(DataComponents.HIDE_TOOLTIP) || stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CamoPasteData sample = stack.get(ModDataComponents.CAMO_SAMPLE.get());
        if (sample == null) {
            tooltip.add(Component.translatable("tooltip.auric.camo_empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.auric.camo_sample", sample.disguise().getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("tooltip.auric.camo_brush_paste", pasteCount(stack), pasteCapacity()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.auric.camo_remove").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CAMO_SAMPLE.get()) || super.isFoil(stack);
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        BundleContents contents = itemEntity.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            itemEntity.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            ItemUtils.onContainerDestroyed(itemEntity, contents.itemsCopy());
        }
    }

    private static boolean applyPositionCamouflage(Level level, BlockPos pos, BlockState disguise) {
        if (!(level instanceof ServerLevel serverLevel) || !canUseAsCamouflage(disguise)) {
            return false;
        }

        List<BlockPos> targets = CamoLinkedBlocks.linkedPositions(serverLevel, pos);
        for (BlockPos target : targets) {
            if (!canUseAsCamouflage(level.getBlockState(target))) {
                return false;
            }
        }

        PositionCamoSavedData data = PositionCamoSavedData.get(serverLevel);
        for (BlockPos target : targets) {
            BlockState targetState = serverLevel.getBlockState(target);
            BlockState targetDisguise = CamoLinkedBlocks.disguiseForTarget(disguise, targetState);
            data.set(target, targetDisguise);
            CamoNetworking.sendUpdate(serverLevel, target, Optional.of(targetDisguise));
            refreshBlock(serverLevel, target);
        }
        return true;
    }

    private static boolean removePositionCamouflage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        boolean removed = false;
        PositionCamoSavedData data = PositionCamoSavedData.get(serverLevel);
        for (BlockPos target : CamoLinkedBlocks.linkedPositions(serverLevel, pos)) {
            if (data.remove(target)) {
                CamoNetworking.sendUpdate(serverLevel, target, Optional.empty());
                refreshBlock(serverLevel, target);
                removed = true;
            }
        }
        return removed;
    }

    private static void refreshBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean canUseAsCamouflage(BlockState state) {
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL;
    }

    private static boolean canStore(ItemStack stack) {
        return stack.is(ModItems.CAMO_PASTE.get()) && stack.canFitInsideContainerItems();
    }

    private static boolean consumePaste(ItemStack brush) {
        BundleContents contents = brush.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        List<ItemStack> kept = new ArrayList<>();
        boolean consumed = false;
        for (ItemStack stored : contents.itemsCopy()) {
            ItemStack copy = stored.copy();
            if (!consumed && copy.is(ModItems.CAMO_PASTE.get())) {
                copy.shrink(1);
                consumed = true;
            }
            if (!copy.isEmpty()) {
                kept.add(copy);
            }
        }

        if (consumed) {
            brush.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(kept));
        }
        return consumed;
    }

    private static int pasteCount(ItemStack brush) {
        int count = 0;
        BundleContents contents = brush.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        for (ItemStack stored : contents.items()) {
            if (stored.is(ModItems.CAMO_PASTE.get())) {
                count += stored.getCount();
            }
        }
        return count;
    }

    private static int pasteCapacity() {
        return new ItemStack(ModItems.CAMO_PASTE.get()).getMaxStackSize();
    }

    private static boolean dropContents(ItemStack brush, Player player) {
        BundleContents contents = brush.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null && !contents.isEmpty()) {
            brush.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            if (player instanceof ServerPlayer) {
                contents.itemsCopy().forEach(item -> player.drop(item, true));
            }
            return true;
        }
        return false;
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playDropContentsSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }
}
