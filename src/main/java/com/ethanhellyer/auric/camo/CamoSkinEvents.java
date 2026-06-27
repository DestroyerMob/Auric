package com.ethanhellyer.auric.camo;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

public class CamoSkinEvents {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CamoNetworking.sendFull(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CamoNetworking.sendFull(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CamoNetworking.sendFull(player);
        }
    }

    @SubscribeEvent
    public void onChunkSent(ChunkWatchEvent.Sent event) {
        CamoNetworking.sendChunk(event.getPlayer(), event.getLevel(), event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled()) {
            clear(event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()) {
            clear(event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockMultiPlaced(BlockEvent.EntityMultiPlaceEvent event) {
        if (!event.isCanceled()) {
            event.getReplacedBlockSnapshots().forEach(snapshot -> clear(snapshot.getLevel(), snapshot.getPos()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isCanceled() && !event.isSimulated() && !event.getFinalState().equals(event.getState())) {
            clear(event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!event.isCanceled() && !event.getNewState().equals(event.getOriginalState())) {
            clear(event.getLevel(), event.getPos());
        }
    }

    private static void clear(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PositionCamoSavedData data = PositionCamoSavedData.get(serverLevel);
        for (BlockPos linkedPos : CamoLinkedBlocks.linkedPositions(serverLevel, pos)) {
            if (data.remove(linkedPos)) {
                CamoNetworking.sendUpdate(serverLevel, linkedPos, Optional.empty());
                BlockState state = serverLevel.getBlockState(linkedPos);
                serverLevel.sendBlockUpdated(linkedPos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }
}
