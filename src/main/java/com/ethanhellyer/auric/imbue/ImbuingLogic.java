package com.ethanhellyer.auric.imbue;

import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public final class ImbuingLogic {
    private static final String MOBS_TOOL_FORGING_NAMESPACE = "mobstoolforging";
    private static final String MTF_TOOL_CONSTRUCTION_COMPONENT = "tool_construction";
    private static final String MTF_TOOL_PART_COMPONENT = "tool_part";
    private static final List<TagKey<Item>> MTF_IMBUABLE_PART_TAGS = List.of(
            mtfPartTag("sword_blade"),
            mtfPartTag("shovel_head"),
            mtfPartTag("pickaxe_head"),
            mtfPartTag("axe_head"),
            mtfPartTag("hoe_head"),
            mtfPartTag("screwdriver_head"),
            mtfPartTag("gem_cutters_blade")
    );

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
        if (isMtfFinishedTool(stack)) {
            return false;
        }
        if (isMtfToolPart(stack)) {
            return isMtfImbuableToolHead(stack);
        }
        return isStandardImbuableTarget(stack);
    }

    public static boolean isActiveImbueCarrier(ItemStack stack) {
        if (stack.isEmpty() || isMtfToolPart(stack)) {
            return false;
        }
        return isMtfFinishedTool(stack) || isStandardImbuableTarget(stack);
    }

    private static boolean isStandardImbuableTarget(ItemStack stack) {
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

    private static boolean isMtfImbuableToolHead(ItemStack stack) {
        for (TagKey<Item> tag : MTF_IMBUABLE_PART_TAGS) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMtfFinishedTool(ItemStack stack) {
        return hasMtfComponent(stack, MTF_TOOL_CONSTRUCTION_COMPONENT);
    }

    private static boolean isMtfToolPart(ItemStack stack) {
        return hasMtfComponent(stack, MTF_TOOL_PART_COMPONENT);
    }

    private static boolean hasMtfComponent(ItemStack stack, String componentPath) {
        for (DataComponentType<?> component : stack.getComponents().keySet()) {
            ResourceLocation componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);
            if (componentId != null
                    && MOBS_TOOL_FORGING_NAMESPACE.equals(componentId.getNamespace())
                    && componentPath.equals(componentId.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> mtfPartTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOBS_TOOL_FORGING_NAMESPACE, "parts/" + path));
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
        if (potionId == null) {
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
                || effect.isInstantenous()
                || isDisallowed(effectId)
                || ImbueRules.ruleForPotion(potionId, effectId).isEmpty()) {
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
        if (current == null || isDisallowed(current.effect()) || ImbueRules.ruleForEffect(current.effect()).isEmpty()) {
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

    public static boolean isHolderImbue(ImbueData data) {
        return ImbueRules.ruleForEffect(data.effect())
                .filter(rule -> rule.handling() == ImbueRules.Handling.HOLDER)
                .filter(rule -> !isDisallowed(rule.effect()))
                .isPresent();
    }

    public static boolean isTargetOnHitImbue(ImbueData data) {
        return ImbueRules.ruleForEffect(data.effect())
                .filter(rule -> rule.handling() == ImbueRules.Handling.TARGET_ON_HIT)
                .filter(rule -> !isDisallowed(rule.effect()))
                .isPresent();
    }

    public static int heldDurationTicks(ImbueData data) {
        return ImbueRules.ruleForEffect(data.effect())
                .map(ImbueRules.Rule::heldDurationTicks)
                .orElse(2);
    }

    public static int targetDurationTicks(ImbueData data) {
        return ImbueRules.ruleForEffect(data.effect())
                .map(ImbueRules.Rule::targetDurationTicks)
                .orElse(100);
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
