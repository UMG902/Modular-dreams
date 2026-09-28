package com.modulardreams.client.book;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.client.book.BookContent.Chapter;
import com.modulardreams.client.book.BookContent.Heading;
import com.modulardreams.client.book.BookContent.ItemLine;

/**
 * The guide book GUI - a Tinkers'-style custom screen (inspired by the Tinkers'
 * Construct 1.20.1 book, adapted to Modular Dreams):
 *
 * - a two-page vanilla book spread; chapters are the tabs on the right edge;
 * - chapter content is a flowing document built from the live registries
 *   ({@link BookContent}) and paginated automatically, so no page is ever a
 *   stub;
 * - page turning via the vanilla book corner buttons, mouse wheel or the
 *   arrow keys; item icons render inline next to their text.
 */
public class GuideBookScreen extends Screen {

        private static final Identifier BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/book.png");
        private static final int IMAGE_WIDTH = 192;
        private static final int IMAGE_HEIGHT = 192;
        private static final int TEXT_WIDTH = 114;
        private static final int PAGE_TEXT_X_LEFT = 36;
        private static final int PAGE_TEXT_Y = 30;
        /** max stacked line height per page; keeps the text clear of the page
         * buttons at y=157 (vanilla's own 128px area ends at y=156) */
        private static final int PAGE_HEIGHT = 124;
        private static final int PAGE_BUTTON_Y = 157;
        private static final int PAGE_BACK_BUTTON_X = 43;
        private static final int PAGE_FORWARD_BUTTON_X = 116;

        private static final int TAB_WIDTH = 22;
        private static final int TAB_HEIGHT = 24;
        private static final int TAB_TEXT_COLOR = 0xFF373737;
        private static final int TAB_BORDER = 0xFF373737;
        private static final int TAB_FILL = 0xFFDCD1AC;
        private static final int TAB_FILL_SELECTED = 0xFFF8F0D8;

        /** One paginated display line of the book. */
        private record Line(int height, Renderer renderer) {
                interface Renderer {
                        void render(GuiGraphicsExtractor graphics, int x, int y);
                }
        }

        private final List<Chapter> chapters;
        private int chapterIndex;
        private List<List<Line>> pages = List.of();
        private int pageIndex;
        private PageButton backButton;
        private PageButton forwardButton;

        public GuideBookScreen() {
                super(Component.translatable("guide.modular_dreams.title"));
                this.chapters = BookContent.build();
        }

        // ------------------------------------------------------------------ lifecycle

        @Override
        protected void init() {
                this.repaginate();
                int left = this.bookLeft();
                int top = this.bookTop();
                this.backButton = this.addRenderableWidget(new PageButton(left + PAGE_BACK_BUTTON_X, top + PAGE_BUTTON_Y,
                                false, button -> this.pageBack(), true));
                this.forwardButton = this.addRenderableWidget(new PageButton(left + PAGE_FORWARD_BUTTON_X,
                                top + PAGE_BUTTON_Y, true, button -> this.pageForward(), true));
                this.updateButtonVisibility();
        }

        private int bookLeft() {
                return (this.width - IMAGE_WIDTH) / 2;
        }

        private int bookTop() {
                return (this.height - IMAGE_HEIGHT) / 2;
        }

        private void setChapter(int index) {
                this.chapterIndex = index;
                this.repaginate();
                this.pageIndex = 0;
                this.updateButtonVisibility();
        }

        private void repaginate() {
                List<Line> lines = this.flatten(this.chapters.get(this.chapterIndex));
                List<List<Line>> paginated = new ArrayList<>();
                List<Line> current = new ArrayList<>();
                int height = 0;
                for (Line line : lines) {
                        if (height + line.height() > PAGE_HEIGHT && !current.isEmpty()) {
                                paginated.add(current);
                                current = new ArrayList<>();
                                height = 0;
                        }
                        current.add(line);
                        height += line.height();
                }
                if (!current.isEmpty()) {
                        paginated.add(current);
                }
                this.pages = paginated;
                if (this.pageIndex >= this.pages.size()) {
                        this.pageIndex = Math.max(0, this.pages.size() - 1);
                }
        }

