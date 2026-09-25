package com.modulardreams.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;

import net.minecraft.core.registries.Registries;

import com.modulardreams.ModularDreams;

import java.util.List;

/**
 * Custom data components of Modular Dreams.
 *
 * {@link #MODULAR_DATA} is the heart of the system: it records which vanilla
 * materials every part of a piece of equipment is made of, plus the levels of
 * every applied modifier. All gameplay stats are derived from it and baked into
 * vanilla components by the {@code StatsEngine}.
 */
public class ModDataComponents {

	/** One part of an assembled item: a part type id + a material id. */
	public record PartData(String part, String material) {
		public static final Codec<PartData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("part").forGetter(PartData::part),
				Codec.STRING.fieldOf("material").forGetter(PartData::material)
		).apply(i, PartData::new));
	}

	/** One applied modifier: its id and current level (1-based). */
	public record ModifierEntry(String id, int level) {
		public static final Codec<ModifierEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("id").forGetter(ModifierEntry::id),
				Codec.INT.fieldOf("level").forGetter(ModifierEntry::level)
		).apply(i, ModifierEntry::new));
	}

	/** The full modular definition of an assembled item. */
	public record ModularData(String equipmentType, List<PartData> parts, List<ModifierEntry> modifiers) {

		public static final Codec<ModularData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("equipment_type").forGetter(ModularData::equipmentType),
				PartData.CODEC.listOf().fieldOf("parts").forGetter(ModularData::parts),
				ModifierEntry.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(ModularData::modifiers)
		).apply(i, ModularData::new));

		public static final ModularData EMPTY = new ModularData("none", List.of(), List.of());

		public java.util.Optional<String> materialOf(String partType) {
			return parts.stream().filter(p -> p.part().equals(partType))
					.map(PartData::material).findFirst();
		}

		public int levelOf(String modifierId) {
			return modifiers.stream().filter(m -> m.id().equals(modifierId))
					.mapToInt(ModifierEntry::level).findFirst().orElse(0);
		}

		public ModularData withModifier(String modifierId, int newLevel) {
			List<ModifierEntry> updated = modifiers.stream()
					.filter(m -> !m.id().equals(modifierId))
					.collect(java.util.stream.Collectors.toList());
			if (newLevel > 0) {
				updated.add(new ModifierEntry(modifierId, newLevel));
			}
			return new ModularData(equipmentType, parts, List.copyOf(updated));
		}
	}

	public static final ResourceKey<DataComponentType<?>> MODULAR_DATA_KEY = ResourceKey
			.create(Registries.DATA_COMPONENT_TYPE, ModularDreams.id("modular_data"));

	public static final DataComponentType<ModularData> MODULAR_DATA = DataComponentType.<ModularData>builder()
			.persistent(ModularData.CODEC)
			.networkSynchronized(ByteBufCodecs.fromCodec(ModularData.CODEC))
			.build();

	public static void initialize() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, MODULAR_DATA_KEY, MODULAR_DATA);
	}
}
