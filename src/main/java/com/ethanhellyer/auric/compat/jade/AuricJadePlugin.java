package com.ethanhellyer.auric.compat.jade;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.block.PotionCauldronBlock;
import com.ethanhellyer.auric.blockentity.PotionCauldronBlockEntity;
import com.ethanhellyer.auric.tag.ModBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(Auric.MOD_ID)
public class AuricJadePlugin implements IWailaPlugin {
    private static final PotionCauldronProvider POTION_CAULDRON_PROVIDER = new PotionCauldronProvider();

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(POTION_CAULDRON_PROVIDER, PotionCauldronBlock.class);
    }

    private static final class PotionCauldronProvider implements IBlockComponentProvider {
        private static final ResourceLocation UID = Auric.id("potion_cauldron");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof PotionCauldronBlockEntity cauldron)) {
                return;
            }

            Holder<Potion> potion = cauldron.getPotion();
            tooltip.add(Component.translatable(
                    "jade.auric.potion_cauldron.potion",
                    PotionContents.createItemStack(Items.POTION, potion).getHoverName()
            ).withStyle(potion.is(Potions.WATER) ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE));

            BlockState state = accessor.getBlockState();
            if (state.hasProperty(LayeredCauldronBlock.LEVEL)) {
                tooltip.add(Component.translatable(
                        "jade.auric.potion_cauldron.fill",
                        state.getValue(LayeredCauldronBlock.LEVEL),
                        LayeredCauldronBlock.MAX_FILL_LEVEL
                ).withStyle(ChatFormatting.GRAY));
            }

            boolean heated = isHeated(accessor.getLevel(), accessor.getPosition());
            tooltip.add(Component.translatable(
                    heated ? "jade.auric.potion_cauldron.heated" : "jade.auric.potion_cauldron.unheated"
            ).withStyle(heated ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));

            boolean hasVisibleEffects = false;
            for (MobEffectInstance effect : potion.value().getEffects()) {
                if (!effect.isVisible()) {
                    continue;
                }
                hasVisibleEffects = true;
                tooltip.add(Component.translatable(
                        "jade.auric.potion_cauldron.effect",
                        Component.translatable(effect.getDescriptionId()),
                        effect.getAmplifier() + 1,
                        MobEffectUtil.formatDuration(effect, 1.0F, accessor.tickRate())
                ).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (!hasVisibleEffects) {
                tooltip.add(Component.translatable("jade.auric.potion_cauldron.no_effects").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        private static boolean isHeated(Level level, BlockPos cauldronPos) {
            BlockState heat = level.getBlockState(cauldronPos.below());
            if (!heat.is(ModBlockTags.CAULDRON_HEAT_SOURCES)) {
                return false;
            }
            return !heat.hasProperty(BlockStateProperties.LIT) || heat.getValue(BlockStateProperties.LIT);
        }
    }
}
