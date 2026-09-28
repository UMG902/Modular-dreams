package com.modulardreams.equipment;

import java.util.List;
import java.util.Optional;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.modulardreams.part.PartType;

/**
 * The modular tool &amp; weapon types, mirroring the vanilla tool set plus the
 * spear. Every tool is assembled from exactly three parts - handle, binding,
 * head (overhaul step 1; the mace was removed in a later step).
 *
 * <p>Per-tool behavior constants follow Legacy's Construct (the TiC 1.12.2
 * port the balance follows):
 * <ul>
 *   <li>{@code durabilityMultiplier} multiplies the assembled base durability</li>
 *   <li>{@code miningSpeedMultiplier} multiplies the head mining speed</li>
 *   <li>{@code attackMultiplier} / {@code baseAttack} / {@code damagePotential}
 *       form the attack: {@code (headAttack * attackMultiplier + baseAttack) * damagePotential}</li>
 *   <li>{@code attackSpeed} is the vanilla attack-speed attribute value</li>
 * </ul>
 *
 * <p>Values: pickaxe(1, 1, 1, 0, 1, 1.2), shovel(1, 1, 1, 0, 0.9, 1.0),
 * hatchet/axe(1, 1, 1, 0.5, 1.1, 1.1), kama/hoe(1, 1, 1, 0, 1, 1.3),
 * broadsword/sword(1.1, 1, 1, 0, 1, 1.6).
 * The spear has no LC counterpart (1, 1, 1, 0, 0.9, 1.0).
 */
public enum ModularToolType {
        PICKAXE("pickaxe", 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.2F, BlockTags.MINEABLE_WITH_PICKAXE, 1, 0.0F),
        AXE("axe", 1.0F, 1.0F, 1.0F, 0.5F, 1.1F, 1.1F, BlockTags.MINEABLE_WITH_AXE, 1, 5.0F),
        SHOVEL("shovel", 1.0F, 1.0F, 1.0F, 0.0F, 0.9F, 1.0F, BlockTags.MINEABLE_WITH_SHOVEL, 1, 0.0F),
        HOE("hoe", 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.3F, BlockTags.MINEABLE_WITH_HOE, 1, 0.0F),
        SWORD("sword", 1.1F, 1.0F, 1.0F, 0.0F, 1.0F, 1.6F, null, 1, 0.0F),
        SPEAR("spear", 1.0F, 1.0F, 1.0F, 0.0F, 0.9F, 1.0F, null, 1, 0.0F);

        ModularToolType(String id, float durabilityMultiplier, float miningSpeedMultiplier,
                        float attackMultiplier, float baseAttack, float damagePotential, float attackSpeed,
                        TagKey<Block> mineableTag, int weaponDamagePerAttack, float disableBlockingForSeconds) {
                this.id = id;
                this.durabilityMultiplier = durabilityMultiplier;
                this.miningSpeedMultiplier = miningSpeedMultiplier;
                this.attackMultiplier = attackMultiplier;
                this.baseAttack = baseAttack;
                this.damagePotential = damagePotential;
                this.attackSpeed = attackSpeed;
                this.mineableTag = mineableTag;
                this.weaponDamagePerAttack = weaponDamagePerAttack;
                this.disableBlockingForSeconds = disableBlockingForSeconds;
        }

        public final String id;
        public final float durabilityMultiplier;
        public final float miningSpeedMultiplier;
        public final float attackMultiplier;
        public final float baseAttack;
        public final float damagePotential;
        public final float attackSpeed;
        public final TagKey<Block> mineableTag;
        public final int weaponDamagePerAttack;
        public final float disableBlockingForSeconds;

        /** The three parts of this tool, in assembly order: handle, binding, head. */
        public List<PartType> parts() {
                return PartType.partsOfTool(this.id);
        }

        public String translationKey() {
                return "equipment.modular_dreams." + id;
        }

        public PartType headPart() {
                return partOfRole(PartType.Role.HEAD);
        }

        public PartType bindingPart() {
                return partOfRole(PartType.Role.BINDING);
        }

        public PartType handlePart() {
                // every tool assembles with the one universal handle; the sword
                // and the spear only RENDER their own handle shapes
                return PartType.handlePartForTool(this.id);
        }

        private PartType partOfRole(PartType.Role role) {
                for (PartType part : parts()) {
                        if (part.role == role) {
                                return part;
                        }
                }
                throw new IllegalStateException("Tool " + id + " has no " + role + " part");
        }

        public boolean isRangedChargeWeapon() {
                return this == SPEAR;
        }

        /** @return the tool type with the given id, or empty. */
        public static Optional<ModularToolType> byId(String id) {
                for (ModularToolType type : values()) {
                        if (type.id.equals(id)) {
                                return Optional.of(type);
                        }
                }
                return Optional.empty();
        }
}
