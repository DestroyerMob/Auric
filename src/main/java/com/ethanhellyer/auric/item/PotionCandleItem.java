package com.ethanhellyer.auric.item;

import com.ethanhellyer.auric.blockentity.PotionCandleBlockEntity;
import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PotionCandleItem extends BlockItem {
    private static final int STORED_EFFECT_DURATION = 1;

    public PotionCandleItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static PotionContents getContents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.POTION_CANDLE_CONTENTS.get(), PotionContents.EMPTY);
    }

    public static void setContents(ItemStack stack, PotionContents contents) {
        PotionContents normalized = normalizeAuraContents(contents);
        if (!normalized.hasEffects()) {
            stack.remove(ModDataComponents.POTION_CANDLE_CONTENTS.get());
        } else {
            stack.set(ModDataComponents.POTION_CANDLE_CONTENTS.get(), normalized);
        }
    }

    public static PotionContents normalizeAuraContents(PotionContents contents) {
        if (contents == null || !contents.hasEffects()) {
            return PotionContents.EMPTY;
        }

        List<MobEffectInstance> auraEffects = new ArrayList<>();
        contents.forEachEffect(effect -> {
            if (!effect.getEffect().value().isInstantenous()) {
                auraEffects.add(new MobEffectInstance(effect.getEffect(), STORED_EFFECT_DURATION, effect.getAmplifier(), true, true, true));
            }
        });
        return auraEffects.isEmpty() ? PotionContents.EMPTY : new PotionContents(java.util.Optional.empty(), contents.customColor(), auraEffects);
    }

    public static boolean hasAuraEffects(PotionContents contents) {
        if (contents == null || !contents.hasEffects()) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.getEffect().value().isInstantenous()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return canStackWithExisting(context.getLevel(), context.getClickedPos(), getContents(context.getItemInHand()))
                && super.canPlace(context, state);
    }

    private static boolean canStackWithExisting(Level level, BlockPos pos, PotionContents stackContents) {
        BlockState existingState = level.getBlockState(pos);
        if (!existingState.is(ModBlocks.POTION_CANDLE.get())) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof PotionCandleBlockEntity candle) {
            if (AuricConfig.ALLOW_MIXED_POTION_CANDLE_EFFECTS.get()) {
                return true;
            }
            PotionContents normalizedStackContents = normalizeAuraContents(stackContents);
            return candle.contentsList().stream()
                    .map(PotionCandleItem::normalizeAuraContents)
                    .allMatch(existingContents -> !hasAuraEffects(existingContents) || existingContents.equals(normalizedStackContents));
        }
        return !hasAuraEffects(stackContents);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasAuraEffects(getContents(stack)) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        PotionContents contents = getContents(stack);
        if (!hasAuraEffects(contents)) {
            tooltip.add(Component.translatable("tooltip.auric.potion_candle_empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.getEffect().value().isInstantenous()) {
                tooltip.add(Component.translatable("tooltip.auric.potion_candle_effect", effectName(effect)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    private static Component effectName(MobEffectInstance effect) {
        MutableComponent name = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0) {
            name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + effect.getAmplifier()));
        }
        return name;
    }
}
