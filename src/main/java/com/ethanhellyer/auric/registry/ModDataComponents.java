package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.camo.CamoPasteData;
import com.ethanhellyer.auric.imbue.ImbueData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Auric.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImbueData>> IMBUE =
            DATA_COMPONENTS.registerComponentType("imbue", builder -> builder
                    .persistent(ImbueData.CODEC)
                    .networkSynchronized(ImbueData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CamoPasteData>> CAMO_SAMPLE =
            DATA_COMPONENTS.registerComponentType("camo_sample", builder -> builder
                    .persistent(CamoPasteData.CODEC)
                    .networkSynchronized(CamoPasteData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PotionContents>> POTION_CANDLE_CONTENTS =
            DATA_COMPONENTS.registerComponentType("potion_candle_contents", builder -> builder
                    .persistent(PotionContents.CODEC)
                    .networkSynchronized(PotionContents.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_EXPERIENCE =
            DATA_COMPONENTS.registerComponentType("stored_experience", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
