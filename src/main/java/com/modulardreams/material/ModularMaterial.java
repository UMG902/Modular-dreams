package com.modulardreams.material;

import java.util.List;
import java.util.Optional;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

import net.minecraft.world.item.crafting.Ingredient;

import com.modulardreams.stats.ModTraits;

/**
 * Definition of one material inside Modular Dreams (overhaul step 1: tools).
 *
 * <p>Stats follow Tinkers' Construct 3.12 (1.20.1) wherever TiC defines the
 * material: head values are copied verbatim, the TiC handle durability
 * percentage becomes the LC-style handle multiplier ({@code 1 + p}), and
 * TiC's statless bindings keep no bonus - except the materials that ONLY
 * exist as bindings, which keep a small flat bonus so they have a use.
 * Stone has no TiC 3 equivalent (flint covers it), so it keeps the
 * TiC 1.12 / Legacy's Construct values; gold heads use vanilla gold-tool
 * stats; blaze and breeze rods use iron-like handle stats per design.
 *
 * @param id                 machine id, e.g. "iron"
 * @param color              material tint (RGB) applied to grayscale part textures
 * @param crafting           ingredient the Part Builder accepts for this material
 *                           (tags allowed - e.g. any planks build generic wood parts)
 * @param repairTag          items repairing tools whose HEAD is this material
 * @param tier               mining tier of tools whose head is made of this
 * @param durability         head durability (TiC 3: iron = 250, wood = 60)
 * @param speed              head mining speed (TiC 3: iron = 6.0, wood = 2.0)
 * @param attackDamageBonus  head attack stat (TiC 3: iron = 2.0, flint = 1.25)
 * @param enchantmentValue   enchantability contribution (from the head)
 * @param handleModifier     durability multiplier of the tool when used as the
 *                           handle (TiC 3 handle % + 1: iron = 1.1, flint = 0.85)
 * @param handleStatBoost    multiplier applied to the assembled tool's mining
 *                           speed and attack damage when used as the handle
 *                           (1.0 = no boost; diamond = 1.1 - build 21)
 * @param handleDurabilityBonus flat durability added by the handle (TiC 3: always 0)
 * @param extraDurabilityBonus  flat durability added when used as a binding
 * @param traits             material traits applied wherever the material is used
 * @param fireResistant      whether items built from this material resist fire
 */
public record ModularMaterial(
                String id,
                int color,
                Supplier<Ingredient> craftingSupplier,
                TagKey<Item> repairTag,
                MaterialTier tier,
                int durability,
                float speed,
                float attackDamageBonus,
                int enchantmentValue,
                float handleModifier,
                float handleStatBoost,
                int handleDurabilityBonus,
                int extraDurabilityBonus,
                List<ModTraits> traits,
                boolean fireResistant
) {

        /** The ingredient the Part Builder accepts (built lazily: tags bind after init). */
        public Ingredient crafting() {
                return craftingSupplier.get();
        }

        public String nameKey() {
                return "material.modular_dreams." + id;
        }

        public boolean hasTrait(ModTraits trait) {
                return traits.contains(trait);
        }

        public Optional<ModTraits> firstTrait() {
                return traits.isEmpty() ? Optional.empty() : Optional.of(traits.get(0));
        }
}
