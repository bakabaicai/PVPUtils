package com.pvp_utils.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicManager;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.TermsScreen;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleKeybindManager {
    public static final String ACTION_CLICK_GUI = "action.clickgui";
    public static final String ACTION_OPEN_MUSIC = "action.netease_music";
    public static final String ACTION_ZOOM = "action.zoom";
    public static final String ACTION_FREELOOK = "action.freelook";
    private static final int MOUSE_KEY_OFFSET = -1000;
    private static final Map<String, Integer> KEYBINDS = new LinkedHashMap<>();
    private static final Map<String, SettingModule> MODULES = new LinkedHashMap<>();
    private static final Map<String, Boolean> LAST_DOWN = new LinkedHashMap<>();
    private static String captureId = "";
    private static boolean initialized;

    private ModuleKeybindManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        readBindings();
    }

    public static void tick(Minecraft client) {
        initialize();
        if (client == null) return;
        for (Map.Entry<String, Integer> entry : KEYBINDS.entrySet()) {
            String id = entry.getKey();
            int key = entry.getValue();
            boolean down = isKeyDown(client, key);
            boolean previous = LAST_DOWN.getOrDefault(id, false);
            LAST_DOWN.put(id, down);
            if (down && !previous && client.screen == null && !isCapturing()) {
                trigger(client, id);
            }
        }
    }

    public static boolean beginCapture(String id) {
        if (id == null || id.isBlank()) return false;
        captureId = id;
        return true;
    }

    public static boolean captureKey(int key) {
        if (!isCapturing()) return false;
        String id = captureId;
        captureId = "";
        if (key == GLFW.GLFW_KEY_UNKNOWN) return true;
        KEYBINDS.put(id, key);
        LAST_DOWN.put(id, true);
        saveBindings();
        return true;
    }

    public static boolean clearBinding(String id) {
        if (id == null || id.isBlank()) return false;
        boolean removed = KEYBINDS.remove(id) != null;
        LAST_DOWN.remove(id);
        if (id.equals(captureId)) captureId = "";
        if (removed) saveBindings();
        return removed;
    }

    public static void clearAll(boolean save) {
        KEYBINDS.clear();
        LAST_DOWN.clear();
        captureId = "";
        applyDefaultBindings();
        storeBindings();
        if (save) Config.save();
    }

    public static boolean hasBinding(String id) {
        return KEYBINDS.containsKey(id);
    }

    public static boolean isCapturing() {
        return !captureId.isBlank();
    }

    public static boolean isCapturing(String id) {
        return id != null && id.equals(captureId);
    }

    public static String keyName(String id) {
        Integer key = KEYBINDS.get(id);
        if (key == null) return "";
        if (key <= MOUSE_KEY_OFFSET) return "Mouse " + (MOUSE_KEY_OFFSET - key + 1);
        return InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString();
    }

    public static void registerModule(SettingModule module) {
        if (module != null && module.isToggleable() && !module.usesActionKeybind() && !module.getBindingId().isBlank()) {
            MODULES.put(module.getBindingId(), module);
        }
    }

    private static void readBindings() {
        String raw = Config.moduleKeybinds;
        if (raw != null && !raw.isBlank()) {
            for (String entry : raw.split(";")) {
                int split = entry.lastIndexOf('=');
                if (split <= 0 || split >= entry.length() - 1) continue;
                try {
                    int key = Integer.parseInt(entry.substring(split + 1));
                    if (key != GLFW.GLFW_KEY_UNKNOWN) KEYBINDS.put(entry.substring(0, split), key);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        applyDefaultBindings();
    }

    public static boolean isKeyDown(Minecraft client, String id) {
        if (client == null || id == null) return false;
        Integer key = KEYBINDS.get(id);
        return key != null && isKeyDown(client, key);
    }

    private static boolean isKeyDown(Minecraft client, int key) {
        if (key <= MOUSE_KEY_OFFSET) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), MOUSE_KEY_OFFSET - key) == GLFW.GLFW_PRESS;
        }
        return key != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(client.getWindow(), key);
    }

    private static void trigger(Minecraft client, String id) {
        if (ACTION_CLICK_GUI.equals(id)) {
            Config.applyFirstUseLanguageDefault();
            client.setScreen(Config.termsRead ? new NewSettingsScreen(null) : new TermsScreen(null));
            return;
        }
        if (ACTION_OPEN_MUSIC.equals(id)) {
            NeteaseMusicManager.open();
            return;
        }
        if (ACTION_ZOOM.equals(id) || ACTION_FREELOOK.equals(id)) return;
        SettingModule module = MODULES.get(id);
        if (module != null) module.toggleFromKeybind();
    }

    private static void applyDefaultBindings() {
        KEYBINDS.putIfAbsent(ACTION_CLICK_GUI, GLFW.GLFW_KEY_RIGHT_SHIFT);
        KEYBINDS.putIfAbsent(ACTION_OPEN_MUSIC, GLFW.GLFW_KEY_RIGHT_CONTROL);
        KEYBINDS.putIfAbsent(ACTION_ZOOM, GLFW.GLFW_KEY_C);
        KEYBINDS.putIfAbsent(ACTION_FREELOOK, MOUSE_KEY_OFFSET - GLFW.GLFW_MOUSE_BUTTON_4);
    }

    private static void saveBindings() {
        storeBindings();
        Config.save();
    }

    private static void storeBindings() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> entry : KEYBINDS.entrySet()) {
            if (out.length() > 0) out.append(';');
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        Config.moduleKeybinds = out.toString();
    }
}
