package com.ethanhellyer.auric.cauldron;

import com.ethanhellyer.auric.blockentity.PotionCauldronBlockEntity;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModItems;
import com.ethanhellyer.auric.tag.ModBlockTags;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class CauldronBrewingEvents {
    private static final int INGREDIENT_CHECK_INTERVAL = 10;

    @SubscribeEvent
    public void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity) || itemEntity.level().isClientSide || itemEntity.tickCount % INGREDIENT_CHECK_INTERVAL != 0) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel level) || itemEntity.getItem().isEmpty()) {
            return;
        }

        findWaterCauldronContaining(itemEntity).ifPresent(pos -> tryBrew(level, pos, itemEntity));
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        CauldronBrewingData data = CauldronBrewingData.get(serverLevel);
        if (!isBrewableCauldron(state)) {
            data.clear(pos);
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.CAMO_BRUSH.get())) {
            return;
        }

        Optional<Holder<Potion>> brewedPotion = potionAt(serverLevel, data, pos);
        if (held.is(Items.GLASS_BOTTLE) && brewedPotion.isPresent()) {
            bottlePotion(serverLevel, pos, state, player, hand, held, brewedPotion.get());
            if (state.getValue(LayeredCauldronBlock.LEVEL) <= 1) {
                data.clear(pos);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (held.is(Items.BUCKET) && hasPotion(serverLevel, data, pos)) {
            data.clear(pos);
        } else if (!held.isEmpty() && hasPotion(serverLevel, data, pos)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void tryBrew(ServerLevel level, BlockPos pos, ItemEntity itemEntity) {
        BlockState state = level.getBlockState(pos);
        if (!isBrewableCauldron(state) || !hasHeatSource(level, pos)) {
            return;
        }

        ItemStack ingredient = itemEntity.getItem();
        CauldronBrewingData data = CauldronBrewingData.get(level);
        ItemStack currentPotion = currentPotionStack(level, data, pos);
        ItemStack result = level.potionBrewing().mix(ingredient.copyWithCount(1), currentPotion);
        if (result.isEmpty() || !result.is(Items.POTION) || ItemStack.isSameItemSameComponents(currentPotion, result)) {
            return;
        }

        PotionContents resultContents = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Optional<Holder<Potion>> resultPotion = resultContents.potion();
        if (resultPotion.isEmpty() || resultPotion.get().is(Potions.WATER)) {
            clearPotion(level, data, pos, state);
        } else {
            setPotion(level, data, pos, state, resultPotion.get());
        }

        ingredient.shrink(1);
        if (ingredient.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(ingredient);
        }
        playBrewFeedback(level, pos, resultContents.getColor());
    }

    private static ItemStack currentPotionStack(ServerLevel level, CauldronBrewingData data, BlockPos pos) {
        return potionAt(level, data, pos)
                .map(potion -> PotionContents.createItemStack(Items.POTION, potion))
                .orElseGet(() -> PotionContents.createItemStack(Items.POTION, Potions.WATER));
    }

    private static Optional<Holder<Potion>> potionAt(ServerLevel level, CauldronBrewingData data, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PotionCauldronBlockEntity cauldron) {
            return Optional.of(cauldron.getPotion());
        }
        return data.potionAt(pos);
    }

    private static boolean hasPotion(ServerLevel level, CauldronBrewingData data, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PotionCauldronBlockEntity || data.hasPotion(pos);
    }

    private static void setPotion(ServerLevel level, CauldronBrewingData data, BlockPos pos, BlockState state, Holder<Potion> potion) {
        data.setPotion(pos, potion);
        BlockState potionState = ModBlocks.POTION_CAULDRON.get()
                .defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL));
        if (!state.is(ModBlocks.POTION_CAULDRON.get())) {
            level.setBlock(pos, potionState, Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof PotionCauldronBlockEntity cauldron) {
            cauldron.setPotion(potion);
        }
    }

    private static void clearPotion(ServerLevel level, CauldronBrewingData data, BlockPos pos, BlockState state) {
        data.clear(pos);
        if (state.is(ModBlocks.POTION_CAULDRON.get())) {
            BlockState waterState = Blocks.WATER_CAULDRON
                    .defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL));
            level.setBlock(pos, waterState, Block.UPDATE_ALL);
        }
    }

    private static void bottlePotion(ServerLevel level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack bottle, Holder<Potion> potion) {
        Item item = bottle.getItem();
        player.setItemInHand(hand, ItemUtils.createFilledResult(bottle, player, PotionContents.createItemStack(Items.POTION, potion)));
        player.awardStat(Stats.USE_CAULDRON);
        player.awardStat(Stats.ITEM_USED.get(item));
        LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
    }

    private static boolean isBrewableCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON) || state.is(ModBlocks.POTION_CAULDRON.get());
    }

    private static boolean hasHeatSource(Level level, BlockPos cauldronPos) {
        BlockState heat = level.getBlockState(cauldronPos.below());
        if (!heat.is(ModBlockTags.CAULDRON_HEAT_SOURCES)) {
            return false;
        }
        return !heat.hasProperty(BlockStateProperties.LIT) || heat.getValue(BlockStateProperties.LIT);
    }

    private static Optional<BlockPos> findWaterCauldronContaining(ItemEntity itemEntity) {
        BlockPos pos = itemEntity.blockPosition();
        if (isInsideCauldronContent(itemEntity, pos)) {
            return Optional.of(pos);
        }
        BlockPos below = pos.below();
        if (isInsideCauldronContent(itemEntity, below)) {
            return Optional.of(below);
        }
        return Optional.empty();
    }

    private static boolean isInsideCauldronContent(ItemEntity itemEntity, BlockPos pos) {
        BlockState state = itemEntity.level().getBlockState(pos);
        if (!isBrewableCauldron(state)) {
            return false;
        }

        double localX = itemEntity.getX() - pos.getX();
        double localY = itemEntity.getY() - pos.getY();
        double localZ = itemEntity.getZ() - pos.getZ();
        double contentHeight = (6.0D + state.getValue(LayeredCauldronBlock.LEVEL) * 3.0D) / 16.0D;
        return localX >= 0.125D
                && localX <= 0.875D
                && localZ >= 0.125D
                && localZ <= 0.875D
                && localY >= 0.25D
                && localY <= contentHeight + 0.35D;
    }

    private static void playBrewFeedback(ServerLevel level, BlockPos pos, int color) {
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.2F);
        level.levelEvent(2002, pos, color);
    }
}
