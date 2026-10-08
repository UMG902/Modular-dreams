package com.modulardreams.material;

import java.util.ArrayList;
import java.util.List;

import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.component.ModDataComponents.ModifierEntry;

/**
 * Named traits of Modular Dreams v2. Every material owns a trait that shares
 * its name: the trait of wooden parts is {@code wood}, the trait of a rose
 * gold head is {@code rose_gold}, the trait of a blaze rod handle is
 * {@code blaze_rod}. The smithing table modifiers extend the same vocabulary
 * to raw materials (3x quartz grants the {@code quartz} trait, 3x flint the
 * {@code flint} trait, and so on).
 *
 * <p>Traits are stored on an assembled tool in two places:
 * <ul>
 *   <li>INHERENT - derived from the part materials of the tool (its head and
 *       handle). It is never written down; it simply follows from
 *       {@link ModularData#parts()} because a material's trait id equals the
 *       material id.</li>
 *   <li>APPLIED - granted by smithing table modifiers and stored in
 *       {@link ModularData#modifiers()} as one {@link ModifierEntry} per
 *       applied modifier (level 1 for now).</li>
 * </ul>
 *
 * <p>Because the inherent traits follow from the parts, re-applying one of
 * them through the smithing table is refused by the recipe - there is
 * nothing to grant.
 *
 * <p>Gameplay effects of individual traits (Luck from lapis, fire immunity
 * from blaze rods, ...) arrive with the traits milestone; this milestone
 * only establishes the vocabulary, storage, matching and tooltip.
 */
public final class ModTraits {

    // --- full materials (head + handle) ---
    public static final String WOOD = "wood";
    public static final String STONE = "stone";
    public static final String COPPER = "copper";
    public static final String IRON = "iron";
    public static final String GOLD = "gold";
    public static final String ROSE_GOLD = "rose_gold";
    public static final String DIAMOND = "diamond";
    public static final String NETHERITE = "netherite";

    // --- handle-only materials ---
    public static final String BLAZE_ROD = "blaze_rod";
    public static final String BREEZE_ROD = "breeze_rod";

    // --- smithing-table-only traits (no part material carries these) ---
    public static final String QUARTZ = "quartz";
    public static final String REDSTONE = "redstone";
    public static final String LAPIS = "lapis";
    public static final String EMERALD = "emerald";
    public static final String PRISMARINE = "prismarine";
    public static final String AMETHYST = "amethyst";
    public static final String SLIME = "slime";
    public static final String STRING = "string";
    public static final String LEATHER = "leather";
    public static final String FLINT = "flint";
    public static final String BONE = "bone";

    // --- lining-only traits (carried by the armor linings) ---
    public static final String WOOL = "wool";
    public static final String MAGMA_CREAM = "magma_cream";
    public static final String PHANTOM_MEMBRANE = "phantom_membrane";

    /** Every trait in the v2 vocabulary. */
    public static final List<String> ALL = List.of(
            WOOD, STONE, COPPER, IRON, GOLD, ROSE_GOLD, DIAMOND, NETHERITE,
            BLAZE_ROD, BREEZE_ROD,
            QUARTZ, REDSTONE, LAPIS, EMERALD, PRISMARINE, AMETHYST,
            SLIME, STRING, LEATHER, FLINT, BONE,
            WOOL, MAGMA_CREAM, PHANTOM_MEMBRANE);

    private ModTraits() {}

    /** Translation key of a trait's display name. */
    public static String nameKey(String trait) {
        return "trait.modular_dreams." + trait;
    }

    /**
     * The trait of a tool material - by definition the material's own id
     * ("wood" material -> "wood" trait).
     */
    public static String traitOfMaterial(String materialId) {
        return materialId;
    }

    /** Traits the tool INHERENTLY carries, from its parts (handle first, then head). */
    public static List<String> inherentTraits(ModularData data) {
        List<String> traits = new ArrayList<>();
        for (var part : data.parts()) {
            String trait = traitOfMaterial(part.material());
            if (!traits.contains(trait)) {
                traits.add(trait);
            }
        }
        return List.copyOf(traits);
    }

    /** Traits APPLIED to the tool by smithing table modifiers. */
    public static List<String> appliedTraits(ModularData data) {
        return data.modifiers().stream().map(ModifierEntry::id).toList();
    }

    /** Whether the tool already carries a trait, inherent (via its parts) or applied. */
    public static boolean toolHasTrait(ModularData data, String trait) {
        return dataHasTrait(data, trait);
    }

    /** Allocation-free trait check for hot paths that already have ModularData. */
    public static boolean dataHasTrait(ModularData data, String trait) {
        if (data == null) {
            return false;
        }
        for (var part : data.parts()) {
            if (traitOfMaterial(part.material()).equals(trait)) {
                return true;
            }
        }
        for (ModifierEntry modifier : data.modifiers()) {
            if (modifier.id().equals(trait)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a copy of the data with a trait applied (replacing any
     * previous entry of the same trait). Pure - the input record is not
     * changed. Traits have no levels.
     */
    public static ModularData withAppliedTrait(ModularData data, String trait) {
        List<ModifierEntry> mods = new ArrayList<>(data.modifiers());
        mods.removeIf(m -> m.id().equals(trait));
        mods.add(new ModifierEntry(trait));
        return new ModularData(data.equipmentType(), data.parts(), List.copyOf(mods), data.unlocks());
    }

    /**
     * Returns a copy of the data with its LAST applied modifier removed and
     * the dragon egg extracted (unlocks 3 -> 2). Used by the dragon egg
     * extraction recipe - the egg is returned to the player, the price is
     * the most recently applied modifier.
     */
    public static ModularData withDragonEggExtracted(ModularData data) {
        if (data.modifiers().isEmpty()) {
            return new ModularData(data.equipmentType(), data.parts(), List.of(),
                    Math.min(ModularData.MAX_UNLOCKS - 1, data.unlocks()));
        }
        List<ModifierEntry> mods = new ArrayList<>(data.modifiers());
        mods.remove(mods.size() - 1);
        return new ModularData(data.equipmentType(), data.parts(), List.copyOf(mods),
                Math.min(ModularData.MAX_UNLOCKS - 1, data.unlocks()));
    }
}
