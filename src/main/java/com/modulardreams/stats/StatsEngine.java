package com.modulardreams.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.modifier.Modifier.Effect;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;
import com.modulardreams.stats.StatsEngine;

/**
 * Derives every gameplay stat of an assembled tool from its
 * {@link ModularData} and bakes the results into vanilla data components.
 *
 * <p>The math follows Legacy's Construct (the TiC 1.12.2 port):
 * <ul>
 *   <li>tool durability = (head durability + binding bonus) x handle modifier + handle bonus,
 *       then x the tool's durability multiplier (sword 1.1, ...)</li>
 *   <li>mining speed = head speed x the tool's mining multiplier, tier from the head</li>
 *   <li>attack = (head attack x attack multiplier + base attack) x damage potential</li>
 *   <li>since build 21 the handle can also boost mining speed and attack
 *       (diamond handle = 1.1x both) via {@link ModularMaterial#handleStatBoost()}</li>
 * </ul>
 * Material stats come from Tinkers' Construct 3.12 (see {@link ModMaterials});
 * material traits are currently the EMPTY placeholder, so the trait branches
 * below are dormant until the traits milestone.
 */
public final class StatsEngine {

        /**
         * Bump whenever the stat formulas or material values change. Assembled stacks
         * carry this stamp in {@code modular_dreams:stats_version}; any stack whose
         * stamp is missing or older is re-baked from its modular data the first time
         * it ticks in a player inventory (see {@link #refreshIfStale}), so tools from
         * previous builds automatically adopt the current stats without re-assembly.
         */
        public static final int STATS_VERSION = 4;

        private StatsEngine() {}

        // ------------------------------------------------------------------ stat records (for the guide book)

        public record ToolStats(int durability, float miningSpeed, MaterialTier tier, float attackDamage,
                        float attackSpeed, int enchantability) {
                public float totalAttackDamage() {
                        return 1.0F + attackDamage;
                }

                public float totalAttackSpeed() {
                        return 4.0F + attackSpeed;
                }
        }

        // ------------------------------------------------------------------ public entry points

        /** Recomputes all stats of an assembled item and bakes them into vanilla components. */
        public static void bake(ItemStack stack, ModularData data) {
                if (data.equipmentType().equals("none")) {
                        return;
                }
                stack.set(ModDataComponents.STATS_VERSION, STATS_VERSION);
                ModularToolType.byId(data.equipmentType()).ifPresentOrElse(
                                type -> bakeTool(stack, data, type),
                                () -> ModularDreams.LOGGER.warn("Unknown equipment type '{}', skipping bake",
                                                data.equipmentType()));
        }

        /**
         * Re-bakes the stack if its stats were computed by an older formula version.
         * Called from every equipment item's {@code inventoryTick} (server side), so
         * tools carried over from previous builds silently adopt the current stats
         * the moment they enter a player inventory - no manual re-assembly required.
         * Stacks without modular data are ignored.
         */
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

