package com.ethanhellyer.auric.config;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AuricConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue IMBUE_STRENGTH_SCALE;
    public static final ModConfigSpec.IntValue IMBUING_XP_LEVEL_COST;
    public static final ModConfigSpec.IntValue DRAGON_BREATH_MAX_IMBUE_LEVEL;
    public static final ModConfigSpec.IntValue EFFECT_TICK_INTERVAL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISALLOWED_EFFECTS;
    public static final ModConfigSpec.BooleanValue ALLOW_MIXED_POTION_CANDLE_EFFECTS;
    public static final ModConfigSpec.BooleanValue GENERATE_FORGOTTEN_BLADE_SHRINES;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("imbuing");
        IMBUE_STRENGTH_SCALE = BUILDER
                .comment("Multiplier for imbued potion levels. Level I stays Level I for any positive scale below 1.0.")
                .defineInRange("imbue_strength_scale", 0.5D, 0.0D, 16.0D);
        IMBUING_XP_LEVEL_COST = BUILDER
                .comment("XP levels consumed when taking an imbued item from the Imbuing Table.")
                .defineInRange("imbuing_xp_level_cost", 5, 0, 1000);
        DRAGON_BREATH_MAX_IMBUE_LEVEL = BUILDER
                .comment("Maximum displayed potion level reachable by upgrading an imbued item with Dragon's Breath.")
                .defineInRange("dragon_breath_max_imbue_level", 3, 1, MobEffectInstance.MAX_AMPLIFIER + 1);
        EFFECT_TICK_INTERVAL = BUILDER
                .comment("Legacy setting kept for compatibility. Active imbues are now checked every tick so tool effects do not linger after hotbar swaps.")
                .defineInRange("effect_tick_interval", 1, 1, 1200);
        DISALLOWED_EFFECTS = BUILDER
                .comment("Mob effect ids that cannot be imbued, such as minecraft:jump_boost.")
                .defineListAllowEmpty("disallowed_effects", List.of(), () -> "", value -> value instanceof String);
        BUILDER.pop();

        BUILDER.push("potion_candles");
        ALLOW_MIXED_POTION_CANDLE_EFFECTS = BUILDER
                .comment("Whether stacked Scented Candles can each carry different potion effects.")
                .define("allow_mixed_potion_candle_effects", true);
        BUILDER.pop();

        BUILDER.push("worldgen");
        GENERATE_FORGOTTEN_BLADE_SHRINES = BUILDER
                .comment("Whether Auric's rare Forgotten Blade Shrines generate in new overworld chunks.")
                .define("generate_forgotten_blade_shrines", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private AuricConfig() {
    }
}
