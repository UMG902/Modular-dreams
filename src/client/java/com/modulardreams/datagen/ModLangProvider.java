package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * English language file of the overhaul build: stations, molds, generic
 * parts, materials (TiC 3 stats), the guide book and tooltips.
 */
public class ModLangProvider extends FabricLanguageProvider {

        protected ModLangProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
                super(output, registryLookup);
        }

        private static String title(String s) {
                return s.substring(0, 1).toUpperCase() + s.substring(1);
        }

        /** Display names for materials (used in tool names and part items). */
        private static String materialName(String id) {
                return switch (id) {
                        case "wood" -> "Wooden";
                        default -> title(id.replace('_', ' '));
                };
        }

        /**
         * Display names for part shapes. The sword's dedicated binding is
         * called the GUARD in the language file only - the definition keeps
         * the {@code sword_binding} id.
         */
        private static String partName(PartType part) {
                return switch (part) {
                        case SWORD_BINDING -> "Sword Guard";
                        default -> title(part.id.replace('_', ' '));
                };
        }

        @Override
        public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
                // creative tab
                builder.add("itemGroup.modular_dreams", "Modular Dreams");

                // station & mold blocks
                // (26.3 BlockItems use the ITEM description id -> both the
                // block.* and the item.* keys are needed)
                builder.add("block.modular_dreams.part_builder", "Part Builder");
                builder.add("block.modular_dreams.assembly_table", "Assembly Table");
                builder.add("block.modular_dreams.clay_mold", "Clay Mold");
                builder.add("block.modular_dreams.terracotta_mold", "Terracotta Mold");
                builder.add("block.modular_dreams.melting_upgrade", "Melting Upgrade");
                builder.add("block.modular_dreams.melting_furnace", "Melting Furnace");
                builder.add("item.modular_dreams.part_builder", "Part Builder");
                builder.add("item.modular_dreams.assembly_table", "Assembly Table");
                builder.add("item.modular_dreams.clay_mold", "Clay Mold");
                builder.add("item.modular_dreams.terracotta_mold", "Terracotta Mold");
                builder.add("item.modular_dreams.melting_upgrade", "Melting Upgrade");
                builder.add("container.modular_dreams.part_builder", "Part Builder");
                builder.add("container.modular_dreams.assembly_table", "Assembly Table");
                builder.add("container.modular_dreams.melting_upgrade", "Melting Upgrade");
                builder.add("container.modular_dreams.need_material", "Not enough material!");
                builder.add("container.modular_dreams.cost", "Cost: %s material(s)");
                builder.add("container.modular_dreams.select_part", "Select a part shape:");

                // modular equipment
                builder.add("item.modular_dreams.modular_pickaxe", "Modular Pickaxe");
                builder.add("item.modular_dreams.modular_pickaxe.named", "%s Modular Pickaxe");
                builder.add("item.modular_dreams.modular_axe", "Modular Axe");
                builder.add("item.modular_dreams.modular_axe.named", "%s Modular Axe");
                builder.add("item.modular_dreams.modular_shovel", "Modular Shovel");
                builder.add("item.modular_dreams.modular_shovel.named", "%s Modular Shovel");
                builder.add("item.modular_dreams.modular_hoe", "Modular Hoe");
                builder.add("item.modular_dreams.modular_hoe.named", "%s Modular Hoe");
                builder.add("item.modular_dreams.modular_sword", "Modular Sword");
                builder.add("item.modular_dreams.modular_sword.named", "%s Modular Sword");
                builder.add("item.modular_dreams.modular_spear", "Modular Spear");
                builder.add("item.modular_dreams.modular_spear.named", "%s Modular Spear");
                builder.add("item.modular_dreams.modular_guidebook", "Materials & You (Guide Book)");

                // molds (both are the block items of their block)
                builder.add("tooltip.modular_dreams.mold_shape", "Shaped into: %s");
                builder.add("tooltip.modular_dreams.mold_unshaped", "Unshaped - right-click a placed CLAY mold with a part");
                builder.add("tooltip.modular_dreams.mold_uses", "Uses left: %s");

                // guide book page indicator (bottom of the page, between the arrows)
                builder.add("guide.modular_dreams.book.page", "Page %s of %s");

                // part type names (one universal handle + one universal
                // binding + dedicated sword/spear bindings, per-tool heads)
                for (PartType part : PartType.values()) {
                        builder.add(part.translationKey(), partName(part));
                }
                builder.add("part.modular_dreams.material_line", "Material: %s");

                // material names
                for (ModularMaterial material : ModMaterials.materials()) {
                        builder.add(material.nameKey(), materialName(material.id()));
                }

                // part items: "<Material> <Part>" (e.g. "Iron Pickaxe Head")
                for (PartType part : PartType.values()) {
                        for (ModularMaterial material : part.allowedMaterials()) {
                                builder.add("item.modular_dreams." + material.id() + "_" + part.id,
                                                materialName(material.id()) + " " + partName(part));
                        }
                }

                // traits (only EMPTY is used in the current overhaul step)
                addTrait(builder, ModTraits.EMPTY, "Empty", "A placeholder - real traits come with the next update.");

                // tooltips
                builder.add("tooltip.modular_dreams.parts", "Parts:");
                builder.add("tooltip.modular_dreams.modifiers", "Modifiers:");
                builder.add("tooltip.modular_dreams.durability", "Durability: %s / %s");

                // equipment type names (used in the guide)
                builder.add("equipment.modular_dreams.pickaxe", "Pickaxe");
                builder.add("equipment.modular_dreams.axe", "Axe");
                builder.add("equipment.modular_dreams.shovel", "Shovel");
                builder.add("equipment.modular_dreams.hoe", "Hoe");
                builder.add("equipment.modular_dreams.sword", "Sword");
                builder.add("equipment.modular_dreams.spear", "Spear");

                // advancements
                builder.add("advancements.modular_dreams.root.title", "Modular Dreams");
                builder.add("advancements.modular_dreams.root.description",
                                "Build equipment from parts instead of replacing it. Read Materials & You to get started!");
                builder.add("advancements.modular_dreams.first_tool.title", "It's Alive!");
                builder.add("advancements.modular_dreams.first_tool.description",
                                "Assemble your first modular tool from a handle, a binding and a head.");
                builder.add("advancements.modular_dreams.modded.title", "Fully Loaded");
                builder.add("advancements.modular_dreams.modded.description",
                                "Apply 3 different modifiers to a single piece of equipment.");

                addGuideText(builder);
        }

        private void addTrait(TranslationBuilder builder, ModTraits trait, String name, String desc) {
                builder.add(trait.nameKey(), name);
                builder.add(trait.descriptionKey(), desc);
        }

        private void addGuideText(TranslationBuilder builder) {
                builder.add("guide.modular_dreams.title", "Materials & You");
                builder.add("guide.modular_dreams.intro.1",
                                "Why throw away a worn-out tool when you can repair and upgrade it? Modular Dreams lets you build vanilla tools and weapons from generic parts made of vanilla materials.");
                builder.add("guide.modular_dreams.intro.2",
                                "Keep this book handy - it documents every station, material and part.");
                builder.add("guide.modular_dreams.intro.3", "- The Modular Dreams Team");
                builder.add("guide.modular_dreams.book.flow.title", "The Flow");
                builder.add("guide.modular_dreams.book.flow.1",
                                "1. PART BUILDER - shape raw materials into generic parts. All woods make wooden parts; there is no oak or cherry, just wood.");
                builder.add("guide.modular_dreams.book.flow.2",
                                "2. ASSEMBLY TABLE - combine a handle, a binding and a head of one tool type into the finished tool.");
                builder.add("guide.modular_dreams.book.flow.3",
                                "3. MELTING - copper, iron and gold parts cannot be carved; melt ingots in a furnace with a Melting Upgrade and cast them in molds.");
                builder.add("guide.modular_dreams.intro.philosophy.title", "Philosophy");
                builder.add("guide.modular_dreams.intro.philosophy.1",
                                "Modular tools cost a little more material than their vanilla counterparts, but they are an investment: repair them forever and re-purpose them with new parts.");
                builder.add("guide.modular_dreams.intro.philosophy.2",
                                "Stats are inherited from the materials of each part. An iron head mines like iron; a bone handle trades durability for speed. Mix and match to shape the tool you need.");
                builder.add("guide.modular_dreams.intro.parts.title", "Tool Parts");
                builder.add("guide.modular_dreams.intro.parts.1",
                                "Every tool is assembled from exactly three parts - a head, a binding and a handle. There is ONE universal handle and ONE universal binding that fit every tool of their kind, including the sword and the spear; those two simply render with their own special shapes (the sword's binding is called the Guard)." );
                builder.add("guide.modular_dreams.book.started.partbuilder.title", "The Part Builder");
                builder.add("guide.modular_dreams.book.started.partbuilder.1",
                                "The PART BUILDER works like a stonecutter: drop a material into the input slot, click the part shape you want from the grid, and take it from the output slot.");
                builder.add("guide.modular_dreams.book.started.partbuilder.2",
                                "Any planks make generic wooden parts; cobblestone makes stone parts. Flint, bone, leather, vine, string, slime and blaze/breeze rods have their own parts.");
                builder.add("guide.modular_dreams.book.started.partbuilder.3",
                                "Each shape costs a few units of material - a pickaxe head takes 3, a binding 2, a shovel head 1.");
                builder.add("guide.modular_dreams.book.started.partbuilder.4",
                                "Metals cannot be carved: copper, iron and gold parts must be melted (see the Molds chapter).");
                builder.add("guide.modular_dreams.started.2.title", "The Assembly Table");
                builder.add("guide.modular_dreams.started.2.1",
                                "The ASSEMBLY TABLE has three input slots in fixed order: HEAD on top, BINDING in the middle, HANDLE at the bottom.");
                builder.add("guide.modular_dreams.started.2.2",
                                "Place the three parts of one tool type into their slots and the assembled tool appears on the right. The parts stay in the table when you close it.");
                builder.add("guide.modular_dreams.started.2.3",
                                "The head decides the tool type. The universal handle and the universal binding fit every tool - even a sword or a spear - and each tool just renders its own special part shapes (the sword's binding is the Guard).");
                builder.add("guide.modular_dreams.molds.title", "Clay Molds");
                builder.add("guide.modular_dreams.molds.1",
                                "Craft a CLAY MOLD from four clay balls and place it on the ground - both molds sit flat like a cutting board.");
                builder.add("guide.modular_dreams.molds.2",
                                "Right-click the placed CLAY mold with any part to press that shape into the clay. The part is only a template - it is not consumed. Changed your mind? Use a different part on the mold to RE-SHAPE it as often as you like. The mold surface is CUT OUT where the part would sit, leaving a real mold cavity - in the world and on the item. Terracotta molds cannot be shaped by hand: they keep the shape they were baked with.");
                builder.add("guide.modular_dreams.molds.3",
                                "Break the shaped mold to pick it up, then cook it in a furnace: clay molds become TERRACOTTA molds of the same shape. The terracotta mold is a block too - you can place it down just like the clay one.");
                builder.add("guide.modular_dreams.molds.4",
                                "A clay mold can be used directly for one casting, but it breaks afterwards. A terracotta mold survives three casts.");
                builder.add("guide.modular_dreams.molds.melting.title", "The Melting Upgrade");
                builder.add("guide.modular_dreams.molds.melting.1",
                                "Place a MELTING UPGRADE directly UNDERNEATH a furnace, then insert a shaped mold: right-click the upgrade with the mold, or open it (right-click with anything else) and drop the mold into the bottom slot.");
                builder.add("guide.modular_dreams.molds.melting.2",
                                "Put copper, iron or gold ingots into the furnace's top slot. With a mold inserted, the metal melts down instead of sitting idle.");
                builder.add("guide.modular_dreams.molds.melting.3",
                                "Each melted ingot fills the mold: the finished part appears in the MELTING UPGRADE's GUI, in the slot above the mold. The mold loses one use per cast.");
                builder.add("guide.modular_dreams.molds.melting.4",
                                "Metals work for every head, binding and handle. Without a mold nothing melts - the ingots simply wait.");
                builder.add("guide.modular_dreams.materials.title", "Materials");
                builder.add("guide.modular_dreams.materials.intro.1",
                                "Head stats are copied from Tinkers' Construct. Handles change durability by a percentage, bindings add a small flat bonus.");
                builder.add("guide.modular_dreams.materials.intro.2",
                                "Materials without head stats (leather, vine, string, slime) are binding-only; blaze and breeze rods are handle-only with iron-like handles.");
                builder.add("guide.modular_dreams.material.tier", "tier");
                builder.add("guide.modular_dreams.material.no_head", "Binding only - cannot be used as a head.");
                builder.add("guide.modular_dreams.material.durability", "Head durability:");
                builder.add("guide.modular_dreams.material.speed", "Head mining speed:");
                builder.add("guide.modular_dreams.material.damage", "Head attack:");
                builder.add("guide.modular_dreams.material.enchantability", "Enchantability:");
                builder.add("guide.modular_dreams.material.handle", "Handle (multiplier / bonus):");
                builder.add("guide.modular_dreams.material.binding", "Binding bonus:");
                builder.add("guide.modular_dreams.tools.title", "Tools & Stats");
                builder.add("guide.modular_dreams.tools.1",
                                "Tool durability = (head durability + binding bonus) x handle multiplier + handle bonus, then x the tool's own multiplier.");
                builder.add("guide.modular_dreams.tools.2",
                                "Mining speed and mining tier come from the head; attack damage comes from the head plus the tool type.");
                builder.add("guide.modular_dreams.tools.stats.title", "Stat Lines");
                builder.add("guide.modular_dreams.tools.stats.durability", "Durability:");
                builder.add("guide.modular_dreams.tools.stats.speed", "Mining speed:");
                builder.add("guide.modular_dreams.tools.stats.tier", "Mining tier:");
                builder.add("guide.modular_dreams.tools.stats.damage", "Attack damage:");
                builder.add("guide.modular_dreams.tools.stats.attackspeed", "Attack speed:");
                builder.add("guide.modular_dreams.tools.stats.enchantability", "Enchantability:");
                builder.add("guide.modular_dreams.tools.stats.note",
                                "Every assembled tool shows its parts, materials and exact stats in the tooltip.");
                builder.add("guide.modular_dreams.tools.spear.title", "The Spear");
                builder.add("guide.modular_dreams.tools.spear.1",
                                "The modular spear reaches further than any vanilla weapon - charge the attack to stab from range.");
                builder.add("guide.modular_dreams.tools.spear.2",
                                "It can also be thrown by holding use, trading the tool itself for a long-range hit.");
                builder.add("guide.modular_dreams.book.tab.intro", "Introduction");
                builder.add("guide.modular_dreams.book.tab.started", "Getting Started");
                builder.add("guide.modular_dreams.book.tab.molds", "Molds & Melting");
                builder.add("guide.modular_dreams.book.tab.materials", "Materials");
                builder.add("guide.modular_dreams.book.tab.tools", "Tools & Stats");
                builder.add("guide.modular_dreams.book.tab.tips", "Tips");
                builder.add("guide.modular_dreams.tips.title", "Tips & Tricks");
                builder.add("guide.modular_dreams.book.tips.b1",
                                "A flint head is cheaper than bone and nearly as fast - great for early tools.");
                builder.add("guide.modular_dreams.book.tips.b2",
                                "An iron handle multiplies durability by 1.1; a wooden handle adds a small bonus and keeps things cheap.");
                builder.add("guide.modular_dreams.book.tips.b3",
                                "Blaze rod handles have iron-like handles - useful when wood is scarce, like in the Nether.");
                builder.add("guide.modular_dreams.book.tips.b4",
                                "Gold parts mine extremely fast but break quickly - pair a gold head with an iron handle to soften the blow.");
                builder.add("guide.modular_dreams.book.tips.b5",
                                "Terracotta molds last three casts - bake your clay molds before big casting sessions.");
                builder.add("guide.modular_dreams.book.tips.b6",
                                "Repair your tools with the material of their head: iron ingots for iron, planks for wood.");
                builder.add("guide.modular_dreams.book.tips.b7",
                                "Parts of the same material stack neatly in chests - generic parts mean fewer item types.");
                builder.add("guide.modular_dreams.book.tips.b8",
                                "The melting upgrade works while the furnace does other things - keep it fed with ingots.");
                builder.add("guide.modular_dreams.book.tips.b9",
                                "Modifiers and real material traits return in a future update - the Empty trait is a placeholder.");
                builder.add("guide.modular_dreams.book.tips.b10",
                                "Diamond and netherite are not part of this build yet - they arrive with a later update.");
        }
}
