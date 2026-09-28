package com.modulardreams.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.function.Supplier;

import net.minecraft.world.item.crafting.Ingredient;

import com.modulardreams.ModularDreams;
import com.modulardreams.stats.ModTraits;

/**
 * All materials of the Modular Dreams overhaul (step 1), with Tinkers'
 * Construct 3.12.1 stats where TiC defines them.
 *
 * <p>Sources (see download/TiC3-Stat-Formulas-1.20.1.md and the TiC 3.12.1
 * data dump this project was built from):
 * <ul>
 *   <li><b>Heads copied verbatim from TiC 3.12:</b> wood 60/2.0/0.0,
 *       flint 85/3.5/1.25, bone 100/2.5/1.25, copper 210/5.0/0.5,
 *       iron 250/6.0/2.0 (durability / mining speed / attack bonus).</li>
 *   <li><b>Handles from TiC 3 percentages</b> (multiplier = 1 + value):
 *       wood 1.0, flint 0.85, bone 0.75, copper 0.8, iron 1.1. TiC 3 handles
 *       add no flat durability, so the flat bonus is 0 for those.</li>
 *   <li><b>Bindings are statless in TiC 3</b> - bonus 0 for the shared
 *       materials. The binding-only materials (leather, vine, string, slime)
 *       keep a small flat bonus so they have a purpose: slime's 50 comes from
 *       its TiC "slime" stat, the others follow TiC 1.12 extras.</li>
 *   <li><b>Fallbacks where TiC 3 has no material:</b> stone keeps the
 *       TiC 1.12 / Legacy's Construct stats (120/4.0/3.0, handle 0.5),
 *       gold heads use vanilla gold-tool stats (32/12.0/0.0, wood tier),
 *       blaze and breeze rods use iron-like handles per design.</li>
 *   <li><b>Rods are HANDLE-ONLY</b> (build 18): blaze and breeze rods can
 *       no longer be used as bindings - they register without the binding
 *       role. All material tints use the original build-17 palette
 *       (restored in build 20).</li>
 *   <li>All traits are the EMPTY placeholder until the traits milestone.</li>
 * </ul>
 */
public class ModMaterials {

        private static final Map<String, ModularMaterial> BY_ID = new HashMap<>();
        private static final List<ModularMaterial> MATERIALS = new ArrayList<>();
        private static final List<ModularMaterial> HEAD_MATERIALS = new ArrayList<>();
        private static final List<ModularMaterial> BINDING_MATERIALS = new ArrayList<>();
        private static final List<ModularMaterial> HANDLE_MATERIALS = new ArrayList<>();

        private static final List<ModTraits> PLACEHOLDER = List.of(ModTraits.EMPTY);

