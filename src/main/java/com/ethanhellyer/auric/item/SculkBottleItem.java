package com.ethanhellyer.auric.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SculkBottleItem extends Item {
    public static final int EXPERIENCE_PER_BOTTLE = 10;

    public SculkBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!player.getAbilities().instabuild && player.totalExperience < EXPERIENCE_PER_BOTTLE) {
            player.displayClientMessage(Component.translatable("message.auric.sculk_bottle_needs_xp"), true);
            return InteractionResultHolder.fail(stack);
        }

        int storedExperience = player.getAbilities().instabuild ? EXPERIENCE_PER_BOTTLE : removeExperience(player, EXPERIENCE_PER_BOTTLE);
        if (storedExperience != EXPERIENCE_PER_BOTTLE) {
            if (storedExperience > 0 && !player.getAbilities().instabuild) {
                player.giveExperiencePoints(storedExperience);
            }
            player.displayClientMessage(Component.translatable("message.auric.sculk_bottle_needs_xp"), true);
            return InteractionResultHolder.fail(stack);
        }

        ItemStack filled = SculkBottleOfEnchantingItem.withExperience(storedExperience);
        ItemStack result = ItemUtils.createFilledResult(stack, player, filled);
        player.setItemInHand(hand, result);
        player.awardStat(Stats.ITEM_USED.get(this));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.PLAYERS, 0.8F, 1.2F);
        return InteractionResultHolder.sidedSuccess(result, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.auric.sculk_bottle", EXPERIENCE_PER_BOTTLE).withStyle(ChatFormatting.GRAY));
    }

    private static int removeExperience(Player player, int amount) {
        int before = player.totalExperience;
        player.giveExperiencePoints(-amount);
        return Math.max(0, before - player.totalExperience);
    }
}
