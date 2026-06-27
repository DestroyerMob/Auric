package com.ethanhellyer.auric.block;

import com.ethanhellyer.auric.blockentity.PotionCauldronBlockEntity;
import com.ethanhellyer.auric.tag.ModBlockTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PotionCauldronBlock extends LayeredCauldronBlock implements EntityBlock {
    public static final MapCodec<PotionCauldronBlock> CODEC = simpleCodec(PotionCauldronBlock::new);
    private static final double PARTICLE_INSET = 0.22D;

    public PotionCauldronBlock(Properties properties) {
        super(Biome.Precipitation.RAIN, CauldronInteraction.WATER, properties);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public MapCodec codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionCauldronBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!(level.getBlockEntity(pos) instanceof PotionCauldronBlockEntity cauldron)) {
            return;
        }

        boolean heated = hasHeatSource(level, pos);
        int strongestLevel = strongestVisiblePotionLevel(cauldron);
        if (strongestLevel > 0) {
            spawnPotionWisp(state, level, pos, random, cauldron.getPotionColor(), strongestLevel, heated);
        }
        if (heated) {
            spawnHeatBubble(state, level, pos, random, strongestLevel);
        }
    }

    private void spawnPotionWisp(BlockState state, Level level, BlockPos pos, RandomSource random, int color, int strongestLevel, boolean heated) {
        int chance = Math.max(2, 10 - strongestLevel * 2 - (heated ? 2 : 0));
        if (random.nextInt(chance) != 0) {
            return;
        }

        double x = pos.getX() + PARTICLE_INSET + random.nextDouble() * (1.0D - PARTICLE_INSET * 2.0D);
        double y = pos.getY() + getContentHeight(state) + 0.03D;
        double z = pos.getZ() + PARTICLE_INSET + random.nextDouble() * (1.0D - PARTICLE_INSET * 2.0D);
        double driftX = (random.nextDouble() - 0.5D) * 0.015D;
        double driftY = 0.012D + random.nextDouble() * (0.006D + strongestLevel * 0.003D);
        double driftZ = (random.nextDouble() - 0.5D) * 0.015D;
        level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color), x, y, z, driftX, driftY, driftZ);
    }

    private void spawnHeatBubble(BlockState state, Level level, BlockPos pos, RandomSource random, int strongestLevel) {
        int chance = strongestLevel > 0 ? 8 : 14;
        if (random.nextInt(chance) != 0) {
            return;
        }

        double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
        double y = pos.getY() + getContentHeight(state) + 0.015D;
        double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0D, 0.015D, 0.0D);
        if (strongestLevel > 1 && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y + 0.03D, z, 0.0D, 0.02D, 0.0D);
        }
    }

    private static int strongestVisiblePotionLevel(PotionCauldronBlockEntity cauldron) {
        int strongest = 0;
        for (MobEffectInstance effect : cauldron.getPotion().value().getEffects()) {
            if (effect.isVisible()) {
                strongest = Math.max(strongest, effect.getAmplifier() + 1);
            }
        }
        return strongest;
    }

    private static boolean hasHeatSource(Level level, BlockPos cauldronPos) {
        BlockState heat = level.getBlockState(cauldronPos.below());
        if (!heat.is(ModBlockTags.CAULDRON_HEAT_SOURCES)) {
            return false;
        }
        return !heat.hasProperty(BlockStateProperties.LIT) || heat.getValue(BlockStateProperties.LIT);
    }
}
