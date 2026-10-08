package com.modulardreams.equipment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.modulardreams.ModularDreams;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PlatingType;
import com.modulardreams.registry.ModRegistry;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The armor platings of Modular Dreams v2: four slot-specific plating types
 * (helmet, chestplate, leggings, boots) in every full material EXCEPT
 * netherite. Items are registered as {@code <plating>_<material>}
 * (e.g. {@code helmet_plating_iron}), matching the tool part order.
 *
 * <p>Platings are shaped at the Part Picker like tool parts and are the
 * outer layer of the future armor pieces. For now they are plain items:
 * no armor value, no traits (traits come from linings), and no modifiers.
 *
 * <p>Why no netherite plating: netherite armor is reachable ONLY through
 * the smithing upgrade, and that upgrade only applies to armor with
 * DIAMOND plating (vanilla parity). A craftable netherite plating would
 * make that gate meaningless.
 */
public class ModPlatingItems {

    /** (plating id, material id) -> item. */
    // insertion-ordered so the creative tab lists platings in registry order;
    // written only during mod init
    private static final Map<String, Item> PLATINGS = new LinkedHashMap<>();

    /** item -> identity, for O(1) resolve() (called for every inventory stack). */
    private static final Map<Item, PlatingIdentity> BY_ITEM = new ConcurrentHashMap<>();

    /** A resolved plating identity: which plating type in which material. */
    public record PlatingIdentity(PlatingType plating, ModularMaterial material) {}

    public static void registerAll() {
        for (PlatingType plating : PlatingType.values()) {
            for (ModularMaterial material : platingMaterials()) {
                registerPlating(plating, material);
            }
        }
        // Startup invariant, same policy as ModPartItems: every plating the
        // creative tab and the Part Picker can ask for MUST resolve.
        for (PlatingType plating : PlatingType.values()) {
            for (ModularMaterial material : platingMaterials()) {
                if (!PLATINGS.containsKey(key(plating.id, material.id()))) {
                    throw new IllegalStateException("Plating item failed to register: "
                            + plating.id + "_" + material.id());
                }
            }
        }
        ModularDreams.LOGGER.info("Registered {} armor plating items ({} plating types, {} materials)",
                PLATINGS.size(), PlatingType.values().length, platingMaterials().size());
    }

    private static void registerPlating(PlatingType plating, ModularMaterial material) {
        String name = plating.id + "_" + material.id();
        Item item = ModRegistry.registerItem(name, properties -> new PlatingItem(properties, material.id()));
        PLATINGS.put(key(plating.id, material.id()), item);
        BY_ITEM.put(item, new PlatingIdentity(plating, material));
    }

    private static String key(String platingId, String materialId) {
        return platingId + "|" + materialId;
    }

    /**
     * Materials platings exist in: every full (non handle-only) material
     * except netherite - see the class doc for why netherite is excluded.
     */
    public static List<ModularMaterial> platingMaterials() {
        List<ModularMaterial> mats = new ArrayList<>();
        for (ModularMaterial mat : ModMaterials.materials()) {
            if (isPlatingMaterial(mat)) {
                mats.add(mat);
            }
        }
        return List.copyOf(mats);
    }

    /** Whether platings exist in this material (full materials except netherite). */
    public static boolean isPlatingMaterial(ModularMaterial material) {
        return !material.handleOnly() && !material.id().equals("netherite");
    }

    /**
     * @return the plating item for a plating+material pair, or null if it
     *         was not registered. Client-safe lookup (creative tab, tooltips).
     */
    public static Item find(PlatingType plating, ModularMaterial material) {
        return PLATINGS.get(key(plating.id, material.id()));
    }

    public static Item get(PlatingType plating, ModularMaterial material) {
        return getOrThrow(plating.id, material.id());
    }

    public static Item getOrThrow(String platingId, String materialId) {
        Item item = PLATINGS.get(key(platingId, materialId));
        if (item == null) {
            throw new IllegalStateException("No plating item for " + materialId + " " + platingId);
        }
        return item;
    }

    /** @return the plating identity of a stack, or empty for foreign items. */
    public static Optional<PlatingIdentity> resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ITEM.get(stack.getItem()));
    }

    /** @return the registered item id of a plating, for datagen-style lookups. */
    public static net.minecraft.resources.Identifier idOf(PlatingType plating, String materialId) {
        return ModularDreams.id(plating.id + "_" + materialId);
    }

    /** @return every plating item (registry order). */
    public static List<Item> allItems() {
        return List.copyOf(PLATINGS.values());
    }
}
