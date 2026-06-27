package com.ethanhellyer.auric.blockentity;

import com.ethanhellyer.auric.camo.CamouflageModelProperties;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class CamouflagedBlockEntity extends BlockEntity {
    private static final String REAL_STATE_KEY = "RealState";
    private static final String DISGUISE_STATE_KEY = "DisguiseState";
    private static final String REAL_BLOCK_ENTITY_KEY = "RealBlockEntity";

    private BlockState realState = Blocks.STONE.defaultBlockState();
    private BlockState disguiseState = Blocks.STONE.defaultBlockState();
    @Nullable
    private CompoundTag realBlockEntityTag;

    public CamouflagedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CAMOUFLAGED_BLOCK.get(), pos, state);
    }

    public BlockState getRealState() {
        return realState;
    }

    public BlockState getDisguiseState() {
        return disguiseState;
    }

    @Nullable
    public CompoundTag getRealBlockEntityTag() {
        return realBlockEntityTag == null ? null : realBlockEntityTag.copy();
    }

    public void setStates(BlockState realState, BlockState disguiseState, @Nullable CompoundTag realBlockEntityTag) {
        this.realState = realState.is(ModBlocks.CAMOUFLAGED_BLOCK.get()) ? Blocks.STONE.defaultBlockState() : realState;
        this.disguiseState = disguiseState.is(ModBlocks.CAMOUFLAGED_BLOCK.get()) ? Blocks.STONE.defaultBlockState() : disguiseState;
        this.realBlockEntityTag = realBlockEntityTag == null ? null : realBlockEntityTag.copy();
        setChanged();
        refreshRenderData();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        refreshRenderData();
    }

    @Override
    public void onLoad() {
        refreshRenderData();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(CamouflageModelProperties.DISGUISE_STATE, disguiseState)
                .with(CamouflageModelProperties.REAL_STATE, realState)
                .build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        encodeState(realState).result().ifPresent(stateTag -> tag.put(REAL_STATE_KEY, stateTag));
        encodeState(disguiseState).result().ifPresent(stateTag -> tag.put(DISGUISE_STATE_KEY, stateTag));
        if (realBlockEntityTag != null) {
            tag.put(REAL_BLOCK_ENTITY_KEY, realBlockEntityTag.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(REAL_STATE_KEY)) {
            realState = decodeState(tag.get(REAL_STATE_KEY), realState);
        }
        if (tag.contains(DISGUISE_STATE_KEY)) {
            disguiseState = decodeState(tag.get(DISGUISE_STATE_KEY), disguiseState);
        }
        realBlockEntityTag = tag.contains(REAL_BLOCK_ENTITY_KEY, Tag.TAG_COMPOUND)
                ? tag.getCompound(REAL_BLOCK_ENTITY_KEY).copy()
                : null;
        refreshRenderData();
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private static DataResult<Tag> encodeState(BlockState state) {
        return BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state);
    }

    private static BlockState decodeState(Tag tag, BlockState fallback) {
        return BlockState.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(fallback);
    }

    private void refreshLight() {
        if (level != null) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    private void refreshRenderData() {
        refreshLight();
        requestModelDataUpdate();
    }
}
