package com.ethanhellyer.auric.worldgen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ShrineSwordRewards {
    private static final ResourceLocation MTF_IRON = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "iron");
    private static final ResourceLocation MTF_DIAMOND = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "diamond");
    private static final ResourceLocation MTF_OAK = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "oak");
    private static final int MTF_DEFAULT_QUALITY = 100;
    private static final List<Candidate> CANDIDATES = List.of(
            candidate("mobstoolforging:sword", "minecraft:iron_sword", MTF_IRON, true, 8),
            candidate("mobstoolforging:sword", "minecraft:diamond_sword", MTF_DIAMOND, true, 2),
            candidate("mobsmoreweapons:katana", "mobsmoreweapons:iron_katana", MTF_IRON, false, 5),
            candidate("mobsmoreweapons:great_sword", "mobsmoreweapons:iron_great_sword", MTF_IRON, true, 4),
            candidate("mobsmoreweapons:machete", "mobsmoreweapons:iron_machete", MTF_IRON, false, 3),
            candidate("mobsmoreweapons:knife", "mobsmoreweapons:iron_knife", MTF_IRON, false, 2),
            candidate("mobsmoreweapons:katana", "mobsmoreweapons:diamond_katana", MTF_DIAMOND, false, 1),
            candidate("mobsmoreweapons:great_sword", "mobsmoreweapons:diamond_great_sword", MTF_DIAMOND, true, 1)
    );

    private ShrineSwordRewards() {
    }

    static ItemStack create(RandomSource random) {
        List<WeightedStack> available = new ArrayList<>();
        for (Candidate candidate : CANDIDATES) {
            ItemStack stack = candidate.create();
            if (!stack.isEmpty()) {
                available.add(new WeightedStack(stack, candidate.weight()));
            }
        }

        if (available.isEmpty()) {
            return new ItemStack(Items.IRON_SWORD);
        }

        int totalWeight = 0;
        for (WeightedStack choice : available) {
            totalWeight += choice.weight();
        }

        int roll = random.nextInt(totalWeight);
        for (WeightedStack choice : available) {
            roll -= choice.weight();
            if (roll < 0) {
                return choice.stack().copy();
            }
        }
        return available.getFirst().stack().copy();
    }

    private static Candidate candidate(String toolType, String fallbackItem, ResourceLocation material, boolean usesGuard, int weight) {
        return new Candidate(ResourceLocation.parse(toolType), ResourceLocation.parse(fallbackItem), material, usesGuard, weight);
    }

    private static Optional<ItemStack> createMtfTool(ResourceLocation toolType, ResourceLocation material, boolean usesGuard) {
        try {
            Class<?> registryClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeRegistry");
            Class<?> constructionClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolConstructionData");
            Method toolTypeMethod = registryClass.getMethod("toolType", ResourceLocation.class);
            Object optionalDefinition = toolTypeMethod.invoke(null, toolType);
            if (!(optionalDefinition instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }

            Constructor<?> constructor = constructionClass.getConstructor(
                    ResourceLocation.class,
                    ResourceLocation.class,
                    ResourceLocation.class,
                    Optional.class,
                    Optional.class,
                    Optional.class,
                    Optional.class,
                    int.class
            );
            Object construction = constructor.newInstance(
                    toolType,
                    material,
                    MTF_OAK,
                    usesGuard ? Optional.of(material) : Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    MTF_DEFAULT_QUALITY
            );
            Object created = optional.get().getClass().getMethod("createTool", constructionClass).invoke(optional.get(), construction);
            if (created instanceof ItemStack stack && !stack.isEmpty()) {
                return Optional.of(stack);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return Optional.empty();
    }

    private static ItemStack fallbackStack(ResourceLocation fallbackItem) {
        Item item = BuiltInRegistries.ITEM.get(fallbackItem);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private record Candidate(ResourceLocation toolType, ResourceLocation fallbackItem, ResourceLocation material, boolean usesGuard, int weight) {
        private ItemStack create() {
            return createMtfTool(toolType, material, usesGuard).orElseGet(() -> fallbackStack(fallbackItem));
        }
    }

    private record WeightedStack(ItemStack stack, int weight) {
    }
}
