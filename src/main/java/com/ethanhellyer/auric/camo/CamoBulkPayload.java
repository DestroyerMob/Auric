package com.ethanhellyer.auric.camo;

import com.ethanhellyer.auric.Auric;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record CamoBulkPayload(ResourceKey<Level> dimension, Map<Long, BlockState> entries, boolean replaceAll) implements CustomPacketPayload {
    public static final Type<CamoBulkPayload> TYPE = new Type<>(Auric.id("camo_bulk"));
    private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_CODEC =
            ByteBufCodecs.fromCodecWithRegistriesTrusted(BlockState.CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, CamoBulkPayload> STREAM_CODEC =
            CustomPacketPayload.codec(CamoBulkPayload::write, CamoBulkPayload::read);

    public CamoBulkPayload {
        entries = Map.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceKey(dimension);
        buffer.writeBoolean(replaceAll);
        buffer.writeVarInt(entries.size());
        for (Map.Entry<Long, BlockState> entry : entries.entrySet()) {
            buffer.writeLong(entry.getKey());
            BLOCK_STATE_CODEC.encode(buffer, entry.getValue());
        }
    }

    private static CamoBulkPayload read(RegistryFriendlyByteBuf buffer) {
        ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
        boolean replaceAll = buffer.readBoolean();
        int size = buffer.readVarInt();
        Map<Long, BlockState> entries = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            entries.put(buffer.readLong(), BLOCK_STATE_CODEC.decode(buffer));
        }
        return new CamoBulkPayload(dimension, entries, replaceAll);
    }
}
