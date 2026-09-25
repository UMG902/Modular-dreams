package com.modulardreams.stats;

/**
 * Material traits, heavily inspired by Tinkers' Construct / Construct's Armory.
 *
 * Every trait is logically derived from the material's vanilla uses or
 * characteristics — no arbitrary fantasy effects. Effects are applied either by
 * baking stat adjustments (StatsEngine), attribute bonuses (armor), or small
 * gameplay hooks (ModularToolItem).
 */
public enum ModTraits {
	// ---- tool traits ----
	/** Netherite head: strikes ignite enemies for a short moment. */
	FIERY("fiery", Category.TOOL),
	/** Obsidian head: extremely hard — takes 20% less durability damage. */
	DENSE("dense", Category.TOOL),
	/** Bone handle: light and nimble — +0.15 attack speed, but fragile. */
	FEATHERWEIGHT("featherweight", Category.TOOL),
	/** Amethyst binding: resonates with the tool — +10% mining speed. */
	RESONANT("resonant", Category.TOOL),
	/** Diamond binding: the tool softly glimmers (cosmetic glint). */
	PRECIOUS("precious", Category.TOOL),
	/** Gold binding: attracted to magic — +3 enchantability. */
	GILDED("gilded", Category.TOOL),
	/** Wood handle: living material — the tool can also be repaired with sticks. */
	ROOTED("rooted", Category.TOOL),
	/** Flint head: crude but vicious edges — +0.5 attack damage. */
	SPIKY("spiky", Category.TOOL),

	// ---- universal traits ----
	/** Iron parts: dependable — +5% durability. */
	STURDY("sturdy", Category.ANY),
	/** Netherite parts: immune to fire and lava (item does not burn). */
	FIREPROOF("fireproof", Category.ANY),

	// ---- armor traits ----
	/** Slime lining: cushions impacts — increases safe fall distance. */
	CUSHIONED("cushioned", Category.ARMOR),
	/** Phantom membrane lining: lighter than air — reduces fall damage. */
	FEATHERLIGHT("featherlight", Category.ARMOR),
	/** Wool lining: quiet padding — faster sneaking. */
	COZY("cozy", Category.ARMOR),
	/** Turtle shell plate: built for water — faster swimming. */
	AQUATIC("aquatic", Category.ARMOR),
	/** Armadillo scute plate: plated hide — slight knockback resistance. */
	PLATED("plated", Category.ARMOR),
	/** Leather lining: soft, comfortable padding — small armor bonus. */
	SOFT("soft", Category.ARMOR),
	/** Rabbit hide lining: springy — slightly stronger jumps. */
	SPRINGY("springy", Category.ARMOR);

	public enum Category {
		TOOL, ARMOR, ANY;

		public boolean matches(boolean tool) {
			return this == ANY || (tool ? this == TOOL : this == ARMOR);
		}
	}

	public final String id;
	public final Category category;

	ModTraits(String id, Category category) {
		this.id = id;
		this.category = category;
	}

	public String nameKey() {
		return "trait.modular_dreams." + id;
	}

	public String descriptionKey() {
		return "trait.modular_dreams." + id + ".desc";
	}
}
