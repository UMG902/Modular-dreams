package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.menu.MeltingFurnaceMenu;

/**
 * Screen of the MELTING FURNACE. Reuses the vanilla furnace GUI texture and
 * sprites so it looks exactly like a furnace, with ONE difference: there is
 * no output item - instead the output position shows the mold that is
 * inserted into the upgrade below (a ghost preview, nothing when there is
 * no mold). The preview sits exactly at the vanilla RESULT slot position
 * (116,35), inside the output cell baked into the vanilla texture, so it
 * reads as the intended slot.
 */
public class MeltingFurnaceScreen extends AbstractContainerScreen<MeltingFurnaceMenu> {

        private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace(
                        "textures/gui/container/furnace.png");
        private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace(
                        "container/furnace/lit_progress");
        private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace(
                        "container/furnace/burn_progress");

        public MeltingFurnaceScreen(MeltingFurnaceMenu menu, Inventory playerInventory, Component title) {
                super(menu, playerInventory, title, 176, 166);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractBackground(graphics, mouseX, mouseY, partialTick);
                int left = this.leftPos;
                int top = this.topPos;

                graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, left, top,
                                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

                // burning fuel flame (vanilla coordinates)
                if (this.menu.isLit()) {
                        int lit = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE,
                                        14, 14, 0, 14 - lit, left + 56, top + 36 + 14 - lit, 14, lit);
                }

                // melt progress arrow (vanilla coordinates)
                int progress = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
                if (progress > 0) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE,
                                        24, 16, 0, 0, left + 79, top + 34, progress, 16);
                }

                // mold preview: the mold inserted into the upgrade below, drawn
                // exactly at the vanilla furnace RESULT slot position so it
                // sits inside the baked output cell (there is no real slot -
                // the finished part appears in the MELTING UPGRADE's own GUI)
                ItemStack preview = this.menu.getMoldPreview();
                if (!preview.isEmpty()) {
                        graphics.fakeItem(preview, left + MeltingFurnaceMenu.MOLD_PREVIEW_X,
                                        top + MeltingFurnaceMenu.MOLD_PREVIEW_Y);
                }
        }
}
