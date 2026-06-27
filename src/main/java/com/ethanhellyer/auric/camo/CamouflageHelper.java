package com.ethanhellyer.auric.camo;

import com.ethanhellyer.auric.blockentity.CamouflagedBlockEntity;
import com.ethanhellyer.auric.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

public final class CamouflageHelper {
    private static final int PAIRED_BLOCK_UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

    private CamouflageHelper() {
    }

    public static BlockState realState(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CamouflagedBlockEntity camo) {
            return camo.getRealState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    public static BlockState disguiseState(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CamouflagedBlockEntity camo) {
            return camo.getDisguiseState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    public static boolean apply(Level level, BlockPos pos, BlockState disguise) {
        BlockState realState = level.getBlockState(pos);
        if (realState.isAir()) {
            return false;
        }

        BlockEntity existingBlockEntity = level.getBlockEntity(pos);
        if (realState.is(ModBlocks.CAMOUFLAGED_BLOCK.get())) {
            if (existingBlockEntity instanceof CamouflagedBlockEntity camo) {
                return updateDisguise(level, pos, camo, disguise);
            }
            return false;
        }

        if (isDoorState(realState)) {
            return applyDoor(level, pos, realState, disguise);
        }

        return applySingle(level, pos, realState, disguise, captureBlockEntity(level, existingBlockEntity), Block.UPDATE_ALL);
    }

    public static boolean restore(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CamouflagedBlockEntity camo)) {
            return false;
        }

        if (isDoorState(camo.getRealState())) {
            return restoreDoor(level, pos, camo);
        }

        return restoreSingle(level, pos, camo, Block.UPDATE_ALL);
    }

