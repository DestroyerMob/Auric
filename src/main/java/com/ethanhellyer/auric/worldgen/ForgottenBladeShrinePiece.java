package com.ethanhellyer.auric.worldgen;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.block.SwordInStoneBlock;
import com.ethanhellyer.auric.blockentity.SwordInStoneBlockEntity;
import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

public final class ForgottenBladeShrinePiece extends StructurePiece {
    private static final int RADIUS = 2;
    private static final int HEIGHT = 5;
    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            Auric.id("chests/forgotten_blade_shrine")
    );

    public ForgottenBladeShrinePiece(RandomSource random, BlockPos origin) {
        super(ModStructures.FORGOTTEN_BLADE_SHRINE_PIECE.get(), 0, boundsFor(origin));
        this.setOrientation(Direction.Plane.HORIZONTAL.getRandomDirection(random));
    }

    public ForgottenBladeShrinePiece(CompoundTag tag) {
        super(ModStructures.FORGOTTEN_BLADE_SHRINE_PIECE.get(), tag);
    }

    private static BoundingBox boundsFor(BlockPos origin) {
        return new BoundingBox(
                origin.getX() - RADIUS,
                origin.getY(),
                origin.getZ() - RADIUS,
                origin.getX() + RADIUS,
                origin.getY() + HEIGHT - 1,
                origin.getZ() + RADIUS
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBounds,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        if (!AuricConfig.GENERATE_FORGOTTEN_BLADE_SHRINES.get()) {
            return;
        }

        BlockPos origin = origin();
        Direction facing = this.getOrientation();
        if (facing == null) {
            facing = Direction.NORTH;
        }

        placeFoundation(level, chunkBounds, origin, random);
        clearShrineSpace(level, chunkBounds, origin);
        placeFloor(level, chunkBounds, origin, random);
        placePedestal(level, chunkBounds, origin, facing, random);
        placeChest(level, chunkBounds, origin, facing, random);
        placeCandles(level, chunkBounds, origin, facing, random);
    }

    private BlockPos origin() {
        BlockPos center = this.boundingBox.getCenter();
        return new BlockPos(center.getX(), this.boundingBox.minY(), center.getZ());
    }

    public static boolean isFloorPosition(int x, int z) {
        return x * x + z * z <= 5;
    }

    private static void placeFoundation(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin, RandomSource random) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (!isFloorPosition(x, z)) {
                    continue;
                }

                for (int y = -1; y >= -5; y--) {
                    BlockPos supportPos = origin.offset(x, y, z);
                    BlockState supportState = level.getBlockState(supportPos);
                    if (supportState.isFaceSturdy(level, supportPos, Direction.UP) && supportState.getFluidState().isEmpty()) {
                        break;
                    }
                    setBlock(level, chunkBounds, supportPos, agedFoundationStone(random));
                }
            }
        }
    }

    private static void clearShrineSpace(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin) {
        for (int y = 1; y <= 4; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    setBlock(level, chunkBounds, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void placeFloor(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin, RandomSource random) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (isFloorPosition(x, z)) {
                    setBlock(level, chunkBounds, origin.offset(x, 0, z), agedStone(random));
                }
            }
        }
    }

    private static void placePedestal(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin, Direction facing, RandomSource random) {
        setBlock(level, chunkBounds, origin.above(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        placeSwordInStone(level, chunkBounds, origin.above(2), facing, random);

        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        setBlock(level, chunkBounds, origin.relative(left).relative(facing.getOpposite()).above(), Blocks.STONE_BRICK_WALL.defaultBlockState());
        setBlock(level, chunkBounds, origin.relative(right).relative(facing.getOpposite()).above(), Blocks.STONE_BRICK_WALL.defaultBlockState());

        if (random.nextBoolean()) {
            setBlock(level, chunkBounds, origin.relative(facing.getOpposite(), 2).above(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        }
    }

    private static void placeSwordInStone(WorldGenLevel level, BoundingBox chunkBounds, BlockPos pos, Direction facing, RandomSource random) {
        BlockState state = ModBlocks.SWORD_IN_STONE.get()
                .defaultBlockState()
                .setValue(SwordInStoneBlock.FACING, facing)
                .setValue(SwordInStoneBlock.HAS_SWORD, true);
        if (setBlock(level, chunkBounds, pos, state)
                && level.getBlockEntity(pos) instanceof SwordInStoneBlockEntity shrine) {
            shrine.setSword(ShrineSwordRewards.create(random));
        }
    }

    private static void placeChest(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin, Direction facing, RandomSource random) {
        BlockPos chestPos = origin.relative(facing.getOpposite()).above();
        BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
        if (setBlock(level, chunkBounds, chestPos, chestState)) {
            RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, LOOT_TABLE);
        }
    }

    private static void placeCandles(WorldGenLevel level, BoundingBox chunkBounds, BlockPos origin, Direction facing, RandomSource random) {
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        placeCandle(level, chunkBounds, origin.relative(facing, 2).relative(left).above(), random);
        placeCandle(level, chunkBounds, origin.relative(facing, 2).relative(right).above(), random);

        if (random.nextBoolean()) {
            placeCandle(level, chunkBounds, origin.relative(left, 2).above(), random);
        }
        if (random.nextBoolean()) {
            placeCandle(level, chunkBounds, origin.relative(right, 2).above(), random);
        }
    }

    private static void placeCandle(WorldGenLevel level, BoundingBox chunkBounds, BlockPos pos, RandomSource random) {
        BlockState candle = Blocks.CANDLE.defaultBlockState()
                .setValue(CandleBlock.CANDLES, 1 + random.nextInt(3))
                .setValue(CandleBlock.LIT, true);
        if (level.getBlockState(pos).isAir() && candle.canSurvive(level, pos)) {
            setBlock(level, chunkBounds, pos, candle);
        }
    }

    private static boolean setBlock(WorldGenLevel level, BoundingBox chunkBounds, BlockPos pos, BlockState state) {
        if (!chunkBounds.isInside(pos)) {
            return false;
        }

        level.setBlock(pos, state, 2);
        return true;
    }

    private static BlockState agedStone(RandomSource random) {
        int roll = random.nextInt(8);
        if (roll == 0) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (roll <= 2) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState agedFoundationStone(RandomSource random) {
        return random.nextInt(3) == 0
                ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
    }
}
