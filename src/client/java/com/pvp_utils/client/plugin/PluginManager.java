package com.pvp_utils.client.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.render.skia.SkijaUi;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import org.mozilla.javascript.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;

public final class PluginManager {
    public static final PluginManager INSTANCE = new PluginManager();
    private final Map<String, Entry> plugins = new LinkedHashMap<>();
    private JsonObject enabledState = new JsonObject();
    private int revision;
    private boolean initialized;
    private Object currentLevel;
    private Canvas currentCanvas;
    private final List<ScreenRequest> screenRequests = new ArrayList<>();

    private PluginManager() {
    }

    public record PluginInfo(String id, String name, String version, String description, String author,
                             boolean enabled, String error) {
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;
        if (!PluginDirectory.ensureExists()) return;
        Path state = PluginDirectory.path().resolve(".enabled.json");
        try {
            if (Files.exists(state)) {
                if (Files.isSymbolicLink(state) || Files.size(state) > 65536) throw new IOException("Invalid plugin state file");
                enabledState = JsonParser.parseString(Files.readString(state, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (Exception e) {
            PVPUtils.LOGGER.error("Failed to read plugin enabled state", e);
        }
        refresh();
    }

    public int revision() {
        return revision;
    }

    public List<PluginSetting> settings(String id) {
        Entry entry = plugins.get(id);
        return entry == null || entry.runtime == null ? List.of() : entry.runtime.settings();
    }

    public boolean isEnabled(String id) {
        Entry entry = plugins.get(id);
        return entry != null && entry.runtime != null;
    }

    public List<PluginInfo> plugins() {
        List<PluginInfo> result = new ArrayList<>();
        for (Entry entry : plugins.values()) {
            PluginManifest manifest = entry.manifest;
            result.add(new PluginInfo(entry.id, manifest == null ? entry.id : manifest.name(),
                    manifest == null ? "" : manifest.version(), manifest == null ? "" : manifest.description(),
                    manifest == null ? "" : manifest.author(), entry.runtime != null, entry.error));
        }
        return List.copyOf(result);
    }

    public record HudEditItem(String key, String label, PluginRuntime.HudLayout layout, Runnable save,
                              java.util.function.BiConsumer<Float, Float> move) {}

    public List<HudEditItem> hudEditItems(int width, int height) {
        List<HudEditItem> result = new ArrayList<>();
        for (Entry entry : plugins.values()) {
            if (entry.runtime == null) continue;
            for (PluginRuntime.HudLayout layout : entry.runtime.hudLayouts()) {
                if (!layout.draggable()) continue;
                result.add(new HudEditItem("plugin:" + entry.id + ":" + layout.id(), layout.label(), layout,
                        () -> saveHud(entry), (x, y) -> {
                            if (entry.runtime != null) entry.runtime.moveHud(layout.id(), x, y, width, height);
                        }));
            }
        }
        return List.copyOf(result);
    }

    public void saveHudLayouts() {
        for (Entry entry : plugins.values()) {
            saveHud(entry);
        }
    }

    private void saveHud(Entry entry) {
        if (entry.runtime == null) return;
        try {
            entry.runtime.saveHudLayouts();
        } catch (Exception e) {
            fail(entry, e);
        }
    }

    public void openScreen(String plugin, String screen) {
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null || !entry.runtime.hasScreen(screen)) {
            throw new IllegalArgumentException("Unknown plugin screen");
        }
        if (screenRequests.size() >= 32) throw new IllegalStateException("Screen request limit");
        screenRequests.add(new ScreenRequest(plugin, "open", screen));
    }

    public void renderScreen(String plugin, String id, Canvas canvas, int width, int height) {
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null) return;
        Canvas previous = currentCanvas;
        currentCanvas = canvas;
        int save = canvas.save();
        try {
            entry.runtime.renderScreen(id, width, height);
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
        } finally {
            canvas.restoreToCount(save);
            currentCanvas = previous;
        }
    }

    public boolean screenClick(String plugin, String id, double x, double y, int button) {
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null) return false;
        try {
            return entry.runtime.clickScreen(id, x, y, button);
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
            return true;
        }
    }

    public void screenEvent(String plugin, String id, String event) {
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null) return;
        try {
            entry.runtime.screenEvent(id, event);
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
        }
    }

