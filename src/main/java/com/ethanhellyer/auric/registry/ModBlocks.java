package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.block.CamouflagedBlock;
import com.ethanhellyer.auric.block.ImbuingTableBlock;
import com.ethanhellyer.auric.block.PotionCauldronBlock;
import com.ethanhellyer.auric.block.PotionCandleBlock;
import java.util.function.Supplier;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final String IMBUING_TABLE_ID = "imbuing_table";
    public static final String POTION_CAULDRON_ID = "potion_cauldron";
    public static final String CAMOUFLAGED_BLOCK_ID = "camouflaged_block";
    public static final String POTION_CANDLE_ID = "potion_candle";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Auric.MOD_ID);

    public static final Supplier<ImbuingTableBlock> IMBUING_TABLE = BLOCKS.register(
            IMBUING_TABLE_ID,
            () -> new ImbuingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST))
    );

    public static final Supplier<PotionCauldronBlock> POTION_CAULDRON = BLOCKS.register(
            POTION_CAULDRON_ID,
            () -> new PotionCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).dropsLike(Blocks.CAULDRON))
    );

    public static final Supplier<PotionCandleBlock> POTION_CANDLE = BLOCKS.register(
            POTION_CANDLE_ID,
            () -> new PotionCandleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE)
                    .lightLevel(CandleBlock.LIGHT_EMISSION))
    );

    public static final Supplier<CamouflagedBlock> CAMOUFLAGED_BLOCK = BLOCKS.register(
            CAMOUFLAGED_BLOCK_ID,
            () -> new CamouflagedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.0F)
                    .dynamicShape()
                    .noLootTable())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
