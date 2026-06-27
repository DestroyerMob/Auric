package com.ethanhellyer.auric.registry;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.menu.BlockPaletteMenu;
import com.ethanhellyer.auric.menu.ImbuingTableMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, Auric.MOD_ID);

    public static final Supplier<MenuType<ImbuingTableMenu>> IMBUING_TABLE =
            MENUS.register(ModBlocks.IMBUING_TABLE_ID, () -> IMenuTypeExtension.create(ImbuingTableMenu::new));

    public static final Supplier<MenuType<BlockPaletteMenu>> BLOCK_PALETTE =
            MENUS.register(ModItems.BLOCK_PALETTE_ID, () -> IMenuTypeExtension.create(BlockPaletteMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
