package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.crafting.PotionCandleRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final String POTION_CANDLE_ID = "crafting_special_potion_candle";

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Auric.MOD_ID);

    public static final Supplier<RecipeSerializer<PotionCandleRecipe>> POTION_CANDLE =
            RECIPE_SERIALIZERS.register(POTION_CANDLE_ID, () -> new SimpleCraftingRecipeSerializer<>(PotionCandleRecipe::new));

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modBus) {
        RECIPE_SERIALIZERS.register(modBus);
    }
}
