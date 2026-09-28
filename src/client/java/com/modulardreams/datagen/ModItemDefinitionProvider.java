package com.modulardreams.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import com.modulardreams.ModularDreams;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PartType;

/**
 * Generates the client item definitions under assets/modular_dreams/items/.
 *
 * - Tools: ONE definition per tool type using the custom
 *   {@code modular_dreams:modular_tool} item model. Each layer (handle,
 *   binding, head) carries one pre-tinted child model per material built from
 *   the SHARED grayscale part texture + a constant material tint
 *   (Tinkers'-style, no per-material textures).
 * - Part items: grayscale part texture + constant material tint.
 * - Molds / guide book: plain models.
 */
public class ModItemDefinitionProvider extends FabricCodecDataProvider<ClientItem> {

        protected ModItemDefinitionProvider(FabricPackOutput output,
                        CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, registriesFuture, PackOutput.Target.RESOURCE_PACK, "items", ClientItem.CODEC);
        }

        @Override
        protected void configure(java.util.function.BiConsumer<Identifier, ClientItem> consumer,
                        HolderLookup.Provider provider) {
                // ---- part items: grayscale texture + material tint ----
                for (PartType part : PartType.values()) {
                        for (ModularMaterial material : part.allowedMaterials()) {
                                Identifier id = ModularDreams.id(material.id() + "_" + part.id);
                                consumer.accept(id, new ClientItem(
                                                ItemModelUtils.tintedModel(ModularDreams.id(part.id),
                                                                ItemModelUtils.constantTint(material.color())),
                                                ClientItem.Properties.DEFAULT));
                        }
                }
        }

        @Override
        public String getName() {
                return "Modular Dreams Item Definitions";
        }
}
