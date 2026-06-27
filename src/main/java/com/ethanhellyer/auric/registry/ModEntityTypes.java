package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.entity.ThrownSculkExperienceBottle;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final String SCULK_EXPERIENCE_BOTTLE_ID = "sculk_experience_bottle";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Auric.MOD_ID);

    public static final Supplier<EntityType<ThrownSculkExperienceBottle>> SCULK_EXPERIENCE_BOTTLE =
            ENTITY_TYPES.register(
                    SCULK_EXPERIENCE_BOTTLE_ID,
                    () -> EntityType.Builder.<ThrownSculkExperienceBottle>of(ThrownSculkExperienceBottle::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(Auric.id(SCULK_EXPERIENCE_BOTTLE_ID).toString())
            );

    private ModEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
