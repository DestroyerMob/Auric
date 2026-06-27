package com.ethanhellyer.auric.camo;

import com.ethanhellyer.auric.Auric;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class PositionCamoSavedData extends SavedData {
    private static final String DATA_NAME = Auric.MOD_ID + "_position_camo";
    private static final String ENTRIES_KEY = "Entries";
    private static final String POS_KEY = "Pos";
    private static final String DISGUISE_KEY = "Disguise";
    private static final Factory<PositionCamoSavedData> FACTORY = new Factory<>(
            PositionCamoSavedData::new,
            PositionCamoSavedData::load
    );

    private final Map<Long, BlockState> disguises = new HashMap<>();

    public static PositionCamoSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Nullable
    public BlockState disguiseAt(BlockPos pos) {
        return disguises.get(pos.asLong());
    }

    public boolean has(BlockPos pos) {
        return disguises.containsKey(pos.asLong());
    }

    public void set(BlockPos pos, BlockState disguise) {
        if (!canStore(disguise)) {
            remove(pos);
            return;
        }

        disguises.put(pos.asLong(), disguise);
        setDirty();
    }

    public boolean remove(BlockPos pos) {
        if (disguises.remove(pos.asLong()) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public Map<Long, BlockState> entries() {
        return Map.copyOf(disguises);
    }

    public Map<Long, BlockState> entriesInChunk(ChunkPos chunkPos) {
        Map<Long, BlockState> entries = new HashMap<>();
        for (Map.Entry<Long, BlockState> entry : disguises.entrySet()) {
            long packedPos = entry.getKey();
            if (BlockPos.getX(packedPos) >> 4 == chunkPos.x && BlockPos.getZ(packedPos) >> 4 == chunkPos.z) {
                entries.put(packedPos, entry.getValue());
            }
        }
        return entries;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<Long, BlockState> entry : disguises.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong(POS_KEY, entry.getKey());
            entryTag.put(DISGUISE_KEY, NbtUtils.writeBlockState(entry.getValue()));
            entries.add(entryTag);
        }
        tag.put(ENTRIES_KEY, entries);
        return tag;
    }

    private static PositionCamoSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PositionCamoSavedData data = new PositionCamoSavedData();
        ListTag entries = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        HolderLookup.RegistryLookup<net.minecraft.world.level.block.Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            BlockState disguise = NbtUtils.readBlockState(blocks, entryTag.getCompound(DISGUISE_KEY));
            if (canStore(disguise)) {
                data.disguises.put(entryTag.getLong(POS_KEY), disguise);
            }
        }
        return data;
    }

    private static boolean canStore(BlockState state) {
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL;
    }
}
