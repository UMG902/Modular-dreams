package com.modulardreams.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.menu.PartBuilderMenu;
import com.modulardreams.menu.PartBuilderMenu.PartOption;

/**
 * Part Builder screen - a visual clone of the vanilla 26.3 stonecutter
 * (layout, sprites and interaction byte-verified against the real client jar):
 *
 * - input slot top-left, result slot right, scrollable 4x3 recipe option grid
 *   in the middle, scrollbar on the right;
 * - clicking an option plays the stonecutter select sound, applies the
 *   selection to the LOCAL menu (prediction) and sends the vanilla button
 *   click packet - exactly the vanilla stonecutter flow;
 * - options whose cost exceeds the available input are darkened and show the
 *   material cost in the tooltip.
 *
 * This replaces the previous TiC part_builder layout with its prev/next
 * cycling buttons: cycling is gone, every possible output is visible at once.
 */
public class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {

        private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
        private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
        private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
        private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
        private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
        private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");

        private static final int SCROLLER_WIDTH = 12;
        private static final int SCROLLER_HEIGHT = 15;
        private static final int RECIPES_COLUMNS = 4;
        private static final int RECIPES_ROWS = 3;
        private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
        private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
        private static final int SCROLLER_FULL_HEIGHT = 54;
        private static final int RECIPES_X = 52;
        private static final int RECIPES_Y = 14;

        private float scrollOffs;
        private boolean scrolling;
        private int startIndex;
        private boolean displayRecipes;
        private int lastOptionCount = -1;
        private ItemStack lastInputItem = ItemStack.EMPTY;

        public PartBuilderScreen(PartBuilderMenu menu, Inventory playerInventory, Component title) {
                super(menu, playerInventory, title, 176, 166);
                this.titleLabelY -= 1;
        }

        @Override
        protected void init() {
                super.init();
                this.containerChanged();
        }

        @Override
        public void containerTick() {
                super.containerTick();
                // the option list is pushed by the server payload; reset the scroll
                // whenever it changed or the input item changed (mirrors the vanilla
                // update-listener flow)
                ItemStack currentInput = this.menu.container.getItem(PartBuilderMenu.INPUT_SLOT);
                if (this.menu.getNumOptions() != this.lastOptionCount
                                || !ItemStack.isSameItem(currentInput, this.lastInputItem)) {
                        this.lastInputItem = currentInput.copy();
                        this.containerChanged();
                }
        }

        private void containerChanged() {
                this.lastOptionCount = this.menu.getNumOptions();
                this.displayRecipes = this.menu.hasInputItem();
                this.scrollOffs = 0.0F;
                this.startIndex = 0;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractBackground(graphics, mouseX, mouseY, partialTick);
                graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos,
                                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

                int scrollerY = this.topPos + 9 + (int) (41.0F * this.scrollOffs);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                                this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
                                this.leftPos + 119, scrollerY, SCROLLER_WIDTH, SCROLLER_HEIGHT);

