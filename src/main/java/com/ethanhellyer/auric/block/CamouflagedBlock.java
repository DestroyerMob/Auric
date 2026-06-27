package com.ethanhellyer.auric.block;

import com.ethanhellyer.auric.blockentity.CamouflagedBlockEntity;
import com.ethanhellyer.auric.camo.CamouflageHelper;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CamouflagedBlock extends BaseEntityBlock {
    public static final MapCodec<CamouflagedBlock> CODEC = simpleCodec(CamouflagedBlock::new);

    public CamouflagedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CamouflagedBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return visualState(level, pos).getShape(level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return realState(level, pos).getCollisionShape(level, pos, context);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return visualState(level, pos).getVisualShape(level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState visual = visualState(level, pos);
        return visual.canOcclude() ? visual.getOcclusionShape(level, pos) : Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).getShape(level, pos);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).getBlockSupportShape(level, pos);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).getLightBlock(level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).getShadeBrightness(level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).propagatesSkylightDown(level, pos);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return visualState(level, pos).getLightEmission(level, pos);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return Block.isShapeFullBlock(visualState(level, pos).getCollisionShape(level, pos));
    }

    @Override
    protected boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return Block.isShapeFullBlock(visualState(level, pos).getOcclusionShape(level, pos));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState real = realState(level, pos);
        if (!(real.getBlock() instanceof DoorBlock) || !real.hasProperty(DoorBlock.HALF)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }

        DoubleBlockHalf half = real.getValue(DoorBlock.HALF);
        Direction pairedDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == pairedDirection) {
            return isMatchingDoorNeighbor(real, neighborState, level, neighborPos) ? state : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !real.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            CamouflageHelper.updateHiddenDoorPower(level, pos);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CAMO_BRUSH.get()) || !player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        restore(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) {
            if (CamouflageHelper.canOpenHiddenDoor(level, pos)) {
                if (!level.isClientSide) {
                    CamouflageHelper.toggleHiddenDoor(level, pos, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    private static void restore(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && CamouflageHelper.restore(level, pos)) {
            player.displayClientMessage(Component.translatable("message.auric.camo_restored"), true);
        }
    }

    private static BlockState realState(BlockGetter level, BlockPos pos) {
        return CamouflageHelper.realState(level, pos);
    }

    private static BlockState disguiseState(BlockGetter level, BlockPos pos) {
        return CamouflageHelper.disguiseState(level, pos);
    }

    private static BlockState visualState(BlockGetter level, BlockPos pos) {
        BlockState real = realState(level, pos);
        return real.getBlock() instanceof DoorBlock ? real : disguiseState(level, pos);
    }

    private static boolean isMatchingDoorNeighbor(BlockState real, BlockState neighborState, LevelAccessor level, BlockPos neighborPos) {
        BlockState neighborReal = neighborState;
        if (neighborState.is(ModBlocks.CAMOUFLAGED_BLOCK.get())) {
            BlockEntity neighborBlockEntity = level.getBlockEntity(neighborPos);
            if (neighborBlockEntity instanceof CamouflagedBlockEntity neighborCamo) {
                neighborReal = neighborCamo.getRealState();
            }
        }

        return neighborReal.getBlock() instanceof DoorBlock
                && neighborReal.is(real.getBlock())
                && neighborReal.hasProperty(DoorBlock.HALF)
                && neighborReal.getValue(DoorBlock.HALF) != real.getValue(DoorBlock.HALF);
    }
}
