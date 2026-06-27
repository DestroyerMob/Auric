package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.item.BlockPaletteItem;
import com.ethanhellyer.auric.item.CamoBrushItem;
import com.ethanhellyer.auric.item.CamoPasteItem;
import com.ethanhellyer.auric.item.PotionCandleItem;
import com.ethanhellyer.auric.item.SculkBottleItem;
import com.ethanhellyer.auric.item.SculkBottleOfEnchantingItem;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final String BLOCK_PALETTE_ID = "block_palette";
    public static final String CAMO_PASTE_ID = "camo_paste";
    public static final String CAMO_BRUSH_ID = "camo_brush";
    public static final String SCULK_BOTTLE_ID = "sculk_bottle";
    public static final String SCULK_BOTTLE_OF_ENCHANTING_ID = "sculk_bottle_of_enchanting";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Auric.MOD_ID);

    public static final Supplier<BlockItem> IMBUING_TABLE = ITEMS.register(
            ModBlocks.IMBUING_TABLE_ID,
            () -> new BlockItem(ModBlocks.IMBUING_TABLE.get(), new Item.Properties())
    );

    public static final Supplier<BlockPaletteItem> BLOCK_PALETTE = ITEMS.register(
            BLOCK_PALETTE_ID,
            () -> new BlockPaletteItem(new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY))
    );

    public static final Supplier<CamoPasteItem> CAMO_PASTE = ITEMS.register(
            CAMO_PASTE_ID,
            () -> new CamoPasteItem(new Item.Properties().stacksTo(16))
    );

    public static final Supplier<CamoBrushItem> CAMO_BRUSH = ITEMS.register(
            CAMO_BRUSH_ID,
            () -> new CamoBrushItem(new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY))
    );

    public static final Supplier<SculkBottleItem> SCULK_BOTTLE = ITEMS.register(
            SCULK_BOTTLE_ID,
            () -> new SculkBottleItem(new Item.Properties().stacksTo(16))
    );

    public static final Supplier<SculkBottleOfEnchantingItem> SCULK_BOTTLE_OF_ENCHANTING = ITEMS.register(
            SCULK_BOTTLE_OF_ENCHANTING_ID,
            () -> new SculkBottleOfEnchantingItem(new Item.Properties().stacksTo(16))
    );

    public static final Supplier<PotionCandleItem> POTION_CANDLE = ITEMS.register(
            ModBlocks.POTION_CANDLE_ID,
            () -> new PotionCandleItem(ModBlocks.POTION_CANDLE.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
