package com.ethanhellyer.auric.camo;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CamoNetworking {
    private static final String NETWORK_VERSION = "1";
    private static final String CLIENT_SKINS_CLASS = "com.ethanhellyer.auric.client.ClientCamoSkins";

    private CamoNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(CamoBulkPayload.TYPE, CamoBulkPayload.STREAM_CODEC, CamoNetworking::handleBulk);
        registrar.playToClient(CamoUpdatePayload.TYPE, CamoUpdatePayload.STREAM_CODEC, CamoNetworking::handleUpdate);
    }

    public static void sendFull(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            PositionCamoSavedData data = PositionCamoSavedData.get(level);
            PacketDistributor.sendToPlayer(player, new CamoBulkPayload(level.dimension(), data.entries(), true));
        }
    }

    public static void sendChunk(ServerPlayer player, ServerLevel level, ChunkPos chunkPos) {
        Map<Long, BlockState> entries = PositionCamoSavedData.get(level).entriesInChunk(chunkPos);
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new CamoBulkPayload(level.dimension(), entries, false));
        }
    }

    public static void sendUpdate(ServerLevel level, BlockPos pos, Optional<BlockState> disguise) {
        PacketDistributor.sendToPlayersInDimension(level, new CamoUpdatePayload(level.dimension(), pos.asLong(), disguise));
    }

    private static void handleBulk(CamoBulkPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            invokeClientHandler("handleBulk", CamoBulkPayload.class, payload);
        }
    }

    private static void handleUpdate(CamoUpdatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            invokeClientHandler("handleUpdate", CamoUpdatePayload.class, payload);
        }
    }

    private static void invokeClientHandler(String method, Class<?> payloadType, Object payload) {
        try {
            Class.forName(CLIENT_SKINS_CLASS).getMethod(method, payloadType).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to handle Auric camouflage payload on the client", exception);
        }
    }
}
