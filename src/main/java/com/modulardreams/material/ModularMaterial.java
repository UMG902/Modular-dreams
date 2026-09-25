package com.modulardreams.material;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.resources.ResourceKey;

import com.modulardreams.stats.ModTraits;

/**
 * Definition of one vanilla material inside Modular Dreams.
 *
 * A material may be usable for tool parts (stats in the first block), for armor
 * plates ({@link Plate}), for armor linings ({@link Lining}) or for several of
 * those roles at once. Traits follow the Tinkers' philosophy: they are derived
 * from the material's existing vanilla uses and characteristics.
 *
 * @param id                 machine id, e.g. "iron"
 * @param repairTag          items repairing tools with this material (head/handle)
 * @param tier               mining tier of tools whose head is made of this
 * @param durability         base tool durability when used as a head
 * @param speed              base mining speed when used as a head
 * @param attackDamageBonus  attack damage bonus when used as a head
 * @param enchantmentValue   enchantability contribution
 * @param handleDurability   multiplier added to durability when used as a handle (0.25 = +25%)
 * @param handleSpeed        flat attack speed bonus when used as a handle
 * @param bindingDurability  multiplier added to durability when used as a binding
 * @param traits             material traits applied wherever the material is used
 */
public record ModularMaterial(
                String id,
                TagKey<Item> repairTag,
                MaterialTier tier,
                int durability,
                float speed,
                float attackDamageBonus,
                int enchantmentValue,
                float handleDurability,
                float handleSpeed,
                float bindingDurability,
                List<ModTraits> traits,
                Plate plate,
                Lining lining,
                boolean fireResistant
) {

        /**
         * Armor stats when this material is used as an armor plate.
         *
         * @param durabilityMultiplier base durability multiplier (vanilla iron armor = 15)
         * @param defense              defense points per armor slot
         * @param toughness            armor toughness
         * @param knockbackResistance  knockback resistance
         * @param enchantmentValue     enchantability
         * @param repairTag            items repairing armor plated with this material
         * @param asset                vanilla equipment asset reused for armor rendering
         * @param equipSound           sound played when equipping
         */
        public record Plate(
                        int durabilityMultiplier,
                        Map<ArmorType, Integer> defense,
                        float toughness,
                        float knockbackResistance,
                        int enchantmentValue,
                        TagKey<Item> repairTag,
                        ResourceKey<EquipmentAsset> asset,
                        Holder<SoundEvent> equipSound
        ) {}

        /**
         * Utility stats when this material is used as an armor lining.
         *
         * @param durabilityBonus    flat multiplier added to armor durability (0.1 = +10%)
         * @param enchantmentBonus   extra enchantability
         * @param attributes         attribute modifiers contributed while worn
         */
        public record Lining(
                        float durabilityBonus,
                        int enchantmentBonus,
                        List<AttrBonus> attributes
        ) {}

        /** One attribute contribution (trait-driven), e.g. slime lining fall protection. */
        public record AttrBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {}

        public String nameKey() {
                return "material.modular_dreams." + id;
        }

        public boolean isPlate() {
                return plate != null;
        }

        public boolean isLining() {
                return lining != null;
        }

        public boolean hasTrait(ModTraits trait) {
                return traits.contains(trait);
        }

        public Optional<Plate> plateOpt() {
                return Optional.ofNullable(plate);
        }

        public Optional<Lining> liningOpt() {
                return Optional.ofNullable(lining);
        }
}
