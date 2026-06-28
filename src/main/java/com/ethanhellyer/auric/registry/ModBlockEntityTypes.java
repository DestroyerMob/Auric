package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.blockentity.CamouflagedBlockEntity;
import com.ethanhellyer.auric.blockentity.ImbuingTableBlockEntity;
import com.ethanhellyer.auric.blockentity.PotionCauldronBlockEntity;
import com.ethanhellyer.auric.blockentity.PotionCandleBlockEntity;
import com.ethanhellyer.auric.blockentity.SwordInStoneBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Auric.MOD_ID);

    public static final Supplier<BlockEntityType<ImbuingTableBlockEntity>> IMBUING_TABLE =
            BLOCK_ENTITY_TYPES.register(
                    ModBlocks.IMBUING_TABLE_ID,
                    () -> BlockEntityType.Builder.of(ImbuingTableBlockEntity::new, ModBlocks.IMBUING_TABLE.get()).build(null)
            );

    public static final Supplier<BlockEntityType<PotionCauldronBlockEntity>> POTION_CAULDRON =
            BLOCK_ENTITY_TYPES.register(
                    ModBlocks.POTION_CAULDRON_ID,
                    () -> BlockEntityType.Builder.of(PotionCauldronBlockEntity::new, ModBlocks.POTION_CAULDRON.get()).build(null)
            );

    public static final Supplier<BlockEntityType<PotionCandleBlockEntity>> POTION_CANDLE =
            BLOCK_ENTITY_TYPES.register(
                    ModBlocks.POTION_CANDLE_ID,
                    () -> BlockEntityType.Builder.of(PotionCandleBlockEntity::new, ModBlocks.POTION_CANDLE.get()).build(null)
            );

    public static final Supplier<BlockEntityType<CamouflagedBlockEntity>> CAMOUFLAGED_BLOCK =
            BLOCK_ENTITY_TYPES.register(
                    ModBlocks.CAMOUFLAGED_BLOCK_ID,
                    () -> BlockEntityType.Builder.of(CamouflagedBlockEntity::new, ModBlocks.CAMOUFLAGED_BLOCK.get()).build(null)
            );

    public static final Supplier<BlockEntityType<SwordInStoneBlockEntity>> SWORD_IN_STONE =
            BLOCK_ENTITY_TYPES.register(
                    ModBlocks.SWORD_IN_STONE_ID,
                    () -> BlockEntityType.Builder.of(SwordInStoneBlockEntity::new, ModBlocks.SWORD_IN_STONE.get()).build(null)
            );

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
