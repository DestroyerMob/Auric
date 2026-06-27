package com.ethanhellyer.auric.camo;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class CamoLinkedBlocks {
    private CamoLinkedBlocks() {
    }

    public static List<BlockPos> linkedPositions(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = pos.relative(half.getDirectionToOther());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(state.getBlock())
                    && otherState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == half.getOtherHalf()) {
                return List.of(pos, otherPos);
            }
        }

        if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            BlockPos otherPos = pos.relative(BedBlock.getConnectedDirection(state));
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(state.getBlock())
                    && otherState.hasProperty(BlockStateProperties.BED_PART)
                    && otherState.getValue(BlockStateProperties.BED_PART) != part) {
                return List.of(pos, otherPos);
            }
        }

        return List.of(pos);
    }

    public static BlockState disguiseForTarget(BlockState disguise, BlockState target) {
        BlockState adjusted = disguise;
        if (adjusted.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && target.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            adjusted = adjusted.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, target.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
        }
        if (adjusted.hasProperty(BlockStateProperties.BED_PART) && target.hasProperty(BlockStateProperties.BED_PART)) {
            adjusted = adjusted.setValue(BlockStateProperties.BED_PART, target.getValue(BlockStateProperties.BED_PART));
        }
        return adjusted;
    }
}
