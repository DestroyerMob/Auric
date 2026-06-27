package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.item.SculkBottleItem;
import com.ethanhellyer.auric.item.SculkBottleOfEnchantingItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class CreativeTabEvents {
    private CreativeTabEvents() {
    }

    public static void addContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.IMBUING_TABLE.get());
            event.accept(ModItems.BLOCK_PALETTE.get());
            event.accept(ModItems.CAMO_PASTE.get());
            event.accept(ModItems.CAMO_BRUSH.get());
            event.accept(ModItems.SCULK_BOTTLE.get());
            event.accept(SculkBottleOfEnchantingItem.withExperience(SculkBottleItem.EXPERIENCE_PER_BOTTLE));
            event.accept(ModItems.POTION_CANDLE.get());
        }
    }
}
