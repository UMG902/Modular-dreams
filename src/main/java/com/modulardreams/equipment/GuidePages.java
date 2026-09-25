package com.modulardreams.equipment;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;

import net.minecraft.world.item.equipment.ArmorType;

import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * Builds the pages of the guide book dynamically from the registries, so the
 * documentation always matches the actual materials and modifiers.
 */
final class GuidePages {

        private GuidePages() {}

        private static void add(List<Filterable<Component>> pages, Component page) {
                pages.add(Filterable.passThrough(page));
        }

        private static MutableComponent key(String key) {
                return Component.translatable(key);
        }

        private static MutableComponent title(String key) {
                return key(key).withStyle(ChatFormatting.DARK_BLUE).withStyle(ChatFormatting.BOLD);
        }

        private static MutableComponent label(String key) {
                return key(key).withStyle(ChatFormatting.GRAY);
        }

        private static MutableComponent value(String text) {
                return Component.literal(text).withStyle(ChatFormatting.BLACK);
        }

        private static MutableComponent traitLine(ModTraits trait) {
                return Component.literal("").withStyle(ChatFormatting.DARK_GRAY)
                                .append(key(trait.nameKey()).withStyle(ChatFormatting.DARK_BLUE))
                                .append(Component.literal(": "))
                                .append(key(trait.descriptionKey()));
        }

        // ------------------------------------------------------------------ intro

