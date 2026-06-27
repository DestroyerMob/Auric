package com.ethanhellyer.auric.menu;

import com.ethanhellyer.auric.item.BlockPaletteItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class BlockPaletteContainer extends SimpleContainer {
    private final ItemStack palette;

    public BlockPaletteContainer(ItemStack palette) {
        super(BlockPaletteItem.SLOT_COUNT);
        this.palette = palette;
        NonNullList<ItemStack> contents = BlockPaletteItem.getContents(palette);
        for (int i = 0; i < contents.size(); i++) {
            super.setItem(i, contents.get(i));
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        BlockPaletteItem.saveContents(palette, getItems());
    }
}
