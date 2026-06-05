package com.pvp_utils.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyMapping openSettings;

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("pvp_utils", "pvp_utils")
        );

        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvp_utils.open_settings",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
    }
}