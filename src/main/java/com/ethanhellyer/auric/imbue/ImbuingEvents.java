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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ImbuingEvents {
    private static final Map<UUID, Map<ResourceLocation, ActiveImbue>> LAST_APPLIED = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Map<ResourceLocation, ActiveImbue> strongest = new HashMap<>();
        addImbue(serverPlayer.getMainHandItem(), strongest);

        removeStaleImbues(serverPlayer, strongest);
        for (Map.Entry<ResourceLocation, ActiveImbue> entry : strongest.entrySet()) {
            BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(effect -> refresh(serverPlayer, effect, entry.getValue()));
        }
        LAST_APPLIED.put(serverPlayer.getUUID(), Map.copyOf(strongest));
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getNewDamage() <= 0.0F) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            weapon = player.getMainHandItem();
        }
        applyTargetImbue(weapon, target, player);
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

    private static void addImbue(ItemStack stack, Map<ResourceLocation, ActiveImbue> strongest) {
        ImbueData data = stack.get(ModDataComponents.IMBUE.get());
        if (data == null || !ImbuingLogic.isActiveImbueCarrier(stack) || !ImbuingLogic.isHolderImbue(data)) {
            return;
        }

        int amplifier = ImbuingLogic.scaledAmplifier(data);
        if (amplifier < 0) {
            return;
        }
        ActiveImbue imbue = new ActiveImbue(amplifier, ImbuingLogic.heldDurationTicks(data));
        strongest.merge(data.effect(), imbue, ActiveImbue::stronger);
    }

    private static void applyTargetImbue(ItemStack stack, LivingEntity target, ServerPlayer source) {
        ImbueData data = stack.get(ModDataComponents.IMBUE.get());
        if (data == null || !ImbuingLogic.isActiveImbueCarrier(stack) || !ImbuingLogic.isTargetOnHitImbue(data)) {
            return;
        }

        int amplifier = ImbuingLogic.scaledAmplifier(data);
        int duration = ImbuingLogic.targetDurationTicks(data);
        if (amplifier < 0 || duration <= 0) {
            return;
        }
        BuiltInRegistries.MOB_EFFECT.getHolder(data.effect()).ifPresent(effect -> {
            if (!effect.value().isInstantenous()) {
                target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true), source);
            }
        });
    }

    private static void removeStaleImbues(ServerPlayer player, Map<ResourceLocation, ActiveImbue> active) {
        Map<ResourceLocation, ActiveImbue> previous = LAST_APPLIED.get(player.getUUID());
        if (previous == null || previous.isEmpty()) {
            return;
        }

        for (Map.Entry<ResourceLocation, ActiveImbue> entry : previous.entrySet()) {
            ActiveImbue activeImbue = active.get(entry.getKey());
            if (activeImbue != null && activeImbue.amplifier() >= entry.getValue().amplifier()) {
                continue;
            }
            BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(effect -> removeIfAuricShortEffect(player, effect, entry.getValue()));
        }
    }

    private static void removeIfAuricShortEffect(ServerPlayer player, Holder<MobEffect> effect, ActiveImbue imbue) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() == imbue.amplifier() && existing.getDuration() <= imbue.durationTicks()) {
            player.removeEffect(effect);
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, ActiveImbue imbue) {
        int amplifier = imbue.amplifier();
        int durationTicks = imbue.durationTicks();
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() > amplifier && existing.getDuration() <= durationTicks) {
            player.removeEffect(effect);
            existing = null;
        }
        if (existing != null && (existing.getAmplifier() > amplifier || existing.getDuration() > durationTicks)) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, true, false, false));
    }

    private record ActiveImbue(int amplifier, int durationTicks) {
        private static ActiveImbue stronger(ActiveImbue first, ActiveImbue second) {
            if (second.amplifier > first.amplifier) {
                return second;
            }
            if (first.amplifier > second.amplifier) {
                return first;
            }
            return first.durationTicks >= second.durationTicks ? first : second;
        }
    }
}
