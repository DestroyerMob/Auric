package com.ethanhellyer.auric.entity;

import com.ethanhellyer.auric.item.SculkBottleOfEnchantingItem;
import com.ethanhellyer.auric.registry.ModEntityTypes;
import com.ethanhellyer.auric.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownSculkExperienceBottle extends ThrowableItemProjectile {
    private static final int BREAK_PARTICLE_COLOR = 0x0B3D4A;

    public ThrownSculkExperienceBottle(EntityType<? extends ThrownSculkExperienceBottle> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownSculkExperienceBottle(Level level, LivingEntity owner) {
        super(ModEntityTypes.SCULK_EXPERIENCE_BOTTLE.get(), owner, level);
    }

    public ThrownSculkExperienceBottle(Level level, double x, double y, double z) {
        super(ModEntityTypes.SCULK_EXPERIENCE_BOTTLE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SCULK_BOTTLE_OF_ENCHANTING.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07D;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            level().levelEvent(2002, blockPosition(), BREAK_PARTICLE_COLOR);
            int experience = SculkBottleOfEnchantingItem.getStoredExperience(getItem());
            if (experience > 0) {
                ExperienceOrb.award(serverLevel, position(), experience);
            }
            discard();
        }
    }
}
