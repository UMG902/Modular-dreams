package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.modulardreams.menu.AssemblyTableMenu;

/**
 * Assembly Table screen: three input slots on the left (head, binding,
 * handle - top to bottom), an arrow, and the result slot on the right.
 * Slot outlines and the arrow come from the mod's GUI texture, which is
 * generated to match the menu slot coordinates exactly.
 */
public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {

        private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("modular_dreams",
                        "textures/gui/container/assembly_table.png");
        /** The panel texture is exactly the logical GUI size (176x166) - passing
         * vanilla's 256x256 canvas size here would magnify + crop the panel. */
        private static final int TEX_WIDTH = 176;
        private static final int TEX_HEIGHT = 166;

        public AssemblyTableScreen(AssemblyTableMenu menu, Inventory playerInventory, Component title) {
                super(menu, playerInventory, title, TEX_WIDTH, TEX_HEIGHT);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractBackground(graphics, mouseX, mouseY, partialTick);
                graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos,
                                0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);
        }
}
