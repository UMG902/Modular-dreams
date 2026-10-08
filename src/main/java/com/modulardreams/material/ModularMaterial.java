package com.modulardreams.material;

import java.util.function.Supplier;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One material of Modular Dreams v2.
 *
 * <p>Head stats follow Tinkers' Construct 3 where TiC defines the material
 * (wood 60/2.0/0.0, copper 210/5.0/0.5, iron 250/6.0/2.0, ...); gold uses
 * vanilla gold-tool stats, rose gold sits between iron and diamond, and
 * netherite tops everything as a full material shaped at the diamond
 * Part Picker.
 *
 * @param id                  machine id, e.g. "rose_gold"
 * @param color               material tint (RGB), used by the creative UI and reserved for cosmetics
 * @param craftingSupplier    ingredient the Part Picker accepts for this material (tags allowed)
 * @param repairTag           items repairing tools whose HEAD is this material (may be null)
 * @param tier                mining tier of tools whose head is this material
 * @param stationTier         Part Picker tier that shapes this material (1..6; 6 = diamond station only)
 * @param durability          head durability
 * @param speed               head mining speed
 * @param attackDamageBonus   head attack bonus
 * @param enchantability      enchantability (from the head)
 * @param handleDurabilityMult durability multiplier while used as the handle
 * @param handleStatBoost     multiplier on the assembled tool's mining speed and attack as handle (1.0 = none)
 * @param fireResistant       whether items built from this material resist fire
 * @param handleOnly          rods: can only be used as handles, never as heads
 */
public record ModularMaterial(
        String id,
        int color,
        Supplier<Ingredient> craftingSupplier,
        TagKey<Item> repairTag,
        MaterialTier tier,
        int stationTier,
        int durability,
        float speed,
        float attackDamageBonus,
        int enchantability,
        float handleDurabilityMult,
        float handleStatBoost,
        boolean fireResistant,
        boolean handleOnly
) {

    /** The ingredient the Part Picker accepts (built lazily: tags bind after init). */
    public Ingredient crafting() {
        return craftingSupplier.get();
    }

    public String nameKey() {
        return "material.modular_dreams." + id;
    }
}
