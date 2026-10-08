package com.modulardreams.traits;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.LiningItem;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModPlatingItems;
import com.modulardreams.material.ModTraits;

/**
 * Collects the traits an entity carries, from every kind of v2 item:
 * assembled tools and armor pieces (inherent part traits + applied smithing
 * modifiers), and loose part items (linings, platings, tool parts) whose
 * trait simply IS their material. The spec's inventory-wide rules ("any item
 * in the player's inventory has the Gold trait") read these loose items too.
 */
public final class TraitAggregator {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private TraitAggregator() {}

    /** Every trait a single stack carries (no duplicates). */
    public static List<String> stackTraits(ItemStack stack) {
        List<String> traits = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return traits;
        }
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data != null && !data.equipmentType().equals("none")) {
            traits.addAll(ModTraits.inherentTraits(data));
            traits.addAll(ModTraits.appliedTraits(data));
            return traits;
        }
        // loose v2 items: the trait is the item's own material
        if (stack.getItem() instanceof LiningItem lining) {
            traits.add(lining.materialId());
            return traits;
        }
        ModPlatingItems.resolve(stack).ifPresent(id -> traits.add(id.material().id()));
        if (traits.isEmpty()) {
            ModPartItems.resolve(stack).ifPresent(id -> traits.add(id.material().id()));
        }
        return traits;
    }

    /** Allocation-free trait check for one stack. */
    public static boolean stackHasTrait(ItemStack stack, String trait) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data != null && !data.equipmentType().equals("none")) {
            return ModTraits.dataHasTrait(data, trait);
        }
        if (stack.getItem() instanceof LiningItem lining) {
            return lining.materialId().equals(trait);
        }
        return ModPlatingItems.resolve(stack).map(id -> id.material().id().equals(trait)).orElse(false)
                || ModPartItems.resolve(stack).map(id -> id.material().id().equals(trait)).orElse(false);
    }

    /** Allocation-light early-exit check: does anything worn carry this trait? */
    public static boolean wornHas(Player player, String trait) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (stackHasTrait(player.getItemBySlot(slot), trait)) {
                return true;
            }
        }
        return false;
    }

    /** Allocation-light early-exit check: does either held slot carry this trait? */
    public static boolean heldHas(Player player, String trait) {
        return stackHasTrait(player.getMainHandItem(), trait)
                || stackHasTrait(player.getOffhandItem(), trait);
    }

    /** Traits of everything WORN in the four humanoid armor slots. */
    public static List<String> wornTraits(Player player) {
        List<String> traits = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            for (String trait : stackTraits(player.getItemBySlot(slot))) {
                if (!traits.contains(trait)) {
                    traits.add(trait);
                }
            }
        }
        return traits;
    }

    /** Traits of everything HELD (main hand first, then off hand). */
    public static List<String> heldTraits(Player player) {
        List<String> traits = new ArrayList<>();
        addUniqueTraits(traits, stackTraits(player.getMainHandItem()));
        addUniqueTraits(traits, stackTraits(player.getOffhandItem()));
        return traits;
    }

    private static void addUniqueTraits(List<String> target, List<String> source) {
        for (String trait : source) {
            if (!target.contains(trait)) {
                target.add(trait);
            }
        }
    }

    /** Whether ANY stack in the player's whole inventory (incl. armor + off hand) carries the trait. */
    public static boolean hasTraitInInventory(Player player, String trait) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stackHasTrait(stack, trait)) {
                return true;
            }
        }
        return false;
    }

    /** Worn armor OR held tool carries the trait - the default condition for personal buffs. */
    public static boolean hasTraitWornOrHeld(Player player, String trait) {
        return wornHas(player, trait) || heldHas(player, trait);
    }

    /** All four armor slots are filled with pieces whose lining trait is the given one. */
    public static boolean hasFullLiningSet(Player player, String liningTrait) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!stackHasTrait(player.getItemBySlot(slot), liningTrait)) {
                return false;
            }
        }
        return true;
    }
}
