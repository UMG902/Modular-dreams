package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.modulardreams.menu.MeltingUpgradeMenu;

/**
 * Screen of the MELTING UPGRADE's own GUI: the mold slot at the BOTTOM and
 * the slot for the finished metal part at the TOP (an arrow points from the
 * mold up to the part). The metal itself melts in the melting furnace above;
 * this GUI is only the mold holder + the part basket.
 */
public class MeltingUpgradeScreen extends AbstractContainerScreen<MeltingUpgradeMenu> {

        private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("modular_dreams",
                        "textures/gui/melting_upgrade.png");
        /** The panel texture is exactly the logical GUI size (176x166) - passing
         * vanilla's 256x256 canvas size here would magnify + crop the panel. */
        private static final int TEX_WIDTH = 176;
        private static final int TEX_HEIGHT = 166;

        public MeltingUpgradeScreen(MeltingUpgradeMenu menu, Inventory playerInventory, Component title) {
                super(menu, playerInventory, title, TEX_WIDTH, TEX_HEIGHT);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractBackground(graphics, mouseX, mouseY, partialTick);
                graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos,
                                0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);
        }
}
