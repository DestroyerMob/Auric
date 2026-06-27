package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.camo.CamoBulkPayload;
import com.ethanhellyer.auric.camo.CamoUpdatePayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ClientCamoSkins {
    private static final Map<ResourceKey<Level>, Map<Long, BlockState>> SKINS = new HashMap<>();

    private ClientCamoSkins() {
    }

    public static void handleBulk(CamoBulkPayload payload) {
        if (payload.replaceAll()) {
            Set<Long> dirtyPositions = new HashSet<>(SKINS.getOrDefault(payload.dimension(), Map.of()).keySet());
            dirtyPositions.addAll(payload.entries().keySet());
            if (payload.entries().isEmpty()) {
                SKINS.remove(payload.dimension());
            } else {
                SKINS.put(payload.dimension(), new HashMap<>(payload.entries()));
            }
            markDirty(payload.dimension(), dirtyPositions);
            return;
        }

        SKINS.computeIfAbsent(payload.dimension(), unused -> new HashMap<>()).putAll(payload.entries());
        markDirty(payload.dimension(), payload.entries().keySet());
    }

    public static void handleUpdate(CamoUpdatePayload payload) {
        if (payload.disguise().isPresent()) {
            SKINS.computeIfAbsent(payload.dimension(), unused -> new HashMap<>()).put(payload.pos(), payload.disguise().get());
        } else {
            Map<Long, BlockState> dimensionSkins = SKINS.get(payload.dimension());
            if (dimensionSkins != null) {
                dimensionSkins.remove(payload.pos());
                if (dimensionSkins.isEmpty()) {
                    SKINS.remove(payload.dimension());
                }
            }
        }
        markDirty(payload.dimension(), Set.of(payload.pos()));
    }

    @Nullable
    public static BlockState get(BlockAndTintGetter level, BlockPos pos) {
        Level currentLevel = Minecraft.getInstance().level;
        if (currentLevel == null) {
            return null;
        }
        return get(currentLevel.dimension(), pos);
    }

    @Nullable
    public static BlockState get(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, BlockState> dimensionSkins = SKINS.get(dimension);
        return dimensionSkins == null ? null : dimensionSkins.get(pos.asLong());
    }

    private static void markDirty(ResourceKey<Level> dimension, Iterable<Long> positions) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(dimension) || minecraft.levelRenderer == null) {
            return;
        }

        for (long packedPos : positions) {
            BlockPos pos = BlockPos.of(packedPos);
            minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
