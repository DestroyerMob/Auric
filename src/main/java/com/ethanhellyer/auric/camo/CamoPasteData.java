package com.ethanhellyer.auric.camo;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

public record CamoPasteData(BlockState disguise) {
    public static final Codec<CamoPasteData> CODEC = BlockState.CODEC.xmap(CamoPasteData::new, CamoPasteData::disguise);
    public static final StreamCodec<RegistryFriendlyByteBuf, CamoPasteData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistriesTrusted(BlockState.CODEC).map(CamoPasteData::new, CamoPasteData::disguise);
}
