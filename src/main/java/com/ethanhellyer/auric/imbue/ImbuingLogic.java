package com.ethanhellyer.auric.imbue;

import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public final class ImbuingLogic {
    private static final String VANILLA_NAMESPACE = "minecraft";

    private ImbuingLogic() {
    }

    public static ItemStack createResult(ItemStack target, ItemStack potion) {
        if (!isEligibleTarget(target)) {
            return ItemStack.EMPTY;
        }
        if (isUpgradeCatalyst(potion)) {
            return createUpgradeResult(target);
        }

        Optional<ImbueData> imbue = imbueFromPotion(potion);
        if (target.has(ModDataComponents.IMBUE.get()) || imbue.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = target.copyWithCount(1);
        result.set(ModDataComponents.IMBUE.get(), imbue.get());
        return result;
    }

    public static boolean isEligibleTarget(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem) {
            return true;
        }
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.BOW_ENCHANTABLE)
                || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
                || stack.is(ItemTags.TRIDENT_ENCHANTABLE)
                || stack.is(ItemTags.MACE_ENCHANTABLE);
    }

    public static boolean isPotionCandidate(ItemStack stack) {
        return stack.is(Items.POTION) || isUpgradeCatalyst(stack);
    }

    public static boolean isUpgradeCatalyst(ItemStack stack) {
        return stack.is(Items.DRAGON_BREATH);
    }

    public static Optional<ImbueData> imbueFromPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return Optional.empty();
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Optional<Holder<Potion>> potion = contents.potion();
        if (potion.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion.get().value());
        if (potionId == null || !VANILLA_NAMESPACE.equals(potionId.getNamespace())) {
            return Optional.empty();
        }

        List<MobEffectInstance> effects = new ArrayList<>();
        contents.forEachEffect(effects::add);
        if (effects.size() != 1) {
            return Optional.empty();
        }

        MobEffectInstance effectInstance = effects.getFirst();
        Holder<MobEffect> effectHolder = effectInstance.getEffect();
        MobEffect effect = effectHolder.value();
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (effectId == null
                || !VANILLA_NAMESPACE.equals(effectId.getNamespace())
                || effect.getCategory() != MobEffectCategory.BENEFICIAL
                || effect.isInstantenous()
                || isDisallowed(effectId)) {
            return Optional.empty();
        }

        int maxAmplifier = maxVanillaPotionAmplifier(effectHolder);
        if (effectInstance.getAmplifier() != maxAmplifier) {
            return Optional.empty();
        }

        return Optional.of(new ImbueData(effectId, effectInstance.getAmplifier() + 1));
    }

    public static int scaledAmplifier(ImbueData data) {
        double scale = AuricConfig.IMBUE_STRENGTH_SCALE.get();
        if (scale <= 0.0D) {
            return -1;
        }
        int scaledLevel = Math.max(1, (int) Math.floor(data.sourceLevel() * scale));
        return Math.min(MobEffectInstance.MAX_AMPLIFIER, scaledLevel - 1);
    }

    private static ItemStack createUpgradeResult(ItemStack target) {
        ImbueData current = target.get(ModDataComponents.IMBUE.get());
        if (current == null || isDisallowed(current.effect())) {
            return ItemStack.EMPTY;
        }

        Optional<ImbueData> upgraded = upgradedByDragonBreath(current);
        if (upgraded.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = target.copyWithCount(1);
        result.set(ModDataComponents.IMBUE.get(), upgraded.get());
        return result;
    }

    private static Optional<ImbueData> upgradedByDragonBreath(ImbueData current) {
        if (current.overleveled()) {
            return Optional.empty();
        }

        int currentAmplifier = scaledAmplifier(current);
        if (currentAmplifier < 0) {
            return Optional.empty();
        }

        int currentDisplayedLevel = currentAmplifier + 1;
        int maxDisplayedLevel = Math.min(AuricConfig.DRAGON_BREATH_MAX_IMBUE_LEVEL.get(), MobEffectInstance.MAX_AMPLIFIER + 1);
        if (currentDisplayedLevel >= maxDisplayedLevel) {
            return Optional.empty();
        }

        int targetDisplayedLevel = Math.min(currentDisplayedLevel + 1, maxDisplayedLevel);
        int sourceLevel = Math.max(current.sourceLevel() + 1, sourceLevelForDisplayedLevel(targetDisplayedLevel));
        return Optional.of(new ImbueData(current.effect(), sourceLevel, true));
    }

    private static int sourceLevelForDisplayedLevel(int displayedLevel) {
        double scale = AuricConfig.IMBUE_STRENGTH_SCALE.get();
        if (scale <= 0.0D) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.ceil(displayedLevel / scale));
    }

    public static MutableComponent tooltipName(ImbueData data) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(data.effect());
        Component effectName = effect == null ? Component.literal(data.effect().toString()) : effect.getDisplayName();
        int amplifier = scaledAmplifier(data);
        if (amplifier < 0) {
            return Component.translatable("tooltip.auric.imbued_inactive", effectName);
        }
        return Component.translatable("tooltip.auric.imbued", effectName, roman(amplifier + 1));
    }

    private static int maxVanillaPotionAmplifier(Holder<MobEffect> effect) {
        int max = 0;
        for (Potion potion : BuiltInRegistries.POTION) {
            ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
            if (potionId == null || !VANILLA_NAMESPACE.equals(potionId.getNamespace())) {
                continue;
            }
            for (MobEffectInstance instance : potion.getEffects()) {
                if (instance.is(effect)) {
                    max = Math.max(max, instance.getAmplifier());
                }
            }
        }
        return max;
    }

    private static boolean isDisallowed(ResourceLocation effectId) {
        String value = effectId.toString();
        for (String configured : AuricConfig.DISALLOWED_EFFECTS.get()) {
            if (value.equals(configured)) {
                return true;
            }
        }
        return false;
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
    }
}
