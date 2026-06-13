package com.pvp_utils.client;

import com.pvp_utils.Config;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.util.Locale;

public final class InputMethodManager {
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win");

    private static Pointer windowHandle;
    private static Pointer previousInputContext;
    private static boolean disabled;

    private InputMethodManager() {}

    public static void tick(Minecraft client) {
        if (!WINDOWS) return;

        if (!shouldDisable(client)) {
            restore();
            return;
        }

        disable(client);
    }

    private static void disable(Minecraft client) {
        Pointer hwnd = getWindowHandle(client);
        if (hwnd == null) return;

        if (disabled && windowHandle != null && Pointer.nativeValue(windowHandle) != Pointer.nativeValue(hwnd)) {
            restore();
        }

        if (disabled) return;

        Pointer inputContext = Imm32.INSTANCE.ImmAssociateContext(hwnd, null);
        previousInputContext = inputContext;
        windowHandle = hwnd;
        disabled = true;
    }

    private static void restore() {
        if (!disabled || windowHandle == null) return;

        Imm32.INSTANCE.ImmAssociateContext(windowHandle, previousInputContext);
        windowHandle = null;
        previousInputContext = null;
        disabled = false;
    }

    private static Pointer getWindowHandle(Minecraft client) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().handle());
        return hwnd == 0 ? null : Pointer.createConstant(hwnd);
    }

    private static boolean shouldDisable(Minecraft client) {
        if (!Config.disableImeInGame || client.player == null) return false;
        return !isTextInputFocused(client.screen);
    }

    private static boolean isTextInputFocused(ContainerEventHandler container) {
        if (container == null) return false;

        GuiEventListener focused = container.getFocused();
        if (focused instanceof EditBox) return true;
        if (focused instanceof ContainerEventHandler focusedContainer) {
            return isTextInputFocused(focusedContainer);
        }
        return false;
    }

    private interface Imm32 extends Library {
        Imm32 INSTANCE = Native.load("imm32", Imm32.class);

        Pointer ImmAssociateContext(Pointer hwnd, Pointer inputContext);
    }
}
