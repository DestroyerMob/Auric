package com.ethanhellyer.auric.worldgen;

import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class ForgottenBladeShrineStructure extends Structure {
    public static final MapCodec<ForgottenBladeShrineStructure> CODEC = simpleCodec(ForgottenBladeShrineStructure::new);
    private static final int RADIUS = 2;

    public ForgottenBladeShrineStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!AuricConfig.GENERATE_FORGOTTEN_BLADE_SHRINES.get()) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = highestSurface(context, x, z);
        if (y <= context.chunkGenerator().getSeaLevel() || y <= context.heightAccessor().getMinBuildHeight()) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(new ForgottenBladeShrinePiece(context.random(), origin))));
    }

    private static int highestSurface(GenerationContext context, int centerX, int centerZ) {
        int highest = context.heightAccessor().getMinBuildHeight();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (!ForgottenBladeShrinePiece.isFloorPosition(x, z)) {
                    continue;
                }
                int surface = context.chunkGenerator().getFirstOccupiedHeight(
                        centerX + x,
                        centerZ + z,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        context.heightAccessor(),
                        context.randomState()
                );
                if (surface <= context.chunkGenerator().getSeaLevel()) {
                    return context.heightAccessor().getMinBuildHeight();
                }
                highest = Math.max(highest, surface);
            }
        }
        return highest;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FORGOTTEN_BLADE_SHRINE.get();
    }
}
