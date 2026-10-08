package com.modulardreams.registry;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.modulardreams.ModularDreams;
import com.modulardreams.block.ModBlocks;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModPlatingItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.menu.StationLogic;
import com.modulardreams.part.PartType;
import com.modulardreams.part.PlatingType;

/**
 * The "Modular Dreams" creative tab: stations, rose gold and
 * example equipment, followed by every part of every material.
 */
public class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> MODULAR_TAB_KEY = ResourceKey
            .create(Registries.CREATIVE_MODE_TAB, ModularDreams.id("modular_tab"));

    public static void initialize() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .icon(() -> new ItemStack(ModItems.ROSE_GOLD_INGOT))
                .title(Component.translatable("itemGroup.modular_dreams"))
                .displayItems((parameters, output) -> {
                    // stations
                    output.accept(ModBlocks.PART_PICKER_WOOD);
                    output.accept(ModBlocks.PART_PICKER_STONE);
                    output.accept(ModBlocks.PART_PICKER_COPPER);
                    output.accept(ModBlocks.PART_PICKER_IRON);
                    output.accept(ModBlocks.PART_PICKER_DIAMOND);
                    output.accept(ModBlocks.ASSEMBLER);
                    output.accept(ModBlocks.KILN);

                    // rose gold
                    output.accept(ModItems.ROSE_GOLD_INGOT);

                    // example assembled equipment (iron head + wood handle)
                    for (ModularToolType type : ModularToolType.values()) {
                        output.accept(ModItems.toolStack(type, "iron", "wood"));
                    }

                    // armor linings (wearable on their own, trait carriers)
                    for (Item lining : ModArmorItems.allLinings()) {
                        output.accept(lining);
                    }

                    // armor platings (slot-specific outer-layer parts)
                    for (Item plating : ModPlatingItems.allItems()) {
                        output.accept(plating);
                    }

                    // example assembled armor (iron plating + leather lining)
                    for (PlatingType plating : PlatingType.values()) {
                        output.accept(StationLogic.assembleArmor(plating, "iron", "leather"));
                    }

                    // every part, grouped by part shape, materials in registry order
                    for (PartType part : PartType.values()) {
                        List<ModularMaterial> materials = part.isHandle()
                                ? ModMaterials.handleMaterials()
                                : ModMaterials.headMaterials();
                        for (ModularMaterial material : materials) {
                            Item item = ModPartItems.find(part, material);
                            if (item == null) {
                                // never crash the render thread; log loudly instead
                                ModularDreams.LOGGER.warn(
                                        "Creative tab: missing part item for {} {}, skipping",
                                        material.id(), part.id);
                                continue;
                            }
                            output.accept(item);
                        }
                    }
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MODULAR_TAB_KEY, tab);
    }
}
