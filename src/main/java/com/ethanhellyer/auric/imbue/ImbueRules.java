package com.ethanhellyer.auric.imbue;

import com.ethanhellyer.auric.Auric;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.Potion;

public final class ImbueRules {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_HELD_DURATION_TICKS = 2;
    private static final int DEFAULT_TARGET_DURATION_TICKS = 100;
    private static volatile Map<ResourceLocation, Rule> rulesByEffect = Map.of();

    private ImbueRules() {
    }

    public static Optional<Rule> ruleForPotion(ResourceLocation potionId, ResourceLocation effectId) {
        return ruleForEffect(effectId).filter(rule -> rule.potions().contains(potionId));
    }

    public static Optional<Rule> ruleForEffect(ResourceLocation effectId) {
        return Optional.ofNullable(rulesByEffect.get(effectId));
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "auric/imbue_effects");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, Rule> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
                try {
                    JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "imbue effect rule");
                    if (!GsonHelper.getAsBoolean(json, "enabled", true)) {
                        continue;
                    }
                    Rule rule = parse(entry.getKey(), json);
                    loaded.put(rule.effect(), rule);
                } catch (RuntimeException exception) {
                    Auric.LOGGER.warn("Skipping invalid imbue effect rule {}.", entry.getKey(), exception);
                }
            }
            rulesByEffect = Map.copyOf(loaded);
            Auric.LOGGER.info("Loaded {} imbue effect rule(s).", loaded.size());
        }

        private static Rule parse(ResourceLocation id, JsonObject json) {
            ResourceLocation effectId = json.has("effect")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "effect"))
                    : id;
            Holder.Reference<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown mob effect: " + effectId));
            if (effect.value().isInstantenous()) {
                throw new IllegalArgumentException("Instant mob effects cannot be imbued: " + effectId);
            }

            Handling handling = Handling.parse(GsonHelper.getAsString(json, "handling", Handling.HOLDER.id()));
            Set<ResourceLocation> potions = parsePotions(json);
            if (potions.isEmpty()) {
                throw new IllegalArgumentException("Imbue rule " + id + " must list at least one potion.");
            }
            for (ResourceLocation potionId : potions) {
                Holder.Reference<Potion> potion = BuiltInRegistries.POTION.getHolder(potionId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown potion: " + potionId));
                boolean hasEffect = potion.value().getEffects().stream().anyMatch(instance -> instance.is(effect));
                if (!hasEffect) {
                    throw new IllegalArgumentException("Potion " + potionId + " does not contain effect " + effectId);
                }
            }

            int heldDurationTicks = Math.max(1, GsonHelper.getAsInt(json, "held_duration_ticks", DEFAULT_HELD_DURATION_TICKS));
            int targetDurationTicks = Math.max(1, GsonHelper.getAsInt(json, "target_duration_ticks", DEFAULT_TARGET_DURATION_TICKS));
            return new Rule(effectId, handling, Set.copyOf(potions), heldDurationTicks, targetDurationTicks);
        }

        private static Set<ResourceLocation> parsePotions(JsonObject json) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "potions");
            Set<ResourceLocation> values = new LinkedHashSet<>();
            for (JsonElement element : array) {
                values.add(ResourceLocation.parse(GsonHelper.convertToString(element, "potion")));
            }
            return values;
        }
    }

    public enum Handling {
        HOLDER("holder"),
        TARGET_ON_HIT("target_on_hit");

        private final String id;

        Handling(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        private static Handling parse(String value) {
            for (Handling handling : values()) {
                if (handling.id.equals(value) || handling.name().equalsIgnoreCase(value)) {
                    return handling;
                }
            }
            throw new IllegalArgumentException("Unknown imbue handling: " + value);
        }
    }

    public record Rule(ResourceLocation effect, Handling handling, Set<ResourceLocation> potions, int heldDurationTicks, int targetDurationTicks) {
    }
}
