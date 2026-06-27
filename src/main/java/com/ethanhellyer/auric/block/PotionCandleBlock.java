package com.ethanhellyer.auric.block;

import com.ethanhellyer.auric.blockentity.PotionCandleBlockEntity;
import com.ethanhellyer.auric.item.PotionCandleItem;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

public class PotionCandleBlock extends CandleBlock implements EntityBlock {
    public PotionCandleBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionCandleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntityTypes.POTION_CANDLE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                PotionCandleBlockEntity.serverTick(tickLevel, pos, tickState, (PotionCandleBlockEntity) blockEntity);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PotionCandleBlockEntity candle) {
            candle.addPlacedCandle(PotionCandleItem.getContents(stack), state.getValue(CandleBlock.CANDLES));
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof PotionCandleBlockEntity candle) {
            PotionCandleItem.setContents(stack, candle.firstContents());
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof PotionCandleBlockEntity candle)) {
            return drops;
        }

        List<PotionContents> contents = candle.contentsList();
        if (contents.isEmpty()) {
            return drops;
        }

        List<ItemStack> splitDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (!drop.is(this.asItem())) {
                splitDrops.add(drop);
                continue;
            }
            int count = drop.getCount();
            for (int i = 0; i < count; i++) {
                ItemStack split = new ItemStack(this);
                PotionCandleItem.setContents(split, contents.get(Math.min(i, contents.size() - 1)));
                splitDrops.add(split);
            }
        }
        return splitDrops;
    }
}
