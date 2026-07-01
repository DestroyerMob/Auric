package com.ethanhellyer.auric.worldgen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.WorldGenLevel;

final class ShrineSwordRewards {
    private static final int MAGIC_SWORD_CHANCE = 10;
    private static final int BLEED_ON_MAGIC_SWORD_CHANCE = 4;
    private static final int EXTRA_MAGIC_ENCHANTMENT_CHANCE = 3;
    private static final ResourceLocation MTF_IRON = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "iron");
    private static final ResourceLocation MTF_DIAMOND = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "diamond");
    private static final ResourceLocation MTF_OAK = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "oak");
    private static final ResourceLocation BETTER_ENCHANTING_BLEED = ResourceLocation.fromNamespaceAndPath("betterenchanting", "bleed");
    private static final int MTF_DEFAULT_QUALITY = 100;
    private static final Set<String> MAGIC_BLADE_PART_TYPES = Set.of(
            "sword_blade",
            "great_sword_blade",
            "katana_blade",
            "knife_blade",
            "machete_blade"
    );
    private static final List<MagicEnchantment> MAGIC_ENCHANTMENTS = List.of(
            magic("minecraft:sharpness", 3, 8),
            magic("minecraft:knockback", 2, 3),
            magic("minecraft:fire_aspect", 2, 3),
            magic("minecraft:looting", 2, 3),
            magic("minecraft:smite", 3, 2),
            magic("minecraft:bane_of_arthropods", 3, 2),
            magic("betterenchanting:shocking", 1, 2),
            magic("betterenchanting:frostbite", 2, 2),
            magic("betterenchanting:beheading", 1, 1),
            magic("betterenchanting:perfect_strike", 1, 1)
    );
    private static final MagicEnchantment BLEED_MAGIC_ENCHANTMENT = magic("betterenchanting:bleed", 3, 1);
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

    static ItemStack create(WorldGenLevel level, RandomSource random) {
        if (random.nextInt(MAGIC_SWORD_CHANCE) == 0) {
            Optional<ItemStack> magicSword = createMagicMtfSword(level.registryAccess(), random);
            if (magicSword.isPresent()) {
                return magicSword.get();
            }
        }
        return create(random);
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

    private static MagicEnchantment magic(String enchantment, int maxGeneratedLevel, int weight) {
        return new MagicEnchantment(ResourceLocation.parse(enchantment), maxGeneratedLevel, weight);
    }

    private static Optional<ItemStack> createMagicMtfSword(RegistryAccess registryAccess, RandomSource random) {
        try {
            Optional<MagicCandidate> candidate = pickMagicCandidate(registryAccess, random);
            if (candidate.isEmpty()) {
                return Optional.empty();
            }
            return createMagicMtfTool(candidate.get(), registryAccess, random);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<MagicCandidate> pickMagicCandidate(RegistryAccess registryAccess, RandomSource random) throws ReflectiveOperationException {
        List<MagicCandidate> available = new ArrayList<>();
        for (Candidate candidate : CANDIDATES) {
            Optional<Object> definition = toolDefinition(candidate.toolType());
            if (definition.isEmpty()) {
                continue;
            }
            String primaryPartType = primaryPartType(definition.get());
            if (MAGIC_BLADE_PART_TYPES.contains(primaryPartType)) {
                available.add(new MagicCandidate(candidate, definition.get(), primaryPartType));
            }
        }
        if (available.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = 0;
        for (MagicCandidate candidate : available) {
            totalWeight += candidate.weight();
        }
        int roll = random.nextInt(totalWeight);
        for (MagicCandidate candidate : available) {
            roll -= candidate.weight();
            if (roll < 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(available.getFirst());
    }

    private static Optional<ItemStack> createMagicMtfTool(MagicCandidate magicCandidate, RegistryAccess registryAccess, RandomSource random) throws ReflectiveOperationException {
        Candidate candidate = magicCandidate.candidate();
        ItemStack primaryPart = createMtfPart(magicCandidate.definition(), magicCandidate.primaryPartType(), candidate.material());
        if (primaryPart.isEmpty() || !enchantMagicPart(primaryPart, registryAccess, random)) {
            return Optional.empty();
        }

        List<ItemStack> parts = new ArrayList<>();
        parts.add(primaryPart);
        for (String requiredPartType : requiredAssemblyParts(magicCandidate.definition())) {
            ItemStack requiredPart = createMtfPart(magicCandidate.definition(), requiredPartType, candidate.material());
            if (requiredPart.isEmpty()) {
                return Optional.empty();
            }
            parts.add(requiredPart);
        }
        parts.add(new ItemStack(Items.STICK));

        ItemStack tool = createMtfToolFromDefinition(magicCandidate.definition(), candidate.toolType(), candidate.material(), candidate.usesGuard());
        if (tool.isEmpty()) {
            return Optional.empty();
        }
        if (!mergeMtfPartEnchantments(tool, parts, registryAccess)) {
            return Optional.empty();
        }
        setMtfAssemblyParts(tool, parts);
        return Optional.of(tool);
    }

    private static boolean enchantMagicPart(ItemStack part, RegistryAccess registryAccess, RandomSource random) {
        Registry<Enchantment> enchantments = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        List<ResolvedMagicEnchantment> selected = selectMagicEnchantments(enchantments, random);
        if (selected.isEmpty()) {
            return false;
        }

        EnchantmentHelper.updateEnchantments(part, mutable -> selected.forEach(enchantment -> mutable.set(
                enchantment.holder(),
                enchantment.level(random)
        )));
        return true;
    }

    private static List<ResolvedMagicEnchantment> selectMagicEnchantments(Registry<Enchantment> enchantments, RandomSource random) {
        List<ResolvedMagicEnchantment> pool = resolveMagicEnchantments(enchantments, MAGIC_ENCHANTMENTS);
        List<ResolvedMagicEnchantment> selected = new ArrayList<>();
        pickMagicEnchantment(pool, selected, random).ifPresent(selected::add);
        if (random.nextInt(EXTRA_MAGIC_ENCHANTMENT_CHANCE) == 0) {
            pickMagicEnchantment(pool, selected, random).ifPresent(selected::add);
        }

        Optional<ResolvedMagicEnchantment> bleed = resolveMagicEnchantment(enchantments, BLEED_MAGIC_ENCHANTMENT);
        if (bleed.isPresent()
                && random.nextInt(BLEED_ON_MAGIC_SWORD_CHANCE) == 0
                && compatibleWithAll(bleed.get().holder(), selected)) {
            selected.add(bleed.get());
        }
        return List.copyOf(selected);
    }

    private static List<ResolvedMagicEnchantment> resolveMagicEnchantments(Registry<Enchantment> enchantments, List<MagicEnchantment> magicEnchantments) {
        List<ResolvedMagicEnchantment> resolved = new ArrayList<>();
        for (MagicEnchantment magicEnchantment : magicEnchantments) {
            resolveMagicEnchantment(enchantments, magicEnchantment).ifPresent(resolved::add);
        }
        return List.copyOf(resolved);
    }

    private static Optional<ResolvedMagicEnchantment> resolveMagicEnchantment(Registry<Enchantment> enchantments, MagicEnchantment magicEnchantment) {
        return enchantments
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, magicEnchantment.id()))
                .map(holder -> new ResolvedMagicEnchantment(holder, magicEnchantment));
    }

    private static Optional<ResolvedMagicEnchantment> pickMagicEnchantment(
            List<ResolvedMagicEnchantment> pool,
            List<ResolvedMagicEnchantment> selected,
            RandomSource random
    ) {
        List<ResolvedMagicEnchantment> compatible = pool.stream()
                .filter(enchantment -> compatibleWithAll(enchantment.holder(), selected))
                .toList();
        if (compatible.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = 0;
        for (ResolvedMagicEnchantment enchantment : compatible) {
            totalWeight += enchantment.weight();
        }
        int roll = random.nextInt(totalWeight);
        for (ResolvedMagicEnchantment enchantment : compatible) {
            roll -= enchantment.weight();
            if (roll < 0) {
                return Optional.of(enchantment);
            }
        }
        return Optional.of(compatible.getFirst());
    }

    private static boolean compatibleWithAll(Holder<Enchantment> candidate, List<ResolvedMagicEnchantment> selected) {
        for (ResolvedMagicEnchantment enchantment : selected) {
            if (candidate.equals(enchantment.holder()) || !Enchantment.areCompatible(candidate, enchantment.holder())) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack createMtfPart(Object definition, String partType, ResourceLocation material) throws ReflectiveOperationException {
        Object created = definition.getClass()
                .getMethod("createPart", String.class, ResourceLocation.class, int.class)
                .invoke(definition, partType, material, MTF_DEFAULT_QUALITY);
        return created instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Optional<Object> toolDefinition(ResourceLocation toolType) throws ReflectiveOperationException {
        Class<?> registryClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeRegistry");
        Method toolTypeMethod = registryClass.getMethod("toolType", ResourceLocation.class);
        Object optionalDefinition = toolTypeMethod.invoke(null, toolType);
        if (optionalDefinition instanceof Optional<?> optional && optional.isPresent()) {
            return Optional.of(optional.get());
        }
        return Optional.empty();
    }

    private static String primaryPartType(Object definition) throws ReflectiveOperationException {
        Object value = definition.getClass().getMethod("primaryPartType").invoke(definition);
        return value instanceof String partType ? partType : "";
    }

    private static List<String> requiredAssemblyParts(Object definition) throws ReflectiveOperationException {
        Object value = definition.getClass().getMethod("requiredAssemblyParts").invoke(definition);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<String> parts = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String partType) {
                parts.add(partType);
            }
        }
        return List.copyOf(parts);
    }

    private static ItemStack createMtfToolFromDefinition(Object definition, ResourceLocation toolType, ResourceLocation material, boolean usesGuard) throws ReflectiveOperationException {
        Class<?> constructionClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolConstructionData");
        Constructor<?> constructor = constructionClass.getConstructor(
                ResourceLocation.class,
                ResourceLocation.class,
                ResourceLocation.class,
                Optional.class,
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
                Optional.empty(),
                MTF_DEFAULT_QUALITY
        );
        Object created = definition.getClass().getMethod("createTool", constructionClass).invoke(definition, construction);
        return created instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static boolean mergeMtfPartEnchantments(ItemStack sword, List<ItemStack> parts, HolderLookup.Provider registries) throws ReflectiveOperationException {
        Class<?> enchantmentsClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolAssemblyEnchantments");
        Object merged = enchantmentsClass
                .getMethod("mergeOnto", ItemStack.class, Iterable.class, HolderLookup.Provider.class)
                .invoke(null, sword, parts, registries);
        return Boolean.TRUE.equals(merged);
    }

    private static void setMtfAssemblyParts(ItemStack sword, List<ItemStack> parts) throws ReflectiveOperationException {
        Class<?> dataComponentsClass = Class.forName("org.destroyermob.mobstoolforging.registry.ModDataComponents");
        Class<?> assemblyPartsClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolAssemblyParts");
        Object deferredHolder = dataComponentsClass.getField("TOOL_ASSEMBLY_PARTS").get(null);
        Object component = deferredHolder.getClass().getMethod("get").invoke(deferredHolder);
        Object assemblyParts = assemblyPartsClass.getMethod("from", List.class).invoke(null, parts);
        setComponent(sword, component, assemblyParts);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setComponent(ItemStack stack, Object component, Object value) {
        stack.set((DataComponentType) component, value);
    }

    private static Optional<ItemStack> createMtfTool(ResourceLocation toolType, ResourceLocation material, boolean usesGuard) {
        try {
            Optional<Object> definition = toolDefinition(toolType);
            if (definition.isPresent()) {
                ItemStack stack = createMtfToolFromDefinition(definition.get(), toolType, material, usesGuard);
                if (!stack.isEmpty()) {
                    return Optional.of(stack);
                }
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

    private record MagicCandidate(Candidate candidate, Object definition, String primaryPartType) {
        private int weight() {
            return candidate.weight();
        }
    }

    private record MagicEnchantment(ResourceLocation id, int maxGeneratedLevel, int weight) {
    }

    private record ResolvedMagicEnchantment(Holder<Enchantment> holder, MagicEnchantment enchantment) {
        private int level(RandomSource random) {
            int maxLevel = Math.max(1, Math.min(enchantment.maxGeneratedLevel(), holder.value().getMaxLevel()));
            return 1 + random.nextInt(maxLevel);
        }

        private int weight() {
            return enchantment.weight();
        }
    }

    private record WeightedStack(ItemStack stack, int weight) {
    }
}
