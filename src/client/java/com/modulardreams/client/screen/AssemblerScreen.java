package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.modulardreams.menu.AssemblerMenu;

/**
 * Assembler screen (v2). Custom background; slots: handle and head/vanilla
 * tool input on the left, arrow in the middle, tool output on the right,
 * player inventory below. Slot labels explain the dual role of
 * the head slot (part assembly vs vanilla tool conversion).
 */
public class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {

    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("modular_dreams",
            "textures/gui/container/assembler.png");

    public AssemblerScreen(AssemblerMenu menu, Inventory playerInventory, Component title) {
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
        graphics.text(this.font, Component.translatable("container.modular_dreams.assembler.handle"),
                this.leftPos + 46, this.topPos + AssemblerMenu.HANDLE_Y + 4, color);
        graphics.text(this.font, Component.translatable("container.modular_dreams.assembler.head"),
                this.leftPos + 46, this.topPos + AssemblerMenu.HEAD_Y + 4, color);
    }
}
