package com.ethanhellyer.auric.blockentity;

import com.ethanhellyer.auric.item.PotionCandleItem;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PotionCandleBlockEntity extends BlockEntity {
    private static final String CONTENTS_KEY = "PotionContents";
    private static final String CONTENTS_LIST_KEY = "PotionContentsList";
    private static final int TICK_INTERVAL = 20;
    private static final int EFFECT_DURATION = 40;
    private static final double BASE_RADIUS = 4.0D;
    private static final double RADIUS_PER_EXTRA_CANDLE = 2.0D;

    private final List<PotionContents> contents = new ArrayList<>();

    public PotionCandleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.POTION_CANDLE.get(), pos, state);
    }

    public List<PotionContents> contentsList() {
        return List.copyOf(contents);
    }

    public PotionContents firstContents() {
        return contents.isEmpty() ? PotionContents.EMPTY : contents.getFirst();
    }

    public void setContents(PotionContents contents) {
        this.contents.clear();
        PotionContents normalized = PotionCandleItem.normalizeAuraContents(contents);
        if (normalized.hasEffects()) {
            this.contents.add(normalized);
        }
        sync();
    }

    public void addPlacedCandle(PotionContents contents, int candleCount) {
        PotionContents normalized = PotionCandleItem.normalizeAuraContents(contents);
        int targetCount = Math.clamp(candleCount, 1, CandleBlock.MAX_CANDLES);

        if (targetCount <= 1 || this.contents.isEmpty()) {
            this.contents.clear();
            if (normalized.hasEffects()) {
                this.contents.add(normalized);
            }
            sync();
            return;
        }

        while (this.contents.size() > targetCount - 1) {
            this.contents.removeLast();
        }
        if (normalized.hasEffects()) {
            this.contents.add(normalized);
        }
        while (!this.contents.isEmpty() && this.contents.size() < targetCount) {
            this.contents.add(normalized);
        }
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotionCandleBlockEntity candle) {
        if (level.getGameTime() % TICK_INTERVAL != 0 || !state.getValue(CandleBlock.LIT) || candle.contents.isEmpty()) {
            return;
        }

        int candles = state.getValue(CandleBlock.CANDLES);
        double radius = BASE_RADIUS + Math.max(0, candles - 1) * RADIUS_PER_EXTRA_CANDLE;
        double radiusSqr = radius * radius;
        Vec3 center = Vec3.atCenterOf(pos);
        AABB area = new AABB(pos).inflate(radius);

        for (Player player : level.getEntities(EntityTypeTest.forClass(Player.class), area, player -> !player.isSpectator())) {
            if (player.distanceToSqr(center) > radiusSqr) {
                continue;
            }
            for (PotionContents contents : candle.contents) {
                contents.forEachEffect(effect -> applyAuraEffect(player, effect));
            }
        }
    }

    private static void applyAuraEffect(Player player, MobEffectInstance source) {
        if (source.getEffect().value().isInstantenous()) {
            return;
        }
        player.addEffect(new MobEffectInstance(source.getEffect(), EFFECT_DURATION, source.getAmplifier(), true, false, true));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!contents.isEmpty()) {
            ListTag contentsList = new ListTag();
            for (PotionContents content : contents) {
                PotionContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), content)
                        .result()
                        .ifPresent(contentsList::add);
            }
            if (!contentsList.isEmpty()) {
                tag.put(CONTENTS_LIST_KEY, contentsList);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        if (tag.contains(CONTENTS_LIST_KEY, Tag.TAG_LIST)) {
            ListTag contentsList = tag.getList(CONTENTS_LIST_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < contentsList.size(); i++) {
                readContents(contentsList.get(i), registries);
            }
            return;
        }

        Tag contentsTag = tag.get(CONTENTS_KEY);
        if (contentsTag != null) {
            readContents(contentsTag, registries);
        }
    }

    private void readContents(Tag contentsTag, HolderLookup.Provider registries) {
        PotionContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), contentsTag)
                .result()
                .map(PotionCandleItem::normalizeAuraContents)
                .filter(PotionContents::hasEffects)
                .ifPresent(contents::add);
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
