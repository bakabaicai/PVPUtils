package com.pvp_utils.client.plugin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PluginSmokeTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("pvputils-plugin-test-");
        TestHost host = new TestHost();
        PluginStorage storage = new PluginStorage(directory.resolve("data.json"));
        String script = """
                if (typeof Packages !== "undefined" || typeof java !== "undefined") throw Error("Java globals exposed");
                if (typeof pvp !== "undefined" || pvputils.apiVersion !== 1) throw Error("Plugin namespace mismatch");
                const show = pvputils.settings.bool("Show", true);
                const size = pvputils.settings.number("Size", 12, 8, 48);
                const label = pvputils.settings.text("Label", "Health");
                const color = pvputils.settings.color("Color", "#FFFFFFFF");
                size.value = 16;
                pvputils.events.on("load", () => {
                    pvputils.log("loaded");
                    pvputils.storage.set("object", { value: 42, list: [1, 2] });
                    pvputils.storage.set("null", null);
                });
                pvputils.events.on("tick", () => {
                    const object = pvputils.storage.get("object");
                    if (object.value !== 42 || object.list[1] !== 2) throw Error("Storage mismatch");
                    if (pvputils.storage.get("missing", "fallback") !== "fallback") throw Error("Default mismatch");
                    if (pvputils.storage.get("null", "fallback") !== null) throw Error("Null mismatch");
                    pvputils.notify.show(label.value);
                });
                pvputils.events.on("join", () => pvputils.log("join"));
                pvputils.events.on("leave", () => pvputils.log("leave"));
                pvputils.events.on("unload", () => pvputils.log("unloaded"));
                pvputils.hud.register({
                    id: "health",
                    render(ctx) {
                        if (!show.value) return;
                        const player = pvputils.player.snapshot();
                        ctx.roundedRect(10, 10, 140, 36, 8, "#AA181818");
                        ctx.text(`${label.value}: ${player.health}`, 20, 20, size.value, color.value);
                        if (ctx.width !== 800 || ctx.height !== 600) throw Error("Viewport mismatch");
                    }
                });
                """;
        PluginRuntime runtime = runtime(directory, script, storage, host);
        runtime.start();
        expectFailure(runtime::start, "duplicate start");
        check(runtime.settings().size() == 4, "settings registered");
        check(runtime.settings().get(1).value().equals(16.0), "JS setting accessor");
        runtime.emit("tick");
        runtime.emit("join");
        runtime.emit("leave");
        runtime.render(800, 600);
        check(host.draws.equals(List.of("roundedRect", "text")), "HUD callbacks");
        check(host.messages.contains("Health"), "notifications");
        runtime.close();
        check(host.messages.contains("unloaded"), "unload lifecycle");
        check(runtime.settings().isEmpty(), "resources cleared");
        check(new PluginStorage(directory.resolve("data.json")).get("object").contains("42"), "storage persisted");
        expectFailure(() -> runtime.emit("tick"), "closed runtime");

        PluginRuntime restored = runtime(directory, """
                const size = pvputils.settings.number("Size", 12, 8, 48);
                if (size.value !== 16) throw Error("Setting restore failed");
                """, storage, host);
        restored.start();
        restored.close();
        check(true, "settings restored");

        testFailure(directory, "while (true) {}", "top-level infinite loop", storage, host);
        testFailure(directory, "try { while (true) {} } catch (e) {}", "budget bypass", storage, host);
        testFailure(directory, "pvputils.events.on('unknown', () => {});", "unknown event", storage, host);
        testFailure(directory, "pvputils.events.on('tick', 42);", "invalid callback", storage, host);
        testFailure(directory, "const broken = ;", "syntax error", storage, host);
        testFailure(directory, "for (let i = 0; i < 33; i++) pvputils.log('spam');", "message limit", storage, host);
        testFailure(directory, "pvputils.hud.register({render(){}});", "missing HUD id", storage, host);
        testFailure(directory, "pvputils.storage.set('bad', undefined);", "unsupported storage value", storage, host);

        PluginRuntime loop = runtime(directory, "pvputils.events.on('tick', () => { while (true) {} });", storage, host);
        loop.start();
        expectFailure(() -> loop.emit("tick"), "callback infinite loop");
        loop.close();
        PluginRuntime error = runtime(directory, "pvputils.events.on('tick', () => { throw Error('expected'); });", storage, host);
        error.start();
        expectFailure(() -> error.emit("tick"), "callback exception");
        error.close();
        PluginRuntime retained = runtime(directory, """
                let saved;
                pvputils.events.on("render", ctx => saved = ctx);
                pvputils.events.on("tick", () => saved.rect(0, 0, 1, 1, "#FFFFFF"));
                """, storage, host);
        retained.start();
        retained.render(800, 600);
        expectFailure(() -> retained.emit("tick"), "retained render context");
        retained.close();
        PluginRuntime resized = runtime(directory, """
                pvputils.events.on("render", ctx => {
                    ctx.width = 1;
                    pvputils.log(`${ctx.width}x${ctx.height}`);
                });
                """, storage, host);
        resized.start();
        resized.render(800, 600);
        resized.render(1024, 768);
        check(host.messages.contains("800x600") && host.messages.contains("1024x768"), "cached viewport updated");
        java.util.concurrent.atomic.AtomicBoolean denied = new java.util.concurrent.atomic.AtomicBoolean();
        Thread thread = new Thread(() -> {
            try {
                resized.emit("tick");
            } catch (IllegalStateException expected) {
                denied.set(true);
            }
        });
        thread.start();
        thread.join();
        check(denied.get(), "cross-thread calls denied");
        resized.close();

        PluginRuntime isolatedA = runtime(directory, "Math.pluginMarker = 42;", storage, host);
        isolatedA.start();
        PluginRuntime isolatedB = runtime(directory, "if (Math.pluginMarker !== undefined) throw Error('scope leaked');",
                storage, host);
        isolatedB.start();
        check(true, "plugin scopes isolated");
        isolatedA.close();
        isolatedB.close();

        Files.writeString(directory.resolve("plugin.json"), """
                {"id":"example","name":"Example","apiVersion":1,"main":"main.js"}
                """, StandardCharsets.UTF_8);
        check(PluginManifest.read(directory).id().equals("example"), "manifest parsed");
        Path outside = Files.createTempFile(directory.getParent(), "outside-plugin-", ".js");
        Files.writeString(directory.resolve("plugin.json"), "{\"id\":\"example\",\"main\":\"../"
                + outside.getFileName() + "\"}");
        expectFailure(() -> PluginManifest.read(directory), "entry traversal");
        expectFailure(() -> storage.set("../bad", "\"" + "x".repeat(1048577) + "\""), "storage quota");
        Path example = Path.of("examples", "plugins", "basic-hud");
        PluginManifest sample = PluginManifest.read(example);
        PluginRuntime sampleRuntime = new PluginRuntime(sample,
                new PluginStorage(directory.resolve("example-data.json")), host);
        sampleRuntime.start();
        sampleRuntime.emit("join");
        sampleRuntime.emit("tick");
        sampleRuntime.render(800, 600);
        sampleRuntime.close();
        check(true, "repository example runs");
        System.out.println("Plugin smoke tests passed: " + checks);
    }

    private static PluginRuntime runtime(Path directory, String source, PluginStorage storage, TestHost host) throws Exception {
        Path entry = directory.resolve("main.js");
        Files.writeString(entry, source, StandardCharsets.UTF_8);
        return new PluginRuntime(new PluginManifest("example", "Example", "1.0.0", "", "",
                directory, entry, false), storage, host);
    }

    private static void testFailure(Path directory, String source, String description,
                                    PluginStorage storage, TestHost host) throws Exception {
        PluginRuntime runtime = runtime(directory, source, storage, host);
        expectFailure(runtime::start, description);
        try {
            runtime.close();
        } catch (RuntimeException | PluginRuntime.ScriptLimitException expected) {
            check(true, "failed runtime cleanup");
        }
    }

    private static void expectFailure(ThrowingAction action, String description) throws Exception {
        boolean failed = false;
        try {
            action.run();
        } catch (Exception | PluginRuntime.ScriptLimitException expected) {
            failed = true;
        }
        check(failed, description);
    }

    private static void check(boolean success, String description) {
        if (!success) throw new AssertionError(description);
        checks++;
    }

    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static final class TestHost implements PluginRuntime.Host {
        private final List<String> messages = new ArrayList<>();
        private final List<String> draws = new ArrayList<>();

        @Override
        public void log(String message) {
            messages.add(message);
        }

        @Override
        public void notify(String message) {
            messages.add(message);
        }

        @Override
        public String playerSnapshot() {
            return "{\"health\":20,\"maxHealth\":20,\"name\":\"Test\"}";
        }

        @Override
        public void draw(String operation, Object[] arguments) {
            draws.add(operation);
        }

        @Override
        public double textWidth(Object[] arguments) {
            return org.mozilla.javascript.Context.toString(arguments[0]).length()
                    * org.mozilla.javascript.Context.toNumber(arguments[1]) * 0.5;
        }
    }
}
