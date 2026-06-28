package com.ethanhellyer.auric.blockentity;

import com.ethanhellyer.auric.block.SwordInStoneBlock;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SwordInStoneBlockEntity extends BlockEntity {
    private static final String SWORD_KEY = "Sword";

    private ItemStack sword = ItemStack.EMPTY;

    public SwordInStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SWORD_IN_STONE.get(), pos, state);
    }

    public ItemStack getSword() {
        return sword;
    }

    public boolean hasSword() {
        return !sword.isEmpty();
    }

    public void setSword(ItemStack stack) {
        this.sword = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        sync();
    }

    public ItemStack takeSword() {
        ItemStack result = sword.copy();
        this.sword = ItemStack.EMPTY;
        sync();
        return result;
    }

    public ItemStack removeSwordForDrop() {
        ItemStack result = sword.copy();
        this.sword = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            SwordInStoneBlock.updateSwordState(level, worldPosition, hasSword());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!sword.isEmpty()) {
            ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), sword)
                    .result()
                    .ifPresent(swordTag -> tag.put(SWORD_KEY, swordTag));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Tag swordTag = tag.get(SWORD_KEY);
        sword = swordTag == null
                ? ItemStack.EMPTY
                : ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), swordTag)
                .result()
                .orElse(ItemStack.EMPTY);
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
}
