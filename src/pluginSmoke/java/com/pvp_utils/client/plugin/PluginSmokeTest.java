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
        testPlayers(directory, storage);
        testDocumentation(directory);
        testAdvancedUi(directory);
        System.out.println("Plugin smoke tests passed: " + checks);
    }

    private static void testAdvancedUi(Path directory) throws Exception {
        TestHost host = new TestHost();
        PluginStorage storage = new PluginStorage(directory.resolve("advanced-ui.json"));
        String source = """
                let clicks = 0;
                pvputils.hud.register({
                    id: "advanced",
                    label: "Advanced",
                    layout: { x: 20, y: 30, width: 160, height: 40 },
                    render(ctx) {
                        if (ctx.width !== 160 || ctx.height !== 40 || ctx.screenWidth !== 800) throw Error("HUD dimensions");
                        ctx.shadow(0, 0, 160, 40, 8, 2, 8, "#80000000");
                        ctx.gradient(0, 0, 160, 40, "#102030", "#405060", false, 8);
                        ctx.gradientDiagonal(0, 0, 10, 10, "#000000", "#FFFFFF", 2);
                        ctx.outline(0, 0, 160, 40, 8, 1, "#FFFFFF");
                        ctx.textShadow("Hello", 10, 10, 12, "#FFFFFF", "harmony");
                        ctx.icon("A", 0, 0, 12, "#FFFFFF", "material");
                        ctx.circle(12, 12, 4, "#FFFFFF");
                        ctx.texture("minecraft:textures/gui/icons.png", 0, 0, 10, 10);
                        ctx.blur(0, 0, 160, 40, 8, 6);
                        ctx.glass(0, 0, 160, 40, 8, "#40102030", true, true);
                        ctx.save();
                        ctx.translate(1, 2);
                        ctx.scale(1, 1);
                        ctx.rotate(0);
                        ctx.clip(0, 0, 100, 20);
                        ctx.clipRounded(0, 0, 100, 20, 2);
                        ctx.restore();
                    }
                });
                pvputils.ui.screen({
                    id: "controls",
                    title: "Controls",
                    onOpen() { pvputils.log("opened"); },
                    onClose() { pvputils.log("screen-closed"); },
                    onScroll(x,y,h,v) { return x === 40 && v === 1; },
                    render(ctx) {
                        ctx.button("count", "Count", 20, 20, 100, 30, () => {
                            clicks++;
                            pvputils.log("click=" + clicks);
                        });
                        ctx.button("disabled", "Disabled", 20, 60, 100, 30, () => {
                            throw Error("Disabled button clicked");
                        }, false);
                    }
                });
                pvputils.ui.open("controls");
                """;
        PluginRuntime runtime = runtime(directory, source, storage, host);
        runtime.start();
        check(runtime.hasScreen("controls") && runtime.screenTitle("controls").equals("Controls"), "screen definition");
        check(host.screenRequests.contains("open:controls"), "screen open bridge");
        check(runtime.hudLayouts().size() == 1 && runtime.hudLayouts().getFirst().draggable(), "HUD layout default draggable");
        runtime.render(800, 600);
        check(host.draws.containsAll(List.of("gradient", "gradientDiagonal", "outline", "shadow", "textShadow",
                "icon", "circle", "texture", "blur", "glass", "clip", "clipRounded", "rotate")), "advanced draw bridge");
        check(host.scopes == 0, "draw scopes restored");
        runtime.moveHud("advanced", 100, 200, 800, 600);
        runtime.hudLayouts().getFirst().setScale(1.5f);
        runtime.saveHudLayouts();
        check(new PluginStorage(directory.resolve("advanced-ui.json")).number("hud:advanced:x", 0) == 100, "HUD position persisted");
        runtime.screenEvent("controls", "onOpen");
        runtime.renderScreen("controls", 800, 600);
        check(runtime.clickScreen("controls", 40, 30, 0), "button click consumed");
        check(host.messages.contains("click=1"), "button callback");
        check(runtime.screenInput("controls", "onScroll", new Object[]{40, 30, 0, 1}), "screen input callback");
        check(!runtime.screenInput("controls", "onScroll", new Object[]{40, 30, 0, 2}), "screen input propagation");
        check(!runtime.clickScreen("controls", 40, 30, 1), "secondary click ignored");
        check(!runtime.clickScreen("controls", 40, 70, 0), "disabled button ignored");
        check(!runtime.clickScreen("controls", 500, 500, 0), "outside click ignored");
        runtime.screenEvent("controls", "onClose");
        check(!runtime.clickScreen("controls", 40, 30, 0), "closed screen clears hit boxes");
        runtime.close();
        PluginRuntime restored = runtime(directory, source, storage, host);
        restored.start();
        check(restored.hudLayouts().getFirst().x() == 100 && restored.hudLayouts().getFirst().scale() == 1.5f, "HUD restore scale and position");
        restored.moveHud("advanced", 5000, -10, 800, 600);
        check(restored.hudLayouts().getFirst().x() == 560 && restored.hudLayouts().getFirst().y() == 0, "scaled HUD bounds");
        restored.close();
        testFailure(directory, "pvputils.ui.open('missing');", "unknown screen denied", storage, host);
        testFailure(directory, "pvputils.hud.register({id:'bad',layout:{width:5000},render(ctx){}});",
                "HUD size limit", storage, host);
        PluginRuntime underflow = runtime(directory, "pvputils.events.on('render', ctx => ctx.restore());", storage, host);
        underflow.start();
        expectFailure(() -> underflow.render(800, 600), "drawing restore underflow");
        underflow.close();
        PluginRuntime effects = runtime(directory, """
                pvputils.events.on("render", ctx => {
                    for (let i = 0; i < 9; i++) ctx.blur(0, 0, 100, 30, 4, 5);
                });
                """, storage, host);
        effects.start();
        expectFailure(() -> effects.render(800, 600), "effect frame budget");
        effects.close();
        PluginRuntime screenMutation = runtime(directory, """
                pvputils.ui.screen({id:"test", render(ctx) { pvputils.ui.close(); }});
                """, storage, host);
        screenMutation.start();
        expectFailure(() -> screenMutation.renderScreen("test", 800, 600), "screen change during render denied");
        screenMutation.close();
    }

    private static void testDocumentation(Path directory) throws Exception {
        for (String name : List.of("PLUGINS.md", "PLUGIN_TUTORIAL.md")) {
            String document = Files.readString(Path.of("docs", name), StandardCharsets.UTF_8);
            var scripts = java.util.regex.Pattern.compile("```js\\R(.*?)\\R```",
                    java.util.regex.Pattern.DOTALL).matcher(document);
            int index = 0;
            List<String> sources = new ArrayList<>();
            while (scripts.find()) {
                String source = scripts.group(1);
                sources.add(source);
                TestHost host = new TestHost();
                host.token = "documentation";
                PluginRuntime sample = runtime(directory, source,
                        new PluginStorage(directory.resolve(name + "-" + index++ + ".json")), host);
                sample.start();
                sample.emit("tick");
                sample.render(800, 600);
                sample.close();
                check(true, "documentation script runs: " + name + " #" + index);
            }
            var json = java.util.regex.Pattern.compile("```json\\R(.*?)\\R```",
                    java.util.regex.Pattern.DOTALL).matcher(document);
            check(json.find(), "documentation manifest exists: " + name);
            Files.writeString(directory.resolve("plugin.json"), json.group(1), StandardCharsets.UTF_8);
            check(PluginManifest.read(directory).id().startsWith("my-"), "documentation manifest loads: " + name);
            if (name.equals("PLUGIN_TUTORIAL.md")) {
                TestHost host = new TestHost();
                host.token = "tutorial";
                PluginRuntime combined = runtime(directory, sources.get(1) + "\n" + sources.get(2),
                        new PluginStorage(directory.resolve("combined-tutorial.json")), host);
                combined.start();
                PluginSetting trigger = combined.settings().stream()
                        .filter(setting -> setting.name().equals("切换到第一个快捷栏")).findFirst().orElseThrow();
                trigger.set(true);
                combined.emit("tick");
                combined.emit("tick");
                check(host.operations.stream().filter(operation -> operation.equals("select")).count() == 1,
                        "tutorial one-shot player action");
                check(trigger.value().equals(false), "tutorial trigger resets");
                combined.render(800, 600);
                combined.close();
            }
        }
    }

    private static void testPlayers(Path directory, PluginStorage storage) throws Exception {
        PluginPlayerSession session = new PluginPlayerSession();
        Object player = new Object();
        Object world = new Object();
        Object menu = new Object();
        check(session.playerToken() == null, "no player token outside world");
        session.update(player, world, menu);
        String token = session.playerToken();
        String menuToken = session.menuToken();
        session.update(player, world, menu);
        check(session.validPlayer(token) && session.validMenu(menuToken), "stable session tokens");
        session.update(player, world, new Object());
        check(session.validPlayer(token) && !session.validMenu(menuToken), "old menu invalidated");
        String switchedMenu = session.menuToken();
        session.update(new Object(), world, menu);
        check(!session.validPlayer(token) && !session.validMenu(switchedMenu), "respawn invalidates handles");
        token = session.playerToken();
        session.update(player, new Object(), menu);
        check(!session.validPlayer(token), "world change invalidates player");
        token = session.playerToken();
        session.clear();
        check(!session.validPlayer(token) && session.menuToken() == null, "close clears session");
        session.update(player, world, menu);
        check(!session.validPlayer(token), "old session never revived");
        check(PluginPlayerArguments.integer(new Object[]{8.0}, 0, 0, 8) == 8, "hotbar upper bound");
        expectFailure(() -> PluginPlayerArguments.integer(new Object[]{9}, 0, 0, 8), "invalid hotbar slot");
        expectFailure(() -> PluginPlayerArguments.integer(new Object[]{0.5}, 0, 0, 8), "fractional slot");
        expectFailure(() -> PluginPlayerArguments.number(new Object[]{Double.NaN}, 0, 10), "NaN velocity");
        expectFailure(() -> PluginPlayerArguments.number(new Object[]{Double.POSITIVE_INFINITY}, 0, 10), "infinite velocity");
        expectFailure(() -> PluginPlayerArguments.number(new Object[]{11}, 0, 10), "velocity range");
        expectFailure(() -> PluginPlayerArguments.number(new Object[]{"1"}, 0, 10), "numeric string rejected");
        expectFailure(() -> PluginPlayerArguments.number(new Object[0], 0, 10), "missing player argument");
        expectFailure(() -> PluginPlayerArguments.validate("setRotation", new Object[]{90, 91}), "pitch range");
        expectFailure(() -> PluginPlayerArguments.validate("setRotation", new Object[]{90}), "rotation argument count");
        expectFailure(() -> PluginPlayerArguments.validate("setSprinting", new Object[]{1}), "sprinting boolean required");
        expectFailure(() -> PluginPlayerArguments.validate("swing", new Object[]{"invalid"}), "invalid hand");
        expectFailure(() -> PluginPlayerArguments.validate("click", new Object[]{"menu", 0, 0, "clone"}), "unsupported click type");
        expectFailure(() -> PluginPlayerArguments.validate("click", new Object[]{"menu", 0, 9, "swap"}), "invalid swap button");
        expectFailure(() -> PluginPlayerArguments.validate("click", new Object[]{"menu", -999, 0, "pickup"}), "outside click rejected");
        PluginPlayerArguments.validate("click", new Object[]{"menu", 0, 8, "swap"});
        PluginPlayerArguments.validate("swing", new Object[0]);
        check(true, "valid click and default hand accepted");

        TestHost host = new TestHost();
        host.token = "first";
        PluginRuntime live = runtime(directory, """
                const player = pvputils.player.get();
                if (!player || typeof player.getClass !== "undefined") throw Error("Invalid player facade");
                const menu = player.inventory.getMenu();
                if (player.snapshot().health !== 20) throw Error("Initial snapshot");
                if (player.target().type !== "entity") throw Error("Target bridge");
                if (player.inventory.getItems()[0].count !== 3) throw Error("Items bridge");
                if (player.inventory.getSlot(0).id !== "minecraft:stone") throw Error("Slot bridge");
                if (player.inventory.getSelectedSlot() !== 0) throw Error("Selected bridge");
                player.setRotation(90, 30);
                player.setVelocity(1, 0, 1);
                player.setSprinting(true);
                player.jump();
                player.swing("off");
                player.useItem("main");
                player.stopUsingItem();
                player.attackTarget();
                player.interactTarget("off");
                player.inventory.select(1);
                if (!menu.click(0, 0, "pickup").ok) throw Error("Menu click bridge");
                pvputils.events.on("tick", () => {
                    const snapshot = player.snapshot();
                    if (snapshot !== null && snapshot.health !== 10) throw Error("Snapshot is not live");
                    pvputils.log(snapshot === null ? "stale" : "live");
                    if (snapshot === null && player.jump().code !== "STALE_PLAYER") throw Error("Stale action");
                });
                """, storage, host);
        live.start();
        check(host.operations.containsAll(List.of("setRotation", "setVelocity", "setSprinting", "jump", "swing",
                "useItem", "stopUsingItem", "attackTarget", "interactTarget", "select", "click")), "player actions forwarded");
        check(host.clickedMenu.equals("menu-first"), "menu token is captured");
        host.health = 10;
        live.emit("tick");
        check(host.messages.contains("live"), "player facade reads live values");
        host.token = "second";
        live.emit("tick");
        check(host.messages.contains("stale"), "retained facade stays bound to original player");
        live.close();
        check(host.releases == 1, "player bridge released on unload");
        host.token = null;
        PluginRuntime absent = runtime(directory, """
                if (pvputils.player.get() !== null) throw Error("Missing player must return null");
                """, storage, host);
        absent.start();
        absent.close();
        check(true, "player get is null outside world");
        host.token = "third";
        PluginRuntime renderAction = runtime(directory, """
                pvputils.events.on("render", () => pvputils.player.get().jump());
                """, storage, host);
        renderAction.start();
        int operations = host.operations.size();
        expectFailure(() -> renderAction.render(800, 600), "render mutation denied");
        check(host.operations.size() == operations, "render action never reaches game");
        renderAction.close();
        testFailure(directory, """
                const player = pvputils.player.get();
                for (let i = 0; i < 33; i++) player.jump();
                """, "player action budget", storage, host);
        PluginRuntime throwingUnload = runtime(directory, """
                pvputils.events.on("unload", () => { throw Error("unload failure"); });
                """, storage, host);
        throwingUnload.start();
        int releases = host.releases;
        expectFailure(throwingUnload::close, "unload callback error");
        check(host.releases == releases + 1, "unload error still releases player");
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
        private final List<String> operations = new ArrayList<>();
        private String token;
        private int health = 20;
        private int releases;
        private String clickedMenu;
        private int scopes;
        private final List<String> screenRequests = new ArrayList<>();

        @Override
        public void pushDraw() { scopes++; }

        @Override
        public void popDraw() { scopes--; }

        @Override
        public void screen(String operation, String id) { screenRequests.add(operation + ":" + id); }

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
        public String playerToken() {
            return token;
        }

        @Override
        public String playerCall(String capturedToken, String operation, Object[] arguments) {
            if (!capturedToken.equals(token)) {
                return List.of("snapshot", "target", "getItems", "getSlot", "getSelectedSlot", "menuSnapshot")
                        .contains(operation) ? "null" : "{\"ok\":false,\"code\":\"STALE_PLAYER\"}";
            }
            return switch (operation) {
                case "snapshot" -> "{\"health\":" + health + ",\"yaw\":90,\"pitch\":30,\"inScreen\":false}";
                case "target" -> "{\"type\":\"entity\",\"id\":1}";
                case "getItems" -> "[{\"id\":\"minecraft:stone\",\"count\":3}]";
                case "getSlot" -> "{\"id\":\"minecraft:stone\",\"count\":3}";
                case "getSelectedSlot" -> "0";
                case "menuSnapshot" -> "{\"token\":\"menu-" + token + "\",\"containerId\":0,\"slots\":[]}";
                default -> {
                    operations.add(operation);
                    if (operation.equals("click")) clickedMenu = PluginRuntime.string(arguments, 0);
                    yield "{\"ok\":true,\"code\":\"OK\"}";
                }
            };
        }

        @Override
        public void releasePlayer() {
            releases++;
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
