package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class CustomEnchantmentGlint {
    private static final Identifier ARMOR_GLINT = Identifier.fromNamespaceAndPath("pvp_utils", "enchanted/enchanted_glint_armor.png");
    private static final Identifier ENTITY_GLINT_TEXTURE = Identifier.fromNamespaceAndPath("pvp_utils", "enchanted/enchanted_glint_entity.png");
    private static final Identifier ITEM_GLINT = Identifier.fromNamespaceAndPath("pvp_utils", "enchanted/enchanted_glint_item.png");
    private static GpuTextureView armorTextureView;
    private static GpuTextureView entityTextureView;
    private static GpuTextureView itemTextureView;

    private CustomEnchantmentGlint() {
    }

    public static void tick(Minecraft client) {
        if (!Config.customEnchantmentGlint || client == null || client.getTextureManager() == null) {
            return;
        }
        armorTextureView = loadTextureView(client, ARMOR_GLINT);
        entityTextureView = loadTextureView(client, ENTITY_GLINT_TEXTURE);
        itemTextureView = loadTextureView(client, ITEM_GLINT);
    }

    public static GpuTextureView textureViewFor(String renderTypeName) {
        if (!Config.customEnchantmentGlint || renderTypeName == null) {
            return null;
        }
        return switch (renderTypeName) {
            case "armor_entity_glint" -> armorTextureView;
            case "entity_glint" -> entityTextureView;
            case "glint", "glint_translucent" -> itemTextureView;
            default -> null;
        };
    }

    private static GpuTextureView loadTextureView(Minecraft client, Identifier texture) {
        AbstractTexture abstractTexture = client.getTextureManager().getTexture(texture);
        return abstractTexture.getTextureView();
    }
}