        private static void bakeTool(ItemStack stack, ModularData data, ModularToolType type) {
                ModularMaterial head = material(data, type.headPart());
                ModularMaterial handle = material(data, type.handlePart());
                ModularMaterial binding = material(data, type.bindingPart());

                List<ModularMaterial> allParts = List.of(head, handle, binding);

                int bouncyLvl = data.levelOf("bouncy");
                boolean netherited = data.levelOf("netherited") > 0;

                // ---- durability (Legacy's Construct / TiC 1.12.2 formula) ----
                // (head + binding bonus) x handle modifier + handle bonus, x tool multiplier
                float durability = (head.durability() + binding.extraDurabilityBonus()) * handle.handleModifier()
                                + handle.handleDurabilityBonus();
                durability *= type.durabilityMultiplier;
                int bakedDurability = Math.round(durability);
                float durabilityPct = modifierSum(data, Effect.DURABILITY_PCT);
                int durabilityFlat = Math.round(modifierSum(data, Effect.DURABILITY_FLAT));
                int finalDurability = Math.round(bakedDurability * (1.0F + durabilityPct)) + durabilityFlat;
                finalDurability = Math.max(1, finalDurability);

                // ---- mining speed & tier (head speed x the tool's mining multiplier) ----
                float speed = head.speed() * type.miningSpeedMultiplier;
                // build 21: some handles (diamond) boost the tool's other stats
                speed *= handle.handleStatBoost();
                speed *= (1.0F + modifierSum(data, Effect.MINING_SPEED_PCT));

                MaterialTier tier = head.tier();
                if (netherited) {
                        tier = tier.max(MaterialTier.NETHERITE);
                }

                // ---- attack stats (LC formula) ----
                float damage = (head.attackDamageBonus() * type.attackMultiplier + type.baseAttack)
                                * type.damagePotential;
                // build 21: handle stat boost applies to the tool's final attack
                damage *= handle.handleStatBoost();
                damage += modifierSum(data, Effect.ATTACK_DAMAGE);

                float attackSpeed = type.attackSpeed;
                attackSpeed += modifierSum(data, Effect.ATTACK_SPEED);

                // ---- enchantability ----
                int enchantability = head.enchantmentValue();

                // ---- repair items ----
                List<Holder<Item>> repairItems = new ArrayList<>();
                if (head.repairTag() != null) {
                        BuiltInRegistries.ITEM.getOrThrow(head.repairTag()).forEach(repairItems::add);
                }

                // ---- bake vanilla components ----
                // The sword is a weapon, NOT a mining tool - its leaf/cobweb
                // behaviour is fixed at vanilla speeds instead of scaling with
                // the head's mining speed.
                int damagePerBlock = 1;
                boolean canDestroyInCreative = true;
                List<Tool.Rule> rules = new ArrayList<>();
                if (type == ModularToolType.SWORD) {
                        rules.add(Tool.Rule.minesAndDrops(
                                        HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
                        rules.add(Tool.Rule.overrideSpeed(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES),
                                        Float.MAX_VALUE));
                        rules.add(Tool.Rule.overrideSpeed(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_EFFICIENT),
                                        1.5F));
                        damagePerBlock = 2;
                        canDestroyInCreative = false;
                } else {
                        // every non-sword tool harvests by its head tier and mines
                        // its class blocks at the head speed x the tool's mining
                        // multiplier
                        rules.add(Tool.Rule.deniesDrops(BuiltInRegistries.BLOCK.getOrThrow(tier.incorrectBlocksForDrops)));
                        if (type.mineableTag != null) {
                                rules.add(Tool.Rule.minesAndDrops(BuiltInRegistries.BLOCK.getOrThrow(type.mineableTag),
                                                speed));
                        }
                }
                stack.set(DataComponents.TOOL, new Tool(rules, 1.0F, damagePerBlock, canDestroyInCreative));
                stack.set(DataComponents.MAX_DAMAGE, finalDurability);
                // 26.3 requires the DAMAGE component to be present for
                // isDamageableItem() to be true (vanilla Properties#durability sets
                // MAX_DAMAGE + DAMAGE=0 together); without it tools never lose durability
                stack.set(DataComponents.DAMAGE,
                                Math.min(stack.getOrDefault(DataComponents.DAMAGE, 0), Math.max(0, finalDurability - 1)));
                stack.set(DataComponents.ENCHANTABLE, new Enchantable(Math.max(0, enchantability)));
                stack.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItems)));
                stack.set(DataComponents.WEAPON, new Weapon(type.weaponDamagePerAttack, type.disableBlockingForSeconds));
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                                toolAttributes(type, damage, attackSpeed - 4.0F, bouncyLvl));

                if (type == ModularToolType.SPEAR) {
                        stack.set(DataComponents.KINETIC_WEAPON, spearKineticWeapon(head));
                        stack.set(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                                        java.util.Optional.of(SoundEvents.SPEAR_ATTACK),
                                        java.util.Optional.of(SoundEvents.SPEAR_HIT)));
                        com.modulardreams.registry.ModRegistryAccess.get().ifPresent(access -> stack.set(DataComponents.DAMAGE_TYPE,
                                                access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SPEAR)));
                        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F));
                        stack.set(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F);
                        stack.set(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 13));
                        stack.set(DataComponents.USE_EFFECTS, new UseEffects(true, false, 0.25F));
                }

                applyFireResistant(stack, allParts, netherited);
                applyEnchantModifiers(stack, data, type);
                applyCosmetics(stack, head);

                stack.set(DataComponents.ITEM_NAME, Component.translatable(
                                "item.modular_dreams.modular_" + type.id + ".named", Component.translatable(head.nameKey())));
                // one dynamic definition per tool type: the "modular_dreams:modular_tool"
                // item model stacks the grayscale part textures, tinted by each part's
                // own material color (Tinkers'-style layered rendering)
                stack.set(DataComponents.ITEM_MODEL, ModularDreams.id("modular_" + type.id));
        }

        private static KineticWeapon spearKineticWeapon(ModularMaterial head) {
                boolean wooden = head.tier() == MaterialTier.WOOD;
                return new KineticWeapon(
                                10,
                                15,
                                KineticWeapon.Condition.ofAttackerSpeed(280, 10.0F),
                                KineticWeapon.Condition.ofAttackerSpeed(200, 15.0F),
                                KineticWeapon.Condition.ofRelativeSpeed(300, 4.6F),
                                0.38F,
                                0.7F,
                                java.util.Optional.of(wooden ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                                java.util.Optional.of(wooden ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT));
        }

        private static ItemAttributeModifiers toolAttributes(ModularToolType type, float damage, float attackSpeed,
                        int bouncyLvl) {
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                builder.add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, Math.max(0.0F, damage),
                                                AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND);
                builder.add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed,
                                                AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND);
                if (bouncyLvl > 0) {
                        float kb = modifierOf("bouncy").magnitude() * bouncyLvl;
                        builder.add(Attributes.ATTACK_KNOCKBACK,
                                        new AttributeModifier(ModularDreams.id("modifier_bouncy"), kb,
                                                        AttributeModifier.Operation.ADD_VALUE),
                                        EquipmentSlotGroup.MAINHAND);
                }
                return builder.build();
        }

        // ------------------------------------------------------------------ shared helpers

        private static void applyFireResistant(ItemStack stack, List<ModularMaterial> parts, boolean netherited) {
                boolean fireproof = netherited || parts.stream().anyMatch(ModularMaterial::fireResistant);
                if (fireproof) {
                        com.modulardreams.registry.ModRegistryAccess.get().ifPresent(access -> stack.set(
                                        DataComponents.DAMAGE_RESISTANT, new DamageResistant(access
                                                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                                                        .getOrThrow(net.minecraft.tags.DamageTypeTags.IS_FIRE))));
                }
        }

        private static void applyEnchantModifiers(ItemStack stack, ModularData data, ModularToolType type) {
                ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS,
                                ItemEnchantments.EMPTY));
                boolean changed = false;
                for (var entry : data.modifiers()) {
                        Optional<Modifier> modifier = Modifier.byId(entry.id());
                        if (modifier.isEmpty() || modifier.get().effect() != Effect.ENCHANT) {
                                continue;
                        }
                        Optional<ResourceKey<Enchantment>> key = modifier.get().enchantmentKey();
                        if (key.isEmpty()) {
                                continue;
                        }
                        ResourceKey<Enchantment> actualKey = key.get();
                        // "lucky" adds Fortune to digging tools and Looting to weapons
                        if (modifier.get().id().equals("lucky") && type != null && isWeapon(type)) {
                                actualKey = Enchantments.LOOTING;
                        }
                        Holder<Enchantment> holder = com.modulardreams.registry.ModRegistryAccess.get()
                                        .orElseThrow(() -> new IllegalStateException("No registry access"))
                                        .lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(actualKey);
                        ench.set(holder, entry.level());
                        changed = true;
                }
                if (changed) {
                        stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
                }
        }

        private static boolean isWeapon(ModularToolType type) {
                return type == ModularToolType.SWORD || type == ModularToolType.SPEAR
                                || type == ModularToolType.AXE;
        }

        private static void applyCosmetics(ItemStack stack, ModularMaterial head) {
                // reserved for trait-driven cosmetics (dormant with the EMPTY placeholder)
        }

        /**
         * Tools assembled before the DAMAGE-component fix lack the DAMAGE
         * component, which 26.3 requires for {@code isDamageableItem()}; call
         * this from custom damage handlers (runs inside {@code hurtAndBreak},
         * before that check) so legacy stacks start taking damage again.
         */
        public static void ensureDamageComponent(ItemStack stack) {
                if (stack.get(DataComponents.MAX_DAMAGE) != null && !stack.has(DataComponents.DAMAGE)) {
                        stack.set(DataComponents.DAMAGE, 0);
                }
        }

        /**
         * @return total durability damage reduction from the DENSE trait and the
         *         reinforced modifier (0..1). DENSE is dormant with the EMPTY placeholder.
         */
        public static float durabilityDamageReduction(ItemStack stack, ModularData data) {
                float reduction = 0.0F;
                for (var part : data.parts()) {
                        Optional<ModularMaterial> mat = ModMaterials.byId(part.material());
                        if (mat.isPresent() && mat.get().hasTrait(ModTraits.DENSE)) {
                                reduction += 0.20F;
                                break;
                        }
                }
                int reinforced = data.levelOf("reinforced");
                if (reinforced > 0) {
                        reduction += modifierOf("reinforced").magnitude() * reinforced;
                }
                return Math.min(0.85F, reduction);
        }

        /** @return total fire seconds applied on hit from the FIERY trait and modifier. */
        public static float fierySeconds(ItemStack stack, ModularData data) {
                float seconds = 0.0F;
                for (var part : data.parts()) {
                        Optional<ModularMaterial> mat = ModMaterials.byId(part.material());
                        if (mat.isPresent() && mat.get().hasTrait(ModTraits.FIERY)) {
                                seconds = Math.max(seconds, 1.5F);
                                break;
                        }
                }
                int fiery = data.levelOf("fiery");
                if (fiery > 0) {
                        seconds += modifierOf("fiery").magnitude() * fiery;
                }
                return seconds;
        }

        // ------------------------------------------------------------------ small helpers

        private static Modifier modifierOf(String id) {
                return Modifier.getOrThrow(id);
        }

        private static float modifierSum(ModularData data, Effect effect) {
                float sum = 0.0F;
                for (var entry : data.modifiers()) {
                        Optional<Modifier> modifier = Modifier.byId(entry.id());
                        if (modifier.isPresent() && modifier.get().effect() == effect) {
                                sum += modifier.get().magnitude() * entry.level();
                        }
                }
                return sum;
        }

        private static ModularMaterial material(ModularData data, PartType part) {
                return ModMaterials.getOrThrow(data.materialOf(part.id).orElseThrow(
                                () -> new IllegalStateException("Assembled item is missing part " + part.id)));
        }

        // ------------------------------------------------------------------ guide book stats

        public static ToolStats toolStats(ModularData data) {
                ModularToolType type = ModularToolType.byId(data.equipmentType()).orElseThrow();
                ModularMaterial head = material(data, type.headPart());
                ModularMaterial handle = material(data, type.handlePart());
                ModularMaterial binding = material(data, type.bindingPart());

                float durability = (head.durability() + binding.extraDurabilityBonus()) * handle.handleModifier()
                                + handle.handleDurabilityBonus();
                durability *= type.durabilityMultiplier;
                int bakedDurability = Math.round(durability);
                int finalDurability = Math.round(bakedDurability * (1.0F + modifierSum(data, Effect.DURABILITY_PCT)))
                                + Math.round(modifierSum(data, Effect.DURABILITY_FLAT));

                float speed = head.speed() * type.miningSpeedMultiplier
                                * handle.handleStatBoost()
                                * (1.0F + modifierSum(data, Effect.MINING_SPEED_PCT));
                float damage = (head.attackDamageBonus() * type.attackMultiplier + type.baseAttack)
                                * type.damagePotential * handle.handleStatBoost()
                                + modifierSum(data, Effect.ATTACK_DAMAGE);
                float attackSpeed = type.attackSpeed + modifierSum(data, Effect.ATTACK_SPEED);
                int ench = head.enchantmentValue();
                MaterialTier tier = head.tier();
                if (data.levelOf("netherited") > 0) {
                        tier = tier.max(MaterialTier.NETHERITE);
                }
                return new ToolStats(finalDurability, speed, tier, damage, attackSpeed, ench);
        }
}
