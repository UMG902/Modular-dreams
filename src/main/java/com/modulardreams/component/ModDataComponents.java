package com.modulardreams.component;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;

import com.modulardreams.ModularDreams;

/**
 * Custom data components of Modular Dreams v2.
 *
 * {@link #MODULAR_DATA} records which material every part of an assembled
 * item is made of (handle + head), plus modifier levels reserved for the
 * traits &amp; modifiers milestone. All gameplay stats are derived from it and
 * baked into vanilla components by the {@code StatsEngine}.
 */
public class ModDataComponents {

    /** One part of an assembled item: a part type id + a material id. */
    public record PartData(String part, String material) {
        public static final Codec<PartData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("part").forGetter(PartData::part),
                Codec.STRING.fieldOf("material").forGetter(PartData::material)
        ).apply(i, PartData::new));
    }

    /** One applied modifier. Traits have NO levels: a modifier is either on a tool or not. */
    public record ModifierEntry(String id) {
        public static final Codec<ModifierEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(ModifierEntry::id)
        ).apply(i, ModifierEntry::new));
    }

    /**
     * The full modular definition of an assembled item.
     *
     * <p>{@code unlocks} counts the modifier-slot expansions bought at the
     * smithing table with a nether star (1), an elytra (2) and the dragon
     * egg (3). A tool with unlocks == 3 carries the dragon egg - it is the
     * indestructible 6-slot legendary and its egg can be extracted back.
     */
    public record ModularData(String equipmentType, List<PartData> parts, List<ModifierEntry> modifiers,
            int unlocks) {

        public static final int BASE_MODIFIER_CAP = 3;
        public static final int MAX_UNLOCKS = 3;

        public static final Codec<ModularData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("equipment_type").forGetter(ModularData::equipmentType),
                PartData.CODEC.listOf().fieldOf("parts").forGetter(ModularData::parts),
                ModifierEntry.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(ModularData::modifiers),
                Codec.INT.optionalFieldOf("unlocks", 0).forGetter(ModularData::unlocks)
        ).apply(i, ModularData::new));

        public static final ModularData EMPTY = new ModularData("none", List.of(), List.of(), 0);

        public java.util.Optional<String> materialOf(String partType) {
            return parts.stream().filter(p -> p.part().equals(partType))
                    .map(PartData::material).findFirst();
        }

        public java.util.Optional<PartData> partOf(String partType) {
            return parts.stream().filter(p -> p.part().equals(partType)).findFirst();
        }

        /** Whether the tool carries this applied modifier (traits have no levels). */
        public boolean hasModifier(String modifierId) {
            return modifiers.stream().anyMatch(m -> m.id().equals(modifierId));
        }

        /** The modifier cap for this tool: 3 base + one per unlock tier. */
        public int modifierCap() {
            return BASE_MODIFIER_CAP + Math.max(0, Math.min(MAX_UNLOCKS, unlocks));
        }

        /** Whether this item carries the dragon egg (the third unlock). */
        public boolean hasDragonEgg() {
            return unlocks >= MAX_UNLOCKS;
        }
    }

    public static final DataComponentType<ModularData> MODULAR_DATA = DataComponentType.<ModularData>builder()
            .persistent(ModularData.CODEC)
            .networkSynchronized(ByteBufCodecs.fromCodec(ModularData.CODEC))
            .build();

    /**
     * Version stamp of the stat formula that baked this stack. Stacks whose
     * stamp differs are re-baked from their modular data the first time they
     * tick in an inventory, so older tools self-heal.
     */
    public static final DataComponentType<Integer> STATS_VERSION = DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceKey.create(Registries.DATA_COMPONENT_TYPE, ModularDreams.id("modular_data")),
                MODULAR_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceKey.create(Registries.DATA_COMPONENT_TYPE, ModularDreams.id("stats_version")),
                STATS_VERSION);
    }
}
