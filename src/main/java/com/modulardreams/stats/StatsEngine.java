package com.modulardreams.stats;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModArmorItems;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModArmorMaterials;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModTraits;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.part.PlatingType;
import com.modulardreams.traits.DropHooks;
import com.modulardreams.traits.TraitTuning;

/**
 * Derives every gameplay stat of an assembled v2 tool from its
 * {@link ModularData} (handle + head) and bakes the results into vanilla
 * data components.
 *
 * <p>v2 formulas:
 * <ul>
 *   <li>durability = head durability x handle multiplier</li>
 *   <li>mining speed = head speed; harvest tier from the head</li>
 *   <li>attack = head attack bonus + tool type base damage (vanilla ADD modifier)</li>
 *   <li>some handles boost speed and attack (rose gold 1.05x, diamond 1.1x, netherite 1.1x)</li>
 *   <li>enchantability from the head</li>
 * </ul>
 */
public final class StatsEngine {

    /** Bump whenever the stat formulas or material values change. */
    public static final int STATS_VERSION = 3;

    private StatsEngine() {}

    /**
     * A modular item is BROKEN once it sits at its last durability point:
     * it never breaks (hurtAndBreak clamps it here) but it stops working -
     * tools mine at hand speed and lose their combat stats, armor loses its
     * protection. Wood Regenerative can repair it back to working.
     */
    public static boolean isBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    public record ToolStats(int durability, float miningSpeed, MaterialTier tier, float attackDamage,
            float attackSpeed, int enchantability) {
        public float totalAttackDamage() {
            return 1.0F + attackDamage;
        }

        public float totalAttackSpeed() {
            return 4.0F + attackSpeed;
        }
    }

    // ------------------------------------------------------------------ entry points

    /** Recomputes all stats of an assembled item and bakes them into vanilla components. */
    public static void bake(ItemStack stack, ModularData data) {
        if (data.equipmentType().equals("none")) {
            return;
        }
        // stamped as current only when every registry-dependent component could
        // be resolved; otherwise the stack re-bakes the next time it ticks
        // in an inventory (server side always has the registry access)
        stack.set(ModDataComponents.STATS_VERSION, STATS_VERSION);
        incompleteBake = false;
        ModularToolType.byId(data.equipmentType()).ifPresentOrElse(
                type -> bakeTool(stack, data, type),
                () -> {
                    PlatingType plating = armorTypeOf(data.equipmentType());
                    if (plating != null) {
                        bakeArmor(stack, data, plating);
                    } else {
                        ModularDreams.LOGGER.warn("Unknown equipment type '{}', skipping bake",
                                data.equipmentType());
                    }
                });
        if (incompleteBake) {
            stack.set(ModDataComponents.STATS_VERSION, 0);
        }
    }

    /** Set by registry-dependent bake steps that had no registry access (single-threaded per side). */
    private static boolean incompleteBake;

    /** Runs a registry-dependent bake step, flagging the bake incomplete when no access exists yet. */
    private static void withRegistries(java.util.function.Consumer<net.minecraft.core.HolderLookup.Provider> step) {
        var access = com.modulardreams.registry.ModRegistryAccess.get();
        if (access.isPresent()) {
            step.accept(access.get());
        } else {
            incompleteBake = true;
        }
    }

