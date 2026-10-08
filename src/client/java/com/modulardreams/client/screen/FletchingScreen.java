package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.modulardreams.menu.FletchingMenu;

/**
 * Fletching screen. Two potion slots on the left (each potion coats up to
 * 32 arrows - two potions cover a full stack of 64), the arrow input in the
 * middle, tipped arrow output on the right.
 */
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("modular_dreams",
            "textures/gui/container/fletching.png");

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos,
                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int color = 0x404040;
        graphics.text(this.font, Component.translatable("container.modular_dreams.fletching.potion"),
                this.leftPos + 53, this.topPos + FletchingMenu.POTION_A_Y + 4, color);
        graphics.text(this.font, Component.translatable("container.modular_dreams.fletching.arrows"),
                this.leftPos + 79, this.topPos + FletchingMenu.ARROWS_Y + 4, color);
    }
}
