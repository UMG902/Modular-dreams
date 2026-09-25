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
import net.minecraft.world.entity.ai.attributes.Attribute;
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
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.modulardreams.ModularDreams;
import com.modulardreams.component.ModDataComponents.ModularData;
import com.modulardreams.equipment.ModularArmorType;
import com.modulardreams.equipment.ModularToolType;
import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.modifier.Modifier.Effect;
import com.modulardreams.part.PartType;

/**
 * Derives every gameplay stat of an assembled item from its {@link ModularData}
 * and bakes the results into vanilla data components.
 *
 * The math is Tinkers'-inspired:
 * <ul>
 *   <li>tool durability = head durability x (1 + handle multiplier + binding multiplier)</li>
 *   <li>mining speed &amp; tier come from the head material</li>
 *   <li>attack damage = tool base + head bonus (+ trait/modifier adjustments)</li>
 *   <li>armor durability = plate durability multiplier x slot factor</li>
 *   <li>defense, toughness and knockback resistance come from the plate</li>
 * </ul>
 * Traits and modifiers then adjust the result before it is baked.
 */
public final class StatsEngine {

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

        public record ArmorStats(int durability, int defense, float toughness, float knockbackResistance,
                        int enchantability) {}

        // ------------------------------------------------------------------ public entry points

        /** Recomputes all stats of an assembled item and bakes them into vanilla components. */
        public static void bake(ItemStack stack, ModularData data) {
                if (data.equipmentType().equals("none")) {
                        return;
                }
                Optional<ModularToolType> toolType = ModularToolType.byId(data.equipmentType());
                if (toolType.isPresent()) {
                        bakeTool(stack, data, toolType.get());
                } else {
                        bakeArmor(stack, data, ModularArmorType.byId(data.equipmentType()));
                }
        }

        // ------------------------------------------------------------------ tool baking