    /** Re-bakes the stack if its stats were computed by an older formula version. */
    public static void refreshIfStale(ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.STATS_VERSION, 0) == STATS_VERSION) {
            return;
        }
        ModularData data = stack.get(ModDataComponents.MODULAR_DATA);
        if (data == null || data.equipmentType().equals("none")) {
            return;
        }
        bake(stack, data);
    }

    // ------------------------------------------------------------------ tool baking

    // ------------------------------------------------------------------ armor baking

    /** @return the plating type whose armor piece id matches, or null ("helmet" -> HELMET_PLATING). */
    private static PlatingType armorTypeOf(String equipmentTypeId) {
        for (PlatingType plating : PlatingType.values()) {
            if (plating.armorTypeId().equals(equipmentTypeId)) {
                return plating;
            }
        }
        return null;
    }

    /**
     * Bakes an assembled armor piece. The PLATING material decides everything
     * protective: armor value, toughness, knockback resistance, durability,
     * enchantability, repair items, equip sound and the worn asset - via a
     * plain vanilla {@link net.minecraft.world.item.equipment.ArmorMaterial}
     * record per material ({@code ModArmorMaterials}), so the components are
     * byte-identical in shape to vanilla armor. The lining only carries its
     * trait. The netherite armor upgrade re-runs this bake after swapping the
     * plating material, which rewrites every component at once.
     */
    private static void bakeArmor(ItemStack stack, ModularData data, PlatingType plating) {
        // the plating part is every part that is not the lining (its part id
        // equals the plating type id); its material may be "netherite" after
        // the smithing upgrade
        String platingMaterial = data.parts().stream()
                .filter(p -> !p.part().equals(LINING_PART_ID))
                .map(ModDataComponents.PartData::material)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Armor piece without plating part"));
        net.minecraft.world.item.equipment.ArmorMaterial mat = ModArmorMaterials.getOrThrow(platingMaterial);
        net.minecraft.world.item.equipment.ArmorType armorType = plating.armorType();

        // ---- durability: vanilla armor formula (slot unit x material multiplier)
        int finalDurability = Math.max(1, armorType.getDurability(mat.durability()));
        stack.set(DataComponents.MAX_DAMAGE, finalDurability);
        stack.set(DataComponents.DAMAGE,
                Math.min(stack.getOrDefault(DataComponents.DAMAGE, 0), Math.max(0, finalDurability - 1)));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);

        // ---- protection attributes (armor, toughness, knockback resistance)
        // a BROKEN piece protects nothing ----
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, isBroken(stack)
                ? net.minecraft.world.item.component.ItemAttributeModifiers.builder().build()
                : mat.createAttributes(armorType));

        // ---- enchantability + repair
        stack.set(DataComponents.ENCHANTABLE, new Enchantable(Math.max(0, mat.enchantmentValue())));
        stack.set(DataComponents.REPAIRABLE, new Repairable(
                BuiltInRegistries.ITEM.getOrThrow(mat.repairIngredient())));

        // ---- wearable: slot, equip sound, worn asset (humanoidArmor shape)
        stack.set(DataComponents.EQUIPPABLE,
                net.minecraft.world.item.equipment.Equippable.builder(armorType.getSlot())
                        .setEquipSound(mat.equipSound())
                        .setAsset(mat.assetId())
                        .build());

        // ---- fire resistance (netherite plating), same path as tools
        ModularMaterial platingModMaterial = ModMaterials.byId(platingMaterial).orElse(null);
        if (platingModMaterial != null && platingModMaterial.fireResistant()) {
            withRegistries(access -> stack.set(
                    DataComponents.DAMAGE_RESISTANT,
                    new DamageResistant(access.lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypeTags.IS_FIRE))));
        }

        stack.set(DataComponents.ITEM_NAME, Component.translatable(
                "item.modular_dreams.modular_" + plating.armorTypeId() + ".named",
                Component.translatable(platingModMaterial != null ? platingModMaterial.nameKey()
                        : "material.modular_dreams." + platingMaterial)));
        // one dynamic definition per piece: the layered item model stacks the
        // lining underlayer + plating icons (same custom model type as tools)
        stack.set(DataComponents.ITEM_MODEL, ModularDreams.id("modular_" + plating.armorTypeId()));
    }

    /** The part id linings are stored under in assembled armor pieces. */
    public static final String LINING_PART_ID = "lining";

    private static void bakeTool(ItemStack stack, ModularData data, ModularToolType type) {
        ModularMaterial head = material(data, type.headPart().id);
        ModularMaterial handle = material(data, PartTypeHandleId.INSTANCE.id());

        // ---- smithing modifiers (no levels: applied or not) ----
        boolean power = data.hasModifier(ModTraits.QUARTZ);
        boolean swift = data.hasModifier(ModTraits.FLINT);
        boolean haste = data.hasModifier(ModTraits.REDSTONE);
        boolean reinforced = data.hasModifier(ModTraits.EMERALD);

        // ---- durability: head x handle multiplier, then emerald "Reinforced" ----
        int finalDurability = Math.max(1, Math.round(head.durability() * handle.handleDurabilityMult()));
        finalDurability = Math.max(1, Math.round(finalDurability
                * (float) DropHooks.durableFactor(reinforced)));

        // ---- mining speed & tier ----
        float speed = head.speed() * handle.handleStatBoost();
        MaterialTier tier = head.tier();

        // ---- attack (+ quartz "Power", + flint "Swift") ----
        float damage = head.attackDamageBonus() * handle.handleStatBoost() + type.baseDamage
                + (power ? TraitTuning.POWER_DAMAGE : 0.0F);
        float attackSpeed = type.attackSpeed + (swift ? TraitTuning.SWIFT_SPEED : 0.0F);

        // ---- enchantability ----
        int enchantability = head.enchantability();

        // ---- repair items ----
        List<Holder<Item>> repairItems = new ArrayList<>();
        if (head.repairTag() != null) {
            BuiltInRegistries.ITEM.getOrThrow(head.repairTag()).forEach(repairItems::add);
        }

        // ---- TOOL component ----
        // A BROKEN tool mines everything at hand speed with no harvest rules:
        // it still swings, but it does not work.
        boolean broken = isBroken(stack);
        // The sword is a weapon, NOT a mining tool - its leaf/cobweb behaviour
        // is fixed at vanilla speeds instead of scaling with the head.
        int damagePerBlock = type.damagePerBlock;
        boolean canDestroyInCreative = type != ModularToolType.SWORD;
        List<Tool.Rule> rules = new ArrayList<>();
        if (!broken) {
            if (type == ModularToolType.SWORD) {
                rules.add(Tool.Rule.minesAndDrops(
                        HolderSet.direct(net.minecraft.world.level.block.Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
                rules.add(Tool.Rule.overrideSpeed(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES),
                        Float.MAX_VALUE));
                rules.add(Tool.Rule.overrideSpeed(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F));
            } else {
                // every non-sword tool harvests by its head tier and mines its
                // class blocks at the head speed
                rules.add(Tool.Rule.deniesDrops(BuiltInRegistries.BLOCK.getOrThrow(tier.incorrectBlocksForDrops)));
                if (type.mineableTag != null) {
                    rules.add(Tool.Rule.minesAndDrops(BuiltInRegistries.BLOCK.getOrThrow(type.mineableTag), speed));
                }
            }
        }
        stack.set(DataComponents.TOOL, new Tool(rules, 1.0F, damagePerBlock, canDestroyInCreative));
        stack.set(DataComponents.MAX_DAMAGE, finalDurability);
        // damageable tools must never stack (vanilla durability() sets this too)
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        // 26.3 requires the DAMAGE component to be present for isDamageableItem()
        stack.set(DataComponents.DAMAGE,
                Math.min(stack.getOrDefault(DataComponents.DAMAGE, 0), Math.max(0, finalDurability - 1)));
        stack.set(DataComponents.ENCHANTABLE, new Enchantable(Math.max(0, enchantability)));
        if (!repairItems.isEmpty()) {
            stack.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItems)));
        }
        stack.set(DataComponents.WEAPON, new Weapon(type.weaponDamagePerAttack, type.disableBlockingForSeconds));
        // a broken tool cannot fight: zero attack damage, no swift, no haste
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, toolAttributes(broken ? 0.0F : damage,
                broken ? type.attackSpeed : attackSpeed, !broken && haste));

        if (type == ModularToolType.SPEAR) {
            bakeSpearComponents(stack, head);
        }

        // ---- right-click block transformations ----
        // 26.3 removed AxeItem/ShovelItem/HoeItem: stripping, flattening and
        // tilling now come from the BLOCK_TRANSFORMER item component.
        // A broken tool cannot strip, flatten or till.
        net.minecraft.resources.ResourceKey<net.minecraft.core.component.BlockTransformer> transformer =
                broken ? null : switch (type) {
                    case AXE -> net.minecraft.world.item.component.BlockTransformers.AXE;
                    case SHOVEL -> net.minecraft.world.item.component.BlockTransformers.SHOVEL;
                    case HOE -> net.minecraft.world.item.component.BlockTransformers.HOE;
                    default -> null;
                };
        if (transformer != null) {
            withRegistries(access -> stack.set(DataComponents.BLOCK_TRANSFORMER,
                    access.lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(transformer)));
        } else {
            stack.remove(DataComponents.BLOCK_TRANSFORMER);
        }

        // ---- fire resistance (netherite parts) ----
        boolean fireproof = head.fireResistant() || handle.fireResistant();
        if (fireproof) {
            withRegistries(access -> stack.set(
                    DataComponents.DAMAGE_RESISTANT,
                    new DamageResistant(access.lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypeTags.IS_FIRE))));
        }

        stack.set(DataComponents.ITEM_NAME, Component.translatable(
                "item.modular_dreams.modular_" + type.id + ".named", Component.translatable(head.nameKey())));
        // one dynamic definition per tool type: the custom item model stacks
        // the per-material handle + head textures (Tinkers'-style layering)
        stack.set(DataComponents.ITEM_MODEL, ModularDreams.id("modular_" + type.id));
    }

    private static void bakeSpearComponents(ItemStack stack, ModularMaterial head) {
        boolean wooden = head.tier() == MaterialTier.WOOD;
        stack.set(DataComponents.KINETIC_WEAPON, new KineticWeapon(
                10,
                15,
                KineticWeapon.Condition.ofAttackerSpeed(280, 10.0F),
                KineticWeapon.Condition.ofAttackerSpeed(200, 15.0F),
                KineticWeapon.Condition.ofRelativeSpeed(300, 4.6F),
                0.38F,
                0.7F,
                java.util.Optional.of(wooden ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                java.util.Optional.of(wooden ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)));
        stack.set(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                java.util.Optional.of(SoundEvents.SPEAR_ATTACK),
                java.util.Optional.of(SoundEvents.SPEAR_HIT)));
        withRegistries(access -> stack.set(DataComponents.DAMAGE_TYPE,
                access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SPEAR)));
        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F));
        stack.set(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F);
        stack.set(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 13));
        stack.set(DataComponents.USE_EFFECTS, new UseEffects(true, false, 0.25F));
    }

    private static ItemAttributeModifiers toolAttributes(float damage, float attackSpeed, boolean haste) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        builder.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, Math.max(0.0F, damage),
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        if (haste) {
            // redstone "Haste": +15% mining speed. BLOCK_BREAK_SPEED is the
            // vanilla speed MULTIPLIER (base 1.0), so a multiplied-base entry
            // is exactly the advertised percentage.
            builder.add(Attributes.BLOCK_BREAK_SPEED,
                    new AttributeModifier(ModularDreams.id("trait/haste"), TraitTuning.HASTE_BREAK_MULT,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    // ------------------------------------------------------------------ helpers

    private static ModularMaterial material(ModularData data, String partId) {
        return ModMaterials.getOrThrow(data.materialOf(partId).orElseThrow(
                () -> new IllegalStateException("Assembled item is missing part " + partId)));
    }

    public static ToolStats toolStats(ModularData data) {
        ModularToolType type = ModularToolType.byId(data.equipmentType()).orElseThrow();
        ModularMaterial head = material(data, type.headPart().id);
        ModularMaterial handle = material(data, PartTypeHandleId.INSTANCE.id());
        int durability = Math.max(1, Math.round(head.durability() * handle.handleDurabilityMult()
                * (float) DropHooks.durableFactor(data.hasModifier(ModTraits.EMERALD))));
        float speed = head.speed() * handle.handleStatBoost();
        float damage = head.attackDamageBonus() * handle.handleStatBoost() + type.baseDamage
                + (data.hasModifier(ModTraits.QUARTZ) ? TraitTuning.POWER_DAMAGE : 0.0F);
        float attackSpeed = type.attackSpeed
                + (data.hasModifier(ModTraits.FLINT) ? TraitTuning.SWIFT_SPEED : 0.0F);
        return new ToolStats(durability, speed, head.tier(), damage, attackSpeed, head.enchantability());
    }

    /** Marker type so the handle lookup stays readable (the handle part id is always "handle"). */
    private static final class PartTypeHandleId {
        static final PartTypeHandleId INSTANCE = new PartTypeHandleId();

        String id() {
            return com.modulardreams.part.PartType.HANDLE.id;
        }
    }
}
