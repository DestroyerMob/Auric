package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.menu.ImbuingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ImbuingTableScreen extends AbstractContainerScreen<ImbuingTableMenu> {
    private static final ResourceLocation TEXTURE = Auric.id("textures/gui/imbuing_table.png");
    private static final ResourceLocation POTION_SLOT = Auric.id("textures/gui/slot/potion.png");
    private static final int POTION_SLOT_INDEX = 1;

    public ImbuingTableScreen(ImbuingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 60;
        titleLabelY = 18;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (!menu.getSlot(POTION_SLOT_INDEX).hasItem()) {
            graphics.blit(POTION_SLOT, leftPos + menu.getSlot(POTION_SLOT_INDEX).x, topPos + menu.getSlot(POTION_SLOT_INDEX).y, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
