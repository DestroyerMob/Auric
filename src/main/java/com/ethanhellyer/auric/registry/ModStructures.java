package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.worldgen.ForgottenBladeShrinePiece;
import com.ethanhellyer.auric.worldgen.ForgottenBladeShrineStructure;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Auric.MOD_ID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Auric.MOD_ID);

    public static final Supplier<StructureType<ForgottenBladeShrineStructure>> FORGOTTEN_BLADE_SHRINE =
            STRUCTURE_TYPES.register("forgotten_blade_shrine", () -> () -> ForgottenBladeShrineStructure.CODEC);

    public static final Supplier<StructurePieceType> FORGOTTEN_BLADE_SHRINE_PIECE =
            STRUCTURE_PIECES.register("forgotten_blade_shrine", () -> (StructurePieceType.ContextlessType) ForgottenBladeShrinePiece::new);

    private ModStructures() {
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
    }
}
