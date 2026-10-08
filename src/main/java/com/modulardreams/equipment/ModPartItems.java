package com.modulardreams.equipment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.modulardreams.ModularDreams;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;
import com.modulardreams.registry.ModRegistry;

/**
 * Every tool part of v2: the universal handle in 10 handle materials and
 * one head per tool type in the 8 head materials. Items are registered as
 * {@code <part>_<material>} (e.g. {@code pickaxe_head_copper},
 * {@code handle_wood}) matching the art pipeline (textures, item models
 * and client item definitions all use this order).
 */
public class ModPartItems {

    /** (part id, material id) -> item. */
    // insertion-ordered (registry order); written only during mod init
    private static final Map<String, Item> PARTS = new LinkedHashMap<>();

    /** item -> identity, for O(1) resolve() (called for every inventory stack). */
    private static final Map<Item, PartIdentity> BY_ITEM = new ConcurrentHashMap<>();

    /** A resolved part identity: which part type in which material. */
    public record PartIdentity(PartType part, ModularMaterial material) {}

    public static void registerAll() {
        for (ModularMaterial material : ModMaterials.handleMaterials()) {
            registerPart(PartType.HANDLE, material);
        }
        for (ModularMaterial material : ModMaterials.headMaterials()) {
            for (PartType part : PartType.values()) {
                if (part.isHead()) {
                    registerPart(part, material);
                }
            }
        }
        // Startup invariant: every part+material pair the creative tab and the
        // Part Picker can ask for MUST resolve. Failing here (at mod init, on
        // both sides) beats crashing the client's render thread later with
        // "No part item for ..." when the creative inventory first opens.
        for (PartType part : PartType.values()) {
            List<ModularMaterial> materials = part.isHandle()
                    ? ModMaterials.handleMaterials()
                    : ModMaterials.headMaterials();
            for (ModularMaterial material : materials) {
                if (!PARTS.containsKey(key(part.id, material.id()))) {
                    throw new IllegalStateException(
                            "Part item failed to register: " + part.id + "_" + material.id());
                }
            }
        }
        ModularDreams.LOGGER.info("Registered {} tool part items ({} handle materials, {} head materials)",
                PARTS.size(), ModMaterials.handleMaterials().size(), ModMaterials.headMaterials().size());
    }

    private static void registerPart(PartType part, ModularMaterial material) {
        String name = part.id + "_" + material.id();
        Item item = ModRegistry.registerItem(name, properties -> new Item(properties));
        PARTS.put(key(part.id, material.id()), item);
        BY_ITEM.put(item, new PartIdentity(part, material));
    }

    private static String key(String partId, String materialId) {
        return partId + "|" + materialId;
    }

    /**
     * @return the part item for a part+material pair, or null if it was not
     *         registered. Use this on the client (creative tab, tooltips)
     *         where a hard throw would crash the render thread.
     */
    public static Item find(PartType part, ModularMaterial material) {
        return PARTS.get(key(part.id, material.id()));
    }

    public static Item get(PartType part, ModularMaterial material) {
        return getOrThrow(part.id, material.id());
    }

    public static Item getOrThrow(String partId, String materialId) {
        Item item = PARTS.get(key(partId, materialId));
        if (item == null) {
            throw new IllegalStateException("No part item for " + materialId + " " + partId);
        }
        return item;
    }

    /** @return the part identity of a stack, or empty for foreign items. */
    public static Optional<PartIdentity> resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ITEM.get(stack.getItem()));
    }

    /** @return the part item name for a part+material pair, e.g. "pickaxe_head_copper". */
    public static String itemName(PartType part, String materialId) {
        return part.id + "_" + materialId;
    }

    /** @return the registered item id of a part item, for datagen-style lookups. */
    public static net.minecraft.resources.Identifier idOf(PartType part, String materialId) {
        return ModularDreams.id(itemName(part, materialId));
    }

    /** @return every part item (registry order). */
    public static List<Item> allItems() {
        return List.copyOf(PARTS.values());
    }
}
