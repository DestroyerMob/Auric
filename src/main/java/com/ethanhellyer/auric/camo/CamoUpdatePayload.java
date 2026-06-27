package com.ethanhellyer.auric.camo;

import com.ethanhellyer.auric.Auric;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record CamoUpdatePayload(ResourceKey<Level> dimension, long pos, Optional<BlockState> disguise) implements CustomPacketPayload {
    public static final Type<CamoUpdatePayload> TYPE = new Type<>(Auric.id("camo_update"));
    private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_CODEC =
            ByteBufCodecs.fromCodecWithRegistriesTrusted(BlockState.CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, CamoUpdatePayload> STREAM_CODEC =
            CustomPacketPayload.codec(CamoUpdatePayload::write, CamoUpdatePayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceKey(dimension);
        buffer.writeLong(pos);
        buffer.writeBoolean(disguise.isPresent());
        disguise.ifPresent(state -> BLOCK_STATE_CODEC.encode(buffer, state));
    }

    private static CamoUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
        long pos = buffer.readLong();
        Optional<BlockState> disguise = buffer.readBoolean()
                ? Optional.of(BLOCK_STATE_CODEC.decode(buffer))
                : Optional.empty();
        return new CamoUpdatePayload(dimension, pos, disguise);
    }
}
