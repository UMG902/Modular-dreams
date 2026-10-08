package com.modulardreams.registry;

import java.util.Optional;

import net.minecraft.core.HolderLookup;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Captures the runtime {@link HolderLookup.Provider} (for data-pack registries
 * like damage types) from whichever side is available.
 *
 * Datapack registries are not part of BuiltInRegistries, so code that needs
 * damage-type holders must go through this holder. The client entrypoint
 * additionally captures access from the loaded level.
 */
public final class ModRegistryAccess {

    // server-side access (integrated or dedicated server) and the client's
    // connection access are tracked separately so neither side can leave a
    // stale provider from a previous world behind
    private static volatile HolderLookup.Provider provider;
    private static volatile HolderLookup.Provider clientProvider;

    private ModRegistryAccess() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> provider = server.registryAccess());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> provider = null);
    }

    /**
     * Allows the client entrypoint to provide (or, with null, clear) the
     * client-side registry access of the currently joined world.
     */
    public static void set(HolderLookup.Provider lookup) {
        clientProvider = lookup;
    }

    public static Optional<HolderLookup.Provider> get() {
        HolderLookup.Provider server = provider;
        return Optional.ofNullable(server != null ? server : clientProvider);
    }
}