                this.extractButtons(graphics, mouseX, mouseY, this.startIndex + 12);
                this.extractRecipes(graphics, mouseX, mouseY, this.startIndex);
        }

        private void extractButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int lastVisible) {
                for (int i = this.startIndex; i < lastVisible && i < this.menu.getNumOptions(); i++) {
                    int visible = i - this.startIndex;
                    int x = this.leftPos + RECIPES_X + (visible % RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_WIDTH;
                    int y = this.topPos + RECIPES_Y + (visible / RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_HEIGHT;
                    Identifier sprite;
                    if (i == this.menu.getSelectedRecipeIndex()) {
                        sprite = RECIPE_SELECTED_SPRITE;
                    } else if (this.isHovering(x - this.leftPos, y - this.topPos, RECIPES_IMAGE_SIZE_WIDTH,
                                    RECIPES_IMAGE_SIZE_HEIGHT, mouseX, mouseY)) {
                        sprite = RECIPE_HIGHLIGHTED_SPRITE;
                    } else {
                        sprite = RECIPE_SPRITE;
                    }
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y,
                                    RECIPES_IMAGE_SIZE_WIDTH, RECIPES_IMAGE_SIZE_HEIGHT);
                }
        }

        private void extractRecipes(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int startIndex) {
                if (!this.displayRecipes) {
                        return;
                }
                int affordable = 0;
                for (int i = startIndex; i < startIndex + RECIPES_COLUMNS * RECIPES_ROWS
                                && i < this.menu.getNumOptions(); i++) {
                    int visible = i - startIndex;
                    int x = this.leftPos + RECIPES_X + (visible % RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_WIDTH;
                    int y = this.topPos + RECIPES_Y + (visible / RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_HEIGHT;
                    PartOption option = this.menu.getOption(i);
                    boolean canAfford = this.menu.inputCount() >= option.cost();
                    if (canAfford) {
                        affordable++;
                    }
                    Component name = option.result().getHoverName();
                    graphics.item(option.result(), x, y);
                    if (!canAfford) {
                        graphics.fill(x, y, x + 16, y + 16, 0x60A0A0A0);
                    }
                    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + RECIPES_IMAGE_SIZE_HEIGHT) {
                        net.minecraft.ChatFormatting costColor = canAfford ? net.minecraft.ChatFormatting.GRAY
                                        : net.minecraft.ChatFormatting.RED;
                        Component costLine = Component.translatable("container.modular_dreams.cost", option.cost())
                                        .withStyle(costColor);
                        if (!canAfford) {
                            costLine = costLine.copy().append(" - ")
                                            .append(Component.translatable("container.modular_dreams.need_material")
                                                            .withStyle(net.minecraft.ChatFormatting.RED));
                        }
                        graphics.setTooltipForNextFrame(this.font, java.util.List.of(name, costLine),
                                        java.util.Optional.empty(), mouseX, mouseY);
                    }
                }
                // hint under the grid when nothing affordable is visible / nothing at all
                if (affordable == 0) {
                        graphics.centeredText(this.font,
                                        Component.translatable("container.modular_dreams.need_material"),
                                        this.leftPos + RECIPES_X + (RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH) / 2,
                                        this.topPos + RECIPES_Y + RECIPES_ROWS * RECIPES_IMAGE_SIZE_HEIGHT + 6,
                                        0xFFFF5555);
                }
        }

        private boolean isScrollBarActive() {
                return this.displayRecipes && this.menu.getNumOptions() > RECIPES_COLUMNS * RECIPES_ROWS;
        }

        protected int getOffscreenRows() {
                return (this.menu.getNumOptions() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
                this.scrolling = false;
                if (this.displayRecipes) {
                    int gridLeft = this.leftPos + RECIPES_X;
                    int gridTop = this.topPos + RECIPES_Y;
                    int lastVisible = this.startIndex + RECIPES_COLUMNS * RECIPES_ROWS;

                    for (int i = this.startIndex; i < lastVisible && i < this.menu.getNumOptions(); i++) {
                        int visible = i - this.startIndex;
                        double dx = event.x() - (gridLeft + (visible % RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_WIDTH);
                        double dy = event.y() - (gridTop + (visible / RECIPES_COLUMNS) * RECIPES_IMAGE_SIZE_HEIGHT);
                        if (dx >= 0.0 && dy >= 0.0 && dx < 16.0 && dy < RECIPES_IMAGE_SIZE_HEIGHT) {
                            if (this.menu.clickMenuButton(this.minecraft.player, i)) {
                                this.minecraft.getSoundManager().play(
                                                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                                                net.minecraft.sounds.SoundEvents.UI_STONECUTTER_SELECT_RECIPE,
                                                                1.0F));
                                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                                return true;
                            }
                        }
                    }

                    int scrollerLeft = this.leftPos + 119;
                    int scrollerTop = this.topPos + 9;
                    if (event.x() >= scrollerLeft && event.x() < scrollerLeft + SCROLLER_WIDTH
                                    && event.y() >= scrollerTop && event.y() < scrollerTop + SCROLLER_FULL_HEIGHT) {
                        this.scrolling = this.isScrollBarActive();
                    }
                }
                return super.mouseClicked(event, doubled);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
                if (this.scrolling && this.isScrollBarActive()) {
                    int top = this.topPos + 9;
                    int bottom = top + SCROLLER_FULL_HEIGHT;
                    this.scrollOffs = Math.max(0.0F,
                                    Math.min(1.0F, (float) (event.y() - top - 7.5) / (bottom - top - 15.0F)));
                    this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * RECIPES_COLUMNS;
                    return true;
                }
                return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
                if (this.isScrollBarActive()) {
                    int rows = this.getOffscreenRows();
                    this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY / (float) rows, 0.0F, 1.0F);
                    this.startIndex = (int) (this.scrollOffs * (float) rows + 0.5) * RECIPES_COLUMNS;
                    return true;
                }
                return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
                this.scrolling = false;
                return super.mouseReleased(event);
        }
}
