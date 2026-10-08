package com.modulardreams.part;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Armor plating parts of Modular Dreams v2 - one plating per armor slot.
 * Plating is the OUTER layer of a future armor piece (the lining is the
 * inner layer); the slot it belongs to is fixed per plating type.
 *
 * <p>Costs are the vanilla armor material costs in units consumed by the
 * Part Picker: helmet 5, chestplate 8, leggings 7, boots 4 - so a modular
 * armor piece costs roughly what the vanilla one does.
 *
 * <p>Design rules (user-directed):
 * <ul>
 *   <li>Platings exist in every full material EXCEPT netherite - netherite
 *       armor is reachable ONLY through the smithing upgrade, and that
 *       upgrade only applies to armor with DIAMOND plating (vanilla parity).</li>
 *   <li>Platings carry no traits and grant no modifiers - the trait of an
 *       armor piece comes from its lining; the modifier system does not
 *       touch armor yet.</li>
 * </ul>
 */
public enum PlatingType {
    HELMET_PLATING("helmet_plating", 5, EquipmentSlot.HEAD),
    CHESTPLATE_PLATING("chestplate_plating", 8, EquipmentSlot.CHEST),
    LEGGINGS_PLATING("leggings_plating", 7, EquipmentSlot.LEGS),
    BOOTS_PLATING("boots_plating", 4, EquipmentSlot.FEET);

    public final String id;
    public final int cost;
    /** The armor slot this plating belongs to. */
    public final EquipmentSlot slot;

    PlatingType(String id, int cost, EquipmentSlot slot) {
        this.id = id;
        this.cost = cost;
        this.slot = slot;
    }

    /** The vanilla armor type this plating builds (attributes, durability, slot). */
    public ArmorType armorType() {
        return switch (this) {
            case HELMET_PLATING -> ArmorType.HELMET;
            case CHESTPLATE_PLATING -> ArmorType.CHESTPLATE;
            case LEGGINGS_PLATING -> ArmorType.LEGGINGS;
            case BOOTS_PLATING -> ArmorType.BOOTS;
        };
    }

    /** The equipment type id of the assembled armor piece ("helmet", ...). */
    public String armorTypeId() {
        return this.id.substring(0, this.id.length() - "_plating".length());
    }

    /** Translation key of the plating shape ("Helmet Plating", ...). */
    public String translationKey() {
        return "plating.modular_dreams." + id;
    }
}