        private static void bakeTool(ItemStack stack, ModularData data, ModularToolType type) {
                PartType headPart = type.headPart();
                ModularMaterial head = material(data, headPart);
                Optional<ModularMaterial> handle = materialOpt(data, PartType.HANDLE);
                Optional<ModularMaterial> binding = type.bindingPart().flatMap(p -> materialOpt(data, p));

                // ---- trait / modifier aggregation ----
                List<ModularMaterial> allParts = new ArrayList<>();
                allParts.add(head);
                handle.ifPresent(allParts::add);
                binding.ifPresent(allParts::add);

                int hastyLvl = data.levelOf("hasty");
                int sharpLvl = data.levelOf("sharp");
                int grippyLvl = data.levelOf("grippy");
                int bouncyLvl = data.levelOf("bouncy");
                int reinforcedLvl = data.levelOf("reinforced");
                int diamondedLvl = data.levelOf("diamonded");
                int emeraledLvl = data.levelOf("emeraled");
                boolean netherited = data.levelOf("netherited") > 0;

                // ---- durability ----
                float handleMult = handle.map(ModularMaterial::handleDurability).orElse(0.0F);
                float bindingMult = binding.map(ModularMaterial::bindingDurability).orElse(0.0F);
                int durability = Math.round(head.durability() * (1.0F + handleMult + bindingMult));
                int sturdyCount = 0;
                for (ModularMaterial part : allParts) {
                        if (part.hasTrait(ModTraits.STURDY)) {
                                sturdyCount++;
                        }
                }
                durability = Math.round(durability * (1.0F + 0.05F * sturdyCount));
                float durabilityPct = 0.0F;
                int durabilityFlat = 0;
                durabilityPct += modifierSum(data, Effect.DURABILITY_PCT);
                durabilityFlat += Math.round(modifierSum(data, Effect.DURABILITY_FLAT));
                durability = Math.round(durability * (1.0F + durabilityPct)) + durabilityFlat;
                durability = Math.max(1, durability);

                // ---- mining speed & tier ----
                float speed = head.speed();
                if (binding.isPresent() && binding.get().hasTrait(ModTraits.RESONANT)) {
                        speed *= 1.10F;
                }
                speed *= (1.0F + modifierSum(data, Effect.MINING_SPEED_PCT));

                MaterialTier tier = head.tier();
                if (netherited) {
                        tier = tier.max(MaterialTier.NETHERITE);
                }

                // ---- attack stats ----
                float damage = type.baseDamage + head.attackDamageBonus();
                if (head.hasTrait(ModTraits.SPIKY)) {
                        damage += 0.5F;
                }
                damage += modifierSum(data, Effect.ATTACK_DAMAGE);

                float attackSpeed = type.baseSpeed;
                if (handle.isPresent() && handle.get().hasTrait(ModTraits.FEATHERWEIGHT)) {
                        attackSpeed += 0.15F;
                }
                attackSpeed += modifierSum(data, Effect.ATTACK_SPEED);

                // ---- enchantability ----
                int enchantability = head.enchantmentValue();
                if (binding.isPresent() && binding.get().hasTrait(ModTraits.GILDED)) {
                        enchantability += 3;
                }

                // ---- repair items ----
                List<Holder<Item>> repairItems = new ArrayList<>();
                BuiltInRegistries.ITEM.getOrThrow(head.repairTag()).forEach(repairItems::add);
                boolean rooted = allParts.stream().anyMatch(m -> m.hasTrait(ModTraits.ROOTED));
                if (rooted) {
                        repairItems.add(BuiltInRegistries.ITEM.wrapAsHolder(Items.STICK));
                }

                // ---- bake vanilla components ----
                List<Tool.Rule> rules = new ArrayList<>();
                rules.add(Tool.Rule.deniesDrops(BuiltInRegistries.BLOCK.getOrThrow(tier.incorrectBlocksForDrops)));
                if (type.mineableTag != null) {
                        rules.add(Tool.Rule.minesAndDrops(BuiltInRegistries.BLOCK.getOrThrow(type.mineableTag), speed));
                } else if (type == ModularToolType.SWORD) {
                        rules.add(Tool.Rule.minesAndDrops(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_EFFICIENT), speed));
                } else if (type == ModularToolType.MACE) {
                        rules.add(Tool.Rule.overrideSpeed(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES),
                                        speed));
                }
                stack.set(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true));
                stack.set(DataComponents.MAX_DAMAGE, durability);
                stack.set(DataComponents.ENCHANTABLE, new Enchantable(Math.max(0, enchantability)));
                stack.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItems)));
                stack.set(DataComponents.WEAPON, new Weapon(type.weaponDamagePerAttack, type.disableBlockingForSeconds));
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, toolAttributes(type, damage, attackSpeed, bouncyLvl));

                if (type == ModularToolType.SPEAR) {
                        stack.set(DataComponents.KINETIC_WEAPON, spearKineticWeapon(head));
                        stack.set(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                                        Optional.of(SoundEvents.SPEAR_ATTACK), Optional.of(SoundEvents.SPEAR_HIT)));
                        com.modulardreams.registry.ModRegistryAccess.get().ifPresent(access -> stack.set(DataComponents.DAMAGE_TYPE,
						access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SPEAR)));
                        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F));
                        stack.set(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F);
                        stack.set(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 13));
                        stack.set(DataComponents.USE_EFFECTS, new UseEffects(true, false, 0.25F));
                }

                applyFireResistant(stack, allParts, netherited);
                applyEnchantModifiers(stack, data, type);
                applyCosmetics(stack, head, binding);

                stack.set(DataComponents.ITEM_NAME, Component.translatable(
                                "item.modular_dreams.modular_" + type.id + ".named", Component.translatable(head.nameKey())));
                stack.set(DataComponents.ITEM_MODEL, ModularDreams.id("modular_" + type.id + "_" + head.id()));
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
                                Optional.of(wooden ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                                Optional.of(wooden ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT));
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

        // ------------------------------------------------------------------ armor baking

        private static void bakeArmor(ItemStack stack, ModularData data, ModularArmorType type) {
                ModularMaterial plate = material(data, PartType.PLATE);
                Optional<ModularMaterial> lining = materialOpt(data, PartType.LINING);
                ModularMaterial.Plate plateStats = plate.plateOpt().orElseThrow();

                // ---- durability ----
                int durability = type.armorType.getDurability(plateStats.durabilityMultiplier());
                float liningBonus = lining.flatMap(ModularMaterial::liningOpt)
                                .map(ModularMaterial.Lining::durabilityBonus).orElse(0.0F);
                durability = Math.round(durability * (1.0F + liningBonus));
                float durabilityPct = modifierSum(data, Effect.DURABILITY_PCT);
                int durabilityFlat = Math.round(modifierSum(data, Effect.DURABILITY_FLAT));
                durability = Math.round(durability * (1.0F + durabilityPct)) + durabilityFlat;
                durability = Math.max(1, durability);

                // ---- defense ----
                int defense = plateStats.defense().getOrDefault(type.armorType, 0);
                float toughness = plateStats.toughness();
                float kbResistance = plateStats.knockbackResistance();
                kbResistance += modifierSum(data, Effect.KB_RESISTANCE);

                // ---- enchantability ----
                int enchantability = plateStats.enchantmentValue();
                enchantability += lining.flatMap(ModularMaterial::liningOpt)
                                .map(ModularMaterial.Lining::enchantmentBonus).orElse(0);

                // ---- attribute modifiers ----
                EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.slot);
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                if (defense != 0) {
                        builder.add(Attributes.ARMOR, new AttributeModifier(ModularDreams.id("armor"), defense,
                                        AttributeModifier.Operation.ADD_VALUE), group);
                }
                if (toughness != 0) {
                        builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ModularDreams.id("armor_toughness"),
                                        toughness, AttributeModifier.Operation.ADD_VALUE), group);
                }
                if (kbResistance != 0) {
                        builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(ModularDreams.id("armor_kb"),
                                        kbResistance, AttributeModifier.Operation.ADD_VALUE), group);
                }
                // trait attributes (turtle plate water affinity)
                if (plate.hasTrait(ModTraits.AQUATIC)) {
                        builder.add(Attributes.WATER_MOVEMENT_EFFICIENCY, new AttributeModifier(
                                        ModularDreams.id("trait_aquatic"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
                }
                // lining attribute bonuses
                lining.flatMap(ModularMaterial::liningOpt).ifPresent(l -> {
                        List<ModularMaterial.AttrBonus> attrs = l.attributes();
                        for (int i = 0; i < attrs.size(); i++) {
                                ModularMaterial.AttrBonus bonus = attrs.get(i);
                                builder.add(bonus.attribute(), new AttributeModifier(
                                                ModularDreams.id("lining_" + i), bonus.amount(), bonus.operation()), group);
                        }
                });
                // armor modifier attribute bonuses
                addModifierAttributes(data, builder, group);
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());

                // ---- equippable (per instance: vanilla armor visuals of the plate material) ----
                stack.set(DataComponents.EQUIPPABLE, Equippable.builder(type.slot)
                                .setEquipSound(plateStats.equipSound())
                                .setAsset(plateStats.asset())
                                .setDamageOnHurt(true)
                                .build());

                stack.set(DataComponents.MAX_DAMAGE, durability);
                stack.set(DataComponents.ENCHANTABLE, new Enchantable(Math.max(0, enchantability)));
                stack.set(DataComponents.REPAIRABLE, new Repairable(BuiltInRegistries.ITEM.getOrThrow(plateStats.repairTag())));

                applyFireResistant(stack, List.of(plate), data.levelOf("netherited") > 0);
                applyEnchantModifiers(stack, data, null);
                applyCosmetics(stack, plate, Optional.empty());

                stack.set(DataComponents.ITEM_NAME, Component.translatable(
                                "item.modular_dreams.modular_" + type.id + ".named", Component.translatable(plate.nameKey())));
                stack.set(DataComponents.ITEM_MODEL, ModularDreams.id("modular_" + type.id + "_" + plate.id()));
        }

        // ------------------------------------------------------------------ shared helpers

        private static void applyFireResistant(ItemStack stack, List<ModularMaterial> parts, boolean netherited) {
                boolean fireproof = netherited || parts.stream().anyMatch(m -> m.fireResistant());
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
					.orElseThrow(() -> new IllegalStateException("No registry access")).lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(actualKey);
                        ench.set(holder, entry.level());
                        changed = true;
                }
                if (changed) {
                        stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
                }
        }

        private static boolean isWeapon(ModularToolType type) {
                return type == ModularToolType.SWORD || type == ModularToolType.MACE || type == ModularToolType.SPEAR
                                || type == ModularToolType.AXE;
        }

        private static void applyCosmetics(ItemStack stack, ModularMaterial head, Optional<ModularMaterial> binding) {
                // Diamond bindings softly glint (PRECIOUS)
                if (binding.isPresent() && binding.get().hasTrait(ModTraits.PRECIOUS)) {
                        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                }
        }

        /** @return total durability damage reduction from the DENSE trait and the reinforced modifier (0..1). */
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

        private static void addModifierAttributes(ModularData data, ItemAttributeModifiers.Builder builder,
                        EquipmentSlotGroup group) {
                for (var entry : data.modifiers()) {
                        Optional<Modifier> modifier = Modifier.byId(entry.id());
                        if (modifier.isEmpty()) {
                                continue;
                        }
                        float amount = modifier.get().magnitude() * entry.level();
                        Identifier modifierId = ModularDreams.id("modifier_" + modifier.get().id());
                        switch (modifier.get().effect()) {
                                case FALL_DAMAGE_MULT -> builder.add(Attributes.FALL_DAMAGE_MULTIPLIER,
                                                new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                                                group);
                                case WATER_SPEED -> builder.add(Attributes.WATER_MOVEMENT_EFFICIENCY,
                                                new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                                                group);
                                case SNEAK_SPEED -> builder.add(Attributes.SNEAKING_SPEED,
                                                new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                                                group);
                                default -> {
                                }
                        }
                }
        }

        private static ModularMaterial material(ModularData data, PartType part) {
                return ModMaterials.getOrThrow(data.materialOf(part.id).orElseThrow(
                                () -> new IllegalStateException("Assembled item is missing part " + part.id)));
        }

        private static Optional<ModularMaterial> materialOpt(ModularData data, PartType part) {
                return data.materialOf(part.id).flatMap(ModMaterials::byId);
        }

        // ------------------------------------------------------------------ guide book stats

        public static ToolStats toolStats(ModularData data) {
                ModularToolType type = ModularToolType.byId(data.equipmentType()).orElseThrow();
                ModularMaterial head = material(data, type.headPart());
                Optional<ModularMaterial> handle = materialOpt(data, PartType.HANDLE);
                Optional<ModularMaterial> binding = type.bindingPart().flatMap(p -> materialOpt(data, p));

                float handleMult = handle.map(ModularMaterial::handleDurability).orElse(0.0F);
                float bindingMult = binding.map(ModularMaterial::bindingDurability).orElse(0.0F);
                int durability = Math.round(head.durability() * (1.0F + handleMult + bindingMult));
                durability = Math.round(durability * (1.0F + modifierSum(data, Effect.DURABILITY_PCT)))
                                + Math.round(modifierSum(data, Effect.DURABILITY_FLAT));

                float speed = head.speed() * (1.0F + modifierSum(data, Effect.MINING_SPEED_PCT));
                if (binding.isPresent() && binding.get().hasTrait(ModTraits.RESONANT)) {
                        speed *= 1.10F;
                }
                float damage = type.baseDamage + head.attackDamageBonus() + modifierSum(data, Effect.ATTACK_DAMAGE);
                if (head.hasTrait(ModTraits.SPIKY)) {
                        damage += 0.5F;
                }
                float attackSpeed = type.baseSpeed + modifierSum(data, Effect.ATTACK_SPEED);
                if (handle.isPresent() && handle.get().hasTrait(ModTraits.FEATHERWEIGHT)) {
                        attackSpeed += 0.15F;
                }
                int ench = head.enchantmentValue();
                if (binding.isPresent() && binding.get().hasTrait(ModTraits.GILDED)) {
                        ench += 3;
                }
                MaterialTier tier = head.tier();
                if (data.levelOf("netherited") > 0) {
                        tier = tier.max(MaterialTier.NETHERITE);
                }
                return new ToolStats(durability, speed, tier, damage, attackSpeed, ench);
        }

        public static ArmorStats armorStats(ModularData data) {
                ModularArmorType type = ModularArmorType.byId(data.equipmentType());
                ModularMaterial plate = material(data, PartType.PLATE);
                ModularMaterial.Plate plateStats = plate.plateOpt().orElseThrow();
                Optional<ModularMaterial> lining = materialOpt(data, PartType.LINING);

                int durability = type.armorType.getDurability(plateStats.durabilityMultiplier());
                float liningBonus = lining.flatMap(ModularMaterial::liningOpt)
                                .map(ModularMaterial.Lining::durabilityBonus).orElse(0.0F);
                durability = Math.round(durability * (1.0F + liningBonus));
                durability = Math.round(durability * (1.0F + modifierSum(data, Effect.DURABILITY_PCT)))
                                + Math.round(modifierSum(data, Effect.DURABILITY_FLAT));

                int defense = plateStats.defense().getOrDefault(type.armorType, 0);
                if (lining.isPresent() && lining.get().hasTrait(ModTraits.SOFT)) {
                        defense += 1;
                }
                return new ArmorStats(durability, defense, plateStats.toughness(), plateStats.knockbackResistance(),
                                plateStats.enchantmentValue());
        }
}