        /** Repair items for bone-headed tools (no vanilla tag exists). */
        public static final TagKey<Item> REPAIRS_BONE_TOOLS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        ModularDreams.id("repairs_bone_tools"));

        /** Repair items for flint-headed tools (no vanilla tag exists). */
        public static final TagKey<Item> REPAIRS_FLINT_TOOLS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        ModularDreams.id("repairs_flint_tools"));

        /**
         * Ingredient for a tag, resolving it whenever the tags are bound
         * (in game) and falling back to an unresolved named set during
         * datagen (where vanilla tags never bind).
         */
        public static Ingredient ingredientOfTag(TagKey<Item> tag) {
                try {
                        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
                } catch (IllegalStateException e) {
                        return Ingredient.of(net.minecraft.core.HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag));
                }
        }

        // ------------------------------------------------------------------
        // registration helpers
        // ------------------------------------------------------------------

        /** Full material (head + binding + handle). */
        private static void full(String id, int color, Supplier<Ingredient> crafting, TagKey<Item> repair, MaterialTier tier,
                        int dur, float speed, float atk, int ench, float handleMod, int extraDur) {
                register(new ModularMaterial(id, color, crafting, repair, tier, dur, speed, atk, ench,
                                handleMod, 0, extraDur, PLACEHOLDER, false), true);
        }

        /** Head + binding only (no handle variant). */
        private static void headBinding(String id, int color, Supplier<Ingredient> crafting, TagKey<Item> repair,
                        MaterialTier tier, int dur, float speed, float atk, int ench, int extraDur) {
                register(new ModularMaterial(id, color, crafting, repair, tier, dur, speed, atk, ench,
                                1.0F, 0, extraDur, PLACEHOLDER, false), true);
        }

        /** Handle only (no head, NO binding variant - e.g. blaze/breeze rods). */
        private static void handleOnly(String id, int color, Supplier<Ingredient> crafting, float handleMod, int extraDur) {
                register(new ModularMaterial(id, color, crafting, null, MaterialTier.WOOD, 0, 0.0F, 0.0F, 0,
                                handleMod, 0, extraDur, PLACEHOLDER, false), false);
        }

        /** Binding only. */
        private static void binding(String id, int color, Supplier<Ingredient> crafting, int extraDur) {
                register(new ModularMaterial(id, color, crafting, null, MaterialTier.WOOD, 0, 0.0F, 0.0F, 0,
                                1.0F, 0, extraDur, PLACEHOLDER, false), true);
        }

        private static void register(ModularMaterial mat, boolean canBind) {
                BY_ID.put(mat.id(), mat);
                MATERIALS.add(mat);
                // head materials need real head stats
                if (mat.durability() > 0) {
                        HEAD_MATERIALS.add(mat);
                }
                // bindings are opt-in (build 18): blaze/breeze rods are HANDLE-ONLY
                if (canBind) {
                        BINDING_MATERIALS.add(mat);
                }
                // handle materials: wood/stone/flint/bone/blaze/breeze per design, plus metals
                if (mat.handleModifier() != 1.0F || mat.durability() > 0 || mat.id().endsWith("rod")) {
                        HANDLE_MATERIALS.add(mat);
                }
        }

        // ------------------------------------------------------------------

        public static void initialize() {
                // --- TiC 3.12 heads + handles, LC extras for bindings ---
                full("wood", 0x7A5A36, () -> ingredientOfTag(ItemTags.PLANKS),
                                ItemTags.PLANKS, MaterialTier.WOOD, 60, 2.0F, 0.0F, 15, 1.0F, 15);
                full("stone", 0x7F7F7F,
                                () -> ingredientOfTag(ItemTags.STONE_TOOL_MATERIALS),
                                ItemTags.STONE_TOOL_MATERIALS, MaterialTier.STONE, 120, 4.0F, 3.0F, 5, 0.5F, 20);
                full("flint", 0x4A4A52, () -> Ingredient.of(Items.FLINT), REPAIRS_FLINT_TOOLS,
                                MaterialTier.STONE, 85, 3.5F, 1.25F, 8, 0.85F, 40);
                full("bone", 0xE9E7DC, () -> Ingredient.of(Items.BONE), REPAIRS_BONE_TOOLS,
                                MaterialTier.STONE, 100, 2.5F, 1.25F, 8, 0.75F, 65);
                full("copper", 0xE77C56, () -> Ingredient.of(Items.COPPER_INGOT), ItemTags.COPPER_TOOL_MATERIALS,
                                MaterialTier.COPPER, 210, 5.0F, 0.5F, 13, 0.8F, 50);
                full("iron", 0xD8D8D8, () -> Ingredient.of(Items.IRON_INGOT), ItemTags.IRON_TOOL_MATERIALS,
                                MaterialTier.IRON, 250, 6.0F, 2.0F, 14, 1.1F, 50);
                full("gold", 0xF9DE4B, () -> Ingredient.of(Items.GOLD_INGOT), ItemTags.GOLD_TOOL_MATERIALS,
                                MaterialTier.GOLD, 32, 12.0F, 0.0F, 22, 1.0F, 100);

                // --- binding-only materials (Part Builder bindings) ---
                binding("leather", 0xA0683F, () -> Ingredient.of(Items.LEATHER), 30);
                binding("vine", 0x4C7A28, () -> Ingredient.of(Items.VINE), 20);
                binding("string", 0xE6E2D3, () -> Ingredient.of(Items.STRING), 25);
                binding("slime", 0x6FC356, () -> Ingredient.of(Items.SLIME_BALL), 50);

                // --- handle-only rods (build 18: no binding role, original tints) ---
                handleOnly("blaze_rod", 0xFDB02F, () -> Ingredient.of(Items.BLAZE_ROD), 1.1F, 0);
                handleOnly("breeze_rod", 0x8FB6C8, () -> Ingredient.of(Items.BREEZE_ROD), 1.1F, 0);
        }

        // ------------------------------------------------------------------

        public static Optional<ModularMaterial> byId(String id) {
                return Optional.ofNullable(BY_ID.get(id));
        }

        public static ModularMaterial getOrThrow(String id) {
                ModularMaterial mat = BY_ID.get(id);
                if (mat == null) {
                        throw new IllegalStateException("Unknown modular material: " + id);
                }
                return mat;
        }

        public static List<ModularMaterial> materials() {
                return List.copyOf(MATERIALS);
        }

        public static List<ModularMaterial> headMaterials() {
                return List.copyOf(HEAD_MATERIALS);
        }

        public static List<ModularMaterial> bindingMaterials() {
                return List.copyOf(BINDING_MATERIALS);
        }

        public static List<ModularMaterial> handleMaterials() {
                return List.copyOf(HANDLE_MATERIALS);
        }

        /** Materials that can be melted into parts (copper, iron, gold). */
        public static boolean isMeltableMetal(String id) {
                return id.equals("copper") || id.equals("iron") || id.equals("gold");
        }
}
