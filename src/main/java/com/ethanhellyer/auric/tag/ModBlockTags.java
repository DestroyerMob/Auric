package com.ethanhellyer.auric.tag;

import com.ethanhellyer.auric.Auric;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> CAULDRON_HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, Auric.id("cauldron_heat_sources"));

    private ModBlockTags() {
    }
}
