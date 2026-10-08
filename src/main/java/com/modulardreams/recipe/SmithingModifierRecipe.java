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
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.material.ModTraits;

/**
 * A smithing table modifier of Modular Dreams v2: one modular tool plus a
 * set amount of one raw material (default 3) grants the material's trait to
 * the tool. Registering under the vanilla {@link RecipeType#SMITHING} makes
 * the vanilla smithing table show and craft these natively - no custom menu.
 *
 * <p>Matching rules:
 * <ul>
 *   <li>by default the template slot must be EMPTY - any template (including
 *       vanilla smithing templates) refuses the recipe, keeping vanilla
 *       template recipes unambiguous and slot 0 never eaten by mistake;</li>
 *   <li>a recipe WITH a required template (the netherite modifier needs the
 *       vanilla {@code netherite_upgrade_smithing_template}, exactly like a
 *       vanilla netherite upgrade) demands that item in the template slot,
 *       where vanilla consumes it on take;</li>
 *   <li>the base slot must hold an assembled modular tool;</li>
 *   <li>the addition slot must hold at least {@code count} materials;</li>
 *   <li>the tool must not already carry the trait - inherent traits (head and
 *       handle materials) and already-applied traits both refuse the recipe,
 *       so no materials are ever wasted on a no-op;</li>
 *   <li>a tool carries at most {@code base cap (3) + unlocks} applied
 *       modifiers - traits have no levels and the same modifier can never be
 *       applied twice;</li>
 *   <li>UNLOCK recipes ({@code unlock: 1/2/3} - nether star, elytra, dragon
 *       egg) expand the cap by one each and must be bought in order. They
 *       consume their (single) addition item. unlock 3 marks the tool as the
 *       dragon-egg legendary: indestructible item entity, and its egg can be
 *       extracted back out ({@code DragonEggExtractRecipe}), deleting the
 *       last modifier.</li>
 * </ul>
 *
 * <p>Taking the result consumes the template (vanilla template shrink) and
 * the full {@code count} of materials - the consumption amount is applied by
 * {@code SmithingMenuMixin}. The result is re-baked by {@code StatsEngine}
 * so baked stat modifiers (power/swift/haste/durable) apply immediately.
 */
public class SmithingModifierRecipe extends SimpleSmithingRecipe {

    public static final MapCodec<SmithingModifierRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("base").forGetter(SmithingModifierRecipe::base),
            Ingredient.CODEC.fieldOf("addition").forGetter(SmithingModifierRecipe::addition),
            Codec.INT.optionalFieldOf("count", 3).forGetter(SmithingModifierRecipe::additionCount),
            Codec.STRING.optionalFieldOf("trait", "").forGetter(SmithingModifierRecipe::trait),
            Ingredient.CODEC.optionalFieldOf("template").forGetter(SmithingModifierRecipe::requiredTemplate),
            Codec.INT.optionalFieldOf("unlock", 0).forGetter(SmithingModifierRecipe::unlockTier)
    ).apply(i, SmithingModifierRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SmithingModifierRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, SmithingModifierRecipe::base,
                    Ingredient.CONTENTS_STREAM_CODEC, SmithingModifierRecipe::addition,
                    ByteBufCodecs.VAR_INT, SmithingModifierRecipe::additionCount,
                    ByteBufCodecs.STRING_UTF8, SmithingModifierRecipe::trait,
                    ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), SmithingModifierRecipe::requiredTemplate,
                    ByteBufCodecs.VAR_INT, SmithingModifierRecipe::unlockTier,
                    SmithingModifierRecipe::new);

    /** Serializer registered as {@code modular_dreams:smithing_modifier}. */
    public static final RecipeSerializer<SmithingModifierRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient base;
    private final Ingredient addition;
    private final int additionCount;
    private final String trait;
    /** When present: the smithing template the slot 0 must hold (netherite upgrade). */
    private final Optional<Ingredient> requiredTemplate;
    /** 0 = a normal modifier; 1/2/3 = the nether star / elytra / dragon egg cap unlock. */
    private final int unlockTier;

    public SmithingModifierRecipe(Ingredient base, Ingredient addition, int additionCount, String trait,
            Optional<Ingredient> requiredTemplate, int unlockTier) {
        super(new Recipe.CommonInfo(true));
        this.base = base;
        this.addition = addition;
        this.additionCount = Math.max(1, additionCount);
        this.trait = trait;
        this.requiredTemplate = requiredTemplate;
        this.unlockTier = Math.max(0, Math.min(ModDataComponents.ModularData.MAX_UNLOCKS, unlockTier));
    }

    public Ingredient base() {
        return this.base;
    }

    public Ingredient addition() {
        return this.addition;
    }

    /** How many of the addition material one craft consumes (default 3). */
    public int additionCount() {
        return this.additionCount;
    }

    /** The trait id this modifier grants to the tool. */
    public String trait() {
        return this.trait;
    }

    /** The smithing template slot 0 must hold, or empty when the modifier needs none. */
    public Optional<Ingredient> requiredTemplate() {
        return this.requiredTemplate;
    }

    /** 0 = normal modifier; 1/2/3 = cap unlock with the nether star / elytra / dragon egg. */
    public int unlockTier() {
        return this.unlockTier;
    }

    // ------------------------------------------------------------------ matching

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (this.requiredTemplate.isPresent()) {
            if (!this.requiredTemplate.get().test(input.template())) {
                return false; // e.g. the netherite modifier demands its smithing template
            }
        } else if (!input.template().isEmpty()) {
            return false; // modifiers use no template
        }
        if (!this.base.test(input.base()) || !this.addition.test(input.addition())) {
            return false;
        }
        if (input.addition().getCount() < this.additionCount) {
            return false;
        }
        ModularData data = input.base().get(ModDataComponents.MODULAR_DATA);
        if (data == null || data.equipmentType().equals("none")) {
            return false; // only assembled tools take modifiers
        }
        if (this.unlockTier > 0) {
            // unlock tiers are strictly sequential: star -> elytra -> dragon egg
            return data.unlocks() == this.unlockTier - 1;
        }
        if (data.modifiers().size() >= data.modifierCap()) {
            return false; // the modifier cap (3, or 4/5/6 with the unlocks)
        }
        return !ModTraits.toolHasTrait(data, this.trait);
    }

    // ------------------------------------------------------------------ result

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack out = input.base().copy();
        out.setCount(1);
        ModularData data = out.get(ModDataComponents.MODULAR_DATA);
        if (data != null) {
            ModularData updated;
            if (this.unlockTier > 0) {
                // expand the modifier cap by one; unlock 3 carries the dragon egg
                updated = new ModularData(data.equipmentType(), data.parts(), data.modifiers(),
                        Math.min(ModDataComponents.ModularData.MAX_UNLOCKS, data.unlocks() + 1));
            } else {
                updated = ModTraits.withAppliedTrait(data, this.trait);
            }
            out.set(ModDataComponents.MODULAR_DATA, updated);
            // stat modifiers (power, swift, haste, durable) live in the baked
            // components - re-bake so the new trait takes effect immediately
            com.modulardreams.stats.StatsEngine.bake(out, updated);
        }
        return out;
    }

    // ------------------------------------------------------------------ smithing recipe plumbing

    @Override
    public Optional<Ingredient> templateIngredient() {
        // advertises the required template (if any) to the vanilla SMITHING_TEMPLATE
        // property set; a modifier without one advertises no template at all
        return this.requiredTemplate;
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
                List.of(this.requiredTemplate, Optional.of(this.base), Optional.of(this.addition)));
    }

    @Override
    public RecipeSerializer<? extends SimpleSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
