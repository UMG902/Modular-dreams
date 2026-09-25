package com.modulardreams.material;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Mining tiers, mirroring the vanilla tool material progression.
 * The tier decides which blocks a tool can harvest (via the "incorrect for tool" tag).
 */
public enum MaterialTier {
	WOOD(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 0),
	STONE(BlockTags.INCORRECT_FOR_STONE_TOOL, 1),
	GOLD(BlockTags.INCORRECT_FOR_GOLD_TOOL, 1),
	COPPER(BlockTags.INCORRECT_FOR_COPPER_TOOL, 1),
	IRON(BlockTags.INCORRECT_FOR_IRON_TOOL, 2),
	DIAMOND(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3),
	NETHERITE(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4);

	public final TagKey<Block> incorrectBlocksForDrops;
	public final int level;

	MaterialTier(TagKey<Block> incorrectBlocksForDrops, int level) {
		this.incorrectBlocksForDrops = incorrectBlocksForDrops;
		this.level = level;
	}

	/** @return the highest of two tiers (used by the Netherite upgrade modifier). */
	public MaterialTier max(MaterialTier other) {
		return other.level > this.level ? other : this;
	}
}
