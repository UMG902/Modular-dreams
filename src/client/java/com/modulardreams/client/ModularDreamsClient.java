package com.modulardreams.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.modulardreams.client.book.GuideBookScreen;
import com.modulardreams.equipment.GuideBookItem;
import com.modulardreams.menu.ModMenuTypes;
import com.modulardreams.menu.PartBuilderMenu;
import com.modulardreams.menu.PartBuilderMenu.PartOption;
import com.modulardreams.network.ModNetworking;
import com.modulardreams.registry.ModRegistryAccess;

@Environment(EnvType.CLIENT)
public class ModularDreamsClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                registerScreens();
                registerNetworking();

                // Modular tool rendering: registers the "modular_dreams:modular_tool"
                // item model type that stacks part layers tinted by their materials
                // (Tinkers'-style). Also self-registers via static init for datagen.
                com.modulardreams.client.itemmodel.ModularToolItemModel.Unbaked.register();

                // The guide book opens a custom client-only GUI (Tinkers'-style tabs
                // and a two-page spread); armor reuses vanilla equipment assets.
                GuideBookItem.opener = (player, hand) -> Minecraft.getInstance()
                                .setScreenAndShow(new GuideBookScreen());

                // Datapack registries (damage types / enchantments) are needed when
                // building example stacks for the creative tab on dedicated-server
                // clients, so capture the client's registry access once a level exists.
                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        if (client.level != null && ModRegistryAccess.get().isEmpty()) {
                                ModRegistryAccess.set(client.level.registryAccess());
                        }
                });
        }

        /**
         * Binds the station screens. MenuScreens.register is access-widened via the
         * classtweaker (it is private in 26.3).
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static void registerScreens() {
                MenuScreens.ScreenConstructor<com.modulardreams.menu.PartBuilderMenu, com.modulardreams.client.screen.PartBuilderScreen> partBuilder = com.modulardreams.client.screen.PartBuilderScreen::new;
                MenuScreens.ScreenConstructor<com.modulardreams.menu.AssemblyTableMenu, com.modulardreams.client.screen.AssemblyTableScreen> assemblyTable = com.modulardreams.client.screen.AssemblyTableScreen::new;
                MenuScreens.ScreenConstructor<com.modulardreams.menu.MeltingUpgradeMenu, com.modulardreams.client.screen.MeltingUpgradeScreen> meltingUpgrade = com.modulardreams.client.screen.MeltingUpgradeScreen::new;
                MenuScreens.ScreenConstructor<com.modulardreams.menu.MeltingFurnaceMenu, com.modulardreams.client.screen.MeltingFurnaceScreen> meltingFurnace = com.modulardreams.client.screen.MeltingFurnaceScreen::new;
                MenuScreens.register(ModMenuTypes.PART_BUILDER, partBuilder);
                MenuScreens.register(ModMenuTypes.ASSEMBLY_TABLE, assemblyTable);
                MenuScreens.register(ModMenuTypes.MELTING_UPGRADE, meltingUpgrade);
                MenuScreens.register(ModMenuTypes.MELTING_FURNACE, meltingFurnace);
        }

        /**
         * Receives the Part Builder option list that the server pushes whenever
         * the input changes (custom recipe types are not part of the vanilla
         * recipe sync in 26.3).
         */
        private static void registerNetworking() {
                ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PartBuilderOptionsPayload.TYPE,
                                (payload, context) -> context.client().execute(() -> {
                                        var player = context.client().player;
                                        if (player != null && player.containerMenu instanceof PartBuilderMenu menu
                                                        && menu.containerId == payload.containerId()) {
                                                List<PartOption> options = new ArrayList<>();
                                                for (ModNetworking.PartOption option : payload.options()) {
                                                        options.add(new PartOption(option.result(), option.cost()));
                                                }
                                                menu.applyClientOptions(options);
                                        }
                                }));
        }
}
