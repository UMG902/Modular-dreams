package com.modulardreams.equipment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import com.modulardreams.ModularDreams;
import com.modulardreams.material.ModTraits;
import com.modulardreams.part.PlatingType;
import com.modulardreams.registry.ModRegistry;

/**
 * The armor linings AND armor pieces of Modular Dreams v2.
 *
 * <p>Linings: one wearable padding item per lining material (leather, wool,
 * slime, magma cream, phantom membrane), registered as
 * {@code <material>_lining} (e.g. {@code leather_lining}). Every lining is a
 * plain {@link Item} with no armor material and no attribute modifiers:
 * protection is literally zero. The only identity it carries is its trait
 * (the material's own name, shown in the tooltip).
 *
 * <p>The vanilla {@code EQUIPPABLE} component is attached as the carrier of
 * the worn-layer asset and the equip sound. 26.3 components hold exactly one
 * slot, so it names HEAD while {@code LivingEntityMixin} opens all four
 * humanoid armor slots for lining items.
 *
 * <p>Pieces: the four modular armor items ({@code modular_helmet},
 * {@code modular_chestplate}, {@code modular_leggings}, {@code modular_boots}).
 * They are blank shells at registration - the assembler bakes every stat
 * (armor value, toughness, durability, worn asset) from plating + lining at
 * assembly time.
 */
public class ModArmorItems {

    /** The lining materials, in registration order (also the creative tab order). */
    public static final List<String> LINING_MATERIALS = List.of(
            ModTraits.LEATHER, ModTraits.WOOL, ModTraits.SLIME,
            ModTraits.MAGMA_CREAM, ModTraits.PHANTOM_MEMBRANE);

    /** (material id) -> lining item, in registration order. */
    private static final Map<String, Item> LININGS = new LinkedHashMap<>();

    /** The four armor piece items, keyed by plating type. */
    private static final Map<PlatingType, Item> PIECES = new LinkedHashMap<>();

    public static void registerAll() {
        for (String material : LINING_MATERIALS) {
            registerLining(material);
        }
        registerPieces();
    }

    private static void registerLining(String material) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                EquipmentAssets.ROOT_ID, ModularDreams.id(material + "_lining"));
        Equippable equippable = Equippable.builder(EquipmentSlot.HEAD) // carrier only - see class doc
                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                .setAsset(asset)
                .build();
        Item item = ModRegistry.registerItem(material + "_lining",
                new Item.Properties().component(DataComponents.EQUIPPABLE, equippable),
                properties -> new LiningItem(properties, material));
        LININGS.put(material, item);
    }

    /** The lining item of a material (throws when the material has none). */
    public static Item lining(String material) {
        Item item = LININGS.get(material);
        if (item == null) {
            throw new IllegalStateException("No lining item for material: " + material);
        }
        return item;
    }

    // ------------------------------------------------------------------ armor pieces

    /** Registers the four blank armor piece items (assembled by the Assembler). */
    private static void registerPieces() {
        for (PlatingType plating : PlatingType.values()) {
            Item item = ModRegistry.registerItem("modular_" + plating.armorTypeId(),
                    ModularArmorItem::new);
            PIECES.put(plating, item);
        }
    }

    /** The armor piece item of a plating type, e.g. {@code modular_helmet}. */
    public static Item piece(PlatingType plating) {
        Item item = PIECES.get(plating);
        if (item == null) {
            throw new IllegalStateException("No armor piece for " + plating.id);
        }
        return item;
    }

    /** Whether the stack is one of the modular armor pieces. */
    public static boolean isPiece(ItemStack stack) {
        return !stack.isEmpty() && PIECES.containsValue(stack.getItem());
    }

    /** The plating type a piece item belongs to, or empty for foreign items. */
    public static Optional<PlatingType> pieceOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return PIECES.entrySet().stream()
                .filter(e -> e.getValue() == stack.getItem())
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** Whether the stack is one of the mod's linings. */
    public static boolean isLining(ItemStack stack) {
        return LiningItem.isLining(stack);
    }

    /** Every lining item (registration order). */
    public static List<Item> allLinings() {
        return List.copyOf(LININGS.values());
    }
}
