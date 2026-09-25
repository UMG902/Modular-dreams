package com.modulardreams.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.modulardreams.registry.ModRegistryAccess;

public class ModularDreamsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The guide book reuses the vanilla written-book UI and armor reuses
		// vanilla equipment assets, so no custom client rendering is needed.
		// Datapack registries (damage types / enchantments) are needed when
		// building example stacks for the creative tab on dedicated-server
		// clients, so capture the client's registry access once a level exists.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null && ModRegistryAccess.get().isEmpty()) {
				ModRegistryAccess.set(client.level.registryAccess());
			}
		});
	}
}