        /** Flattens the chapter's elements into fixed-height display lines. */
        private List<Line> flatten(Chapter chapter) {
                List<Line> lines = new ArrayList<>();
                for (BookContent.Element element : chapter.elements()) {
                        if (element instanceof Heading(Component text)) {
                                for (FormattedCharSequence seq : this.font.split(text, TEXT_WIDTH)) {
                                        // shadow=false: with the default shadow every line is drawn a
                                        // second time 1px down-right (darkened), which reads as the
                                        // same text twice - vanilla book pages draw flat, no shadow
                                        lines.add(new Line(this.font.lineHeight + 4,
                                                        (g, x, y) -> g.text(this.font, seq, x, y, TAB_TEXT_COLOR, false)));
                                }
                                lines.add(new Line(4, (g, x, y) -> {
                                }));
                        } else if (element instanceof BookContent.Paragraph(Component text)) {
                                for (FormattedCharSequence seq : this.font.split(text, TEXT_WIDTH)) {
                                        lines.add(new Line(this.font.lineHeight + 2,
                                                        (g, x, y) -> g.text(this.font, seq, x, y, TAB_TEXT_COLOR, false)));
                                }
                                lines.add(new Line(3, (g, x, y) -> {
                                }));
                        } else if (element instanceof ItemLine(ItemStack icon, Component text)) {
                                List<FormattedCharSequence> wrapped = this.font.split(text, TEXT_WIDTH - 18);
                                int lineHeight = this.font.lineHeight + 2;
                                int height = Math.max(16, wrapped.size() * lineHeight);
                                lines.add(new Line(height + 2, (g, x, y) -> {
                                        g.item(icon, x, y);
                                        int textY = y + (height - wrapped.size() * lineHeight) / 2;
                                        for (FormattedCharSequence seq : wrapped) {
                                                g.text(this.font, seq, x + 18, textY, TAB_TEXT_COLOR, false);
                                                textY += lineHeight;
                                        }
                                }));
                        } else if (element instanceof BookContent.Bullets(List<Component> bulletLines)) {
                                for (Component bullet : bulletLines) {
                                        for (FormattedCharSequence seq : this.font.split(bullet, TEXT_WIDTH)) {
                                                lines.add(new Line(this.font.lineHeight + 2,
                                                                (g, x, y) -> g.text(this.font, seq, x, y, TAB_TEXT_COLOR, false)));
                                        }
                                }
                                lines.add(new Line(3, (g, x, y) -> {
                                }));
                        } else if (element instanceof BookContent.Spacer(int pixels)) {
                                lines.add(new Line(pixels, (g, x, y) -> {
                                }));
                        }
                }
                return lines;
        }

        // ------------------------------------------------------------------ navigation

        private void pageBack() {
                if (this.pageIndex > 0) {
                        this.pageIndex--;
                }
                this.updateButtonVisibility();
        }

        private void pageForward() {
                if (this.pageIndex + 1 < this.pages.size()) {
                        this.pageIndex++;
                }
                this.updateButtonVisibility();
        }

