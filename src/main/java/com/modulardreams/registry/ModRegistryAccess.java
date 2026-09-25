package com.modulardreams.registry;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Captures the runtime {@link HolderLookup.Provider} (for data-pack registries
 * like damage types and enchantments) from whichever side is available.
 *
 * Datapack registries are not part of BuiltInRegistries, so any code that needs
 * enchantment/damage-type holders must go through this holder. The client
 * entrypoint additionally captures access from the loaded level.
 */
public final class ModRegistryAccess {

	private static volatile HolderLookup.Provider provider;

	private ModRegistryAccess() {}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> provider = server.registryAccess());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> provider = null);
	}

	/** Allows the client entrypoint to provide the client-side registry access. */
	public static void set(HolderLookup.Provider lookup) {
		provider = lookup;
	}

	public static Optional<HolderLookup.Provider> get() {
		return Optional.ofNullable(provider);
	}
}
