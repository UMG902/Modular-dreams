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
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.modulardreams.client.itemmodel.ModularToolItemModel;
import com.modulardreams.client.screen.AssemblerScreen;
import com.modulardreams.client.screen.PartPickerScreen;
import com.modulardreams.menu.AssemblerMenu;
import com.modulardreams.menu.ModMenuTypes;
import com.modulardreams.menu.PartPickerMenu;
import com.modulardreams.menu.PartPickerMenu.PartOption;
import com.modulardreams.network.ModNetworking;
import com.modulardreams.registry.ModRegistryAccess;

@Environment(EnvType.CLIENT)
public class ModularDreamsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerScreens();
        registerNetworking();

        // Modular tool rendering: registers the "modular_dreams:modular_tool"
        // item model type that stacks the per-material part textures
        // (Tinkers'-style layering).
        ModularToolItemModel.Unbaked.register();

        // Datapack registries (damage types) are needed when building example
        // stacks for the creative tab, so capture the client's registry access
        // once a level exists.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // track the joined world's registries; clear when leaving so a stale
            // provider from a previous server is never baked into stacks
            ModRegistryAccess.set(client.level != null ? client.level.registryAccess() : null);
        });
    }

    /**
     * Binds the station screens. MenuScreens.register is access-widened via
     * the classtweaker (it is private in 26.3).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerScreens() {
        MenuScreens.ScreenConstructor<PartPickerMenu, PartPickerScreen> partPicker = PartPickerScreen::new;
        MenuScreens.ScreenConstructor<AssemblerMenu, AssemblerScreen> assembler = AssemblerScreen::new;
        MenuScreens.ScreenConstructor<com.modulardreams.menu.KilnMenu, com.modulardreams.client.screen.KilnScreen> kiln =
                com.modulardreams.client.screen.KilnScreen::new;
        MenuScreens.ScreenConstructor<com.modulardreams.menu.FletchingMenu, com.modulardreams.client.screen.FletchingScreen> fletching =
                com.modulardreams.client.screen.FletchingScreen::new;
        MenuScreens.register(ModMenuTypes.PART_PICKER, partPicker);
        MenuScreens.register(ModMenuTypes.ASSEMBLER, assembler);
        MenuScreens.register(ModMenuTypes.KILN, kiln);
        MenuScreens.register(ModMenuTypes.FLETCHING, fletching);
    }

    /**
     * Receives the Part Picker option list that the server pushes whenever
     * the input changes (custom menus get no client recipe sync in 26.3).
     */
    private static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PartPickerOptionsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    var player = context.client().player;
                    if (player != null && player.containerMenu instanceof PartPickerMenu menu
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