    public static boolean canOpenHiddenDoor(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CamouflagedBlockEntity camo && camo.getRealState().getBlock() instanceof DoorBlock door) {
            return door.type().canOpenByHand();
        }
        return false;
    }

    public static boolean toggleHiddenDoor(Level level, BlockPos pos, @Nullable Entity source) {
        if (!(level.getBlockEntity(pos) instanceof CamouflagedBlockEntity camo) || !(camo.getRealState().getBlock() instanceof DoorBlock door)) {
            return false;
        }
        if (!door.type().canOpenByHand()) {
            return false;
        }

        BlockPos lowerPos = lowerDoorPos(pos, camo.getRealState());
        DoorPair pair = doorPair(level, lowerPos);
        if (pair == null) {
            return false;
        }

        setDoorStates(level, lowerPos, pair, !pair.lowerState.getValue(DoorBlock.OPEN), null, source);
        return true;
    }

    public static void updateHiddenDoorPower(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CamouflagedBlockEntity camo) || !isDoorState(camo.getRealState())) {
            return;
        }

        BlockPos lowerPos = lowerDoorPos(pos, camo.getRealState());
        DoorPair pair = doorPair(level, lowerPos);
        if (pair == null) {
            return;
        }

        boolean powered = level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(lowerPos.above());
        if (powered != pair.lowerState.getValue(DoorBlock.POWERED)) {
            setDoorStates(level, lowerPos, pair, powered, powered, null);
        }
    }

    private static boolean applySingle(Level level, BlockPos pos, BlockState realState, BlockState disguise, @Nullable CompoundTag realBlockEntityTag, int flags) {
        if (!level.setBlock(pos, ModBlocks.CAMOUFLAGED_BLOCK.get().defaultBlockState(), flags)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof CamouflagedBlockEntity camo) {
            camo.setStates(realState, disguiseForRealPosition(disguise, realState), realBlockEntityTag);
            return true;
        }
        return false;
    }

    private static boolean applyDoor(Level level, BlockPos clickedPos, BlockState clickedState, BlockState disguise) {
        BlockPos lowerPos = lowerDoorPos(clickedPos, clickedState);
        BlockPos upperPos = lowerPos.above();
        BlockState lowerState = level.getBlockState(lowerPos);
        BlockState upperState = level.getBlockState(upperPos);
        if (!isMatchingDoorPair(lowerState, upperState)) {
            return false;
        }

        CompoundTag lowerBlockEntityTag = captureBlockEntity(level, level.getBlockEntity(lowerPos));
        CompoundTag upperBlockEntityTag = captureBlockEntity(level, level.getBlockEntity(upperPos));

        if (!level.setBlock(lowerPos, ModBlocks.CAMOUFLAGED_BLOCK.get().defaultBlockState(), PAIRED_BLOCK_UPDATE_FLAGS)) {
            return false;
        }
        if (!level.setBlock(upperPos, ModBlocks.CAMOUFLAGED_BLOCK.get().defaultBlockState(), PAIRED_BLOCK_UPDATE_FLAGS)) {
            level.setBlock(lowerPos, lowerState, PAIRED_BLOCK_UPDATE_FLAGS);
            restoreBlockEntity(level, lowerPos, lowerState, lowerBlockEntityTag);
            return false;
        }

        if (level.getBlockEntity(lowerPos) instanceof CamouflagedBlockEntity lowerCamo
                && level.getBlockEntity(upperPos) instanceof CamouflagedBlockEntity upperCamo) {
            lowerCamo.setStates(lowerState, disguiseForRealPosition(disguise, lowerState), lowerBlockEntityTag);
            upperCamo.setStates(upperState, disguiseForRealPosition(disguise, upperState), upperBlockEntityTag);
            return true;
        }

        level.setBlock(upperPos, upperState, PAIRED_BLOCK_UPDATE_FLAGS);
        restoreBlockEntity(level, upperPos, upperState, upperBlockEntityTag);
        level.setBlock(lowerPos, lowerState, PAIRED_BLOCK_UPDATE_FLAGS);
        restoreBlockEntity(level, lowerPos, lowerState, lowerBlockEntityTag);
        return false;
    }

    private static boolean updateDisguise(Level level, BlockPos pos, CamouflagedBlockEntity camo, BlockState disguise) {
        if (!isDoorState(camo.getRealState())) {
            camo.setStates(camo.getRealState(), disguiseForRealPosition(disguise, camo.getRealState()), camo.getRealBlockEntityTag());
            return true;
        }

        BlockPos lowerPos = lowerDoorPos(pos, camo.getRealState());
        BlockPos upperPos = lowerPos.above();
        if (level.getBlockEntity(lowerPos) instanceof CamouflagedBlockEntity lowerCamo
                && level.getBlockEntity(upperPos) instanceof CamouflagedBlockEntity upperCamo
                && isMatchingDoorPair(lowerCamo.getRealState(), upperCamo.getRealState())) {
            lowerCamo.setStates(lowerCamo.getRealState(), disguiseForRealPosition(disguise, lowerCamo.getRealState()), lowerCamo.getRealBlockEntityTag());
            upperCamo.setStates(upperCamo.getRealState(), disguiseForRealPosition(disguise, upperCamo.getRealState()), upperCamo.getRealBlockEntityTag());
            return true;
        }

        camo.setStates(camo.getRealState(), disguiseForRealPosition(disguise, camo.getRealState()), camo.getRealBlockEntityTag());
        return true;
    }

    private static boolean restoreSingle(Level level, BlockPos pos, CamouflagedBlockEntity camo, int flags) {
        BlockState realState = camo.getRealState();
        CompoundTag realBlockEntityTag = camo.getRealBlockEntityTag();
        if (!level.setBlock(pos, realState, flags)) {
            return false;
        }
        restoreBlockEntity(level, pos, realState, realBlockEntityTag);
        return true;
    }

    private static boolean restoreDoor(Level level, BlockPos pos, CamouflagedBlockEntity camo) {
        BlockPos lowerPos = lowerDoorPos(pos, camo.getRealState());
        BlockPos upperPos = lowerPos.above();
        if (level.getBlockEntity(lowerPos) instanceof CamouflagedBlockEntity lowerCamo
                && level.getBlockEntity(upperPos) instanceof CamouflagedBlockEntity upperCamo
                && isMatchingDoorPair(lowerCamo.getRealState(), upperCamo.getRealState())) {
            boolean upperRestored = restoreSingle(level, upperPos, upperCamo, PAIRED_BLOCK_UPDATE_FLAGS);
            boolean lowerRestored = restoreSingle(level, lowerPos, lowerCamo, PAIRED_BLOCK_UPDATE_FLAGS);
            return upperRestored && lowerRestored;
        }

        return restoreSingle(level, pos, camo, Block.UPDATE_ALL);
    }

    private static void setDoorStates(Level level, BlockPos lowerPos, DoorPair pair, boolean open, @Nullable Boolean powered, @Nullable Entity source) {
        boolean wasOpen = pair.lowerState.getValue(DoorBlock.OPEN);
        BlockState lowerState = pair.lowerState.setValue(DoorBlock.OPEN, open);
        BlockState upperState = pair.upperState.setValue(DoorBlock.OPEN, open);
        if (powered != null) {
            lowerState = lowerState.setValue(DoorBlock.POWERED, powered);
            upperState = upperState.setValue(DoorBlock.POWERED, powered);
        }

        pair.lowerCamo.setStates(lowerState, pair.lowerCamo.getDisguiseState(), pair.lowerCamo.getRealBlockEntityTag());
        pair.upperCamo.setStates(upperState, pair.upperCamo.getDisguiseState(), pair.upperCamo.getRealBlockEntityTag());
        if (wasOpen != open && pair.lowerState.getBlock() instanceof DoorBlock door) {
            level.playSound(null, lowerPos, open ? door.type().doorOpen() : door.type().doorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.gameEvent(source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, lowerPos);
        }
    }

    @Nullable
    private static DoorPair doorPair(Level level, BlockPos lowerPos) {
        BlockPos upperPos = lowerPos.above();
        if (level.getBlockEntity(lowerPos) instanceof CamouflagedBlockEntity lowerCamo
                && level.getBlockEntity(upperPos) instanceof CamouflagedBlockEntity upperCamo
                && isMatchingDoorPair(lowerCamo.getRealState(), upperCamo.getRealState())) {
            return new DoorPair(lowerCamo, upperCamo, lowerCamo.getRealState(), upperCamo.getRealState());
        }
        return null;
    }

    private static BlockState disguiseForRealPosition(BlockState disguise, BlockState realState) {
        if (isDoorState(disguise) && isDoorState(realState)) {
            return disguise.setValue(DoorBlock.HALF, realState.getValue(DoorBlock.HALF));
        }
        return disguise;
    }

    private static boolean isDoorState(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.HALF);
    }

    private static boolean isMatchingDoorPair(BlockState lowerState, BlockState upperState) {
        return isDoorState(lowerState)
                && isDoorState(upperState)
                && upperState.is(lowerState.getBlock())
                && lowerState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && upperState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    private static BlockPos lowerDoorPos(BlockPos pos, BlockState doorState) {
        return doorState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Nullable
    private static CompoundTag captureBlockEntity(Level level, @Nullable BlockEntity blockEntity) {
        if (blockEntity != null && level instanceof ServerLevel serverLevel) {
            return blockEntity.saveWithFullMetadata(serverLevel.registryAccess());
        }
        return null;
    }

    private static void restoreBlockEntity(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityTag) {
        if (blockEntityTag != null && level instanceof ServerLevel serverLevel) {
            BlockEntity restored = level.getBlockEntity(pos);
            if (restored != null) {
                restored.loadWithComponents(blockEntityTag, serverLevel.registryAccess());
                restored.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private record DoorPair(CamouflagedBlockEntity lowerCamo, CamouflagedBlockEntity upperCamo, BlockState lowerState, BlockState upperState) {
    }
}
