package com.modulardreams.recipe;

import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import com.modulardreams.ModularDreams;

/**
 * Kiln recipe logic. The kiln has NO recipe entries of its own: it smelts
 * anything that vanilla smelting covers, except the domains of the two
 * specialist furnaces - nothing with a smoking (smoker) recipe and nothing
 * with a blasting (blast furnace) recipe qualifies. Every modded smelting
 * recipe is picked up automatically with zero configuration.
 *
 * <p>Matching is performed dynamically through a swapped-in
 * {@link RecipeManager.CachedCheck} on the kiln block entity (see
 * {@code modular_dreams.classtweaker}), so the vanilla furnace engine handles
 * burn/cook/XP exactly as before. Results, experience and fuel use come from
 * the underlying smelting recipe; the kiln simply cooks at twice the speed
 * via its block entity's speed multiplier.
 */
public final class ModRecipes {

    private ModRecipes() {}

    /**
     * The kiln's recipe check: the input's smelting recipe, if and only if
     * the input belongs to neither the smoker nor the blast furnace.
     */
    public static final RecipeManager.CachedCheck<SingleRecipeInput, AbstractCookingRecipe> KILN_CHECK =
            (SingleRecipeInput input, ServerLevel level) -> {
                RecipeManager manager = (RecipeManager) level.recipeAccess();

                Optional<RecipeHolder<SmeltingRecipe>> smelting =
                        manager.getRecipeFor(RecipeType.SMELTING, input, level);
                if (smelting.isEmpty()) {
                    return Optional.empty();
                }
                if (manager.getRecipeFor(RecipeType.SMOKING, input, level).isPresent()) {
                    return Optional.empty(); // smoker food
                }
                if (manager.getRecipeFor(RecipeType.BLASTING, input, level).isPresent()) {
                    return Optional.empty(); // ores / metals
                }
                RecipeHolder<SmeltingRecipe> recipe = smelting.get();
                return Optional.of(new RecipeHolder<>(recipe.id(), (AbstractCookingRecipe) recipe.value()));
            };

    /**
     * Whether this stack may cook in a kiln (mirrors {@link #KILN_CHECK} for
     * slot validation). Unknown or absent recipe sources stay permissive -
     * the server side re-checks on every cook.
     */
    public static boolean isKilnIngredient(Level level, ItemStack stack) {
        if (level == null) {
            return true;
        }
        if (!(level.recipeAccess() instanceof RecipeManager manager)) {
            return true;
        }
        if (!(manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent())) {
            return false;
        }
        if (manager.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level).isPresent()) {
            return false;
        }
        return manager.getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level).isEmpty();
    }

    public static void initialize() {
        // smithing table modifier serializer (recipes registered under the
        // vanilla SMITHING recipe type, so the vanilla smithing table uses
        // them natively)
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceKey.create(Registries.RECIPE_SERIALIZER, ModularDreams.id("smithing_modifier")),
                SmithingModifierRecipe.SERIALIZER);

        // netherite armor upgrade serializer (diamond-plated armor + 1 ingot
        // + vanilla template -> netherite-plated armor, vanilla consumption)
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceKey.create(Registries.RECIPE_SERIALIZER, ModularDreams.id("armor_netherite_upgrade")),
                ArmorNetheriteUpgradeRecipe.SERIALIZER);

        // dragon egg extraction (the egg-carrying tool ALONE in a crafting
        // grid -> tool minus egg minus its last modifier, plus the egg back)
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceKey.create(Registries.RECIPE_SERIALIZER, ModularDreams.id("dragon_egg_extract")),
                DragonEggExtractRecipe.SERIALIZER);

        // optional headless self-test, only with -Dmodular_dreams.selftest=true
        SmithingSelfTest.initialize();
    }
}