        static void intro(List<Filterable<Component>> pages) {
                add(pages, Component.literal("").withStyle(ChatFormatting.BLACK)
                                .append(title("guide.modular_dreams.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.2").withStyle(ChatFormatting.DARK_GRAY))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.3").withStyle(ChatFormatting.GRAY)));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.intro.philosophy.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.philosophy.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.philosophy.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.intro.parts.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.parts.1"))
                                .append(Component.literal("\n"))
                                .append(partLine(PartType.PICKAXE_HEAD))
                                .append(partLine(PartType.AXE_HEAD))
                                .append(partLine(PartType.SHOVEL_HEAD))
                                .append(partLine(PartType.HOE_HEAD))
                                .append(partLine(PartType.SWORD_BLADE))
                                .append(partLine(PartType.SWORD_GUARD))
                                .append(partLine(PartType.MACE_HEAD))
                                .append(partLine(PartType.SPEAR_HEAD))
                                .append(partLine(PartType.BINDING))
                                .append(partLine(PartType.HANDLE)));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.intro.armor_parts.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.intro.armor_parts.1"))
                                .append(Component.literal("\n"))
                                .append(partLine(PartType.PLATE))
                                .append(partLine(PartType.LINING)));
        }

        private static MutableComponent partLine(PartType part) {
                return Component.literal("\u2022 ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(key(part.translationKey()).withStyle(ChatFormatting.BLACK))
                                .append(Component.literal("\n"));
        }

        // ------------------------------------------------------------------ getting started

        static void gettingStarted(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.started.1.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.1.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.1.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.started.2.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.2.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.2.2"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.2.3")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.started.3.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.3.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.3.2"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.started.3.3")));
        }

        // ------------------------------------------------------------------ tools

        static void toolPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.tools.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.tools.stats.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.stats.durability"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.tools.stats.speed"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.tools.stats.tier"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.tools.stats.damage"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.tools.stats.attackspeed"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.tools.stats.enchantability"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.stats.note").withStyle(ChatFormatting.DARK_GRAY)));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.tools.mace.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.mace.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.mace.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.tools.spear.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.spear.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tools.spear.2")));
        }

        // ------------------------------------------------------------------ materials

        static void materialPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.materials.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.materials.intro.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.materials.intro.2")));
                for (ModularMaterial material : ModMaterials.toolMaterials()) {
                        add(pages, toolMaterialPage(material));
                }
        }

        private static Component toolMaterialPage(ModularMaterial material) {
                MutableComponent page = Component.literal("")
                                .append(key(material.nameKey()).withStyle(ChatFormatting.DARK_BLUE).withStyle(ChatFormatting.BOLD))
                                .append(Component.literal("\n"))
                                .append(tierLabel(material.tier()))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.durability"))
                                .append(value(" " + material.durability()))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.speed"))
                                .append(value(" " + trim(material.speed())))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.damage"))
                                .append(value(" +" + trim(material.attackDamageBonus())))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.enchantability"))
                                .append(value(" " + material.enchantmentValue()))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.handle"))
                                .append(value(" +" + Math.round(material.handleDurability() * 100) + "%"))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.material.binding"))
                                .append(value(" +" + Math.round(material.bindingDurability() * 100) + "%"))
                                .append(Component.literal("\n"));
                if (!material.traits().isEmpty()) {
                        page.append(Component.literal("\n"));
                        for (ModTraits trait : material.traits()) {
                                page.append(traitLine(trait)).append(Component.literal("\n"));
                        }
                }
                return page;
        }

        private static MutableComponent tierLabel(MaterialTier tier) {
                return Component.literal("")
                                .append(label("guide.modular_dreams.material.tier"))
                                .append(value(" " + tier.name().toLowerCase()));
        }

        private static String trim(float f) {
                return f == Math.floor(f) ? String.valueOf((int) f) : String.valueOf(f);
        }

        // ------------------------------------------------------------------ armor

        static void armorPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.armor.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.armor.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.armor.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.armor.stats.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.armor.stats.durability"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.armor.stats.defense"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.armor.stats.toughness"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.armor.stats.knockback"))
                                .append(Component.literal("\n"))
                                .append(key("guide.modular_dreams.armor.stats.enchantability"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.armor.stats.note").withStyle(ChatFormatting.DARK_GRAY)));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.armor.plates.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.armor.plates.1")));
                for (ModularMaterial material : ModMaterials.plateMaterials()) {
                        add(pages, platePage(material));
                }
                for (ModularMaterial material : ModMaterials.liningMaterials()) {
                        add(pages, liningPage(material));
                }
        }

        private static Component platePage(ModularMaterial material) {
                ModularMaterial.Plate plate = material.plateOpt().orElseThrow();
                MutableComponent page = Component.literal("")
                                .append(key(material.nameKey()).withStyle(ChatFormatting.DARK_BLUE).withStyle(ChatFormatting.BOLD))
                                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                                .append(key(PartType.PLATE.translationKey()).withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.durability"))
                                .append(value(" x" + plate.durabilityMultiplier()))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.defense"))
                                .append(value(" " + plate.defense().getOrDefault(ArmorType.BOOTS, 0)
                                                + "/" + plate.defense().getOrDefault(ArmorType.LEGGINGS, 0)
                                                + "/" + plate.defense().getOrDefault(ArmorType.CHESTPLATE, 0)
                                                + "/" + plate.defense().getOrDefault(ArmorType.HELMET, 0)))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.toughness"))
                                .append(value(" " + trim(plate.toughness())))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.enchantability"))
                                .append(value(" " + plate.enchantmentValue()))
                                .append(Component.literal("\n"));
                if (!material.traits().isEmpty()) {
                        page.append(Component.literal("\n"));
                        for (ModTraits trait : material.traits()) {
                                page.append(traitLine(trait)).append(Component.literal("\n"));
                        }
                }
                return page;
        }

        private static Component liningPage(ModularMaterial material) {
                ModularMaterial.Lining lining = material.liningOpt().orElseThrow();
                MutableComponent page = Component.literal("")
                                .append(key(material.nameKey()).withStyle(ChatFormatting.DARK_BLUE).withStyle(ChatFormatting.BOLD))
                                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                                .append(key(PartType.LINING.translationKey()).withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.lining_durability"))
                                .append(value(" +" + Math.round(lining.durabilityBonus() * 100) + "%"))
                                .append(Component.literal("\n"))
                                .append(label("guide.modular_dreams.armor.material.lining_enchantability"))
                                .append(value(" +" + lining.enchantmentBonus()))
                                .append(Component.literal("\n"));
                if (!material.traits().isEmpty()) {
                        page.append(Component.literal("\n"));
                        for (ModTraits trait : material.traits()) {
                                page.append(traitLine(trait)).append(Component.literal("\n"));
                        }
                }
                return page;
        }

        // ------------------------------------------------------------------ modifiers

        static void modifierPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.modifiers.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.modifiers.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.modifiers.2")));
                List<Modifier> all = Modifier.all();
                for (int i = 0; i < all.size(); i += 3) {
                        MutableComponent page = Component.literal("")
                                        .append(title("guide.modular_dreams.modifiers.list.title"))
                                        .append(Component.literal("\n\n"));
                        for (int j = i; j < Math.min(i + 3, all.size()); j++) {
                                Modifier modifier = all.get(j);
                                page.append(key(modifier.nameKey()).withStyle(ChatFormatting.DARK_BLUE))
                                                .append(Component.literal(" (x" + modifier.costPerLevel() + ")\n"))
                                                .append(key(modifier.descriptionKey()).withStyle(ChatFormatting.DARK_GRAY))
                                                .append(Component.literal("\n\n"));
                        }
                        add(pages, page);
                }
        }

        // ------------------------------------------------------------------ repairing & tips

        static void repairPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.repair.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.repair.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.repair.2")));
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.repair.advanced.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.repair.3"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.repair.4")));
        }

        static void tipPages(List<Filterable<Component>> pages) {
                add(pages, Component.literal("")
                                .append(title("guide.modular_dreams.tips.title"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tips.1"))
                                .append(Component.literal("\n\n"))
                                .append(key("guide.modular_dreams.tips.2")));
        }
}
