package com.ethanhellyer.auric.imbue;

import com.ethanhellyer.auric.registry.ModDataComponents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ImbuingEvents {
    private static final int IMBUE_EFFECT_DURATION_TICKS = 2;
    private static final Map<UUID, Map<ResourceLocation, Integer>> LAST_APPLIED = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Map<ResourceLocation, Integer> strongest = new HashMap<>();
        addImbue(serverPlayer.getMainHandItem(), strongest);

        removeStaleImbues(serverPlayer, strongest);
        for (Map.Entry<ResourceLocation, Integer> entry : strongest.entrySet()) {
            BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(effect -> refresh(serverPlayer, effect, entry.getValue()));
        }
        LAST_APPLIED.put(serverPlayer.getUUID(), Map.copyOf(strongest));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_APPLIED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ImbueData data = event.getItemStack().get(ModDataComponents.IMBUE.get());
        if (data != null) {
            event.getToolTip().add(ImbuingLogic.tooltipName(data).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void addImbue(ItemStack stack, Map<ResourceLocation, Integer> strongest) {
        ImbueData data = stack.get(ModDataComponents.IMBUE.get());
        if (data == null || !ImbuingLogic.isActiveImbueCarrier(stack)) {
            return;
        }

        int amplifier = ImbuingLogic.scaledAmplifier(data);
        if (amplifier < 0) {
            return;
        }
        strongest.merge(data.effect(), amplifier, Math::max);
    }

    private static void removeStaleImbues(ServerPlayer player, Map<ResourceLocation, Integer> active) {
        Map<ResourceLocation, Integer> previous = LAST_APPLIED.get(player.getUUID());
        if (previous == null || previous.isEmpty()) {
            return;
        }

        for (Map.Entry<ResourceLocation, Integer> entry : previous.entrySet()) {
            int activeAmplifier = active.getOrDefault(entry.getKey(), -1);
            if (activeAmplifier >= entry.getValue()) {
                continue;
            }
            BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(effect -> removeIfAuricShortEffect(player, effect, entry.getValue()));
        }
    }

    private static void removeIfAuricShortEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() == amplifier && existing.getDuration() <= IMBUE_EFFECT_DURATION_TICKS) {
            player.removeEffect(effect);
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() > amplifier && existing.getDuration() <= IMBUE_EFFECT_DURATION_TICKS) {
            player.removeEffect(effect);
            existing = null;
        }
        if (existing != null && (existing.getAmplifier() > amplifier || existing.getDuration() > IMBUE_EFFECT_DURATION_TICKS)) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, IMBUE_EFFECT_DURATION_TICKS, amplifier, true, false, false));
    }
}
