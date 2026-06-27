package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.camo.CamouflageModelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class CamoRenderData {
    private CamoRenderData() {
    }

    public static ModelData appendSkin(BlockAndTintGetter level, BlockPos pos, ModelData modelData) {
        BlockState disguise = ClientCamoSkins.get(level, pos);
        if (disguise == null || disguise.isAir()) {
            return modelData;
        }
        return modelData.derive()
                .with(CamouflageModelProperties.CAMO_SKIN_STATE, disguise)
                .build();
    }
}
