package com.ethanhellyer.auric.block;

import com.ethanhellyer.auric.blockentity.SwordInStoneBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SwordInStoneBlock extends BaseEntityBlock {
    public static final MapCodec<SwordInStoneBlock> CODEC = simpleCodec(SwordInStoneBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_SWORD = BooleanProperty.create("has_sword");

    private static final VoxelShape SHAPE = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D)
    );

    public SwordInStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_SWORD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwordInStoneBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, HAS_SWORD);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SwordInStoneBlockEntity shrine) {
            if (shrine.hasSword()) {
                retrieveSword(level, pos, player, shrine);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stack.is(ItemTags.SWORDS)) {
                storeSword(level, pos, player, stack, shrine);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SwordInStoneBlockEntity shrine && shrine.hasSword()) {
            retrieveSword(level, pos, player, shrine);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof SwordInStoneBlockEntity shrine
                && shrine.hasSword()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shrine.removeSwordForDrop());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void updateSwordState(Level level, BlockPos pos, boolean hasSword) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SwordInStoneBlock && state.getValue(HAS_SWORD) != hasSword) {
            level.setBlock(pos, state.setValue(HAS_SWORD, hasSword), 3);
        }
    }

    private static void retrieveSword(Level level, BlockPos pos, Player player, SwordInStoneBlockEntity shrine) {
        if (level.isClientSide) {
            return;
        }

        ItemStack sword = shrine.takeSword();
        if (sword.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(sword.copy())) {
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 1.0D, pos.getZ(), sword.copy());
        }
    }

    private static void storeSword(Level level, BlockPos pos, Player player, ItemStack held, SwordInStoneBlockEntity shrine) {
        if (level.isClientSide) {
            return;
        }

        ItemStack stored = held.copyWithCount(1);
        shrine.setSword(stored);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }
}
