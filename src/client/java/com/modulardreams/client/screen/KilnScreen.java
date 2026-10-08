package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.modulardreams.menu.KilnMenu;

/**
 * Kiln screen. Same slot layout as the furnace GUI (input / fuel / result),
 * warm-tinted background with flame and progress arrow drawn from the same
 * texture.
 */
public class KilnScreen extends AbstractContainerScreen<KilnMenu> {

    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("modular_dreams",
            "textures/gui/container/kiln.png");

    public KilnScreen(KilnMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos,
                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        int x = this.leftPos;
        int y = this.topPos;
        if (this.menu.isLit()) {
            // flame fill: draws bottom-up from the flame slot at panel (56, 36)
            int flame = this.menu.getBurnProgress(14);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, x + 56, y + 36 + 14 - flame,
                    176, 14 - flame, 14, flame, 256, 256);
        }
        // progress arrow: filled portion at panel (79, 34), sprite at (176, 28)
        int arrow = this.menu.getCookProgress(24);
        if (arrow > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, x + 79, y + 34,
                    176, 28, arrow, 17, 256, 256);
        }
    }
}
