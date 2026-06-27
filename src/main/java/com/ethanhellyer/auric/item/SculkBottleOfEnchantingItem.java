package com.ethanhellyer.auric.item;

import com.ethanhellyer.auric.entity.ThrownSculkExperienceBottle;
import com.ethanhellyer.auric.registry.ModDataComponents;
import com.ethanhellyer.auric.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SculkBottleOfEnchantingItem extends Item implements ProjectileItem {
    public SculkBottleOfEnchantingItem(Properties properties) {
        super(properties);
    }

    public static ItemStack withExperience(int experience) {
        ItemStack stack = new ItemStack(ModItems.SCULK_BOTTLE_OF_ENCHANTING.get());
        stack.set(ModDataComponents.STORED_EXPERIENCE.get(), Math.max(0, experience));
        return stack;
    }

    public static int getStoredExperience(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(ModDataComponents.STORED_EXPERIENCE.get(), 0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        if (!level.isClientSide) {
            ThrownSculkExperienceBottle bottle = new ThrownSculkExperienceBottle(level, player);
            bottle.setItem(stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
            level.addFreshEntity(bottle);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ThrownSculkExperienceBottle bottle = new ThrownSculkExperienceBottle(level, position.x(), position.y(), position.z());
        bottle.setItem(stack);
        return bottle;
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder()
                .uncertainty(ProjectileItem.DispenseConfig.DEFAULT.uncertainty() * 0.5F)
                .power(ProjectileItem.DispenseConfig.DEFAULT.power() * 1.25F)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.auric.sculk_bottle_stored_xp", getStoredExperience(stack)).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getStoredExperience(stack) > 0 || super.isFoil(stack);
    }
}
