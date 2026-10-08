package com.modulardreams.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.equipment.ModItems;
import com.modulardreams.equipment.ModPartItems;
import com.modulardreams.equipment.ModPlatingItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModTraits;
import com.modulardreams.menu.AssemblerMenu;
import com.modulardreams.menu.StationLogic;
import com.modulardreams.part.PlatingType;
import com.modulardreams.stats.StatsEngine;

/**
 * Headless self-test of the smithing table modifier feature, run once when a
 * server starts IF the system property {@code -Dmodular_dreams.selftest=true}
 * is set - production servers are unaffected.
 *
 * <p>It exercises the real vanilla code paths end to end: recipe resolution
 * through the vanilla smithing lookup, assemble, the negative rules, and -
 * using a minimal offline player - a full {@link SmithingMenu} take so the
 * {@code SmithingMenuMixin} consumption is proven (3 materials for our
 * modifiers, still exactly 1 for the vanilla netherite upgrade).
 */
public final class SmithingSelfTest {

    private final List<String> failures = new ArrayList<>();
    private int checks;

    private SmithingSelfTest() {}

    public static void initialize() {
        boolean enabled = Boolean.getBoolean("modular_dreams.selftest")
                || "true".equals(System.getenv("MODULAR_DREAMS_SELFTEST"));
        if (!enabled) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new SmithingSelfTest().run(server));
    }

    // ------------------------------------------------------------------ run

    private void run(MinecraftServer server) {
        String tag = "[SmithingSelfTest] ";
        try {
            // force the target class to load now: the required mixin either
            // applies cleanly here or the failure is loud and immediate
            Class.forName("net.minecraft.world.inventory.SmithingMenu");

            ServerLevel level = server.overworld();
            RecipeManager manager = (RecipeManager) level.recipeAccess();

            ItemStack tool = ModItems.toolStack(ModularToolType.PICKAXE, "wood", "blaze_rod");

            // --- A: resolution + assemble through the vanilla smithing lookup ---
            SmithingRecipeInput input =
                    new SmithingRecipeInput(ItemStack.EMPTY, tool, new ItemStack(Items.QUARTZ, 3));
            Optional<RecipeHolder<SmithingRecipe>> found =
                    manager.getRecipeFor(RecipeType.SMITHING, input, level);
            check(found.isPresent(), "quartz modifier recipe resolves in the vanilla smithing lookup");
            if (found.isPresent()) {
                check(found.get().value() instanceof SmithingModifierRecipe,
                        "resolved recipe is a SmithingModifierRecipe");
                ItemStack out = found.get().value().assemble(input);
                ModularData outData = out.get(ModDataComponents.MODULAR_DATA);
                check(outData != null && ModTraits.appliedTraits(outData).contains("quartz"),
                        "assembled output carries the quartz trait");
                check(outData != null && "pickaxe".equals(outData.equipmentType()),
                        "output keeps its equipment type");
                check(Math.abs(attackModifierAmount(out)
                                - (attackModifierAmount(tool)
                                        + com.modulardreams.traits.TraitTuning.POWER_DAMAGE)) < 1e-4,
                        "the modifier output is re-baked with the power bonus");
                check(out.getMaxDamage() > 0, "output keeps its baked stats (max damage)");
            }

            // --- B: negative rules ---
            SmithingRecipeInput withTemplate = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    tool, new ItemStack(Items.QUARTZ, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, withTemplate, level).isEmpty(),
                    "a filled template slot refuses modifier recipes");

            SmithingRecipeInput twoQuartz =
                    new SmithingRecipeInput(ItemStack.EMPTY, tool, new ItemStack(Items.QUARTZ, 2));
            check(manager.getRecipeFor(RecipeType.SMITHING, twoQuartz, level).isEmpty(),
                    "2 materials do not satisfy a 3-material modifier");

            ItemStack traited = ModItems.toolStack(ModularToolType.PICKAXE, "wood", "blaze_rod");
            traited.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(traited.get(ModDataComponents.MODULAR_DATA), "quartz"));
            SmithingRecipeInput alreadyTrait =
                    new SmithingRecipeInput(ItemStack.EMPTY, traited, new ItemStack(Items.QUARTZ, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, alreadyTrait, level).isEmpty(),
                    "a tool that already has the quartz trait refuses it again");

            SmithingRecipeInput planks =
                    new SmithingRecipeInput(ItemStack.EMPTY, tool, new ItemStack(Items.OAK_PLANKS, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, planks, level).isEmpty(),
                    "the wood trait is inherent to a wood head, planks are refused");

            ItemStack blankTool = new ItemStack(ModItems.toolItem(ModularToolType.PICKAXE));
            SmithingRecipeInput unassembled =
                    new SmithingRecipeInput(ItemStack.EMPTY, blankTool, new ItemStack(Items.QUARTZ, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, unassembled, level).isEmpty(),
                    "an unassembled tool is refused");

            // --- C: vanilla smithing stays intact ---
            SmithingRecipeInput vanilla = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    new ItemStack(Items.DIAMOND_SWORD),
                    new ItemStack(Items.NETHERITE_INGOT));
            Optional<RecipeHolder<SmithingRecipe>> vanillaFound =
                    manager.getRecipeFor(RecipeType.SMITHING, vanilla, level);
            check(vanillaFound.isPresent() && !(vanillaFound.get().value() instanceof SmithingModifierRecipe),
                    "vanilla netherite upgrade still resolves and is not a modifier");

            // --- D: a real menu take consumes 3 of 3 for our modifier ---
            Player player = new TestPlayer(level);
            SmithingMenu menu = new SmithingMenu(1, player.getInventory());
            menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(ItemStack.EMPTY);
            menu.getSlot(SmithingMenu.BASE_SLOT).set(tool.copy());
            menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.QUARTZ, 3));
            menu.createResult();
            ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            ModularData resultData = result.get(ModDataComponents.MODULAR_DATA);
            check(resultData != null && ModTraits.appliedTraits(resultData).contains("quartz"),
                    "vanilla menu displays the trait-ed tool as its output");
            menu.getSlot(SmithingMenu.RESULT_SLOT).onTake(player, result);
            check(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty(),
                    "empty template slot stays empty on take");
            check(menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(),
                    "base tool is consumed on take");
            check(menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(),
                    "addition consumed 3 of 3 (mixin shrink)");

            // --- E: a vanilla take still consumes exactly 1 addition ---
            SmithingMenu vanillaMenu = new SmithingMenu(2, player.getInventory());
            vanillaMenu.getSlot(SmithingMenu.TEMPLATE_SLOT)
                    .set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            vanillaMenu.getSlot(SmithingMenu.BASE_SLOT).set(new ItemStack(Items.DIAMOND_SWORD));
            vanillaMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.NETHERITE_INGOT, 5));
            vanillaMenu.createResult();
            ItemStack vanillaResult = vanillaMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            check(vanillaResult.is(Items.NETHERITE_SWORD), "vanilla menu upgrades the diamond sword");
            vanillaMenu.getSlot(SmithingMenu.RESULT_SLOT).onTake(player, vanillaResult);
            check(vanillaMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount() == 4,
                    "vanilla take still consumes exactly 1 addition (5 -> 4)");
            check(vanillaMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
                    && vanillaMenu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(),
                    "vanilla take consumes template and base as before");

            // --- G: the netherite modifier now demands its vanilla smithing template ---
            SmithingRecipeInput netheriteOk = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), tool,
                    new ItemStack(Items.NETHERITE_INGOT));
            Optional<RecipeHolder<SmithingRecipe>> netheriteFound =
                    manager.getRecipeFor(RecipeType.SMITHING, netheriteOk, level);
            check(netheriteFound.isPresent() && netheriteFound.get().value() instanceof SmithingModifierRecipe,
                    "the netherite modifier resolves with the vanilla smithing template");

            SmithingRecipeInput netheriteNoTemplate = new SmithingRecipeInput(
                    ItemStack.EMPTY, tool, new ItemStack(Items.NETHERITE_INGOT));
            check(manager.getRecipeFor(RecipeType.SMITHING, netheriteNoTemplate, level).isEmpty(),
                    "the netherite modifier refuses an empty template slot");

            SmithingMenu netMenu = new SmithingMenu(5, player.getInventory());
            netMenu.getSlot(SmithingMenu.TEMPLATE_SLOT)
                    .set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            netMenu.getSlot(SmithingMenu.BASE_SLOT).set(tool.copy());
            netMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.NETHERITE_INGOT, 2));
            netMenu.createResult();
            ItemStack netResult = netMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            ModularData netResultData = netResult.get(ModDataComponents.MODULAR_DATA);
            check(netResultData != null && ModTraits.appliedTraits(netResultData).contains("netherite"),
                    "template netherite craft displays the trait-ed tool");
            netMenu.getSlot(SmithingMenu.RESULT_SLOT).onTake(player, netResult);
            check(netMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty(),
                    "the smithing template is consumed on take (vanilla parity)");
            check(netMenu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(),
                    "base tool consumed on the netherite take");
            check(netMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount() == 1,
                    "netherite take consumes exactly 1 ingot (2 -> 1)");

            // --- H: armor linings - wearable everywhere, no armor value, trait carriers ---
            check(ModTraits.ALL.contains(ModTraits.WOOL) && ModTraits.ALL.contains(ModTraits.MAGMA_CREAM)
                    && ModTraits.ALL.contains(ModTraits.PHANTOM_MEMBRANE),
                    "wool, magma cream and phantom membrane joined the trait vocabulary");
            check(ModArmorItems.allLinings().size() == 5, "all 5 lining items are registered");

            Item leatherLining = ModArmorItems.lining("leather");
            ItemStack liningStack = new ItemStack(leatherLining);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                check(player.isEquippableInSlot(liningStack, slot),
                        "a lining is equippable in " + slot);
            }
            check(!player.isEquippableInSlot(liningStack, EquipmentSlot.MAINHAND),
                    "a lining is NOT equippable in the main hand");
            // 26.3 gives EVERY item an empty ItemAttributeModifiers component by
            // default (plain leather and sticks included) - "no armor value"
            // therefore means: the modifier list is empty, never any entries
            net.minecraft.world.item.component.ItemAttributeModifiers liningAttrs =
                    liningStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            check(liningAttrs == null || liningAttrs.modifiers().isEmpty(),
                    "a lining grants no attribute modifiers (no armor value)");

            long armorSlots = player.inventoryMenu.slots.stream().filter(s -> s instanceof ArmorSlot).count();
            check(armorSlots == 4, "the inventory menu exposes 4 armor slots");
            boolean allAccept = player.inventoryMenu.slots.stream()
                    .filter(s -> s instanceof ArmorSlot)
                    .allMatch(s -> s.mayPlace(liningStack));
            check(allAccept, "every armor slot accepts a lining (isEquippableInSlot gate)");
            ItemStack nonLining = new ItemStack(Items.LEATHER);
            boolean nonLiningRefused = player.inventoryMenu.slots.stream()
                    .filter(s -> s instanceof ArmorSlot)
                    .allMatch(s -> !s.mayPlace(nonLining));
            check(nonLiningRefused, "plain leather still refuses every armor slot (vanilla untouched)");

            // right-click equips into the first EMPTY slot, in order head -> chest -> legs -> feet
            Player liningPlayer = new TestPlayer(level);
            ItemStack fourLinings = new ItemStack(leatherLining, 4);
            liningPlayer.setItemInHand(InteractionHand.MAIN_HAND, fourLinings);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                InteractionResult r = leatherLining.use(level, liningPlayer, InteractionHand.MAIN_HAND);
                check(r.consumesAction() && liningPlayer.getItemBySlot(slot).is(leatherLining),
                        "right-click equips the lining into the empty " + slot);
            }
            check(liningPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "the 4-lining hand stack is fully equipped");
            check(!leatherLining.use(level, liningPlayer, InteractionHand.MAIN_HAND).consumesAction(),
                    "a fully armored player cannot equip another lining");
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                check(!liningPlayer.getItemBySlot(slot).isEmpty(),
                        "the worn lining stays in " + slot);
            }

            // --- I: armor platings - slot-specific parts, no armor value, no netherite ---
            check(ModPlatingItems.allItems().size() == 28, "all 28 plating items are registered (4 slots x 7 materials)");
            check(ModPlatingItems.platingMaterials().size() == 7
                    && ModPlatingItems.platingMaterials().stream().noneMatch(m -> m.id().equals("netherite")),
                    "platings exist in exactly the 7 non-netherite full materials");
            check(ModPlatingItems.find(PlatingType.HELMET_PLATING, ModMaterials.getOrThrow("netherite")) == null,
                    "no netherite plating exists (netherite armor is upgrade-only)");

            ItemStack helmetPlating = StationLogic.makePlating(PlatingType.HELMET_PLATING,
                    ModMaterials.getOrThrow("iron"));
            check(helmetPlating.is(ModPlatingItems.getOrThrow("helmet_plating", "iron")),
                    "makePlating builds the right plating item");
            Optional<ModPlatingItems.PlatingIdentity> identity = ModPlatingItems.resolve(helmetPlating);
            check(identity.isPresent() && identity.get().plating() == PlatingType.HELMET_PLATING
                    && identity.get().material().id().equals("iron")
                    && identity.get().plating().slot == EquipmentSlot.HEAD,
                    "plating resolve() recovers slot type, material and its armor slot");
            net.minecraft.world.item.component.ItemAttributeModifiers platingAttrs =
                    helmetPlating.get(DataComponents.ATTRIBUTE_MODIFIERS);
            check(platingAttrs == null || platingAttrs.modifiers().isEmpty(),
                    "a plating grants no attribute modifiers on its own");
            boolean platingRefused = player.inventoryMenu.slots.stream()
                    .filter(s -> s instanceof ArmorSlot)
                    .allMatch(s -> !s.mayPlace(helmetPlating));
            check(platingRefused, "a plating is NOT wearable in any armor slot (that is the lining's job)");

            // part picker option enumeration
            List<StationLogic.Option> ironOptions = StationLogic.optionsFor(
                    ModMaterials.getOrThrow("iron"), 4);
            check(ironOptions.size() == 11, "a full material offers 11 options (handle + 6 heads + 4 platings)");
            check(ironOptions.subList(7, 11).stream().map(o -> o.prototype().getItem())
                    .toList().equals(List.of(
                            ModPlatingItems.get(PlatingType.HELMET_PLATING, ModMaterials.getOrThrow("iron")),
                            ModPlatingItems.get(PlatingType.CHESTPLATE_PLATING, ModMaterials.getOrThrow("iron")),
                            ModPlatingItems.get(PlatingType.LEGGINGS_PLATING, ModMaterials.getOrThrow("iron")),
                            ModPlatingItems.get(PlatingType.BOOTS_PLATING, ModMaterials.getOrThrow("iron")))),
                    "the plating options come last, in helmet/chestplate/leggings/boots order");
            check(ironOptions.subList(7, 11).stream().map(StationLogic.Option::cost).toList()
                    .equals(List.of(5, 8, 7, 4)),
                    "plating costs are vanilla armor costs (5/8/7/4)");

            List<StationLogic.Option> netheriteOptions = StationLogic.optionsFor(
                    ModMaterials.getOrThrow("netherite"), 5);
            check(netheriteOptions.size() == 7
                    && netheriteOptions.stream().noneMatch(o -> ModPlatingItems.resolve(o.prototype()).isPresent()),
                    "netherite offers tool parts but NO plating options");

            List<StationLogic.Option> rodOptions = StationLogic.optionsFor(
                    ModMaterials.getOrThrow("blaze_rod"), 1);
            check(rodOptions.size() == 1 && rodOptions.get(0).prototype().is(ModPartItems.getOrThrow("handle", "blaze_rod")),
                    "a handle-only rod offers exactly its handle (no phantom head options)");

            check(StationLogic.optionsFor(ModMaterials.getOrThrow("diamond"), 4).size() == 11,
                    "the iron-tier picker already shapes diamond platings (tier +1 rule)");
            check(StationLogic.optionsFor(ModMaterials.getOrThrow("diamond"), 3).isEmpty(),
                    "the copper-tier picker refuses diamond (tier +1 rule)");
            check(StationLogic.optionsFor(ModMaterials.getOrThrow("diamond"), 5).size() == 11,
                    "the diamond picker shapes diamond platings");

            // --- J: armor assembly - plating + lining -> real armor pieces ---
            check(ModArmorItems.isPiece(new ItemStack(ModArmorItems.piece(PlatingType.BOOTS_PLATING))),
                    "all 4 modular armor piece items are registered");

            ItemStack helmet = StationLogic.assembleArmor(PlatingType.HELMET_PLATING, "iron", "leather");
            ModularData helmData = helmet.get(ModDataComponents.MODULAR_DATA);
            check(helmData != null && "helmet".equals(helmData.equipmentType()),
                    "assembled armor keeps its equipment type (helmet)");
            check(helmData != null && "leather".equals(helmData.materialOf("lining").orElse(""))
                    && "iron".equals(helmData.materialOf("helmet_plating").orElse("")),
                    "assembled armor records the lining + plating parts");

            net.minecraft.world.item.component.ItemAttributeModifiers helmAttrs =
                    helmet.get(DataComponents.ATTRIBUTE_MODIFIERS);
            double helmArmor = helmAttrs == null ? 0 : helmAttrs.modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            check(helmArmor == 2.0, "iron helmet grants the vanilla iron armor value (2)");
            check(helmet.get(DataComponents.MAX_DAMAGE) == 165,
                    "iron helmet durability is vanilla (165 = 11 x 15)");
            net.minecraft.world.item.equipment.Equippable helmEquip = helmet.get(DataComponents.EQUIPPABLE);
            check(helmEquip != null && helmEquip.slot() == EquipmentSlot.HEAD
                    && player.isEquippableInSlot(helmet, EquipmentSlot.HEAD)
                    && !player.isEquippableInSlot(helmet, EquipmentSlot.CHEST),
                    "assembled armor is wearable in exactly its own slot");
            check(ModTraits.inherentTraits(helmData).containsAll(List.of("leather", "iron")),
                    "assembled armor carries the plating trait AND the lining trait");

            ItemStack diaChest = StationLogic.assembleArmor(PlatingType.CHESTPLATE_PLATING, "diamond", "wool");
            double chestArmor = diaChest.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            check(chestArmor == 8.0, "diamond chestplate grants the vanilla diamond armor value (8)");
            ItemStack woodBoots = StationLogic.assembleArmor(PlatingType.BOOTS_PLATING, "wood", "leather");
            double bootsArmor = woodBoots.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            check(bootsArmor == 1.0 && woodBoots.get(DataComponents.MAX_DAMAGE) == 65,
                    "wood boots sit at the leather tier (1 armor, 65 durability)");

            // --- K: the netherite armor upgrade (diamond plating gate) ---
            ItemStack diaHelmet = StationLogic.assembleArmor(PlatingType.HELMET_PLATING, "diamond", "leather");
            SmithingRecipeInput upgradeInput = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), diaHelmet,
                    new ItemStack(Items.NETHERITE_INGOT));
            Optional<RecipeHolder<SmithingRecipe>> upFound =
                    manager.getRecipeFor(RecipeType.SMITHING, upgradeInput, level);
            check(upFound.isPresent() && upFound.get().value() instanceof ArmorNetheriteUpgradeRecipe,
                    "the netherite armor upgrade resolves for diamond-plated armor");
            ItemStack upgraded = upFound.map(h -> h.value().assemble(upgradeInput)).orElse(ItemStack.EMPTY);
            ModularData upData = upgraded.get(ModDataComponents.MODULAR_DATA);
            check(upData != null && "netherite".equals(upData.materialOf("helmet_plating").orElse(""))
                    && "leather".equals(upData.materialOf("lining").orElse("")),
                    "the upgrade swaps the plating to netherite and keeps the lining");
            check(ModTraits.inherentTraits(upData).contains("netherite")
                    && !ModTraits.inherentTraits(upData).contains("diamond"),
                    "the upgraded armor carries netherite instead of diamond");
            double upArmor = upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            double upToughness = upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR_TOUGHNESS)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            double upKb = upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.KNOCKBACK_RESISTANCE)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            check(upArmor == 3.0 && upToughness == 3.0 && Math.abs(upKb - 0.1) < 1e-6
                    && upgraded.get(DataComponents.MAX_DAMAGE) == 407,
                    "netherite helmet gets vanilla netherite stats (3 armor, 3 toughness, 0.1 kb, 407 durability)");
            check(upgraded.has(DataComponents.DAMAGE_RESISTANT),
                    "netherite armor is fire resistant");

            ItemStack ironPlatedRefusal = StationLogic.assembleArmor(PlatingType.LEGGINGS_PLATING, "iron", "slime");
            SmithingRecipeInput ironUpgrade = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), ironPlatedRefusal,
                    new ItemStack(Items.NETHERITE_INGOT));
            check(manager.getRecipeFor(RecipeType.SMITHING, ironUpgrade, level).isEmpty(),
                    "non-diamond-plated armor refuses the netherite upgrade");
            SmithingRecipeInput noTemplateUpgrade = new SmithingRecipeInput(
                    ItemStack.EMPTY, diaHelmet, new ItemStack(Items.NETHERITE_INGOT));
            check(manager.getRecipeFor(RecipeType.SMITHING, noTemplateUpgrade, level).isEmpty(),
                    "the armor upgrade refuses an empty template slot");
            SmithingRecipeInput toolBase = new SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), tool,
                    new ItemStack(Items.NETHERITE_INGOT));
            Optional<RecipeHolder<SmithingRecipe>> toolBaseFound =
                    manager.getRecipeFor(RecipeType.SMITHING, toolBase, level);
            check(toolBaseFound.isPresent() && !(toolBaseFound.get().value() instanceof ArmorNetheriteUpgradeRecipe),
                    "the armor upgrade never claims a tool base (tool modifier keeps it)");
            SmithingRecipeInput armorAsModifier = new SmithingRecipeInput(
                    ItemStack.EMPTY, diaHelmet, new ItemStack(Items.QUARTZ, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, armorAsModifier, level).isEmpty(),
                    "the modifier system still refuses armor bases (no armor modifiers)");

            // a real menu take consumes template + piece + exactly 1 ingot
            SmithingMenu armorMenu = new SmithingMenu(6, player.getInventory());
            armorMenu.getSlot(SmithingMenu.TEMPLATE_SLOT)
                    .set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            armorMenu.getSlot(SmithingMenu.BASE_SLOT).set(diaHelmet.copy());
            armorMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.NETHERITE_INGOT, 2));
            armorMenu.createResult();
            ItemStack armorResult = armorMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            check(armorResult.is(ModArmorItems.piece(PlatingType.HELMET_PLATING)),
                    "the vanilla menu displays the upgraded armor");
            armorMenu.getSlot(SmithingMenu.RESULT_SLOT).onTake(player, armorResult);
            check(armorMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
                    && armorMenu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty()
                    && armorMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount() == 1,
                    "the armor take consumes template + piece + exactly 1 ingot (vanilla parity)");

            // --- L: the assembler builds armor from lining + plating ---
            AssemblerMenu assembler = new AssemblerMenu(7, player.getInventory(),
                    net.minecraft.core.BlockPos.ZERO);
            check(assembler.getSlot(AssemblerMenu.HANDLE_SLOT).mayPlace(new ItemStack(ModArmorItems.lining("wool"))),
                    "the assembler handle slot accepts linings");
            check(assembler.getSlot(AssemblerMenu.HEAD_SLOT).mayPlace(
                    new ItemStack(ModPlatingItems.get(PlatingType.CHESTPLATE_PLATING, ModMaterials.getOrThrow("gold")))),
                    "the assembler head slot accepts platings");
            assembler.getSlot(AssemblerMenu.HANDLE_SLOT).set(new ItemStack(ModArmorItems.lining("wool")));
            assembler.getSlot(AssemblerMenu.HEAD_SLOT).set(
                    new ItemStack(ModPlatingItems.get(PlatingType.CHESTPLATE_PLATING, ModMaterials.getOrThrow("gold"))));
            assembler.slotsChanged(assembler.getSlot(AssemblerMenu.HANDLE_SLOT).container);
            ItemStack armorOut = assembler.getSlot(AssemblerMenu.TOOL_RESULT_SLOT).getItem();
            ModularData armorOutData = armorOut.get(ModDataComponents.MODULAR_DATA);
            check(armorOutData != null && "chestplate".equals(armorOutData.equipmentType())
                    && "wool".equals(armorOutData.materialOf("lining").orElse(""))
                    && "gold".equals(armorOutData.materialOf("chestplate_plating").orElse("")),
                    "lining + plating assemble into the chestplate in the real menu");
            assembler.getSlot(AssemblerMenu.TOOL_RESULT_SLOT).onTake(player, armorOut);
            check(assembler.getSlot(AssemblerMenu.HANDLE_SLOT).getItem().isEmpty()
                    && assembler.getSlot(AssemblerMenu.HEAD_SLOT).getItem().isEmpty(),
                    "the armor take consumes the lining + plating");

            // --- M: trait effects -------------------------------------------------
            // M1: quartz "Power" bakes +1 attack damage (no levels: once)
            ItemStack powerBase = ModItems.toolStack(ModularToolType.PICKAXE, "iron", "wood");
            float baseDamage = attackModifierAmount(powerBase);
            ItemStack powered = powerBase.copy();
            powered.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(powered.get(ModDataComponents.MODULAR_DATA), "quartz"));
            StatsEngine.bake(powered, powered.get(ModDataComponents.MODULAR_DATA));
            check(Math.abs(attackModifierAmount(powered) - baseDamage
                    - com.modulardreams.traits.TraitTuning.POWER_DAMAGE) < 1e-4,
                    "quartz modifier bakes +1.0 attack damage into the tool");

            // M2: flint "Swift" bakes +0.25 attack speed (no levels: once)
            ItemStack swifted = powerBase.copy();
            swifted.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(swifted.get(ModDataComponents.MODULAR_DATA), "flint"));
            StatsEngine.bake(swifted, swifted.get(ModDataComponents.MODULAR_DATA));
            check(Math.abs(attackSpeedModifierAmount(swifted) - attackSpeedModifierAmount(powerBase)
                    - com.modulardreams.traits.TraitTuning.SWIFT_SPEED) < 1e-4,
                    "flint modifier bakes +0.25 attack speed into the tool");

            // M2b: the same modifier can never be applied twice (no levels)
            ItemStack doubled = powered.copy();
            doubled.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(doubled.get(ModDataComponents.MODULAR_DATA), "quartz"));
            check(Math.abs(attackModifierAmount(doubled) - attackModifierAmount(powered)) < 1e-4,
                    "re-applying quartz never stacks a second Power");

            // M3: redstone "Haste" bakes +15% mining speed as a multiplied-base
            // entry on BLOCK_BREAK_SPEED (the vanilla speed multiplier attribute)
            ItemStack hasted = powerBase.copy();
            hasted.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(hasted.get(ModDataComponents.MODULAR_DATA), "redstone"));
            StatsEngine.bake(hasted, hasted.get(ModDataComponents.MODULAR_DATA));
            java.util.List<net.minecraft.world.item.component.ItemAttributeModifiers.Entry> hasteEntries =
                    hasted.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.BLOCK_BREAK_SPEED).toList();
            check(hasteEntries.size() == 1
                            && hasteEntries.get(0).modifier().amount() == com.modulardreams.traits.TraitTuning.HASTE_BREAK_MULT
                            && hasteEntries.get(0).modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "redstone modifier bakes +15% mining speed on the break-speed multiplier");

            // M4: emerald "Reinforced" bakes +20% max durability
            ItemStack durabled = powerBase.copy();
            durabled.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(durabled.get(ModDataComponents.MODULAR_DATA), "emerald"));
            StatsEngine.bake(durabled, durabled.get(ModDataComponents.MODULAR_DATA));
            int baseMax = powerBase.getMaxDamage();
            check(durabled.getMaxDamage() == Math.round(baseMax * 1.2F),
                    "emerald modifier bakes +20% max durability into the tool");

            // M5: worn armor traits aggregate (plating + lining)
            player.setItemSlot(EquipmentSlot.HEAD, StationLogic.assembleArmor(
                    PlatingType.HELMET_PLATING, "iron", "leather"));
            var worn = com.modulardreams.traits.TraitAggregator.wornTraits(player);
            check(worn.contains("iron") && worn.contains("leather"),
                    "worn armor aggregates the plating trait and the lining trait");

            // M6: a standalone lining worn on its own carries its trait
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmorItems.lining("wool")));
            check(com.modulardreams.traits.TraitAggregator.wornTraits(player).contains("wool"),
                    "a standalone worn lining carries its trait");

            // M7: gold-trait items anywhere in the inventory count
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            ItemStack goldPlating = StationLogic.makePlating(PlatingType.CHESTPLATE_PLATING,
                    ModMaterials.getOrThrow("gold"));
            player.getInventory().add(goldPlating);
            check(com.modulardreams.traits.TraitAggregator.hasTraitInInventory(player, "gold"),
                    "a gold plating item in the inventory carries the gold trait");

            // M8: piglins - gold trait pacifies, rose gold (Defiant) always hostile
            Player piglinPlain = new TestPlayer(level);
            check(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(piglinPlain),
                    "a plain player does not pacify piglins");
            Player piglinGold = new TestPlayer(level);
            piglinGold.getInventory().add(StationLogic.makePlating(PlatingType.CHESTPLATE_PLATING,
                    ModMaterials.getOrThrow("gold")));
            check(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(piglinGold),
                    "any gold-trait item in the inventory pacifies piglins");
            Player piglinDefiant = new TestPlayer(level);
            piglinDefiant.getInventory().add(StationLogic.makePlating(PlatingType.CHESTPLATE_PLATING,
                    ModMaterials.getOrThrow("gold")));
            piglinDefiant.getInventory().add(StationLogic.makePlating(PlatingType.BOOTS_PLATING,
                    ModMaterials.getOrThrow("rose_gold")));
            check(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(piglinDefiant),
                    "rose gold (Defiant) overrides gold - piglins stay hostile");

            // M9: luck - the lapis modifier reads as Fortune/Looting level
            ItemStack lucky = ModItems.toolStack(ModularToolType.PICKAXE, "iron", "wood");
            lucky.set(ModDataComponents.MODULAR_DATA,
                    ModTraits.withAppliedTrait(lucky.get(ModDataComponents.MODULAR_DATA), "lapis"));
            check(com.modulardreams.traits.LuckHooks.fortuneBonus(lucky) == 1,
                    "the lapis modifier counts as Fortune level 1 for block drops");
            lucky.setCount(1);
            player.setItemSlot(EquipmentSlot.MAINHAND, lucky);
            check(com.modulardreams.traits.LuckHooks.lootingBonus(player) == 1,
                    "a held lapis-trait tool counts as Looting level 1 for kills");
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            check(com.modulardreams.traits.LuckHooks.fortuneBonus(powerBase) == 0,
                    "tools without lapis get no luck bonus");

            // M10: blazing smelt helper resolves vanilla smelting recipes
            var smelted = com.modulardreams.traits.DropHooks.smelted(level,
                    new ItemStack(Items.RAW_IRON, 3));
            check(smelted.isPresent() && smelted.get().is(Items.IRON_INGOT) && smelted.get().getCount() == 3,
                    "blazing auto-smelt resolves raw iron to iron ingots with count kept");
            check(com.modulardreams.traits.DropHooks.smelted(level, new ItemStack(Items.DIRT)).isEmpty(),
                    "blocks without a smelting recipe stay untouched by blazing");

            // M11: wood regenerative repairs damaged wood-trait stacks
            Player woodPlayer = new TestPlayer(level);
            woodPlayer.getInventory().add(ModItems.toolStack(ModularToolType.PICKAXE, "wood", "wood"));
            woodPlayer.getInventory().getItem(0).setDamageValue(5);
            com.modulardreams.traits.TraitEngine.repairWoodItems(woodPlayer);
            check(woodPlayer.getInventory().getItem(0).getDamageValue() == 4,
                    "wood regenerative repairs damaged wood-trait items by 1");

            // M12: phantom void - ONE lining breaks per save, plating stays
            Player phantomPlayer = new TestPlayer(level);
            ItemStack phantomHelmet = StationLogic.assembleArmor(PlatingType.HELMET_PLATING, "iron", "phantom_membrane");
            phantomPlayer.setItemSlot(EquipmentSlot.HEAD, phantomHelmet);
            phantomPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmorItems.lining("phantom_membrane")));
            com.modulardreams.traits.TraitEngine.stripLinings(phantomPlayer);
            ModularData strippedData = phantomHelmet.get(ModDataComponents.MODULAR_DATA);
            check(strippedData != null && strippedData.materialOf("lining").isEmpty()
                    && "iron".equals(strippedData.materialOf("helmet_plating").orElse(""))
                    && phantomHelmet.getMaxDamage() > 0,
                    "the void strips the lining from the first assembled piece and keeps the plating");
            check(!phantomPlayer.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                    "one void save consumes exactly ONE lining (the standalone one survives)");
            com.modulardreams.traits.TraitEngine.stripLinings(phantomPlayer);
            check(phantomPlayer.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                    "the next void save consumes the standalone lining");

            // M13: damage immunities
            // netherite NEVER protects the player from fire/lava (that is what
            // Fire Resistance potions are for) - the ITEMS are fireproof instead
            ServerLevel level2 = level;
            var sources = level2.damageSources();
            Player notImmune = new TestPlayer(level2);
            ItemStack netheriteTool = ModItems.toolStack(ModularToolType.PICKAXE, "netherite", "wood");
            notImmune.setItemSlot(EquipmentSlot.MAINHAND, netheriteTool);
            check(!com.modulardreams.traits.TraitEngine.testCancelDamage(notImmune,
                    sources.source(DamageTypes.LAVA), 5.0F),
                    "netherforged does NOT protect the player from lava (items are the fireproof ones)");
            check(!com.modulardreams.traits.TraitEngine.testCancelDamage(new TestPlayer(level2),
                    sources.source(DamageTypes.LAVA), 5.0F),
                    "players without netherite still burn in lava");
            check(netheriteTool.has(DataComponents.DAMAGE_RESISTANT),
                    "a netherite-trait item is itself fire resistant (baked component)");
            // frostward: a leather lining simply cannot freeze (buildup + damage)
            Player frostward = new TestPlayer(level2);
            frostward.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorItems.lining("leather")));
            check(!frostward.canFreeze(),
                    "frostward (leather lining) makes the player unable to freeze");
            check(new TestPlayer(level2).canFreeze(),
                    "players without a leather lining can still freeze");
            Player bouncy = new TestPlayer(level2);
            bouncy.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmorItems.lining("slime")));
            check(com.modulardreams.traits.TraitEngine.testCancelDamage(bouncy,
                    sources.source(DamageTypes.FALL), 9.0F),
                    "bouncy (slime lining) cancels fall damage");
            Player voidFull = new TestPlayer(level2);
            voidFull.setItemSlot(EquipmentSlot.HEAD, StationLogic.assembleArmor(
                    PlatingType.HELMET_PLATING, "iron", "phantom_membrane"));
            voidFull.setItemSlot(EquipmentSlot.CHEST, StationLogic.assembleArmor(
                    PlatingType.CHESTPLATE_PLATING, "iron", "phantom_membrane"));
            voidFull.setItemSlot(EquipmentSlot.LEGS, StationLogic.assembleArmor(
                    PlatingType.LEGGINGS_PLATING, "iron", "phantom_membrane"));
            voidFull.setItemSlot(EquipmentSlot.FEET, StationLogic.assembleArmor(
                    PlatingType.BOOTS_PLATING, "iron", "phantom_membrane"));
            check(com.modulardreams.traits.TraitEngine.testCancelDamage(voidFull,
                    sources.source(DamageTypes.FELL_OUT_OF_WORLD), 1000.0F),
                    "a full phantom-lining set cancels void death");
            Player voidPartial = new TestPlayer(level2);
            voidPartial.setItemSlot(EquipmentSlot.HEAD, StationLogic.assembleArmor(
                    PlatingType.HELMET_PLATING, "iron", "phantom_membrane"));
            check(!com.modulardreams.traits.TraitEngine.testCancelDamage(voidPartial,
                    sources.source(DamageTypes.FELL_OUT_OF_WORLD), 1000.0F),
                    "an incomplete phantom set does not save from the void");

            // M14: fortunate tier comparison helpers
            check(com.modulardreams.traits.DurabilityHooks.blockTier(
                    net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()) == 0,
                    "dirt has mining tier 0");
            check(com.modulardreams.traits.DurabilityHooks.blockTier(
                    net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()) == 3,
                    "obsidian requires the diamond tier");
            check(com.modulardreams.traits.DurabilityHooks.toolTier(powerBase) == 2
                    && com.modulardreams.traits.DurabilityHooks.toolTier(
                            ModItems.toolStack(ModularToolType.PICKAXE, "diamond", "wood")) == 3,
                    "tool tiers read from the head material (iron 2, diamond 3)");

            // --- N: modifier caps and the nether star / elytra / dragon egg unlocks ---
            ItemStack capTool = ModItems.toolStack(ModularToolType.PICKAXE, "iron", "wood");
            SmithingRecipeInput starUnlock = new SmithingRecipeInput(ItemStack.EMPTY, capTool,
                    new ItemStack(Items.NETHER_STAR));
            Optional<RecipeHolder<SmithingRecipe>> starFound =
                    manager.getRecipeFor(RecipeType.SMITHING, starUnlock, level);
            check(starFound.isPresent() && starFound.get().value() instanceof SmithingModifierRecipe m
                            && m.unlockTier() == 1,
                    "the nether star resolves as the first cap unlock");
            ItemStack starred = starFound.map(h -> h.value().assemble(starUnlock)).orElse(ItemStack.EMPTY);
            ModularData starredData = starred.get(ModDataComponents.MODULAR_DATA);
            check(starredData != null && starredData.unlocks() == 1,
                    "the nether star expands the cap to 4");

            SmithingRecipeInput elytraEarly = new SmithingRecipeInput(ItemStack.EMPTY, capTool,
                    new ItemStack(Items.ELYTRA));
            check(manager.getRecipeFor(RecipeType.SMITHING, elytraEarly, level).isEmpty(),
                    "the elytra unlock refuses a tool without the nether star unlock");
            SmithingRecipeInput elytraOk = new SmithingRecipeInput(ItemStack.EMPTY, starred,
                    new ItemStack(Items.ELYTRA));
            ItemStack elytrated = manager.getRecipeFor(RecipeType.SMITHING, elytraOk, level)
                    .map(h -> h.value().assemble(elytraOk)).orElse(ItemStack.EMPTY);
            check(!elytrated.isEmpty() && elytrated.get(ModDataComponents.MODULAR_DATA).unlocks() == 2,
                    "the elytra unlock applies in order and expands the cap to 5");

            SmithingRecipeInput eggEarly = new SmithingRecipeInput(ItemStack.EMPTY, starred,
                    new ItemStack(Items.DRAGON_EGG));
            check(manager.getRecipeFor(RecipeType.SMITHING, eggEarly, level).isEmpty(),
                    "the dragon egg unlock refuses a tool without the elytra unlock");
            SmithingRecipeInput eggOk = new SmithingRecipeInput(ItemStack.EMPTY, elytrated,
                    new ItemStack(Items.DRAGON_EGG));
            ItemStack eggTool = manager.getRecipeFor(RecipeType.SMITHING, eggOk, level)
                    .map(h -> h.value().assemble(eggOk)).orElse(ItemStack.EMPTY);
            ModularData eggData = eggTool.get(ModDataComponents.MODULAR_DATA);
            check(eggData != null && eggData.unlocks() == 3 && eggData.hasDragonEgg(),
                    "the dragon egg unlock applies last and marks the legendary tool");

            // the extraction price: give the legendary two modifiers, the LAST
            // one (the most recently applied) must go when the egg comes out
            eggData = ModTraits.withAppliedTrait(eggData, "quartz");
            eggData = ModTraits.withAppliedTrait(eggData, "emerald");
            eggTool.set(ModDataComponents.MODULAR_DATA, eggData);
            StatsEngine.bake(eggTool, eggData);

            // a capped tool refuses further modifiers (3 base on a fresh tool)
            ItemStack fullTool = ModItems.toolStack(ModularToolType.PICKAXE, "iron", "wood");
            ModularData fullData = fullTool.get(ModDataComponents.MODULAR_DATA);
            fullData = ModTraits.withAppliedTrait(fullData, "quartz");
            fullData = ModTraits.withAppliedTrait(fullData, "flint");
            fullData = ModTraits.withAppliedTrait(fullData, "redstone");
            fullTool.set(ModDataComponents.MODULAR_DATA, fullData);
            SmithingRecipeInput capped = new SmithingRecipeInput(ItemStack.EMPTY, fullTool,
                    new ItemStack(Items.EMERALD, 3));
            check(manager.getRecipeFor(RecipeType.SMITHING, capped, level).isEmpty(),
                    "a tool at its modifier cap refuses further modifiers");

            // --- O: the dragon egg extraction (tool alone in the crafting grid) ---
            CraftingInput extractInput = CraftingInput.of(1, 1, java.util.List.of(eggTool));
            Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> extractFound =
                    manager.getRecipeFor(RecipeType.CRAFTING, extractInput, level);
            check(extractFound.isEmpty() == false
                            && extractFound.get().value() instanceof DragonEggExtractRecipe,
                    "the egg-carrying tool alone resolves the dragon egg extraction");
            ItemStack extractedTool = extractFound.map(h -> h.value().assemble(extractInput)).orElse(ItemStack.EMPTY);
            ModularData extractedData = extractedTool.get(ModDataComponents.MODULAR_DATA);
            check(extractedData != null && extractedData.unlocks() == 2 && !extractedData.hasDragonEgg(),
                    "extraction removes the egg and drops the cap back to 5");
            check(extractedData != null && extractedData.modifiers().size() == eggData.modifiers().size() - 1,
                    "extraction deletes the LAST modifier as its price");
            check(extractedData != null && extractedData.hasModifier("quartz")
                            && !extractedData.hasModifier("emerald"),
                    "extraction deletes the most recently applied modifier, not the first");
            var remainders = extractFound
                    .map(h -> h.value().getRemainingItems(extractInput))
                    .orElse(net.minecraft.core.NonNullList.create());
            check(remainders.stream().anyMatch(r -> r.is(Items.DRAGON_EGG)),
                    "the extraction returns the dragon egg as the recipe remainder");
            CraftingInput extractWrong = CraftingInput.of(1, 1, java.util.List.of(
                    new ItemStack(Items.STICK)));
            check(manager.getRecipeFor(RecipeType.CRAFTING, extractWrong, level).isEmpty(),
                    "a plain stick never triggers the extraction");
            CraftingInput extractTwo = CraftingInput.of(1, 2, java.util.List.of(
                    eggTool, new ItemStack(Items.STICK)));
            check(manager.getRecipeFor(RecipeType.CRAFTING, extractTwo, level).isEmpty(),
                    "extra items in the grid refuse the extraction");

            // --- P: the broken state - modular items never break, they stop working ---
            ItemStack breaker = ModItems.toolStack(ModularToolType.PICKAXE, "iron", "wood");
            float workingDamage = attackModifierAmount(breaker);
            // natural wear parks the tool at its last durability point...
            breaker.setDamageValue(breaker.getMaxDamage() - 1);
            // ...and the NEXT durability hit must not destroy it: the clamp
            // re-bakes it into the broken state instead
            breaker.hurtAndBreak(1, level, (ServerPlayer) player, s -> {});
            check(!breaker.isEmpty() && breaker.getDamageValue() == breaker.getMaxDamage() - 1,
                    "a tool at its last durability point never breaks (clamped at 1)");
            check(attackModifierAmount(breaker) == 0.0F,
                    "a broken tool loses its baked attack damage");
            net.minecraft.world.item.component.Tool brokenTool = breaker.get(DataComponents.TOOL);
            check(brokenTool != null && brokenTool.rules().isEmpty(),
                    "a broken tool mines at hand speed (no harvest rules)");
            // repairing it back to working (wood trait would do this) re-bakes the stats
            breaker.setDamageValue(0);
            StatsEngine.bake(breaker, breaker.get(ModDataComponents.MODULAR_DATA));
            check(Math.abs(attackModifierAmount(breaker) - workingDamage) < 1e-4,
                    "a repaired tool regains its working stats");
            ItemStack ironHelmet = StationLogic.assembleArmor(PlatingType.HELMET_PLATING, "iron", "leather");
            ironHelmet.setDamageValue(ironHelmet.getMaxDamage() - 1);
            StatsEngine.bake(ironHelmet, ironHelmet.get(ModDataComponents.MODULAR_DATA));
            double brokenArmor = ironHelmet.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(e -> e.attribute() == Attributes.ARMOR)
                    .mapToDouble(e -> e.modifier().amount()).sum();
            check(brokenArmor == 0.0,
                    "broken armor protects nothing");

            // --- Q: netherite heat empowerment scans lava, fire and soul fire ---
            ServerLevel nether = server.getLevel(net.minecraft.world.level.Level.NETHER) != null
                    ? server.getLevel(net.minecraft.world.level.Level.NETHER) : level;
            Player heatPlayer = new TestPlayer(nether);
            heatPlayer.setPos(8.5D, 40.0D, 8.5D);
            nether.setBlock(new net.minecraft.core.BlockPos(8, 40, 12),
                    net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            check(com.modulardreams.traits.TraitEngine.scanForHeat((ServerPlayer) heatPlayer),
                    "lava within 8 blocks registers as nether heat");
            // the false case in the calm overworld spawn (the nether floor is
            // littered with natural fire and lava, which is the point)
            Player calmPlayer = new TestPlayer(level);
            check(!com.modulardreams.traits.TraitEngine.scanForHeat((ServerPlayer) calmPlayer),
                    "no heat nearby means no empowerment");

            if (this.failures.isEmpty()) {
                ModularDreams.LOGGER.info("{}ALL {} CHECKS PASSED", tag, this.checks);
            } else {
                ModularDreams.LOGGER.error("{}{} of {} CHECKS FAILED: {}",
                        tag, this.failures.size(), this.checks, this.failures);
            }
        } catch (Throwable t) {
            ModularDreams.LOGGER.error("{}crashed", tag, t);
        }
    }

    private void check(boolean ok, String what) {
        this.checks++;
        if (ok) {
            ModularDreams.LOGGER.info("[SmithingSelfTest] PASS - {}", what);
        } else {
            this.failures.add(what);
            ModularDreams.LOGGER.error("[SmithingSelfTest] FAIL - {}", what);
        }
    }

    /** The baked ATTACK_DAMAGE modifier amount of a tool stack. */
    private static float attackModifierAmount(ItemStack stack) {
        return (float) stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                .filter(e -> e.attribute() == Attributes.ATTACK_DAMAGE)
                .mapToDouble(e -> e.modifier().amount())
                .findFirst().orElse(0.0D);
    }

    /** The baked ATTACK_SPEED modifier amount of a tool stack. */
    private static float attackSpeedModifierAmount(ItemStack stack) {
        return (float) stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                .filter(e -> e.attribute() == Attributes.ATTACK_SPEED)
                .mapToDouble(e -> e.modifier().amount())
                .findFirst().orElse(0.0D);
    }

    /** A real server player (offline) - hosts menu logic and durability paths in the self-test. */
    private static final class TestPlayer extends net.minecraft.server.level.ServerPlayer {

        private TestPlayer(ServerLevel level) {
            super(level.getServer(), level,
                    new GameProfile(
                            UUID.nameUUIDFromBytes("modular_dreams_smithing_selftest".getBytes()),
                            "SmithingSelfTest"),
                    net.minecraft.server.level.ClientInformation.createDefault());
        }

        @Override
        public GameType gameMode() {
            return GameType.SURVIVAL;
        }

        /** No connection exists in the self-test - recipe-book awards would crash. */
        @Override
        public int awardRecipes(java.util.Collection<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes) {
            return 0;
        }
    }
}
