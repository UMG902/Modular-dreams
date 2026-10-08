package com.modulardreams.equipment;

import java.util.List;
import java.util.Optional;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.modulardreams.part.PartType;

/**
 * The modular tool &amp; weapon types of v2, mirroring the vanilla tool set
 * plus the spear (26.3 added the vanilla spear; ours follows its mechanics).
 * Every tool is assembled from exactly two parts: handle + head.
 *
 * <p>Attack values are the vanilla attribute ADD modifiers: damage is the
 * head's attack bonus plus the type's base damage (sword +3, axe +6,
 * pickaxe +1, shovel +1.5, hoe +0, spear +2), attack speed replaces the
 * vanilla per-type values.
 */
public enum ModularToolType {
    PICKAXE("pickaxe", 1.0F, -2.8F, BlockTags.MINEABLE_WITH_PICKAXE, 1, 0.0F, 1),
    AXE("axe", 6.0F, -3.0F, BlockTags.MINEABLE_WITH_AXE, 1, 5.0F, 1),
    SHOVEL("shovel", 1.5F, -2.8F, BlockTags.MINEABLE_WITH_SHOVEL, 1, 0.0F, 1),
    HOE("hoe", 0.0F, -1.0F, BlockTags.MINEABLE_WITH_HOE, 1, 0.0F, 1),
    SWORD("sword", 3.0F, -2.4F, null, 2, 0.0F, 2),
    SPEAR("spear", 2.0F, -3.0F, null, 1, 0.0F, 1);

    ModularToolType(String id, float baseDamage, float attackSpeed, TagKey<Block> mineableTag,
            int weaponDamagePerAttack, float disableBlockingForSeconds, int damagePerBlock) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.mineableTag = mineableTag;
        this.weaponDamagePerAttack = weaponDamagePerAttack;
        this.disableBlockingForSeconds = disableBlockingForSeconds;
        this.damagePerBlock = damagePerBlock;
    }

    public final String id;
    public final float baseDamage;
    public final float attackSpeed;
    public final TagKey<Block> mineableTag;
    public final int weaponDamagePerAttack;
    public final float disableBlockingForSeconds;
    public final int damagePerBlock;

    /** The two parts of this tool, in assembly order: handle, head. */
    public List<PartType> parts() {
        return List.of(PartType.HANDLE, PartType.headOfTool(this.id));
    }

    public PartType headPart() {
        return PartType.headOfTool(this.id);
    }

    public String translationKey() {
        return "equipment.modular_dreams." + id;
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
