package com.modulardreams.recipe;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.material.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * The netherite armor upgrade of Modular Dreams v2 - the vanilla mirror for
 * modular armor: a DIAMOND-plated armor piece + 1 netherite ingot + the
 * vanilla {@code netherite_upgrade_smithing_template} becomes the same piece
 * with NETHERITE plating (vanilla netherite stats, fire resistant), keeping
 * its lining and every enchant it carries.
 *
 * <p>User-directed rule: the netherite upgrade ONLY applies to armor with
 * diamond plating - platings stop at diamond, so this recipe is the ONLY
 * path to netherite armor. It refuses every other plating material and
 * never touches modular tools (the base ingredient is the
 * {@code #modular_dreams:modular_armor} tag; the smithing modifiers use
 * {@code #modular_dreams:modular_tools}).
 *
 * <p>Consumption is fully vanilla: the template and the piece are consumed
 * by the standard smithing take, the addition is a plain 1-ingot cost (the
 * {@code SmithingMenuMixin} shrink only applies to
 * {@link SmithingModifierRecipe}s). The inherent traits follow from the
 * parts, so the upgraded piece naturally carries {@code netherite} instead
 * of {@code diamond}.
 */
public class ArmorNetheriteUpgradeRecipe extends SimpleSmithingRecipe {

    public static final MapCodec<ArmorNetheriteUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("base").forGetter(ArmorNetheriteUpgradeRecipe::base),
            Ingredient.CODEC.fieldOf("addition").forGetter(ArmorNetheriteUpgradeRecipe::addition),
            Ingredient.CODEC.fieldOf("template").forGetter(ArmorNetheriteUpgradeRecipe::requiredTemplate)
    ).apply(i, ArmorNetheriteUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorNetheriteUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, ArmorNetheriteUpgradeRecipe::base,
                    Ingredient.CONTENTS_STREAM_CODEC, ArmorNetheriteUpgradeRecipe::addition,
                    Ingredient.CONTENTS_STREAM_CODEC, ArmorNetheriteUpgradeRecipe::requiredTemplate,
                    ArmorNetheriteUpgradeRecipe::new);

    /** Serializer registered as {@code modular_dreams:armor_netherite_upgrade}. */
    public static final RecipeSerializer<ArmorNetheriteUpgradeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient base;
    private final Ingredient addition;
    private final Ingredient requiredTemplate;

    public ArmorNetheriteUpgradeRecipe(Ingredient base, Ingredient addition, Ingredient requiredTemplate) {
        super(new Recipe.CommonInfo(true));
        this.base = base;
        this.addition = addition;
        this.requiredTemplate = requiredTemplate;
    }

    public Ingredient base() {
        return this.base;
    }

    public Ingredient addition() {
        return this.addition;
    }

    public Ingredient requiredTemplate() {
        return this.requiredTemplate;
    }

    /** The plating material an armor piece must carry to qualify: diamond only. */
    public static boolean hasDiamondPlating(ItemStack armor) {
        ModularData data = armor.get(ModDataComponents.MODULAR_DATA);
        if (data == null) {
            return false;
        }
        return data.parts().stream()
                .filter(p -> !p.part().equals(StatsEngine.LINING_PART_ID))
                .map(ModDataComponents.PartData::material)
                .anyMatch(ModTraits.DIAMOND::equals);
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!this.requiredTemplate.test(input.template())) {
            return false; // the upgrade demands its vanilla smithing template
        }
        if (!this.base.test(input.base()) || !this.addition.test(input.addition())) {
            return false;
        }
        if (input.addition().getCount() < 1) {
            return false;
        }
        // the diamond-plating gate: only diamond-plated armor upgrades
        return hasDiamondPlating(input.base());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack out = input.base().copy();
        out.setCount(1);
        ModularData data = out.get(ModDataComponents.MODULAR_DATA);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        // swap the plating material to netherite, keep the lining - then the
        // standard bake rewrites every protective component to netherite
        // values and the inherent traits follow (diamond -> netherite)
        List<ModDataComponents.PartData> parts = data.parts().stream()
                .<ModDataComponents.PartData>map(p -> p.part().equals(StatsEngine.LINING_PART_ID) ? p
                        : new ModDataComponents.PartData(p.part(), "netherite"))
                .toList();
        ModularData upgraded = new ModularData(data.equipmentType(), List.copyOf(parts), data.modifiers(), data.unlocks());
        out.set(ModDataComponents.MODULAR_DATA, upgraded);
        StatsEngine.bake(out, upgraded);
        return out;
    }

    // ------------------------------------------------------------------ smithing recipe plumbing

    @Override
    public Optional<Ingredient> templateIngredient() {
        // advertises the netherite template to the vanilla SMITHING_TEMPLATE
        // property set, which is what makes slot 0 accept it
        return Optional.of(this.requiredTemplate);
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(
                List.of(Optional.of(this.requiredTemplate), Optional.of(this.base), Optional.of(this.addition)));
    }

    @Override
    public RecipeSerializer<? extends SimpleSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
