package com.ethanhellyer.auric.cauldron;

import com.ethanhellyer.auric.Auric;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.saveddata.SavedData;

public class CauldronBrewingData extends SavedData {
    private static final String DATA_NAME = Auric.MOD_ID + "_cauldron_brewing";
    private static final String CAULDRONS_KEY = "Cauldrons";
    private static final String POS_KEY = "Pos";
    private static final String POTION_KEY = "Potion";
    private static final Factory<CauldronBrewingData> FACTORY = new Factory<>(
            CauldronBrewingData::new,
            CauldronBrewingData::load
    );

    private final Map<Long, ResourceLocation> potions = new HashMap<>();

    public static CauldronBrewingData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<Holder<Potion>> potionAt(BlockPos pos) {
        ResourceLocation id = potions.get(pos.asLong());
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.POTION.getHolder(id).map(holder -> holder);
    }

    public boolean hasPotion(BlockPos pos) {
        return potions.containsKey(pos.asLong());
    }

    public void setPotion(BlockPos pos, Holder<Potion> potion) {
        ResourceLocation id = BuiltInRegistries.POTION.getKey(potion.value());
        if (id != null) {
            potions.put(pos.asLong(), id);
            setDirty();
        }
    }

    public void clear(BlockPos pos) {
        if (potions.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag cauldrons = new ListTag();
        for (Map.Entry<Long, ResourceLocation> entry : potions.entrySet()) {
            CompoundTag cauldron = new CompoundTag();
            cauldron.putLong(POS_KEY, entry.getKey());
            cauldron.putString(POTION_KEY, entry.getValue().toString());
            cauldrons.add(cauldron);
        }
        tag.put(CAULDRONS_KEY, cauldrons);
        return tag;
    }

    private static CauldronBrewingData load(CompoundTag tag, HolderLookup.Provider registries) {
        CauldronBrewingData data = new CauldronBrewingData();
        ListTag cauldrons = tag.getList(CAULDRONS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < cauldrons.size(); i++) {
            CompoundTag cauldron = cauldrons.getCompound(i);
            ResourceLocation potion = ResourceLocation.tryParse(cauldron.getString(POTION_KEY));
            if (potion != null && BuiltInRegistries.POTION.containsKey(potion)) {
                data.potions.put(cauldron.getLong(POS_KEY), potion);
            }
        }
        return data;
    }
}
