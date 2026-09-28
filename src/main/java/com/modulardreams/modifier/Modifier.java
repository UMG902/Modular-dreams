package com.modulardreams.modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;

import com.modulardreams.stats.ModTraits.Category;

/**
 * Modifiers are the upgrade system of Modular Dreams (Tinkers'-style).
 *
 * A modifier is applied by combining an assembled item with the modifier's
 * cost item(s) in the Tinker Station (a single slot may hold a stack). Costs
 * are deliberately set to 2+ items when the cost item is also a valid repair
 * material, so repair and upgrade can never be confused with each other.
 */
public record Modifier(
                String id,
                Effect effect,
                float magnitude,
                ItemLike costItem,
                int costPerLevel,
                int maxLevel,
                Category category,
                ResourceKey<Enchantment> enchantment
) {

        public enum Effect {
                /** +magnitude*level percent mining speed. */
                MINING_SPEED_PCT,
                /** +magnitude*level flat attack damage. */
                ATTACK_DAMAGE,
                /** +magnitude*level flat attack speed. */
                ATTACK_SPEED,
                /** +magnitude*level attack knockback attribute. */
                ATTACK_KNOCKBACK,
                /** +magnitude*level knockback resistance attribute (armor). */
                KB_RESISTANCE,
                /** +magnitude*level multiplied fall damage reduction (armor). */
                FALL_DAMAGE_MULT,
                /** +magnitude*level multiplied water movement speed (armor). */
                WATER_SPEED,
                /** +magnitude*level multiplied sneaking speed (armor). */
                SNEAK_SPEED,
                /** +magnitude*level percent max durability. */
                DURABILITY_PCT,
                /** +magnitude*level flat max durability. */
                DURABILITY_FLAT,
                /** magnitude*level fraction less durability damage taken. */
                DAMAGE_REDUCTION,
                /** sets the target on fire for magnitude + level seconds on hit. */
                FIERY,
                /** adds the configured enchantment at level = modifier level. */
                ENCHANT,
                /** upgrades the effective mining tier to NETHERITE. */
                TIER_UPGRADE,
                /** grants fire/lava immunity to the item (netherite upgrade). */
                FIRE_RESISTANT
        }

        public Modifier(String id, Effect effect, float magnitude, ItemLike costItem, int costPerLevel, int maxLevel,
                        Category category) {
                this(id, effect, magnitude, costItem, costPerLevel, maxLevel, category, null);
        }

        public String nameKey() {
                return "modifier.modular_dreams." + id;
        }

        public String descriptionKey() {
                return "modifier.modular_dreams." + id + ".desc";
        }

        public Optional<ResourceKey<Enchantment>> enchantmentKey() {
                return Optional.ofNullable(enchantment);
        }

        // ------------------------------------------------------------------

        private static final List<Modifier> ALL = new ArrayList<>();
        private static final Map<String, Modifier> BY_ID = new HashMap<>();

        private static void add(Modifier modifier) {
                ALL.add(modifier);
                BY_ID.put(modifier.id, modifier);
        }

        public static void initialize() {
                if (!ALL.isEmpty()) {
                        return;
                }
                // --- tool modifiers ---
                add(new Modifier("hasty", Effect.MINING_SPEED_PCT, 0.30F, net.minecraft.world.item.Items.REDSTONE, 3, 3,
                                Category.TOOL));
                add(new Modifier("sharp", Effect.ATTACK_DAMAGE, 1.0F, net.minecraft.world.item.Items.QUARTZ, 3, 3,
                                Category.TOOL));
                add(new Modifier("lucky", Effect.ENCHANT, 1.0F, net.minecraft.world.item.Items.LAPIS_LAZULI, 3, 3,
                                Category.TOOL, Enchantments.FORTUNE));
                add(new Modifier("silky", Effect.ENCHANT, 1.0F, net.minecraft.world.item.Items.AMETHYST_SHARD, 4, 1,
                                Category.TOOL, Enchantments.SILK_TOUCH));
                add(new Modifier("fiery", Effect.FIERY, 1.5F, net.minecraft.world.item.Items.BLAZE_POWDER, 2, 3,
                                Category.TOOL));
                add(new Modifier("grippy", Effect.ATTACK_SPEED, 0.10F, net.minecraft.world.item.Items.STRING, 3, 3,
                                Category.TOOL));
                add(new Modifier("bouncy", Effect.ATTACK_KNOCKBACK, 0.5F, net.minecraft.world.item.Items.SLIME_BALL, 3, 2,
                                Category.TOOL));

                // --- universal modifiers ---
                add(new Modifier("reinforced", Effect.DAMAGE_REDUCTION, 0.15F, net.minecraft.world.item.Items.OBSIDIAN, 2, 3,
                                Category.ANY));
                add(new Modifier("diamonded", Effect.DURABILITY_FLAT, 500.0F, net.minecraft.world.item.Items.DIAMOND, 2, 1,
                                Category.ANY));
                add(new Modifier("emeraled", Effect.DURABILITY_PCT, 0.50F, net.minecraft.world.item.Items.EMERALD, 1, 1,
                                Category.ANY));
                add(new Modifier("netherited", Effect.TIER_UPGRADE, 1.0F, net.minecraft.world.item.Items.NETHERITE_INGOT, 2,
                                1, Category.TOOL));

                // --- armor modifiers ---
                add(new Modifier("solid", Effect.KB_RESISTANCE, 0.1F, net.minecraft.world.item.Items.IRON_INGOT, 2, 2,
                                Category.ARMOR));
                add(new Modifier("featherfall", Effect.FALL_DAMAGE_MULT, -0.20F, net.minecraft.world.item.Items.PHANTOM_MEMBRANE,
                                3, 2, Category.ARMOR));
                add(new Modifier("swift_swim", Effect.WATER_SPEED, 0.25F, net.minecraft.world.item.Items.PRISMARINE_CRYSTALS,
                                3, 2, Category.ARMOR));
                add(new Modifier("sneaky", Effect.SNEAK_SPEED, 0.25F, net.minecraft.world.item.Items.RABBIT_HIDE, 3, 2,
                                Category.ARMOR));
        }

        public static Optional<Modifier> byId(String id) {
                return Optional.ofNullable(BY_ID.get(id));
        }

        public static Modifier getOrThrow(String id) {
                Modifier modifier = BY_ID.get(id);
                if (modifier == null) {
                        throw new IllegalStateException("Unknown modifier: " + id);
                }
                return modifier;
        }

        /** @return the modifier whose cost matches this item, if any (items are unique per modifier). */
        public static Optional<Modifier> byCostItem(net.minecraft.world.item.Item item) {
                return ALL.stream().filter(m -> m.costItem.asItem() == item).findFirst();
        }

        public static List<Modifier> all() {
                return List.copyOf(ALL);
        }
}
