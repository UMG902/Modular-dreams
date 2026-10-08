package com.modulardreams.client.itemmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4fc;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;

/**
 * Tinkers'-style modular tool item model (v2, per-material textures).
 *
 * <p>Every tool is rendered as a stack of part layers (handle + head), and
 * each layer picks the COLORED texture of the material that part was built
 * from (the v2 asset pipeline ships one texture per material per part
 * shape). No tinting needed.
 *
 * <p>The layers are declared per tool type in the client item definition
 * (assets/modular_dreams/items/modular_&lt;tool&gt;.json):
 * <pre>{@code
 * {
 *   "model": {
 *     "type": "modular_dreams:modular_tool",
 *     "layers": [
 *       { "part": "handle", "variants": {
 *           "wood": { "type": "minecraft:model", "model": "modular_dreams:item/modular/handle_wood" },
 *           "iron": { ... } } },
 *       { "part": "pickaxe_head", "variants": { ... } }
 *     ],
 *     "fallback": {
 *       "type": "minecraft:composite",
 *       "models": [ ... ]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>At bake time every variant is baked as a child; at render time
 * {@link #update} reads {@link ModularData} from the stack and delegates to
 * the child matching each part's material. Children append their quads in
 * declaration order (handle first, head last so the head overlaps).
 *
 * <p>The type id {@code modular_dreams:modular_tool} is registered into
 * {@code ItemModels.ID_MAPPER} (access widened via the classtweaker).
 */
public final class ModularToolItemModel implements ItemModel {

    /** part id -> (material id -> child model), in render order. */
    private final List<Map.Entry<String, Map<String, ItemModel>>> layers;
    private final ItemModel fallback;

    private ModularToolItemModel(List<Map.Entry<String, Map<String, ItemModel>>> layers, ItemModel fallback) {
        this.layers = layers;
        this.fallback = fallback;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
            ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
        state.appendModelIdentityElement(this);
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        boolean anyLayer = false;
        if (data != null) {
            for (Map.Entry<String, Map<String, ItemModel>> layer : layers) {
                String material = data.materialOf(layer.getKey()).orElse(null);
                ItemModel child = material == null ? null : layer.getValue().get(material);
                if (child != null) {
                    child.update(state, stack, resolver, displayContext, level, owner, seed);
                    anyLayer = true;
                }
            }
        }
        if (!anyLayer) {
            // unassembled (creative) stack or unknown materials -> neutral base model
            fallback.update(state, stack, resolver, displayContext, level, owner, seed);
        }
    }

    // ------------------------------------------------------------------ unbaked / codec

    public record LayerSpec(String part, Map<String, ItemModel.Unbaked> variants) {
        public static final MapCodec<LayerSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("part").forGetter(LayerSpec::part),
                Codec.unboundedMap(Codec.STRING, ItemModels.CODEC).fieldOf("variants")
                        .forGetter(LayerSpec::variants)
        ).apply(i, LayerSpec::new));
    }

    public record Unbaked(List<LayerSpec> layers, ItemModel.Unbaked fallback) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                LayerSpec.MAP_CODEC.codec().listOf().fieldOf("layers").forGetter(Unbaked::layers),
                // any inline item model (minecraft:composite, minecraft:model, ...)
                ItemModels.CODEC.fieldOf("fallback").forGetter(Unbaked::fallback)
        ).apply(i, Unbaked::new));

        /** Registers the custom item model type with vanilla's lazy type mapper. */
        public static void register() {
            ItemModels.ID_MAPPER.put(ModularDreams.id("modular_tool"), MAP_CODEC);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(net.minecraft.client.resources.model.ResolvableModel.Resolver resolver) {
            // every variant declares its own model id
            for (LayerSpec layer : layers) {
                for (ItemModel.Unbaked variant : layer.variants().values()) {
                    variant.resolveDependencies(resolver);
                }
            }
            fallback.resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc rootTransform) {
            List<Map.Entry<String, Map<String, ItemModel>>> built = new ArrayList<>();
            for (LayerSpec layer : layers) {
                Map<String, ItemModel> perMaterial = new LinkedHashMap<>();
                for (Map.Entry<String, ItemModel.Unbaked> variant : layer.variants().entrySet()) {
                    perMaterial.put(variant.getKey(), variant.getValue().bake(context, rootTransform));
                }
                built.add(Map.entry(layer.part(), perMaterial));
            }
            return new ModularToolItemModel(built, fallback.bake(context, rootTransform));
        }
    }
}
