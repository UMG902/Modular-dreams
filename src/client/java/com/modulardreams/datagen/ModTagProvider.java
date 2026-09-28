package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.material.ModMaterials;

/**
 * Item tags: vanilla enchantment/equipment tags for the modular tools, plus
 * the mod's own repair-material tags (bone and flint have no vanilla tags).
 */
public class ModTagProvider extends FabricTagsProvider<Item> {

        protected ModTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, Registries.ITEM, registriesFuture);
        }

        private static TagKey<Item> ench(String name) {
                return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("enchantable/" + name));
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
                tag(ench("durability"), "modular_pickaxe", "modular_axe", "modular_shovel", "modular_hoe",
                                "modular_sword", "modular_spear");

                tag(ench("mining"), "modular_pickaxe", "modular_axe", "modular_shovel", "modular_hoe");
                tag(ench("mining_loot"), "modular_pickaxe", "modular_hoe");
                tag(ench("sharp_weapon"), "modular_sword", "modular_axe", "modular_spear");
                tag(ench("weapon"), "modular_sword", "modular_axe", "modular_spear");
                tag(ench("sweeping"), "modular_sword", "modular_axe");
                tag(ench("fire_aspect"), "modular_sword", "modular_axe", "modular_pickaxe",
                                "modular_shovel");

                // vanilla equipment tags (for parity with vanilla tools)
                tag(ItemTags.PICKAXES, "modular_pickaxe");
                tag(ItemTags.AXES, "modular_axe");
                tag(ItemTags.SHOVELS, "modular_shovel");
                tag(ItemTags.HOES, "modular_hoe");
                tag(ItemTags.SWORDS, "modular_sword");
                tag(ItemTags.SPEARS, "modular_spear");

                // mod repair tags (no vanilla equivalents)
                builder(ModMaterials.REPAIRS_BONE_TOOLS).add(key(Items.BONE), key(Items.BONE_BLOCK));
                builder(ModMaterials.REPAIRS_FLINT_TOOLS).add(key(Items.FLINT));
        }

        private static ResourceKey<Item> key(Item item) {
                return ResourceKey.create(Registries.ITEM,
                                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
        }

        private void tag(TagKey<Item> key, String... itemNames) {
                TagAppender<Item> appender = this.builder(key);
                for (String name : itemNames) {
                        appender.add(ResourceKey.create(Registries.ITEM, ModularDreams.id(name)));
                }
        }

        @Override
        public String getName() {
                return "Modular Dreams Tags";
        }
}
