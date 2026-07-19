package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.RenderSetupAccessor;
import com.pvp_utils.mixin.client.RenderTextureBindingAccessor;
import com.pvp_utils.mixin.client.RenderTypeStateAccessor;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;

import java.util.Map;

public final class HeldItemPositionManager {
    private static final ThreadLocal<Integer> HELD_ITEM_ALPHA = ThreadLocal.withInitial(() -> 255);
    private static final ThreadLocal<Integer> ITEM_RENDER_ALPHA = ThreadLocal.withInitial(() -> 255);

    private HeldItemPositionManager() {
    }

    public static float applySwingSpeed(InteractionHand hand, float swingProgress) {
        if (!Config.heldItemPosition) {
            return swingProgress;
        }
        float speed = hand == InteractionHand.MAIN_HAND ? Config.heldItemMainSwingSpeed : Config.heldItemOffSwingSpeed;
        if (speed <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, swingProgress * speed));
    }

    public static int swingDuration(int original, Player player, InteractionHand hand) {
        float speed = blockingAnimationActive(player, hand) || !Config.heldItemPosition ? Config.animSpeed : swingSpeed(hand);
        if (speed <= 0.0f) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, Math.round(original / speed));
    }

    public static void beginHandRender(InteractionHand hand) {
        HELD_ITEM_ALPHA.set(alphaForHand(hand));
    }

    public static void endHandRender() {
        HELD_ITEM_ALPHA.remove();
    }

    public static int[] applyHeldItemAlpha(int[] colors) {
        int alpha = HELD_ITEM_ALPHA.get();
        if (alpha >= 255) {
            return colors;
        }
        if (colors == null || colors.length == 0) {
            return new int[] { (alpha << 24) | 0x00FFFFFF };
        }
        int[] copy = colors.clone();
        for (int i = 0; i < copy.length; i++) {
            copy[i] = applyAlpha(copy[i], alpha);
        }
        return copy;
    }

    public static RenderType applyHeldItemRenderType(RenderType renderType) {
        if (HELD_ITEM_ALPHA.get() >= 255 || renderType == null) {
            return renderType;
        }
        Identifier texture = textureOf(renderType);
        return texture == null ? renderType : RenderTypes.itemEntityTranslucentCull(texture);
    }

    public static RenderType applySubmittedItemRenderType(RenderType renderType, int[] colors) {
        if (!hasTintAlpha(colors) || renderType == null) {
            return renderType;
        }
        Identifier texture = textureOf(renderType);
        return texture == null ? renderType : RenderTypes.itemEntityTranslucentCull(texture);
    }

    public static boolean isHoldingTransparentItem() {
        return HELD_ITEM_ALPHA.get() < 255;
    }

    public static void beginItemRender(int[] colors) {
        ITEM_RENDER_ALPHA.set(alphaFromColors(colors));
    }

    public static void endItemRender() {
        ITEM_RENDER_ALPHA.remove();
    }

    public static float applyQuadAlpha(float alpha) {
        int itemAlpha = ITEM_RENDER_ALPHA.get();
        if (itemAlpha >= 255) {
            return alpha;
        }
        return alpha * (itemAlpha / 255.0f);
    }

    public static boolean hasTintAlpha(int[] colors) {
        return alphaFromColors(colors) < 255;
    }

    private static float swingSpeed(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? Config.heldItemMainSwingSpeed : Config.heldItemOffSwingSpeed;
    }

    private static boolean blockingAnimationActive(Player player, InteractionHand hand) {
        if (player == null || hand != InteractionHand.MAIN_HAND || !Config.swordBlock || !player.getMainHandItem().is(ItemTags.SWORDS)) {
            return false;
        }
        return Config.autoMode || isUseKeyDown() || isSettingsScreenOpen();
    }

    private static boolean isUseKeyDown() {
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            return client != null && client.options != null && client.options.keyUse.isDown();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSettingsScreenOpen() {
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            return client != null && client.screen instanceof com.pvp_utils.client.gui.SettingsScreen;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int alphaForHand(InteractionHand hand) {
        if (!Config.heldItemPosition) {
            return 255;
        }
        int percent = hand == InteractionHand.MAIN_HAND ? Config.heldItemMainAlpha : Config.heldItemOffAlpha;
        return Math.round(Math.max(0, Math.min(100, percent)) * 2.55f);
    }

    private static Identifier textureOf(RenderType renderType) {
        try {
            RenderSetup setup = ((RenderTypeStateAccessor) renderType).pvp_utils$getState();
            Map<String, ?> textures = ((RenderSetupAccessor) (Object) setup).pvp_utils$getTextures();
            Object sampler0 = textures.get("Sampler0");
            if (sampler0 instanceof RenderTextureBindingAccessor binding) {
                return binding.pvp_utils$getLocation();
            }
            for (Object binding : textures.values()) {
                if (binding instanceof RenderTextureBindingAccessor accessor) {
                    return accessor.pvp_utils$getLocation();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int alphaFromColors(int[] colors) {
        if (colors == null || colors.length == 0) {
            return 255;
        }
        int alpha = 255;
        for (int color : colors) {
            alpha = Math.min(alpha, (color >>> 24) & 0xFF);
        }
        return alpha;
    }

    private static int applyAlpha(int color, int alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }
        int finalAlpha = Math.round(baseAlpha * (alpha / 255.0f));
        return (finalAlpha << 24) | (color & 0x00FFFFFF);
    }
}
