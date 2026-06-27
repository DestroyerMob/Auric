package com.ethanhellyer.auric.blockentity;

import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PotionCauldronBlockEntity extends BlockEntity {
    private static final String POTION_KEY = "Potion";

    private Holder<Potion> potion = Potions.WATER;

    public PotionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.POTION_CAULDRON.get(), pos, state);
    }

    public Holder<Potion> getPotion() {
        return potion;
    }

    public int getPotionColor() {
        return PotionContents.getColor(potion);
    }

    public void setPotion(Holder<Potion> potion) {
        this.potion = potion;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion.value());
        if (potionId != null) {
            tag.putString(POTION_KEY, potionId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Holder<Potion> previousPotion = potion;
        ResourceLocation potionId = ResourceLocation.tryParse(tag.getString(POTION_KEY));
        if (potionId != null) {
            BuiltInRegistries.POTION.getHolder(potionId).ifPresent(holder -> potion = holder);
        }
        if (level != null && level.isClientSide && previousPotion != potion) {
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
}
