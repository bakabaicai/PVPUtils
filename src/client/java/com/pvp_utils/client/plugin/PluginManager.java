package com.pvp_utils.client.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.gui.ConfirmationDialogScreen;
import com.pvp_utils.client.gui.SettingsScreen;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.render.skia.SkijaUi;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import com.pvp_utils.client.render.skia.GlassStyle;
import com.pvp_utils.client.render.skia.SoftShadow;
import com.pvp_utils.client.render.MainUI.PVPUtilsMainUI;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PluginManager {
    public static final PluginManager INSTANCE = new PluginManager();
    private final Map<String, Entry> plugins = new LinkedHashMap<>();
    private JsonObject enabledState = new JsonObject();
    private int revision;
    private boolean initialized;
    private Object currentLevel;
    private Canvas currentCanvas;
    private final List<ScreenRequest> screenRequests = new ArrayList<>();
    private final PluginApprovalGate approvals = new PluginApprovalGate();
    private final ArrayDeque<ApprovalDecision> approvalDecisions = new ArrayDeque<>();
    private PluginApprovalGate.Request activeApproval;
    private ConfirmationDialogScreen approvalDialog;
    private final ArrayBlockingQueue<AuditResult> auditResults = new ArrayBlockingQueue<>(64);
    private final ThreadPoolExecutor auditExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(64), task -> {
                Thread thread = new Thread(task, "pvp-utils-plugin-audit");
                thread.setDaemon(true);
                return thread;
            });
    private boolean checking;

    private PluginManager() {
    }

    public record PluginInfo(String id, String name, String version, String description, String author,
                             boolean enabled, String error, boolean auditing, boolean pendingApproval, List<String> findings) {
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
                    manifest == null ? "" : manifest.author(), entry.runtime != null, entry.error,
                    entry.auditing, approvals.pending(entry.id), findingDetails(entry.report)));
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
        entry.screensSuppressed = false;
        try {
            queueScreen(entry, "open", screen);
        } catch (PluginRuntime.ScriptLimitException error) {
            fail(entry, error);
            throw new IllegalStateException(message(error), error);
        }
    }

    public void dismissScreen(String plugin) {
        Entry entry = plugins.get(plugin);
        if (entry != null) entry.screensSuppressed = true;
        screenRequests.removeIf(request -> request.plugin.equals(plugin));
    }

    public void renderScreen(String plugin, String id, Canvas canvas, int width, int height) {
        if (dialogOpen(Minecraft.getInstance())) return;
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
        if (dialogOpen(Minecraft.getInstance())) return false;
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
        if (dialogOpen(Minecraft.getInstance())) return;
        Entry entry = plugins.get(plugin);
        if (entry == null || entry.runtime == null) return;
        try {
            entry.runtime.screenEvent(id, event);
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
        }
    }

    public boolean screenInput(String plugin, String id, String event, Object... args) {
        if (dialogOpen(Minecraft.getInstance())) return false;
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
        clearApprovals();
        shutdownRuntimes();
        auditResults.clear();
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
        if (enabled && entry.runtime == null && !entry.auditing && !approvals.pending(id)) enable(entry);
        if (!enabled) disable(entry);
        enabledState.addProperty(id, entry.runtime != null);
        saveState();
        revision++;
    }

    public void reload(String id) {
        Entry entry = plugins.get(id);
        if (entry == null || entry.manifest == null) return;
        boolean wasEnabled = entry.runtime != null || entry.auditing || approvals.pending(id);
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
        enabledState.addProperty(id, entry.runtime != null);
        saveState();
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
                    PluginSource source = PluginSource.read(PluginDirectory.path(), directory);
                    PluginSecurityAudit.Report report = PluginSecurityAudit.audit(source.code(), source.manifest().entry().getFileName().toString());
                    String status = report.failed() ? " BLOCKED" : report.requiresApproval() ? " REVIEW" : " OK";
                    result.add(source.manifest().id() + status + (report.findings().isEmpty() ? ""
                            : ": " + String.join("; ", findingDetails(report).stream().limit(4).toList())));
                } catch (Exception e) {
                    result.add(directory.getFileName() + " ERROR: " + message(e));
                }
            }
        } catch (Exception e) {
            result.add("SCAN ERROR: " + message(e));
        }
        return List.copyOf(result);
    }

    public boolean checkAsync(Consumer<List<String>> callback) {
        if (checking) return false;
        checking = true;
        try {
            auditExecutor.execute(() -> {
                List<String> result;
                try {
                    result = check();
                } catch (RuntimeException error) {
                    result = List.of("SCAN ERROR: " + message(error));
                }
                List<String> completed = result;
                Minecraft.getInstance().schedule(() -> {
                    checking = false;
                    callback.accept(completed);
                });
            });
            return true;
        } catch (RuntimeException error) {
            checking = false;
            return false;
        }
    }

    private static List<String> findingDetails(PluginSecurityAudit.Report report) {
        if (report == null) return List.of();
        return report.findings().stream().map(finding -> "[" + finding.ruleId() + ":" + finding.line() + "] "
                + UiText.t(finding.descriptionZh(), finding.descriptionEn())).toList();
    }

    private static String displayText(String text) {
        String clean = text.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").strip();
        return clean.substring(0, Math.min(160, clean.length()));
    }

    private boolean dialogOpen(Minecraft client) {
        return client.screen instanceof ConfirmationDialogScreen || activeApproval != null;
    }

    private void clearApprovals() {
        approvals.clear();
        approvalDecisions.clear();
    }

    private void showNextApproval(Minecraft client) {
        if (activeApproval != null && (!approvals.current(activeApproval) || client.screen != approvalDialog)) {
            approvals.cancel(activeApproval);
            if (client.screen == approvalDialog) approvalDialog.onClose();
            activeApproval = null;
            approvalDialog = null;
        }
        if (activeApproval != null || client.getOverlay() != null || !canShowApproval(client.screen)) return;
        PluginApprovalGate.Request request = approvals.next();
        if (request == null) return;
        PluginManifest manifest = request.source().manifest();
        List<String> details = new ArrayList<>();
        details.add(UiText.t("插件：", "Plugin: ") + manifest.id() + " · " + displayText(manifest.name())
                + " (" + displayText(manifest.version()) + ")");
        details.addAll(findingDetails(request.report()));
        Screen parent = client.screen;
        if (parent instanceof PluginScreen screen) parent = screen.parent();
        activeApproval = request;
        approvalDialog = new ConfirmationDialogScreen(parent,
                UiText.t("检测到安全风险，请手动审批。", "Security risk detected. Manual approval required."),
                UiText.t("你当前正在启用的插件疑似存在安全风险，启用后有可能会使游戏角色无法操作、游戏闪退、电脑文件内容被破坏等，若你执意要启动该插件，请点击下方按钮进行审批",
                        "The plugin you are enabling may pose security risks, including loss of player control, game crashes, or damage to files on your computer. If you still wish to start it, use the button below to approve it."),
                UiText.t("注：该识别并非100%准确，若其为误报，请忽略本提示",
                        "Note: Detection is not 100% accurate. If this is a false positive, you may disregard this warning."),
                UiText.t("禁用插件", "Disable plugin"), UiText.t("启用插件", "Enable plugin"), details,
                approved -> approvalDecisions.addLast(new ApprovalDecision(request, approved)));
        screenRequests.clear();
        client.setScreen(approvalDialog);
    }

    private static boolean canShowApproval(Screen screen) {
        return screen == null || screen instanceof TitleScreen || screen instanceof PauseScreen
                || screen instanceof PVPUtilsMainUI || screen instanceof NewSettingsScreen
                || screen instanceof SettingsScreen || screen instanceof PluginScreen;
    }

    private void processApprovalDecisions() {
        while (!approvalDecisions.isEmpty()) {
            ApprovalDecision decision = approvalDecisions.removeFirst();
            PluginApprovalGate.Request request = decision.request();
            if (activeApproval == request) {
                activeApproval = null;
                approvalDialog = null;
            }
            if (!approvals.current(request)) continue;
            Entry entry = plugins.get(request.id());
            if (entry == null || entry.manifest == null) {
                approvals.cancel(request.id());
                continue;
            }
            if (!decision.approved()) {
                disable(entry);
                enabledState.addProperty(entry.id, false);
                saveState();
                revision++;
                continue;
            }
            try {
                PluginSource latest = readSource(entry);
                if (approvals.approve(request, latest)) {
                    entry.manifest = request.source().manifest();
                    start(entry, request.source());
                } else {
                    enable(entry);
                }
            } catch (Exception | PluginRuntime.ScriptLimitException e) {
                fail(entry, e);
            }
        }
    }

    private void queueScreen(Entry entry, String operation, String id) {
        long now = System.nanoTime();
        if (now - entry.screenWindow >= 1_000_000_000L) {
            entry.screenWindow = now;
            entry.screenActions = 0;
        }
        if (++entry.screenActions > 8 || screenRequests.size() >= 32) {
            throw new PluginRuntime.ScriptLimitException("Plugin screen switching limit exceeded");
        }
        screenRequests.add(new ScreenRequest(entry.id, operation, id));
    }

    public void prepareTick() {
        Minecraft client = Minecraft.getInstance();
        processApprovalDecisions();
        if (!dialogOpen(client) && client.getOverlay() == null) processAuditResults();
        showNextApproval(client);
        if (dialogOpen(client)) {
            screenRequests.clear();
            return;
        }
        List<ScreenRequest> requests = List.copyOf(screenRequests);
        screenRequests.clear();
        for (ScreenRequest request : requests) {
            Entry entry = plugins.get(request.plugin);
            if (entry == null || entry.runtime == null) continue;
            try {
                if (request.operation.equals("close")) {
                    if (client.screen instanceof PluginScreen screen && screen.plugin().equals(request.plugin)) screen.onClose();
                } else if (!entry.screensSuppressed && entry.runtime.hasScreen(request.id)) {
                    if (client.screen instanceof PluginScreen screen && screen.plugin().equals(request.plugin)
                            && screen.screenId().equals(request.id)) continue;
                    if (client.player != null && client.player.containerMenu != client.player.inventoryMenu) client.player.closeContainer();
                    Screen parent = client.screen;
                    if (parent instanceof PluginScreen previous) parent = previous.parent();
                    String title = entry.runtime.screenTitle(request.id);
                    client.setScreen(new PluginScreen(request.plugin, request.id, title, parent));
                }
            } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
                fail(entry, e);
            }
        }
        for (Entry entry : plugins.values()) {
            if (entry.host != null) entry.host.player.synchronize();
        }
    }

    public void tick(Minecraft client) {
        if (dialogOpen(client)) return;
        Object level = client.level;
        if (currentLevel != level) {
            if (currentLevel != null) dispatch("leave");
            currentLevel = level;
            if (level != null) dispatch("join");
        }
        dispatch("tick");
    }

    public void render(Canvas canvas, int width, int height) {
        if (dialogOpen(Minecraft.getInstance())) return;
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
        entry.error = "";
        entry.report = null;
        entry.auditing = true;
        long ticket = ++entry.auditTicket;
        enabledState.addProperty(entry.id, false);
        saveState();
        revision++;
        Path root = PluginDirectory.path();
        Path directory = entry.manifest.directory();
        String id = entry.id;
        try {
            auditExecutor.purge();
            entry.auditTask = auditExecutor.submit(() -> {
                AuditResult result;
                try {
                    PluginSource source = PluginSource.read(root, directory);
                    if (!source.manifest().id().equals(id)) throw new IOException("Plugin id changed; refresh the plugin list");
                    PluginSecurityAudit.Report report = PluginSecurityAudit.audit(source.code(), source.manifest().entry().getFileName().toString());
                    result = new AuditResult(entry, ticket, source, report, null);
                } catch (Exception error) {
                    result = new AuditResult(entry, ticket, null, null, error);
                }
                if (!Thread.currentThread().isInterrupted()) {
                    try {
                        auditResults.put(result);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        } catch (RuntimeException e) {
            fail(entry, e);
        }
    }

    private void processAuditResults() {
        for (int count = 0; count < 2; count++) {
            AuditResult result = auditResults.poll();
            if (result == null) return;
            Entry entry = result.entry();
            if (plugins.get(entry.id) != entry || !entry.auditing || entry.auditTicket != result.ticket()) continue;
            entry.auditing = false;
            entry.auditTask = null;
            try {
                if (result.error() != null) throw result.error();
                PluginSource source = result.source();
                entry.manifest = source.manifest();
                entry.report = result.report();
                revision++;
                if (entry.report.failed()) {
                    throw new IOException(UiText.t("安全审计未完成，已阻止启用：", "Security audit incomplete; activation blocked: ")
                            + String.join("; ", findingDetails(entry.report)));
                }
                if (!source.fingerprint().equals(readSource(entry).fingerprint())) {
                    enable(entry);
                } else if (entry.report.requiresApproval()) {
                    approvals.submit(source, entry.report);
                } else {
                    start(entry, source);
                }
            } catch (Exception | PluginRuntime.ScriptLimitException error) {
                fail(entry, error);
            }
        }
    }

    private PluginSource readSource(Entry entry) throws IOException {
        PluginSource source = PluginSource.read(PluginDirectory.path(), entry.manifest.directory());
        if (!source.manifest().id().equals(entry.id)) throw new IOException("Plugin id changed; refresh the plugin list");
        return source;
    }

    private void start(Entry entry, PluginSource source) throws Exception {
        try {
            Path root = PluginDirectory.path().toRealPath();
            Path data = root.resolve(".data");
            Files.createDirectories(data);
            if (!data.toRealPath().startsWith(root)) throw new IOException("Plugin storage outside root");
            Path file = data.resolve(entry.id + ".json");
            if (Files.isSymbolicLink(file)) throw new IOException("Plugin storage is a symbolic link");
            PluginStorage storage = new PluginStorage(file);
            entry.screensSuppressed = false;
            entry.screenWindow = System.nanoTime();
            entry.screenActions = 0;
            entry.host = new GameHost(entry.id);
            entry.runtime = new PluginRuntime(entry.manifest, storage, entry.host);
            entry.runtime.start(source.code());
            entry.error = "";
            enabledState.addProperty(entry.id, true);
            saveState();
            revision++;
            PVPUtils.LOGGER.info("Loaded JS plugin {} {}", entry.id, entry.manifest.version());
        } catch (Exception | PluginRuntime.ScriptLimitException e) {
            fail(entry, e);
        }
    }

    private void fail(Entry entry, Throwable error) {
        entry.error = message(error);
        PVPUtils.LOGGER.error("JS plugin {} failed", entry.id, error);
        disable(entry);
        enabledState.addProperty(entry.id, false);
        saveState();
        revision++;
        NotificationOverlay.getInstance().show(UiText.t("插件运行失败：", "Plugin failed: ") + entry.id);
    }

    private void disable(Entry entry) {
        approvals.cancel(entry.id);
        entry.auditing = false;
        entry.auditTicket++;
        if (entry.auditTask != null) entry.auditTask.cancel(true);
        entry.auditTask = null;
        auditResults.removeIf(result -> result.entry() == entry);
        PluginRuntime runtime = entry.runtime;
        entry.runtime = null;
        screenRequests.removeIf(request -> request.plugin.equals(entry.id));
        Minecraft client = Minecraft.getInstance();
        client.schedule(() -> {
            if (!isEnabled(entry.id) && client.screen instanceof PluginScreen screen && screen.plugin().equals(entry.id)) screen.onClose();
        });
        try {
            if (runtime != null) {
                runtime.close();
            }
        } catch (RuntimeException | PluginRuntime.ScriptLimitException e) {
            PVPUtils.LOGGER.error("Failed to unload JS plugin {}", entry.id, e);
        } finally {
            screenRequests.removeIf(request -> request.plugin.equals(entry.id));
            if (entry.host != null) entry.host.player.close();
            entry.host = null;
        }
    }

    public void close() {
        clearApprovals();
        shutdownRuntimes();
        auditExecutor.shutdownNow();
        auditResults.clear();
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
        private PluginSecurityAudit.Report report;
        private boolean screensSuppressed;
        private long screenWindow;
        private int screenActions;
        private boolean auditing;
        private long auditTicket;
        private Future<?> auditTask;

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
            Entry entry = plugins.get(id);
            if (entry == null || entry.runtime == null || entry.host != this) throw new IllegalStateException("Plugin is no longer active");
            if (dialogOpen(Minecraft.getInstance())) throw new IllegalStateException("Player operations are suspended during confirmation");
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
            Entry entry = plugins.get(id);
            if (entry == null || entry.runtime == null || entry.screensSuppressed || dialogOpen(Minecraft.getInstance())) return;
            queueScreen(entry, operation, screen);
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
                    GlassStyle glass = glassStyle(args);
                    int save = currentCanvas.save();
                    try {
                        currentCanvas.setMatrix(baseMatrix);
                        if (shadow) SoftShadow.drawRounded(currentCanvas, x, y, w, h, r, 0.35f, 0xB0000000);
                        if (!LiquidGlassRenderer.getInstance().renderPanel(Minecraft.getInstance(), x, y, w, h, r, tint, glass)) {
                            SkijaUi.rounded(currentCanvas, x, y, w, h, r, tint);
                        }
                    } finally {
                        currentCanvas.restoreToCount(save);
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

    private static GlassStyle glassStyle(Object[] args) {
        GlassStyle style = new GlassStyle();
        if (args.length <= 7 || !(args[7] instanceof Scriptable options)) return style;
        style.blur = glassOption(options, "blur", style.blur, 0f, 30f);
        style.refraction = glassOption(options, "refraction", style.refraction, 1f, 5f);
        style.dispersion = glassOption(options, "dispersion", style.dispersion, 0f, 3f);
        style.rim = glassOption(options, "rim", style.rim, 0f, 1f);
        style.alpha = glassOption(options, "alpha", style.alpha, 0f, 1f);
        style.glow = glassOption(options, "glow", style.glow, 0f, 1f);
        style.thickness = glassOption(options, "thickness", style.thickness, 1f, 120f);
        return style;
    }

    private static float glassOption(Scriptable options, String key, float fallback, float min, float max) {
        Object value = ScriptableObject.getProperty(options, key);
        if (value == Scriptable.NOT_FOUND) return fallback;
        double number = Context.toNumber(value);
        if (!Double.isFinite(number) || number < min || number > max) throw new IllegalArgumentException("Invalid glass parameter: " + key);
        return (float) number;
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

    private record ApprovalDecision(PluginApprovalGate.Request request, boolean approved) {}

    private record AuditResult(Entry entry, long ticket, PluginSource source, PluginSecurityAudit.Report report,
                               Exception error) {}

    private static int color(Object[] args, int index) {
        String value = PluginRuntime.string(args, index);
        if (!value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB");
        }
        long parsed = Long.parseLong(value.substring(1), 16);
        return (int) (value.length() == 7 ? parsed | 0xFF000000L : parsed);
    }
}
