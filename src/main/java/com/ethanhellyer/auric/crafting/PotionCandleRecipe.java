package com.ethanhellyer.auric.crafting;

import com.ethanhellyer.auric.item.PotionCandleItem;
import com.ethanhellyer.auric.registry.ModItems;
import com.ethanhellyer.auric.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PotionCandleRecipe extends CustomRecipe {
    public PotionCandleRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.POTION_CANDLE.get());
        PotionCandleItem.setContents(result, inputs.contents());
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(Items.POTION)) {
                remaining.set(slot, new ItemStack(Items.GLASS_BOTTLE));
            } else if (stack.getItem().hasCraftingRemainingItem()) {
                remaining.set(slot, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.POTION_CANDLE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.POTION_CANDLE.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        boolean hasString = false;
        boolean hasHoneycomb = false;
        PotionContents contents = PotionContents.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.STRING) && !hasString) {
                hasString = true;
                continue;
            }
            if (stack.is(Items.HONEYCOMB) && !hasHoneycomb) {
                hasHoneycomb = true;
                continue;
            }
            if (stack.is(Items.POTION) && !contents.hasEffects()) {
                PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                if (PotionCandleItem.hasAuraEffects(potionContents)) {
                    contents = potionContents;
                    continue;
                }
            }
            return null;
        }

        return hasString && hasHoneycomb && PotionCandleItem.hasAuraEffects(contents) ? new Inputs(contents) : null;
    }

    private record Inputs(PotionContents contents) {
    }
}