    public boolean screenInput(String plugin, String id, String event, Object... args) {
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null) return false;
        try {
            return entry.runtime.screenInput(id, event, args);
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
            return true;
        }
    }

    public void refresh() {
        shutdownRuntimes();
        plugins.clear();
        revision++;
        if (!PluginDirectory.ensureExists()) return;
        try (var children = Files.list(PluginDirectory.path())) {
            Path root = PluginDirectory.path().toRealPath();
            for (Path directory : children.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith(".")).sorted().limit(64).toList()) {
                if (!Files.exists(directory.resolve("plugin.json"))) continue;
                String id = directory.getFileName().toString();
                Entry entry = new Entry(id);
                try {
                    if (!directory.toRealPath().startsWith(root)) throw new IOException("Plugin directory outside root");
                    entry.manifest = PluginManifest.read(directory);
                    entry.id = entry.manifest.id();
                    if (plugins.containsKey(entry.id)) throw new IOException("Duplicate plugin id: " + entry.id);
                    plugins.put(entry.id, entry);
                    boolean enabled = enabledState.has(entry.id)
                            ? enabledState.get(entry.id).getAsBoolean() : entry.manifest.defaultEnabled();
                    if (enabled) enable(entry);
                } catch (Exception e) {
                    plugins.values().removeIf(current -> current == entry);
                    entry.id = "invalid:" + id;
                    entry.manifest = null;
                    entry.error = message(e);
                    plugins.put(entry.id, entry);
                    PVPUtils.LOGGER.error("Failed to discover plugin {}", directory, e);
                }
            }
        } catch (Exception e) {
            PVPUtils.LOGGER.error("Failed to scan plugin directory", e);
        }
    }

    public void setEnabled(String id, boolean enabled) {
        Entry entry = plugins.get(id);
        if (entry == null || entry.manifest == null) return;
        if (enabled && entry.runtime == null) enable(entry);
        if (!enabled) disable(entry);
        enabledState.addProperty(id, entry.runtime != null);
        saveState();
        revision++;
    }

    public void reload(String id) {
        Entry entry = plugins.get(id);
        if (entry == null || entry.manifest == null) return;
        boolean wasEnabled = entry.runtime != null;
        disable(entry);
        try {
            PluginManifest updated = PluginManifest.read(entry.manifest.directory());
            if (!updated.id().equals(id)) throw new IOException("Plugin id changed; refresh the plugin list");
            entry.manifest = updated;
            entry.error = "";
            if (wasEnabled) enable(entry);
        } catch (Exception e) {
            entry.error = message(e);
            PVPUtils.LOGGER.error("Failed to reload plugin {}", id, e);
        }
        revision++;
    }

    public List<String> check() {
        List<String> result = new ArrayList<>();
        if (!PluginDirectory.ensureExists()) return List.of("插件目录创建失败");
        try (var children = Files.list(PluginDirectory.path())) {
            for (Path directory : children.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith(".")).sorted().limit(64).toList()) {
                if (!Files.exists(directory.resolve("plugin.json"))) continue;
                try {
                    PluginManifest manifest = PluginManifest.read(directory);
                    PluginRuntime.validateScript(manifest);
                    result.add(manifest.id() + " OK");
                } catch (Exception e) {
                    result.add(directory.getFileName() + " ERROR: " + message(e));
                }
            }
        } catch (Exception e) {
            result.add("SCAN ERROR: " + message(e));
        }
        return List.copyOf(result);
    }

    public void prepareTick() {
        List<ScreenRequest> requests = List.copyOf(screenRequests);
        screenRequests.clear();
        Minecraft client = Minecraft.getInstance();
        for (ScreenRequest request : requests) {
            Entry entry = plugins.get(request.plugin);
            if (entry == null || entry.runtime == null) continue;
            if (request.operation.equals("close")) {
                if (client.screen instanceof PluginScreen screen && screen.plugin().equals(request.plugin)) screen.onClose();
            } else if (entry.runtime.hasScreen(request.id)) {
                if (client.player != null && client.player.containerMenu != client.player.inventoryMenu) client.player.closeContainer();
                net.minecraft.client.gui.screens.Screen parent = client.screen;
                if (parent instanceof PluginScreen previous) parent = previous.parent();
                client.setScreen(new PluginScreen(request.plugin, request.id, entry.runtime.screenTitle(request.id), parent));
            }
        }
        for (Entry entry : plugins.values()) {
            if (entry.host != null) entry.host.player.synchronize();
        }
    }

    public void tick(Minecraft client) {
        Object level = client.level;
        if (currentLevel != level) {
            if (currentLevel != null) dispatch("leave");
            currentLevel = level;
            if (level != null) dispatch("join");
        }
        dispatch("tick");
    }

    public void render(Canvas canvas, int width, int height) {
        currentCanvas = canvas;
        try {
            for (Entry entry : List.copyOf(plugins.values())) {
                if (entry.runtime == null) continue;
                int save = canvas.save();
                try {
                    entry.runtime.render(width, height);
                } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
                    fail(entry, e);
                } finally {
                    canvas.restoreToCount(save);
                }
            }
        } finally {
            currentCanvas = null;
        }
    }

    private void dispatch(String event) {
        for (Entry entry : List.copyOf(plugins.values())) {
            if (entry.runtime == null) continue;
            try {
                entry.runtime.emit(event);
            } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
                fail(entry, e);
            }
        }
    }

    private void enable(Entry entry) {
        try {
            PluginManifest latest = PluginManifest.read(entry.manifest.directory());
            if (!latest.id().equals(entry.id)) throw new IOException("Plugin id changed; refresh the plugin list");
            entry.manifest = latest;
            Path root = PluginDirectory.path().toRealPath();
            Path data = root.resolve(".data");
            Files.createDirectories(data);
            if (!data.toRealPath().startsWith(root)) throw new IOException("Plugin storage outside root");
            Path file = data.resolve(entry.id + ".json");
            if (Files.isSymbolicLink(file)) throw new IOException("Plugin storage is a symbolic link");
            PluginStorage storage = new PluginStorage(file);
            entry.host = new GameHost(entry.id);
            entry.runtime = new PluginRuntime(entry.manifest, storage, entry.host);
            entry.runtime.start();
            entry.error = "";
            PVPUtils.LOGGER.info("Loaded JS plugin {} {}", entry.id, entry.manifest.version());
        } catch (Exception | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
        }
    }

    private void fail(Entry entry, Throwable error) {
        entry.error = message(error);
        PVPUtils.LOGGER.error("JS plugin {} failed", entry.id, error);
        disable(entry);
        revision++;
        NotificationOverlay.getInstance().show(UiText.t("插件运行失败：", "Plugin failed: ") + entry.id);
    }

    private void disable(Entry entry) {
        PluginRuntime runtime = entry.runtime;
        entry.runtime = null;
        screenRequests.removeIf(request -> request.plugin.equals(entry.id));
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof PluginScreen screen && screen.plugin().equals(entry.id)) screen.onClose();
        try {
            if (runtime != null) {
                runtime.close();
            }
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            PVPUtils.LOGGER.error("Failed to unload JS plugin {}", entry.id, e);
        } finally {
            if (entry.host != null) entry.host.player.close();
            entry.host = null;
        }
    }

    public void close() {
        shutdownRuntimes();
        currentLevel = null;
    }

    private void shutdownRuntimes() {
        for (Entry entry : plugins.values()) disable(entry);
    }

    private void saveState() {
        try {
            Path statePath = PluginDirectory.path().resolve(".enabled.json");
            if (Files.isSymbolicLink(statePath)) throw new IOException("Plugin state is a symbolic link");
            PluginStorage state = new PluginStorage(statePath);
            state.replace(enabledState);
        } catch (Exception e) {
            PVPUtils.LOGGER.error("Failed to save plugin enabled state", e);
        }
    }

    private static String message(Throwable error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message.substring(0, Math.min(512, message.length()));
    }

    private static final class Entry {
        private String id;
        private PluginManifest manifest;
        private PluginRuntime runtime;
        private GameHost host;
        private String error = "";

        private Entry(String id) {
            this.id = id;
        }
    }

    private final class GameHost implements PluginRuntime.Host {
        private final String id;
        private final PluginPlayerBridge player = new PluginPlayerBridge();
        private final ArrayDeque<Integer> drawScopes = new ArrayDeque<>();
        private Matrix33 baseMatrix;

        private GameHost(String id) {
            this.id = id;
        }

        @Override
        public void log(String message) {
            PVPUtils.LOGGER.info("[plugin:{}] {}", id, message);
        }

        @Override
        public void notify(String message) {
            NotificationOverlay.getInstance().show(message);
        }

        @Override
        public String playerSnapshot() {
            return player.snapshot();
        }

        @Override
        public String playerToken() {
            return player.token();
        }

        @Override
        public String playerCall(String token, String operation, Object[] arguments) {
            return player.call(token, operation, arguments);
        }

        @Override
        public void releasePlayer() {
            player.close();
        }

        @Override
        public double textWidth(Object[] args) {
            float size = number(args, 1);
            if (size < 1 || size > 128) throw new IllegalArgumentException("Invalid font size");
            return args.length > 2
                    ? SkijaUi.textWidthWithFallback(PluginRuntime.string(args, 0), size, PluginRuntime.string(args, 2))
                    : SkijaUi.textWidthWithFallback(PluginRuntime.string(args, 0), size);
        }

        @Override
        public void pushDraw() {
            if (drawScopes.isEmpty()) baseMatrix = currentCanvas.getLocalToDeviceAsMatrix33();
            drawScopes.push(currentCanvas.save());
        }

        @Override
        public void popDraw() { currentCanvas.restoreToCount(drawScopes.pop()); }

        @Override
        public double mouseX() { return Minecraft.getInstance().mouseHandler.getScaledXPos(Minecraft.getInstance().getWindow()); }

        @Override
        public double mouseY() { return Minecraft.getInstance().mouseHandler.getScaledYPos(Minecraft.getInstance().getWindow()); }

        @Override
        public void screen(String operation, String screen) {
            if (screenRequests.size() >= 32) throw new IllegalStateException("Screen request limit");
            screenRequests.add(new ScreenRequest(id, operation, screen));
        }

        @Override
        public void draw(String operation, Object[] args) {
            if (currentCanvas == null) throw new IllegalStateException("No active HUD frame");
            switch (operation) {
                case "text", "textShadow" -> {
                    String text = PluginRuntime.string(args, 0);
                    float size = number(args, 3);
                    if (size < 1 || size > 128) throw new IllegalArgumentException("Invalid font size");
                    float height = SkijaUi.textMetrics(size).getDescent() - SkijaUi.textMetrics(size).getAscent();
                    if (operation.equals("textShadow")) {
                        if (args.length > 5) {
                            SkijaUi.textShadowWithFallback(currentCanvas, text, number(args, 1), number(args, 2), height, color(args, 4), size, PluginRuntime.string(args, 5));
                        } else {
                            SkijaUi.textShadowWithFallback(currentCanvas, text, number(args, 1), number(args, 2), height, color(args, 4), size);
                        }
                    } else {
                        if (args.length > 5) {
                            SkijaUi.textWithFallback(currentCanvas, text, number(args, 1), number(args, 2), height, color(args, 4), size, PluginRuntime.string(args, 5));
                        } else {
                            SkijaUi.textWithFallback(currentCanvas, text, number(args, 1), number(args, 2), height, color(args, 4), size);
                        }
                    }
                }
                case "rect" -> SkijaUi.fill(currentCanvas, number(args, 0), number(args, 1),
                        number(args, 2), number(args, 3), color(args, 4));
                case "roundedRect" -> SkijaUi.rounded(currentCanvas, number(args, 0), number(args, 1),
                        number(args, 2), number(args, 3), number(args, 4), color(args, 5));
                case "line" -> SkijaUi.line(currentCanvas, number(args, 0), number(args, 1),
                        number(args, 2), number(args, 3), number(args, 4), color(args, 5));
                case "outline" -> SkijaUi.outline(currentCanvas, number(args, 0), number(args, 1), size(args, 2),
                        size(args, 3), size(args, 4), size(args, 5), color(args, 6));
                case "gradient" -> SkijaUi.gradient(currentCanvas, number(args, 0), number(args, 1),
                        size(args, 2), size(args, 3), color(args, 4), color(args, 5),
                        args.length > 6 && Context.toBoolean(args[6]), args.length > 7 ? size(args, 7) : 0);
                case "gradientDiagonal" -> SkijaUi.gradientDiagonal(currentCanvas, number(args, 0), number(args, 1),
                        size(args, 2), size(args, 3), color(args, 4), color(args, 5), args.length > 6 ? size(args, 6) : 0);
                case "shadow" -> SkijaUi.dropShadowRounded(currentCanvas, number(args, 0), number(args, 1),
                        size(args, 2), size(args, 3), size(args, 4), number(args, 5), effect(args, 6), color(args, 7));
                case "save" -> currentCanvas.save();
                case "restore" -> currentCanvas.restore();
                case "resetTransform" -> currentCanvas.setMatrix(baseMatrix);
                case "translate" -> currentCanvas.translate(number(args, 0), number(args, 1));
                case "scale" -> currentCanvas.scale(number(args, 0), number(args, 1));
                case "rotate" -> currentCanvas.rotate(number(args, 0));
                case "clip" -> currentCanvas.clipRect(Rect.makeXYWH(number(args, 0), number(args, 1), size(args, 2), size(args, 3)), true);
                case "clipRounded" -> currentCanvas.clipRRect(RRect.makeXYWH(number(args, 0), number(args, 1), size(args, 2), size(args, 3), size(args, 4)), true);
                case "blur" -> {
                    float x = number(args, 0), y = number(args, 1), w = size(args, 2), h = size(args, 3);
                    SkijaRenderer.drawBlurredBackdrop(currentCanvas, RRect.makeXYWH(x, y, w, h, size(args, 4)), x, y, w, h, effect(args, 5));
                }
                case "glass" -> {
                    float x = number(args, 0), y = number(args, 1), w = size(args, 2), h = size(args, 3), r = size(args, 4);
                    int tint = args.length > 5 ? color(args, 5) : LiquidGlassRenderer.panelTint();
                    boolean shadow = args.length < 7 || Context.toBoolean(args[6]);
                    boolean highlight = args.length < 8 || Context.toBoolean(args[7]);
                    if (!LiquidGlassRenderer.getInstance().renderPanel(Minecraft.getInstance(), x, y, w, h, r, tint, shadow, highlight, 0, 0)) {
                        int save = currentCanvas.save();
                        try {
                            currentCanvas.setMatrix(baseMatrix);
                            SkijaUi.rounded(currentCanvas, x, y, w, h, r, tint);
                        } finally {
                            currentCanvas.restoreToCount(save);
                        }
                    }
                }
                case "circle" -> {
                    try (Paint paint = new Paint().setAntiAlias(true).setColor(color(args, 3))) {
                        currentCanvas.drawCircle(number(args, 0), number(args, 1), size(args, 2), paint);
                    }
                }
                case "icon" -> {
                    float fontSize = size(args, 3);
                    if (fontSize < 1 || fontSize > 128) throw new IllegalArgumentException("Invalid icon size");
                    String set = args.length > 5 ? PluginRuntime.string(args, 5) : "material";
                    if (!set.equals("material") && !set.equals("pvp")) throw new IllegalArgumentException("Unknown icon set");
                    SkijaUi.icon(currentCanvas, PluginRuntime.string(args, 0), number(args, 1), number(args, 2),
                            fontSize, color(args, 4), fontSize,
                            set.equals("pvp") ? SkijaUi.IconSet.PVP_CONTROLS : SkijaUi.IconSet.MATERIAL_SYMBOLS);
                }
                case "texture" -> {
                    Identifier resource = Identifier.parse(PluginRuntime.string(args, 0));
                    try (SkijaRenderer.BorrowedImage borrowed = SkijaRenderer.borrowTexture(resource)) {
                        if (borrowed != null) currentCanvas.drawImageRect(borrowed.image(),
                                Rect.makeXYWH(number(args, 1), number(args, 2), size(args, 3), size(args, 4)));
                    }
                }
                case "button" -> {
                    float x = number(args, 1), y = number(args, 2), w = size(args, 3), h = size(args, 4);
                    boolean enabled = Context.toBoolean(args[5]), hover = Context.toBoolean(args[6]);
                    SkijaUi.rounded(currentCanvas, x, y, w, h, 7, !enabled ? 0xFF303540 : hover ? 0xFF4C6FFF : 0xFF3451D6);
                    String text = PluginRuntime.string(args, 0);
                    float fs = Math.max(1, Math.min(13, h * 0.5f));
                    SkijaUi.textWithFallback(currentCanvas, text,
                            x + (w - SkijaUi.textWidthWithFallback(text, fs)) / 2, y, h,
                            enabled ? 0xFFFFFFFF : 0xFF9298A4, fs);
                }
                default -> throw new IllegalArgumentException("Unknown draw operation");
            }
        }
    }

    private static float number(Object[] args, int index) {
        if (index >= args.length) throw new IllegalArgumentException("Missing numeric argument");
        double value = Context.toNumber(args[index]);
        if (!Double.isFinite(value) || Math.abs(value) > 100000) throw new IllegalArgumentException("Invalid number");
        return (float) value;
    }

    private static float size(Object[] args, int index) {
        float value = number(args, index);
        if (value < 0 || value > 4096) throw new IllegalArgumentException("Invalid size");
        return value;
    }

    private static float effect(Object[] args, int index) {
        float value = size(args, index);
        if (value > 64) throw new IllegalArgumentException("Effect radius limit is 64");
        return value;
    }

    private record ScreenRequest(String plugin, String operation, String id) {}

    private static int color(Object[] args, int index) {
        String value = PluginRuntime.string(args, index);
        if (!value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB");
        }
        long parsed = Long.parseLong(value.substring(1), 16);
        return (int) (value.length() == 7 ? parsed | 0xFF000000L : parsed);
    }
}
