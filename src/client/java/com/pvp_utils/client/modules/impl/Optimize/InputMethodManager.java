package com.pvp_utils.client.modules.impl.Optimize;

import com.pvp_utils.Config;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
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

        if (!Config.disableImeInGame || client.player == null || shouldAllowInputMethod(client.screen)) {
            restore();
            return;
        }

        disable(client);
    }

    private static boolean shouldAllowInputMethod(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof ChatScreen || screen instanceof InBedChatScreen) return true;
        if (screen instanceof AbstractSignEditScreen || screen instanceof BookEditScreen) return true;
        if (screen instanceof AbstractCommandBlockEditScreen || screen instanceof AnvilScreen) return true;

        GuiEventListener focused = screen.getFocused();
        return focused instanceof EditBox || focused instanceof MultiLineEditBox;
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

    private interface Imm32 extends Library {
        Imm32 INSTANCE = Native.load("imm32", Imm32.class);

        Pointer ImmAssociateContext(Pointer hwnd, Pointer inputContext);
    }
}
