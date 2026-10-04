package com.modulardreams.client.book;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * Builds the chapters of the guide book ("Materials & You") live from the
 * registries, so the documentation always matches the mod.
 *
 * The guide is a Tinkers'-style custom GUI (see {@link GuideBookScreen}):
 * chapters are flowing documents; the screen paginates them automatically.
 */
public final class BookContent {

        private BookContent() {}

        // ------------------------------------------------------------------ element model

        public sealed interface Element permits Heading, Paragraph, BookContent.ItemLine, BookContent.Bullets,
                        BookContent.Spacer {
        }

        public record Heading(Component text) implements Element {
        }

        public record Paragraph(Component text) implements Element {
        }

        /** An item icon with text next to it (used for modifier costs and more). */
        public record ItemLine(ItemStack icon, Component text) implements Element {
        }

        public record Bullets(List<Component> lines) implements Element {
        }

        public record Spacer(int pixels) implements Element {
        }

        public record Chapter(Component title, ItemStack icon, List<Element> elements) {
        }

        // ------------------------------------------------------------------ helpers

        private static MutableComponent trans(String key) {
                return Component.translatable(key);
        }

        private static MutableComponent gray(String key) {
                return Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY);
        }

        private static MutableComponent statLine(String labelKey, String value) {
                return Component.translatable(labelKey).withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal(" " + value));
        }

        private static MutableComponent bullet(Component text) {
                return Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY).append(text);
        }

        private static List<Component> traitBullets(ModularMaterial material) {
                List<Component> list = new ArrayList<>();
                for (ModTraits trait : material.traits()) {
                        list.add(bullet(Component.translatable(trait.nameKey()).withStyle(ChatFormatting.DARK_BLUE)
                                        .append(Component.literal(": "))
                                        .append(Component.translatable(trait.descriptionKey()))));
                }
                return list;
        }

        private static ItemStack partIcon(PartType part, String materialId) {
                for (ModularMaterial material : part.allowedMaterials()) {
                        if (material.id().equals(materialId)) {
                                return new ItemStack(ModPartItems.get(part, material));
                        }
                }
                return new ItemStack(Items.IRON_INGOT);
        }

        private static String trim(float f) {
                return f == Math.floor(f) ? String.valueOf((int) f) : String.valueOf(f);
        }

        // ------------------------------------------------------------------ chapters

        public static List<Chapter> build() {
                List<Chapter> chapters = new ArrayList<>();
                chapters.add(introChapter());
                chapters.add(startedChapter());
                chapters.add(moldsChapter());
                chapters.add(materialsChapter());
                chapters.add(toolsChapter());
                chapters.add(tipsChapter());
                return List.copyOf(chapters);
        }

        private static Chapter introChapter() {
                ItemStack guide = ModItems.byName("modular_guidebook").map(ItemStack::new)
                                .orElse(new ItemStack(Items.BOOK));
                List<Element> elements = new ArrayList<>();
                elements.add(new Spacer(6));
                elements.add(new Heading(trans("guide.modular_dreams.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.intro.1")));
                elements.add(new Spacer(4));
                elements.add(new Paragraph(gray("guide.modular_dreams.intro.2")));
                elements.add(new Paragraph(gray("guide.modular_dreams.intro.3")));
                elements.add(new Heading(trans("guide.modular_dreams.book.flow.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.flow.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.flow.2")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.flow.3")));
                elements.add(new Heading(trans("guide.modular_dreams.intro.philosophy.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.intro.philosophy.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.intro.philosophy.2")));
                elements.add(new Heading(trans("guide.modular_dreams.intro.parts.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.intro.parts.1")));
                List<Component> parts = new ArrayList<>();
                for (PartType part : PartType.values()) {
                        parts.add(bullet(trans(part.translationKey())));
                }
                elements.add(new Bullets(parts));
                return new Chapter(trans("guide.modular_dreams.book.tab.intro"), guide, elements);
        }

        private static Chapter startedChapter() {
                List<Element> elements = new ArrayList<>();
                elements.add(new Heading(trans("guide.modular_dreams.book.started.partbuilder.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.started.partbuilder.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.started.partbuilder.2")));
                elements.add(new Paragraph(trans("guide.modular_dreams.book.started.partbuilder.3")));
                elements.add(new Paragraph(gray("guide.modular_dreams.book.started.partbuilder.4")));
                elements.add(new Heading(trans("guide.modular_dreams.started.2.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.started.2.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.started.2.2")));
                elements.add(new Paragraph(gray("guide.modular_dreams.started.2.3")));
                return new Chapter(trans("guide.modular_dreams.book.tab.started"),
                                partIcon(PartType.PICKAXE_HEAD, "iron"), elements);
        }

        private static Chapter moldsChapter() {
                List<Element> elements = new ArrayList<>();
                elements.add(new Heading(trans("guide.modular_dreams.molds.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.2")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.3")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.4")));
                elements.add(new Heading(trans("guide.modular_dreams.molds.melting.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.melting.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.melting.2")));
                elements.add(new Paragraph(trans("guide.modular_dreams.molds.melting.3")));
                elements.add(new Paragraph(gray("guide.modular_dreams.molds.melting.4")));
                return new Chapter(trans("guide.modular_dreams.book.tab.molds"),
                                new ItemStack(com.modulardreams.block.ModBlocks.TERRACOTTA_MOLD.asItem()),
                                elements);
        }

        private static Chapter materialsChapter() {
                List<Element> elements = new ArrayList<>();
                elements.add(new Heading(trans("guide.modular_dreams.materials.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.materials.intro.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.materials.intro.2")));
                for (ModularMaterial material : ModMaterials.materials()) {
                        elements.add(new Heading(trans(material.nameKey())
                                        .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.translatable("guide.modular_dreams.material.tier")
                                                        .withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.literal(" " + material.tier().name().toLowerCase() + ")")
                                                        .withStyle(ChatFormatting.DARK_GRAY))));
                        if (material.durability() > 0) {
                                elements.add(new Paragraph(statLine("guide.modular_dreams.material.durability",
                                                String.valueOf(material.durability()))));
                                elements.add(new Paragraph(statLine("guide.modular_dreams.material.speed",
                                                trim(material.speed()))));
                                elements.add(new Paragraph(statLine("guide.modular_dreams.material.damage",
                                                "+" + trim(material.attackDamageBonus()))));
                                elements.add(new Paragraph(statLine("guide.modular_dreams.material.enchantability",
                                                String.valueOf(material.enchantmentValue()))));
                        } else {
                                elements.add(new Paragraph(gray("guide.modular_dreams.material.no_head")));
                        }
                        elements.add(new Paragraph(statLine("guide.modular_dreams.material.handle",
                                        trim(material.handleModifier()) + "x / "
                                                        + (material.handleDurabilityBonus() >= 0 ? "+" : "")
                                                        + material.handleDurabilityBonus())));
                        if (material.handleStatBoost() != 1.0F) {
                                // build 21: diamond handle boosts the tool's other stats
                                elements.add(new Paragraph(statLine("guide.modular_dreams.material.stat_boost",
                                                trim(material.handleStatBoost()) + "x mining speed & attack")));
                        }
                        elements.add(new Paragraph(statLine("guide.modular_dreams.material.binding",
                                        (material.extraDurabilityBonus() >= 0 ? "+" : "")
                                                        + material.extraDurabilityBonus())));
                        List<Component> traits = traitBullets(material);
                        if (!traits.isEmpty()) {
                                elements.add(new Bullets(traits));
                        }
                }
                return new Chapter(trans("guide.modular_dreams.book.tab.materials"),
                                new ItemStack(Items.IRON_INGOT), elements);
        }

        private static Chapter toolsChapter() {
                List<Element> elements = new ArrayList<>();
                elements.add(new Heading(trans("guide.modular_dreams.tools.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.tools.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.tools.2")));
                elements.add(new Heading(trans("guide.modular_dreams.tools.stats.title")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.durability", "")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.speed", "")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.tier", "")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.damage", "")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.attackspeed", "")));
                elements.add(new Paragraph(statLine("guide.modular_dreams.tools.stats.enchantability", "")));
                elements.add(new Paragraph(gray("guide.modular_dreams.tools.stats.note")));
                elements.add(new Heading(trans("guide.modular_dreams.tools.spear.title")));
                elements.add(new Paragraph(trans("guide.modular_dreams.tools.spear.1")));
                elements.add(new Paragraph(trans("guide.modular_dreams.tools.spear.2")));
                return new Chapter(trans("guide.modular_dreams.book.tab.tools"),
                                partIcon(PartType.AXE_HEAD, "iron"), elements);
        }

        private static Chapter tipsChapter() {
                List<Element> elements = new ArrayList<>();
                elements.add(new Heading(trans("guide.modular_dreams.tips.title")));
                elements.add(new Bullets(List.of(
                                bullet(trans("guide.modular_dreams.book.tips.b1")),
                                bullet(trans("guide.modular_dreams.book.tips.b2")),
                                bullet(trans("guide.modular_dreams.book.tips.b3")),
                                bullet(trans("guide.modular_dreams.book.tips.b4")),
                                bullet(trans("guide.modular_dreams.book.tips.b5")))));
                elements.add(new Spacer(4));
                elements.add(new Bullets(List.of(
                                bullet(trans("guide.modular_dreams.book.tips.b6")),
                                bullet(trans("guide.modular_dreams.book.tips.b7")),
                                bullet(trans("guide.modular_dreams.book.tips.b8")),
                                bullet(trans("guide.modular_dreams.book.tips.b9")),
                                bullet(trans("guide.modular_dreams.book.tips.b10")))));
                return new Chapter(trans("guide.modular_dreams.book.tab.tips"),
                                new ItemStack(Items.DIAMOND), elements);
        }
}
