package com.ethanhellyer.auric.camo;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class CamouflageModelProperties {
    public static final ModelProperty<BlockState> DISGUISE_STATE = new ModelProperty<>(state -> state != null);
    public static final ModelProperty<BlockState> REAL_STATE = new ModelProperty<>(state -> state != null);
    public static final ModelProperty<BlockState> CAMO_SKIN_STATE = new ModelProperty<>(state -> state != null);

    private CamouflageModelProperties() {
    }
}
