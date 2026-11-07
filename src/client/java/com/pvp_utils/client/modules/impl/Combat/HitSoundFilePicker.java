package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.gui.clickgui.UiText;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HitSoundFilePicker {
    private static final AtomicBoolean SELECTING = new AtomicBoolean();

    private HitSoundFilePicker() {
    }

    public static void select(boolean ranged) {
        if (!SELECTING.compareAndSet(false, true)) return;
        Minecraft client = Minecraft.getInstance();
        String current = ranged ? Config.hitSoundRangedFile : Config.hitSoundMeleeFile;
        String title = ranged ? UiText.t("选择远程命中音效", "Select Ranged Hit Sound")
                : UiText.t("选择近战命中音效", "Select Melee Hit Sound");
        Thread worker = new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(5);
                filters.put(stack.UTF8("*.wav")).put(stack.UTF8("*.aiff")).put(stack.UTF8("*.aif"))
                        .put(stack.UTF8("*.au")).put(stack.UTF8("*.snd")).flip();
                String selected = TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath(current), filters,
                        "WAV / AIFF / AU", false);
                if (selected == null || !client.isRunning()) return;
                String path = Path.of(selected).toAbsolutePath().normalize().toString();
                client.execute(() -> {
                    if (ranged) Config.hitSoundRangedFile = path;
                    else Config.hitSoundMeleeFile = path;
                    Config.save();
                });
            } catch (RuntimeException | LinkageError failure) {
                PVPUtils.LOGGER.warn("Unable to select a custom hit sound", failure);
            } finally {
                SELECTING.set(false);
            }
        }, "pvp-utils-hit-sound-picker");
        worker.setDaemon(true);
        worker.start();
    }

    private static String defaultPath(String current) {
        if (current == null || current.isBlank()) return "";
        try {
            return Path.of(current).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException ignored) {
            return "";
        }
    }
}
