package com.pvp_utils.client.plugin;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Undefined;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PluginRuntime implements AutoCloseable {
    public interface Host {
        void log(String message);
        void notify(String message);
        String playerSnapshot();
        void draw(String operation, Object[] arguments);
        double textWidth(Object[] arguments);
    }

    private static final Set<String> EVENTS = Set.of("load", "unload", "tick", "render", "join", "leave");
    private final BudgetFactory factory = new BudgetFactory();
    private final Map<String, List<Function>> listeners = new LinkedHashMap<>();
    private final List<HudRegistration> huds = new ArrayList<>();
    private final List<PluginSetting> settings = new ArrayList<>();
    private final Thread owner = Thread.currentThread();
    private final PluginManifest manifest;
    private final PluginStorage storage;
    private final Host host;
    private ScriptableObject scope;
    private boolean closed;
    private boolean rendering;
    private int drawCalls;
    private int messages;
    private Scriptable renderContext;
    private int renderWidth;
    private int renderHeight;
    private Function parseJson;
    private Function stringifyJson;

    public PluginRuntime(PluginManifest manifest, PluginStorage storage, Host host) {
        this.manifest = manifest;
        this.storage = storage;
        this.host = host;
    }

    public void start() throws Exception {
        if (scope != null) throw new IllegalStateException("Plugin already started");
        String source = Files.readString(manifest.entry(), StandardCharsets.UTF_8);
        factory.starting = true;
        try {
            run(cx -> {
                scope = cx.initSafeStandardObjects();
                Scriptable json = (Scriptable) ScriptableObject.getProperty(scope, "JSON");
                parseJson = (Function) ScriptableObject.getProperty(json, "parse");
                stringifyJson = (Function) ScriptableObject.getProperty(json, "stringify");
                Scriptable api = object(cx);
                ScriptableObject.putProperty(scope, "pvputils", api);
                property(api, "apiVersion", 1);
                property(api, "id", manifest.id());
                function(api, "log", (context, args) -> {
                    if (++messages > 32) throw new ScriptLimitException();
                    host.log(string(args, 0));
                    return Undefined.instance;
                });
                Scriptable notifications = object(cx);
                property(api, "notify", notifications);
                function(notifications, "show", (context, args) -> {
                    if (++messages > 32) throw new ScriptLimitException();
                    host.notify(string(args, 0));
                    return Undefined.instance;
                });
                Scriptable events = object(cx);
                property(api, "events", events);
                function(events, "on", (context, args) -> {
                    String event = string(args, 0);
                    if (!EVENTS.contains(event)) throw new IllegalArgumentException("Unknown event: " + event);
                    if (args.length < 2 || !(args[1] instanceof Function callback)) {
                        throw new IllegalArgumentException("Event callback must be a function");
                    }
                    List<Function> callbacks = listeners.computeIfAbsent(event, key -> new ArrayList<>());
                    if (callbacks.size() >= 32) throw new IllegalArgumentException("Too many event callbacks");
                    callbacks.add(callback);
                    return Undefined.instance;
                });
                Scriptable player = object(cx);
                property(api, "player", player);
                function(player, "snapshot", (context, args) -> parse(context, host.playerSnapshot()));
                Scriptable store = object(cx);
                property(api, "storage", store);
                function(store, "get", (context, args) -> {
                    String key = string(args, 0);
                    return !storage.contains(key) && args.length > 1 ? args[1] : parse(context, storage.get(key));
                });
                function(store, "set", (context, args) -> {
                    if (args.length < 2) throw new IllegalArgumentException("Missing storage value");
                    Object encoded = stringifyJson.call(context, scope, scope, new Object[]{args[1]});
                    if (encoded instanceof Undefined) throw new IllegalArgumentException("Value is not JSON serializable");
                    storage.set(string(args, 0), Context.toString(encoded));
                    return Undefined.instance;
                });
                Scriptable settingsApi = object(cx);
                property(api, "settings", settingsApi);
                for (String type : List.of("bool", "number", "text", "color")) {
                    function(settingsApi, type, (context, args) -> {
                        if (args.length < 2 || settings.size() >= 64) {
                            throw new IllegalArgumentException("Invalid setting arguments or too many settings");
                        }
                        String name = string(args, 0);
                        if (settings.stream().anyMatch(setting -> setting.name().equals(name))) {
                            throw new IllegalArgumentException("Duplicate setting: " + name);
                        }
                        double min = type.equals("number") && args.length > 2 ? Context.toNumber(args[2]) : 0;
                        double max = type.equals("number") && args.length > 3 ? Context.toNumber(args[3]) : 100;
                        if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) {
                            throw new IllegalArgumentException("Invalid setting range");
                        }
                        PluginSetting setting = new PluginSetting(name, type, args[1], min, max, storage);
                        settings.add(setting);
                        ScriptableObject handle = (ScriptableObject) object(context);
                        handle.setGetterOrSetter("value", 0, createFunction((current, unused) -> setting.value()), false);
                        handle.setGetterOrSetter("value", 0, createFunction((current, values) -> {
                            setting.set(values[0]);
                            return Undefined.instance;
                        }), true);
                        return handle;
                    });
                }
                Scriptable hud = object(cx);
                property(api, "hud", hud);
                function(hud, "register", (context, args) -> {
                    if (args.length == 0 || !(args[0] instanceof Scriptable definition)
                            || !(ScriptableObject.getProperty(definition, "render") instanceof Function callback)) {
                        throw new IllegalArgumentException("HUD requires a render function");
                    }
                    if (huds.size() >= 16) throw new IllegalArgumentException("Too many HUDs");
                    String id = Context.toString(ScriptableObject.getProperty(definition, "id"));
                    if (!id.matches("[a-z][a-z0-9_-]{0,63}") || huds.stream().anyMatch(existing -> existing.id().equals(id))) {
                        throw new IllegalArgumentException("Invalid or duplicate HUD id");
                    }
                    huds.add(new HudRegistration(id, definition, callback));
                    return Undefined.instance;
                });
                cx.evaluateString(scope, source, manifest.entry().getFileName().toString(), 1, null);
                emit(cx, "load", new Object[0]);
                return null;
            });
        } finally {
            factory.starting = false;
        }
    }

    public List<PluginSetting> settings() {
        return List.copyOf(settings);
    }

    public void emit(String event) {
        if (!closed && Thread.currentThread() == owner && listeners.getOrDefault(event, List.of()).isEmpty()) return;
        run(cx -> {
            emit(cx, event, new Object[0]);
            return null;
        });
    }

    public void render(int width, int height) {
        if (!closed && Thread.currentThread() == owner && huds.isEmpty()
                && listeners.getOrDefault("render", List.of()).isEmpty()) return;
        run(cx -> {
            rendering = true;
            drawCalls = 0;
            renderWidth = width;
            renderHeight = height;
            try {
                if (renderContext == null) renderContext = drawingContext(cx);
                Scriptable context = renderContext;
                emit(cx, "render", new Object[]{context});
                for (HudRegistration hud : List.copyOf(huds)) {
                    hud.callback().call(cx, scope, hud.definition(), new Object[]{context});
                }
            } finally {
                rendering = false;
            }
            return null;
        });
    }

    private Scriptable drawingContext(Context cx) {
        ScriptableObject context = (ScriptableObject) object(cx);
        context.setGetterOrSetter("width", 0, createFunction((current, args) -> renderWidth), false);
        context.setGetterOrSetter("height", 0, createFunction((current, args) -> renderHeight), false);
        context.setAttributes("width", ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        context.setAttributes("height", ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        for (String operation : List.of("text", "textWidth", "rect", "roundedRect", "line")) {
            function(context, operation, (current, args) -> {
                if (!rendering) throw new IllegalStateException("Drawing is only available during render");
                if (++drawCalls > 2048) throw new IllegalStateException("Too many draw calls");
                if (operation.equals("textWidth")) {
                    return host.textWidth(args);
                }
                host.draw(operation, args);
                return Undefined.instance;
            });
        }
        return context;
    }

    private void emit(Context cx, String event, Object[] arguments) {
        for (Function listener : List.copyOf(listeners.getOrDefault(event, List.of()))) {
            listener.call(cx, scope, scope, arguments);
        }
    }

    private Object parse(Context cx, String json) {
        return parseJson.call(cx, scope, scope, new Object[]{json});
    }

    private Scriptable object(Context cx) {
        return cx.newObject(scope);
    }

    private static void property(Scriptable target, String key, Object value) {
        ((ScriptableObject) target).defineProperty(key, value, ScriptableObject.READONLY | ScriptableObject.PERMANENT);
    }

    private void function(Scriptable target, String key, ApiFunction implementation) {
        property(target, key, createFunction(implementation));
    }

    private BaseFunction createFunction(ApiFunction implementation) {
        BaseFunction function = new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable self, Object[] args) {
                try {
                    return implementation.call(cx, args);
                } catch (Exception e) {
                    var error = ScriptRuntime.typeError(e.getMessage() == null ? "Plugin API call failed" : e.getMessage());
                    error.initCause(e);
                    throw error;
                }
            }
        };
        function.setParentScope(scope);
        function.setPrototype(ScriptableObject.getFunctionPrototype(scope));
        return function;
    }

    private <T> T run(org.mozilla.javascript.ContextAction<T> action) {
        if (closed) throw new IllegalStateException("Plugin is closed");
        if (Thread.currentThread() != owner) throw new IllegalStateException("Plugin called from another thread");
        messages = 0;
        return factory.call(action);
    }

    @Override
    public void close() {
        if (closed) return;
        try {
            if (scope != null) emit("unload");
        } finally {
            closed = true;
            listeners.clear();
            huds.clear();
            settings.clear();
            scope = null;
            parseJson = null;
            stringifyJson = null;
            renderContext = null;
        }
    }

    public static String string(Object[] arguments, int index) {
        if (index >= arguments.length) throw new IllegalArgumentException("Missing argument");
        String value = Context.toString(arguments[index]);
        if (value.length() > 8192) throw new IllegalArgumentException("Text exceeds limit");
        return value;
    }

    private interface ApiFunction {
        Object call(Context cx, Object[] arguments) throws Exception;
    }

    private record HudRegistration(String id, Scriptable definition, Function callback) {
    }

    private static final class BudgetContext extends Context {
        private final long deadline;
        private final int instructionLimit;
        private int instructions;

        private BudgetContext(BudgetFactory factory) {
            super(factory);
            deadline = System.nanoTime() + (factory.starting ? 1_000_000_000L : 20_000_000L);
            instructionLimit = factory.starting ? 2000000 : 100000;
        }
    }

    private static final class BudgetFactory extends ContextFactory {
        private boolean starting;
        @Override
        protected Context makeContext() {
            Context cx = new BudgetContext(this);
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            cx.setInstructionObserverThreshold(1000);
            cx.setClassShutter(name -> false);
            return cx;
        }

        @Override
        protected void observeInstructionCount(Context cx, int count) {
            BudgetContext budget = (BudgetContext) cx;
            budget.instructions += count;
            if (budget.instructions > budget.instructionLimit || System.nanoTime() > budget.deadline) {
                throw new ScriptLimitException();
            }
        }
    }

    public static final class ScriptLimitException extends Error {
        public ScriptLimitException() {
            super("Plugin execution budget exceeded");
        }
    }
}