        private void updateButtonVisibility() {
                if (this.backButton != null) {
                        this.backButton.visible = this.pageIndex > 0;
                }
                if (this.forwardButton != null) {
                        this.forwardButton.visible = this.pageIndex + 1 < this.pages.size();
                }
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
                if (super.keyPressed(event)) {
                        return true;
                }
                // GLFW key codes (GLFW_KEY_LEFT = 263, GLFW_KEY_RIGHT = 262); the
                // client compile classpath does not export the lwjgl-glfw module.
                if (event.key() == 263) {
                        this.pageBack();
                        return true;
                }
                if (event.key() == 262) {
                        this.pageForward();
                        return true;
                }
                return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                        return true;
                }
                if (scrollY < 0.0) {
                        this.pageForward();
                } else if (scrollY > 0.0) {
                        this.pageBack();
                }
                return true;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
                int left = this.bookLeft();
                int top = this.bookTop();
                for (int i = 0; i < this.chapters.size(); i++) {
                        int x = left + IMAGE_WIDTH + 2;
                        int y = top + 8 + i * (TAB_HEIGHT + 2);
                        if (event.x() >= x && event.x() < x + TAB_WIDTH && event.y() >= y
                                        && event.y() < y + TAB_HEIGHT) {
                                if (i != this.chapterIndex) {
                                        this.setChapter(i);
                                        this.minecraft.getSoundManager().play(
                                                        net.minecraft.client.resources.sounds.SimpleSoundInstance
                                                                        .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,
                                                                                        1.0F));
                                }
                                return true;
                        }
                }
                return super.mouseClicked(event, doubled);
        }

        // ------------------------------------------------------------------ rendering

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractRenderState(graphics, mouseX, mouseY, partialTick);
                int left = this.bookLeft();
                int top = this.bookTop();

                graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION, left, top,
                                0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);

                // chapter title across the top (the vanilla 26.3 book screen is a
                // SINGLE-page layout: one page per view, text column at x=36 with
                // width 114 - drawing a second spread page here made both texts
                // overlap in the same column). Manual centering + shadow=false
                // (centeredText always draws the 1px-offset shadow copy).
                Component title = this.chapters.get(this.chapterIndex).title();
                int titleWidth = this.font.width(title);
                graphics.text(this.font, title, left + PAGE_TEXT_X_LEFT + (TEXT_WIDTH - titleWidth) / 2,
                                top + 16, TAB_TEXT_COLOR, false);

                // the current page
                this.drawPage(graphics, this.pageIndex, left + PAGE_TEXT_X_LEFT, top + PAGE_TEXT_Y);

                // page indicator, centered between the two page buttons
                if (!this.pages.isEmpty()) {
                        Component indicator = Component.translatable("guide.modular_dreams.book.page",
                                        Math.min(this.pageIndex + 1, this.pages.size()), this.pages.size());
                        int width = this.font.width(indicator);
                        graphics.text(this.font, indicator, left + PAGE_TEXT_X_LEFT + (TEXT_WIDTH - width) / 2,
                                        top + PAGE_BUTTON_Y + 2, TAB_TEXT_COLOR, false);
                }

                this.drawTabs(graphics, mouseX, mouseY);
        }

        private void drawPage(GuiGraphicsExtractor graphics, int page, int x, int y) {
                if (page < 0 || page >= this.pages.size()) {
                        return;
                }
                int lineY = y;
                for (Line line : this.pages.get(page)) {
                        line.renderer().render(graphics, x, lineY);
                        lineY += line.height();
                }
        }

        private void drawTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
                int left = this.bookLeft();
                int top = this.bookTop();
                for (int i = 0; i < this.chapters.size(); i++) {
                        int x = left + IMAGE_WIDTH + 2;
                        int y = top + 8 + i * (TAB_HEIGHT + 2);
                        boolean selected = i == this.chapterIndex;
                        boolean hovered = mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y
                                        && mouseY < y + TAB_HEIGHT;
                        graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, TAB_BORDER);
                        graphics.fill(x + 1, y + 1, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1,
                                        selected ? TAB_FILL_SELECTED : (hovered ? TAB_FILL_SELECTED : TAB_FILL));
                        ItemStack icon = this.chapters.get(i).icon();
                        graphics.item(icon, x + (TAB_WIDTH - 16) / 2, y + (TAB_HEIGHT - 16) / 2);
                        if (hovered) {
                                graphics.setTooltipForNextFrame(this.font, this.chapters.get(i).title(), mouseX, mouseY);
                        }
                }
        }
}
