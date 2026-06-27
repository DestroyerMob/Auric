package com.ethanhellyer.auric.imbue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ImbueData(ResourceLocation effect, int sourceLevel, boolean overleveled) {
    public ImbueData(ResourceLocation effect, int sourceLevel) {
        this(effect, sourceLevel, false);
    }

    public static final Codec<ImbueData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ImbueData::effect),
            Codec.INT.fieldOf("source_level").forGetter(ImbueData::sourceLevel),
            Codec.BOOL.optionalFieldOf("overleveled", false).forGetter(ImbueData::overleveled)
    ).apply(instance, ImbueData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImbueData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ImbueData::effect,
            ByteBufCodecs.VAR_INT,
            ImbueData::sourceLevel,
            ByteBufCodecs.BOOL,
            ImbueData::overleveled,
            ImbueData::new
    );
}
