package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * English language file: item names, guide book text, tooltips, traits, modifiers.
 */
public class ModLangProvider extends FabricLanguageProvider {

	protected ModLangProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(output, registryLookup);
	}

	private static String title(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
		// creative tab
		builder.add("itemGroup.modular_dreams", "Modular Dreams");

		// modular equipment
		builder.add("item.modular_dreams.modular_pickaxe", "Modular Pickaxe");
		builder.add("item.modular_dreams.modular_pickaxe.named", "%s Modular Pickaxe");
		builder.add("item.modular_dreams.modular_axe", "Modular Axe");
		builder.add("item.modular_dreams.modular_axe.named", "%s Modular Axe");
		builder.add("item.modular_dreams.modular_shovel", "Modular Shovel");
		builder.add("item.modular_dreams.modular_shovel.named", "%s Modular Shovel");
		builder.add("item.modular_dreams.modular_hoe", "Modular Hoe");
		builder.add("item.modular_dreams.modular_hoe.named", "%s Modular Hoe");
		builder.add("item.modular_dreams.modular_sword", "Modular Sword");
		builder.add("item.modular_dreams.modular_sword.named", "%s Modular Sword");
		builder.add("item.modular_dreams.modular_mace", "Modular Mace");
		builder.add("item.modular_dreams.modular_mace.named", "%s Modular Mace");
		builder.add("item.modular_dreams.modular_spear", "Modular Spear");
		builder.add("item.modular_dreams.modular_spear.named", "%s Modular Spear");
		builder.add("item.modular_dreams.modular_helmet", "Modular Helmet");
		builder.add("item.modular_dreams.modular_helmet.named", "%s Modular Helmet");
		builder.add("item.modular_dreams.modular_chestplate", "Modular Chestplate");
		builder.add("item.modular_dreams.modular_chestplate.named", "%s Modular Chestplate");
		builder.add("item.modular_dreams.modular_leggings", "Modular Leggings");
		builder.add("item.modular_dreams.modular_leggings.named", "%s Modular Leggings");
		builder.add("item.modular_dreams.modular_boots", "Modular Boots");
		builder.add("item.modular_dreams.modular_boots.named", "%s Modular Boots");
		builder.add("item.modular_dreams.modular_guidebook", "Materials & You (Guide Book)");

		// part type names
		builder.add("part.modular_dreams.pickaxe_head", "Pickaxe Head");
		builder.add("part.modular_dreams.axe_head", "Axe Head");
		builder.add("part.modular_dreams.shovel_head", "Shovel Head");
		builder.add("part.modular_dreams.hoe_head", "Hoe Head");
		builder.add("part.modular_dreams.sword_blade", "Sword Blade");
		builder.add("part.modular_dreams.sword_guard", "Sword Guard");
		builder.add("part.modular_dreams.mace_head", "Mace Head");
		builder.add("part.modular_dreams.spear_head", "Spear Head");
		builder.add("part.modular_dreams.binding", "Tool Binding");
		builder.add("part.modular_dreams.handle", "Tool Handle");
		builder.add("part.modular_dreams.plate", "Armor Plate");
		builder.add("part.modular_dreams.lining", "Armor Lining");
		builder.add("part.modular_dreams.material_line", "Material: %s");

		// material names (deduplicated: some materials serve several roles)
		java.util.Set<String> namedMaterials = new java.util.HashSet<>();
		java.util.List<ModularMaterial> allMaterials = new java.util.ArrayList<>();
		allMaterials.addAll(ModMaterials.toolMaterials());
		allMaterials.addAll(ModMaterials.plateMaterials());
		allMaterials.addAll(ModMaterials.liningMaterials());
		for (ModularMaterial material : allMaterials) {
			if (namedMaterials.add(material.id())) {
				builder.add(material.nameKey(), title(material.id().replace('_', ' ')));
			}
		}

		// part items: "<Material> <Part>" (e.g. "Iron Pickaxe Head")
		for (PartType part : PartType.values()) {
			for (ModularMaterial material : part.allowedMaterials()) {
				builder.add("item.modular_dreams." + material.id() + "_" + part.id,
						title(material.id().replace('_', ' ')) + " " + title(part.id.replace('_', ' ')));
			}
		}

		// traits
		addTrait(builder, ModTraits.FIERY, "Fiery", "Strikes ignite your enemies.");
		addTrait(builder, ModTraits.DENSE, "Dense", "Extremely hard — takes 20% less durability damage.");
		addTrait(builder, ModTraits.FEATHERWEIGHT, "Featherweight", "Light and nimble: +0.15 attack speed.");
		addTrait(builder, ModTraits.RESONANT, "Resonant", "Resonates with the tool: +10% mining speed.");
		addTrait(builder, ModTraits.PRECIOUS, "Precious", "A flawless gem — the tool softly glimmers.");
		addTrait(builder, ModTraits.GILDED, "Gilded", "Attracted to magic: +3 enchantability.");
		addTrait(builder, ModTraits.ROOTED, "Rooted", "Living material — also repairable with sticks.");
		addTrait(builder, ModTraits.SPIKY, "Spiky", "Crude but vicious edges: +0.5 attack damage.");
		addTrait(builder, ModTraits.STURDY, "Sturdy", "Dependable: +5% durability per sturdy part.");
		addTrait(builder, ModTraits.FIREPROOF, "Fireproof", "Immune to fire and lava.");
		addTrait(builder, ModTraits.CUSHIONED, "Cushioned", "Soft padding: increases safe fall distance.");
		addTrait(builder, ModTraits.FEATHERLIGHT, "Featherlight", "Lighter than air: 15% less fall damage.");
		addTrait(builder, ModTraits.COZY, "Cozy", "Quiet padding: 15% faster sneaking.");
		addTrait(builder, ModTraits.AQUATIC, "Aquatic", "Built for water: 20% faster swimming.");
		addTrait(builder, ModTraits.PLATED, "Plated", "Plated hide: +5% knockback resistance.");
		addTrait(builder, ModTraits.SOFT, "Soft", "Comfortable padding: +0.5 armor.");
		addTrait(builder, ModTraits.SPRINGY, "Springy", "Springy hide: slightly stronger jumps.");

		// modifiers
		addModifier(builder, "hasty", "Hasty", "+30% mining speed per level.");
		addModifier(builder, "sharp", "Sharp", "+1 attack damage per level.");
		addModifier(builder, "lucky", "Lucky", "Adds Fortune (digging tools) or Looting (weapons), level equals modifier level.");
		addModifier(builder, "silky", "Silky", "Adds Silk Touch to the tool.");
		addModifier(builder, "fiery", "Fiery", "Sets targets on fire; longer with each level.");
		addModifier(builder, "grippy", "Grippy", "+0.1 attack speed per level.");
		addModifier(builder, "bouncy", "Bouncy", "+0.5 attack knockback per level.");
		addModifier(builder, "reinforced", "Reinforced", "15% less durability damage per level.");
		addModifier(builder, "diamonded", "Diamonded", "+500 max durability.");
		addModifier(builder, "emeraled", "Emeraled", "+50% max durability.");
		addModifier(builder, "netherited", "Netherited", "Upgrades the tool to netherite mining tier and makes it fireproof.");
		addModifier(builder, "solid", "Solid", "+10% knockback resistance per level.");
		addModifier(builder, "featherfall", "Featherfall", "20% less fall damage per level.");
		addModifier(builder, "swift_swim", "Swift Swim", "25% faster swimming per level.");
		addModifier(builder, "sneaky", "Sneaky", "25% faster sneaking per level.");

		// tooltips
		builder.add("tooltip.modular_dreams.parts", "Parts:");
		builder.add("tooltip.modular_dreams.modifiers", "Modifiers:");

		// equipment type names (used in the guide)
		builder.add("equipment.modular_dreams.pickaxe", "Pickaxe");
		builder.add("equipment.modular_dreams.axe", "Axe");
		builder.add("equipment.modular_dreams.shovel", "Shovel");
		builder.add("equipment.modular_dreams.hoe", "Hoe");
		builder.add("equipment.modular_dreams.sword", "Sword");
		builder.add("equipment.modular_dreams.mace", "Mace");
		builder.add("equipment.modular_dreams.spear", "Spear");

		// advancements
		builder.add("advancements.modular_dreams.root.title", "Modular Dreams");
		builder.add("advancements.modular_dreams.root.description",
				"Build equipment from parts instead of replacing it. Read Materials & You to get started!");
		builder.add("advancements.modular_dreams.first_tool.title", "It's Alive!");
		builder.add("advancements.modular_dreams.first_tool.description",
				"Assemble your first modular tool from a head, a binding and a handle.");
		builder.add("advancements.modular_dreams.modded.title", "Fully Loaded");
		builder.add("advancements.modular_dreams.modded.description",
				"Apply 3 different modifiers to a single piece of equipment.");

		addGuideText(builder);
	}

	private void addTrait(TranslationBuilder builder, ModTraits trait, String name, String desc) {
		builder.add(trait.nameKey(), name);
		builder.add(trait.descriptionKey(), desc);
	}

	private void addModifier(TranslationBuilder builder, String id, String name, String desc) {
		builder.add("modifier.modular_dreams." + id, name);
		builder.add("modifier.modular_dreams." + id + ".desc", desc);
	}

	private void addGuideText(TranslationBuilder builder) {
		builder.add("guide.modular_dreams.title", "Materials & You");
		builder.add("guide.modular_dreams.intro.1",
				"Why throw away a worn-out tool when you can repair and upgrade it? Modular Dreams lets you build vanilla tools, weapons and armor from parts made of vanilla materials.");
		builder.add("guide.modular_dreams.intro.2", "Keep this book handy - it documents every material, part and modifier.");
		builder.add("guide.modular_dreams.intro.3", "- The Modular Dreams Team");
		builder.add("guide.modular_dreams.intro.philosophy.title", "Philosophy");
		builder.add("guide.modular_dreams.intro.philosophy.1",
				"Modular tools cost a little more material than their vanilla counterparts, but they are an investment: repair them forever, re-purpose them with new parts and enhance them with modifiers.");
		builder.add("guide.modular_dreams.intro.philosophy.2",
				"Stats are inherited from the materials of each part. A pickaxe with an iron head mines like iron; a spruce handle adds durability. Mix and match to shape the tool you need.");
		builder.add("guide.modular_dreams.intro.parts.title", "Tool Parts");
		builder.add("guide.modular_dreams.intro.parts.1", "Every tool is assembled from these parts, each crafted from a vanilla material:");
		builder.add("guide.modular_dreams.intro.armor_parts.title", "Armor Parts");
		builder.add("guide.modular_dreams.intro.armor_parts.1", "Armor uses two part kinds: sturdy plates define your protection, and the lining adds comfort and utility.");
		builder.add("guide.modular_dreams.started.1.title", "Getting Started");
		builder.add("guide.modular_dreams.started.1.1",
				"1. Craft part items from a material (e.g. 3 iron ingots in a row make an Iron Pickaxe Head).");
		builder.add("guide.modular_dreams.started.1.2",
				"2. Combine the parts in a crafting table in any arrangement to assemble your tool.");
		builder.add("guide.modular_dreams.started.2.title", "Assembly Recipes");
		builder.add("guide.modular_dreams.started.2.1",
				"Pickaxe/Axe/Spear: head + binding + handle. Sword: blade + guard + handle. Shovel/Hoe/Mace: head + handle.");
		builder.add("guide.modular_dreams.started.2.2",
				"Armor: Helmet 3 plates + lining, Chestplate 4 plates + lining, Leggings 3 plates + lining, Boots 2 plates + lining.");
		builder.add("guide.modular_dreams.started.2.3", "Turtle scute plates only fit the helmet - it is a shell, not sheet metal!");
		builder.add("guide.modular_dreams.started.3.title", "The Part Choice");
		builder.add("guide.modular_dreams.started.3.1",
				"The HEAD defines mining speed, mining tier and adds the most attack damage. The HANDLE multiplies durability and can add attack speed. The BINDING adds even more durability and can grant traits.");
		builder.add("guide.modular_dreams.started.3.2",
				"Example: an iron head on a spruce handle with an amethyst binding is fast, durable and resonant.");
		builder.add("guide.modular_dreams.started.3.3",
				"Lining choice matters too: slime cushions falls, phantom membrane lightens them, wool quiets your steps.");
		builder.add("guide.modular_dreams.tools.title", "Tools & Weapons");
		builder.add("guide.modular_dreams.tools.1",
				"Modular versions exist of every classic tool plus the mace and the spear. The mace keeps its full smash attack; the spear keeps its charged dash - both scale with your head material.");
		builder.add("guide.modular_dreams.tools.2", "Modular tools can do anything their vanilla counterparts can: enchant them, trim armor, repair with mending...");
		builder.add("guide.modular_dreams.tools.stats.title", "Tool Stats");
		builder.add("guide.modular_dreams.tools.stats.durability", "Durability: how much use before repair (head + handle + binding bonuses).");
		builder.add("guide.modular_dreams.tools.stats.speed", "Mining speed: blocks per second multiplier (head material).");
		builder.add("guide.modular_dreams.tools.stats.tier", "Mining tier: which blocks drop loot (head material).");
		builder.add("guide.modular_dreams.tools.stats.damage", "Attack damage: tool base + head material.");
		builder.add("guide.modular_dreams.tools.stats.attackspeed", "Attack speed: swings per second x4 (handle can improve it).");
		builder.add("guide.modular_dreams.tools.stats.enchantability", "Enchantability: better books, better enchants.");
		builder.add("guide.modular_dreams.tools.stats.note", "Every stat can be tuned with modifiers and traits - build the tool you want.");
		builder.add("guide.modular_dreams.tools.mace.title", "The Mace");
		builder.add("guide.modular_dreams.tools.mace.1",
				"The modular mace keeps the vanilla smash attack: fall further, hit harder. Its density comes from the head material.");
		builder.add("guide.modular_dreams.tools.mace.2", "An obsidian or netherite head makes a truly brutal mace.");
		builder.add("guide.modular_dreams.tools.spear.title", "The Spear");
		builder.add("guide.modular_dreams.tools.spear.1",
				"The modular spear keeps the vanilla charge: sprint or ride to strike with reach and momentum. Its damage grows with the head material.");
		builder.add("guide.modular_dreams.tools.spear.2", "Pair it with the Bouncy modifier for devastating cavalry charges.");
		builder.add("guide.modular_dreams.materials.title", "Tool Materials");
		builder.add("guide.modular_dreams.materials.intro.1",
				"Every material has a role: woods are cheap handles, stones are honest heads, metals are dependable, gems are exceptional.");
		builder.add("guide.modular_dreams.materials.intro.2", "The following pages list every tool material. Durability and speed apply to heads; handle/binding show their durability bonus.");
		builder.add("guide.modular_dreams.material.durability", "Durability:");
		builder.add("guide.modular_dreams.material.speed", "Mining speed:");
		builder.add("guide.modular_dreams.material.damage", "Attack damage:");
		builder.add("guide.modular_dreams.material.enchantability", "Enchantability:");
		builder.add("guide.modular_dreams.material.handle", "Handle durability:");
		builder.add("guide.modular_dreams.material.binding", "Binding durability:");
		builder.add("guide.modular_dreams.material.tier", "Tier:");
		builder.add("guide.modular_dreams.armor.title", "Modular Armor");
		builder.add("guide.modular_dreams.armor.1",
				"Modular armor is assembled from plates and a lining. Plates define defense, toughness and durability; the lining adds utility and a little extra enchantability.");
		builder.add("guide.modular_dreams.armor.2", "Armor made of a material looks and protects like its vanilla counterpart - iron plates look like iron armor.");
		builder.add("guide.modular_dreams.armor.stats.title", "Armor Stats");
		builder.add("guide.modular_dreams.armor.stats.durability", "Durability: plate multiplier x slot factor x (1 + lining bonus).");
		builder.add("guide.modular_dreams.armor.stats.defense", "Defense: armor points from the plate material.");
		builder.add("guide.modular_dreams.armor.stats.toughness", "Toughness: reduces heavy hits (diamond, netherite).");
		builder.add("guide.modular_dreams.armor.stats.knockback", "Knockback resistance: from netherite and armadillo plates.");
		builder.add("guide.modular_dreams.armor.stats.enchantability", "Enchantability: plate value + lining bonus.");
		builder.add("guide.modular_dreams.armor.stats.note", "Armor accepts trims, enchantments and all armor modifiers.");
		builder.add("guide.modular_dreams.armor.plates.title", "Plates & Linings");
		builder.add("guide.modular_dreams.armor.plates.1", "Defense values are listed as boots/legs/chest/helmet.");
		builder.add("guide.modular_dreams.armor.material.durability", "Durability:");
		builder.add("guide.modular_dreams.armor.material.defense", "Defense (B/L/C/H):");
		builder.add("guide.modular_dreams.armor.material.toughness", "Toughness:");
		builder.add("guide.modular_dreams.armor.material.enchantability", "Enchantability:");
		builder.add("guide.modular_dreams.armor.material.lining_durability", "Durability bonus:");
		builder.add("guide.modular_dreams.armor.material.lining_enchantability", "Enchantability bonus:");
		builder.add("guide.modular_dreams.modifiers.title", "Modifiers");
		builder.add("guide.modular_dreams.modifiers.1",
				"Modifiers upgrade assembled equipment. Place the item and the cost in a crafting grid - no station needed. Levels stack up to the maximum.");
		builder.add("guide.modular_dreams.modifiers.2", "Tool modifiers only fit tools, armor modifiers only fit armor, and universal ones fit both. The next pages list every modifier and its cost.");
		builder.add("guide.modular_dreams.modifiers.list.title", "Modifier List");
		builder.add("guide.modular_dreams.repair.title", "Repairing");
		builder.add("guide.modular_dreams.repair.1",
				"Combine a damaged item with ONE repair material in a crafting grid to restore 10% of its max durability. Repeat as often as you like - the item is never consumed.");
		builder.add("guide.modular_dreams.repair.2",
				"What repairs what? Tools: their head material's crafting item (iron ingots repair iron heads). Armor: the plate material's repair items. Wood handles also accept sticks.");
		builder.add("guide.modular_dreams.repair.advanced.title", "Repairing, continued");
		builder.add("guide.modular_dreams.repair.3",
				"Anvils and Mending work too - modular items know their repair items. Enchanted gear keeps every enchantment and modifier while being repaired.");
		builder.add("guide.modular_dreams.repair.4",
				"Note: upgrade modifiers cost 2 or more of their item, so a single item always repairs instead of upgrading.");
		builder.add("guide.modular_dreams.tips.title", "Tips & Tricks");
		builder.add("guide.modular_dreams.tips.1",
				"- A gold binding gilds any tool with +3 enchantability.\n- An amethyst binding resonates: +10% mining speed.\n- Bone handles swing faster; obsidian heads shrug off damage.\n- Netherite parts are fireproof - and so is your gear.");
		builder.add("guide.modular_dreams.tips.2",
				"- Hasty + resonant + diamond head = instant mining.\n- Turtle shell helmets swim; slime linings land softly.\n- You can re-craft improved versions any time - parts are never wasted, and repairs keep gear alive forever.");
	}
}
