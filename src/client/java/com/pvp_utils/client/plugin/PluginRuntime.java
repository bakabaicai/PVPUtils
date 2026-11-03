package com.pvp_utils.client.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Undefined;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

public final class PluginRuntime implements AutoCloseable {
    public interface Host {
        void log(String message);
        void notify(String message);
        String playerSnapshot();
        default String playerToken() {
            return null;
        }
        default String playerCall(String token, String operation, Object[] arguments) {
            throw new IllegalStateException("Player bridge is not available");
        }
        default void releasePlayer() {
        }
        void draw(String operation, Object[] arguments);
        double textWidth(Object[] arguments);
        default void pushDraw() {}
        default void popDraw() {}
        default double mouseX() { return -1; }
        default double mouseY() { return -1; }
        default void screen(String operation, String id) {}
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
    private int playerActions;
    private int storageWrites;
    private final RateBudget messageBudget = new RateBudget(32);
    private final RateBudget storageBudget = new RateBudget(16);
    private final RateBudget screenBudget = new RateBudget(8);
    private final RateBudget playerBudget = new RateBudget(128);
    private Scriptable renderContext;
    private int renderWidth;
    private int renderHeight;
    private Function parseJson;
    private final Map<String, Scriptable> screens = new LinkedHashMap<>();
    private final List<UiButton> buttons = new ArrayList<>();
    private String drawingScreen;
    private int stateDepth;
    private int uiActions;
    private boolean transformed;
    private int screenWidth;
    private int screenHeight;
    private int effectCalls;
    private float originX;
    private float originY;
    private float originScale = 1;
    private double transformScaleX = 1;
    private double transformScaleY = 1;
    private final ArrayDeque<double[]> transformStack = new ArrayDeque<>();

    public PluginRuntime(PluginManifest manifest, PluginStorage storage, Host host) {
        this.manifest = manifest;
        this.storage = storage;
        this.host = host;
    }

    public static void validateScript(PluginManifest manifest) throws Exception {
        validateScript(manifest, PluginSource.readUtf8(manifest.entry(), 1048576));
    }

    public static void validateScript(PluginManifest manifest, String source) {
        ContextFactory factory = new ContextFactory();
        factory.call(cx -> {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);
            cx.setClassShutter(name -> false);
            cx.compileString(source, manifest.entry().getFileName().toString(), 1, null);
            return null;
        });
    }

    public void start(String source) throws Exception {
        if (scope != null) throw new IllegalStateException("Plugin already started");
        if (source == null || source.length() > 1048576) throw new IllegalArgumentException("Invalid plugin source");
        factory.starting = true;
        try {
            run(cx -> {
                scope = cx.initSafeStandardObjects();
                Scriptable json = (Scriptable) ScriptableObject.getProperty(scope, "JSON");
                parseJson = (Function) ScriptableObject.getProperty(json, "parse");
                Scriptable api = object(cx);
                ScriptableObject.putProperty(scope, "pvputils", api);
                property(api, "apiVersion", 1);
                property(api, "id", manifest.id());
                function(api, "log", (context, args) -> {
                    if (++messages > 32) throw new ScriptLimitException();
                    messageBudget.take();
                    host.log(string(args, 0));
                    return Undefined.instance;
                });
                Scriptable notifications = object(cx);
                property(api, "notify", notifications);
                function(notifications, "show", (context, args) -> {
                    if (++messages > 32) throw new ScriptLimitException();
                    messageBudget.take();
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
                function(player, "get", (context, args) -> {
                    String token = host.playerToken();
                    return token == null ? null : playerHandle(context, token);
                });
                Scriptable store = object(cx);
                property(api, "storage", store);
                function(store, "get", (context, args) -> {
                    String key = string(args, 0);
                    return !storage.contains(key) && args.length > 1 ? args[1] : parse(context, storage.get(key));
                });
                function(store, "set", (context, args) -> {
                    if (args.length < 2) throw new IllegalArgumentException("Missing storage value");
                    consumeStorageWrite();
                    JsonElement encoded = storageValue(args[1], 0, new int[]{0, 0},
                            Collections.newSetFromMap(new IdentityHashMap<>()));
                    storage.set(string(args, 0), encoded.toString());
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
                            consumeStorageWrite();
                            setting.set(values[0]);
                            return Undefined.instance;
                        }), true);
                        return handle;
                    });
                }
                Scriptable hud = object(cx);
                Scriptable ui = object(cx);
                property(api, "ui", ui);
                function(ui, "screen", (context, args) -> {
                    if (args.length != 1 || !(args[0] instanceof Scriptable definition)
                            || !(ScriptableObject.getProperty(definition, "render") instanceof Function)) {
                        throw new IllegalArgumentException("Screen requires a render callback");
                    }
                    String id = Context.toString(ScriptableObject.getProperty(definition, "id"));
                    if (!id.matches("[a-z][a-z0-9_-]{0,63}") || screens.containsKey(id) || screens.size() >= 8) {
                        throw new IllegalArgumentException("Invalid, duplicate or excessive screen id");
                    }
                    screens.put(id, definition);
                    return Undefined.instance;
                });
                for (String operation : List.of("open", "close")) {
                    function(ui, operation, (context, args) -> {
                        if (rendering) throw new IllegalStateException("Screen changes require a non-render callback");
                        if (++uiActions > 8) throw new ScriptLimitException();
                        screenBudget.take();
                        String id = operation.equals("open") ? string(args, 0) : "";
                        if (operation.equals("open") && !screens.containsKey(id)) throw new IllegalArgumentException("Unknown screen");
                        host.screen(operation, id);
                        return Undefined.instance;
                    });
                }
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
                    huds.add(new HudRegistration(id, definition, callback, hudLayout(definition, id)));
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

    public List<HudLayout> hudLayouts() {
        return huds.stream().map(HudRegistration::layout).filter(layout -> layout != null).toList();
    }

    public void moveHud(String id, float x, float y, int width, int height) {
        for (HudRegistration hud : huds) {
            if (!hud.id().equals(id) || hud.layout() == null) continue;
            hud.layout().move(x, y, width, height);
        }
    }

    public void saveHudLayouts() throws java.io.IOException {
        com.google.gson.JsonObject values = new com.google.gson.JsonObject();
        for (HudLayout layout : hudLayouts()) {
            values.addProperty(layout.keyX(), layout.x);
            values.addProperty(layout.keyY(), layout.y);
            values.addProperty("hud:" + layout.id + ":scale", layout.scale);
        }
        if (values.size() > 0) storage.update(values);
    }

    private HudLayout hudLayout(Scriptable definition, String id) {
        Object raw = ScriptableObject.getProperty(definition, "layout");
        if (!(raw instanceof Scriptable layout)) return null;
        float x = finite(ScriptableObject.getProperty(layout, "x"), 20);
        float y = finite(ScriptableObject.getProperty(layout, "y"), 20);
        float width = finite(ScriptableObject.getProperty(layout, "width"), 160);
        float height = finite(ScriptableObject.getProperty(layout, "height"), 40);
        Object drag = ScriptableObject.getProperty(layout, "draggable");
        boolean draggable = drag == Scriptable.NOT_FOUND || Context.toBoolean(drag);
        String key = "hud:" + id + ":";
        x = (float) storage.number(key + "x", x);
        y = (float) storage.number(key + "y", y);
        Object label = ScriptableObject.getProperty(definition, "label");
        if (!Float.isFinite(x + y + width + height) || width <= 0 || height <= 0 || width > 4096 || height > 4096
                || Math.abs(x) > 100000 || Math.abs(y) > 100000) throw new IllegalArgumentException("Invalid HUD layout");
        HudLayout result = new HudLayout(id, label == Scriptable.NOT_FOUND ? id : Context.toString(label),
                x, y, Math.max(1, width), Math.max(1, height), draggable, key + "x", key + "y");
        double savedScale = storage.number(key + "scale", 1);
        result.scale = Double.isFinite(savedScale) ? (float) Math.max(0.5, Math.min(2, savedScale)) : 1;
        return result;
    }

    private static float finite(Object value, float fallback) {
        if (value == Scriptable.NOT_FOUND) return fallback;
        double number = Context.toNumber(value);
        if (!Double.isFinite(number) || Math.abs(number) > 100000) throw new IllegalArgumentException("Invalid layout number");
        return (float) number;
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
            effectCalls = 0;
            screenWidth = width;
            screenHeight = height;
            renderWidth = width;
            renderHeight = height;
            try {
                if (renderContext == null) renderContext = drawingContext(cx);
                Scriptable context = renderContext;
                drawScoped(() -> emit(cx, "render", new Object[]{context}));
                for (HudRegistration hud : List.copyOf(huds)) {
                    drawScoped(() -> {
                        HudLayout layout = hud.layout();
                        if (layout != null) {
                            layout.move(layout.x, layout.y, width, height);
                            renderWidth = Math.round(layout.width);
                            renderHeight = Math.round(layout.height);
                            host.draw("translate", new Object[]{layout.x, layout.y});
                            host.draw("scale", new Object[]{layout.scale, layout.scale});
                            originX = layout.x;
                            originY = layout.y;
                            originScale = layout.scale;
                        }
                        hud.callback().call(cx, scope, hud.definition(), new Object[]{context});
                    });
                    renderWidth = width;
                    renderHeight = height;
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
        for (String key : List.of("screenWidth", "screenHeight", "mouseX", "mouseY")) {
            context.setGetterOrSetter(key, 0, createFunction((current, args) -> switch (key) {
                case "screenWidth" -> screenWidth;
                case "screenHeight" -> screenHeight;
                case "mouseX" -> host.mouseX();
                default -> host.mouseY();
            }), false);
            context.setAttributes(key, ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        }
        for (String operation : List.of("text", "textWidth", "rect", "roundedRect", "line", "outline",
                "gradient", "gradientDiagonal", "shadow", "textShadow", "icon", "texture", "blur",
                "circle", "glass", "save", "restore", "translate", "scale", "rotate", "clip", "clipRounded")) {
            function(context, operation, (current, args) -> {
                if (!rendering) throw new IllegalStateException("Drawing is only available during render");
                if (++drawCalls > 2048) throw new ScriptLimitException("Too many draw calls");
                args = drawingArguments(operation, args);
                if (operation.equals("save")) {
                    if (stateDepth >= 32) throw new ScriptLimitException("Drawing stack limit");
                    transformStack.push(new double[]{transformScaleX, transformScaleY});
                    stateDepth++;
                }
                if (operation.equals("restore")) {
                    if (stateDepth <= 0) throw new IllegalStateException("Unbalanced restore");
                    stateDepth--;
                    double[] previous = transformStack.pop();
                    transformScaleX = previous[0];
                    transformScaleY = previous[1];
                }
                if (List.of("translate", "scale", "rotate", "clip", "clipRounded").contains(operation)) transformed = true;
                if (List.of("shadow", "blur", "glass").contains(operation) && ++effectCalls > 8) {
                    throw new ScriptLimitException("At most 8 filtered effects per frame");
                }
                if (operation.equals("glass") || operation.equals("blur")) {
                    if (transformed || stateDepth != 0) throw new IllegalStateException("Backdrop effects require base coordinates");
                    if (args.length < 5) throw new IllegalArgumentException("Missing backdrop bounds");
                    Object[] adjusted = args.clone();
                    adjusted[0] = originX + Context.toNumber(args[0]) * originScale;
                    adjusted[1] = originY + Context.toNumber(args[1]) * originScale;
                    for (int i = 2; i <= 4; i++) adjusted[i] = Context.toNumber(args[i]) * originScale;
                    if (operation.equals("blur")) {
                        host.pushDraw();
                        try {
                            host.draw("resetTransform", new Object[0]);
                            host.draw(operation, adjusted);
                        } finally {
                            host.popDraw();
                        }
                    } else {
                        host.draw(operation, adjusted);
                    }
                    return Undefined.instance;
                }
                if (operation.equals("textWidth")) {
                    return host.textWidth(args);
                }
                host.draw(operation, args);
                return Undefined.instance;
            });
        }
        function(context, "button", (current, args) -> {
            if (!rendering || drawingScreen == null || transformed) {
                throw new IllegalStateException("Interactive buttons require an untransformed custom screen");
            }
            if (buttons.size() >= 128 || ++drawCalls > 2048) throw new ScriptLimitException("Too many buttons");
            if (args.length < 7 || !(args[6] instanceof Function callback)) throw new IllegalArgumentException("Invalid button");
            String id = string(args, 0);
            if (!id.matches("[a-z][a-z0-9_-]{0,63}") || buttons.stream().anyMatch(b -> b.id.equals(id))) {
                throw new IllegalArgumentException("Invalid or duplicate button id");
            }
            float x = finite(args[2], Float.NaN);
            float y = finite(args[3], Float.NaN);
            float width = finite(args[4], Float.NaN);
            float height = finite(args[5], Float.NaN);
            if (!Float.isFinite(x + y + width + height) || width <= 0 || height <= 0
                    || Math.abs(x) > 100000 || Math.abs(y) > 100000 || width > 4096 || height > 4096) {
                throw new IllegalArgumentException("Invalid button bounds");
            }
            boolean enabled = args.length < 8 || Context.toBoolean(args[7]);
            boolean hover = host.mouseX() >= x && host.mouseY() >= y
                    && host.mouseX() < x + width && host.mouseY() < y + height;
            host.draw("button", new Object[]{string(args, 1), x, y, width, height, enabled, hover});
            buttons.add(new UiButton(drawingScreen, id, x, y, width, height, enabled, callback));
            return Undefined.instance;
        });
        return context;
    }

    private Object[] drawingArguments(String operation, Object[] values) {
        Object[] args = values.clone();
        switch (operation) {
            case "text", "textShadow", "icon" -> {
                textArgument(args, 0);
                numericArgument(args, 1, -100000, 100000);
                numericArgument(args, 2, -100000, 100000);
                numericArgument(args, 3, 1, 128);
                textArgument(args, 4);
                if (args.length > 5) textArgument(args, 5);
            }
            case "textWidth" -> {
                textArgument(args, 0);
                numericArgument(args, 1, 1, 128);
                if (args.length > 2) textArgument(args, 2);
            }
            case "texture" -> {
                textArgument(args, 0);
                boundsArguments(args, 1);
            }
            case "rect", "roundedRect", "outline", "gradient", "gradientDiagonal", "shadow",
                    "clip", "clipRounded", "blur", "glass" -> {
                boundsArguments(args, 0);
                switch (operation) {
                    case "rect" -> textArgument(args, 4);
                    case "roundedRect", "outline", "shadow", "clipRounded", "blur", "glass" ->
                            numericArgument(args, 4, 0, 4096);
                }
                switch (operation) {
                    case "roundedRect" -> textArgument(args, 5);
                    case "outline" -> {
                        numericArgument(args, 5, 0, 256);
                        textArgument(args, 6);
                    }
                    case "gradient", "gradientDiagonal" -> {
                        textArgument(args, 4);
                        textArgument(args, 5);
                        if (operation.equals("gradient")) {
                            if (args.length > 6) args[6] = Context.toBoolean(args[6]);
                            if (args.length > 7) numericArgument(args, 7, 0, 4096);
                        } else if (args.length > 6) numericArgument(args, 6, 0, 4096);
                    }
                    case "shadow" -> {
                        numericArgument(args, 5, -256, 256);
                        numericArgument(args, 6, 0, 64);
                        textArgument(args, 7);
                    }
                    case "blur" -> numericArgument(args, 5, 0, 64);
                    case "glass" -> {
                        if (args.length > 5) textArgument(args, 5);
                        if (args.length > 6) args[6] = Context.toBoolean(args[6]);
                        if (args.length > 7) args[7] = Context.toBoolean(args[7]);
                    }
                }
            }
            case "line" -> {
                for (int i = 0; i < 4; i++) numericArgument(args, i, -100000, 100000);
                numericArgument(args, 4, 0, 256);
                textArgument(args, 5);
            }
            case "circle" -> {
                numericArgument(args, 0, -100000, 100000);
                numericArgument(args, 1, -100000, 100000);
                numericArgument(args, 2, 0, 4096);
                if ((double) args[2] * transformScaleX > 4096) throw new ScriptLimitException("Transformed radius exceeds limit");
                textArgument(args, 3);
            }
            case "translate" -> {
                numericArgument(args, 0, -100000, 100000);
                numericArgument(args, 1, -100000, 100000);
            }
            case "scale" -> {
                numericArgument(args, 0, -64, 64);
                numericArgument(args, 1, -64, 64);
                double x = Math.abs((double) args[0]);
                double y = Math.abs((double) args[1]);
                double maximum = transformScaleX * Math.max(x, y);
                double minimum = transformScaleY * Math.min(x, y);
                if (maximum > 64 || minimum < 1.0 / 64) throw new ScriptLimitException("Cumulative scale exceeds limit");
                transformScaleX = maximum;
                transformScaleY = minimum;
            }
            case "rotate" -> numericArgument(args, 0, -360000, 360000);
        }
        return args;
    }

    private void boundsArguments(Object[] args, int offset) {
        numericArgument(args, offset, -100000, 100000);
        numericArgument(args, offset + 1, -100000, 100000);
        numericArgument(args, offset + 2, 0, 4096);
        numericArgument(args, offset + 3, 0, 4096);
        if ((double) args[offset + 2] * transformScaleX > 8192
                || (double) args[offset + 3] * transformScaleX > 8192) {
            throw new ScriptLimitException("Transformed drawing bounds exceed limit");
        }
    }

    private static void numericArgument(Object[] args, int index, double minimum, double maximum) {
        if (index >= args.length) throw new IllegalArgumentException("Missing drawing argument");
        double value = Context.toNumber(args[index]);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new ScriptLimitException("Drawing argument exceeds limit");
        }
        args[index] = value;
    }

    private static void textArgument(Object[] args, int index) {
        args[index] = string(args, index);
    }

    private JsonElement storageValue(Object value, int depth, int[] size, Set<Scriptable> active) {
        if (depth > 64 || ++size[0] > 20000) throw new ScriptLimitException("Storage structure exceeds limit");
        factory.check(Context.getCurrentContext());
        if (value == null || value == Undefined.instance || value == Scriptable.NOT_FOUND) return JsonNull.INSTANCE;
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        if (value instanceof Number number) {
            return Double.isFinite(number.doubleValue()) ? new JsonPrimitive(number) : JsonNull.INSTANCE;
        }
        if (value instanceof CharSequence text) {
            if (text.length() > 1048576 - size[1]) throw new ScriptLimitException("Storage text exceeds limit");
            size[1] += text.length();
            return new JsonPrimitive(text.toString());
        }
        if (!(value instanceof NativeArray) && !(value instanceof NativeObject)) {
            throw new IllegalArgumentException("Storage accepts only JSON values");
        }
        Scriptable object = (Scriptable) value;
        if (!active.add(object)) throw new IllegalArgumentException("Cyclic storage value");
        try {
            if (object instanceof NativeArray array) {
                if (array.getLength() > 16384) throw new ScriptLimitException("Storage array exceeds limit");
                JsonArray result = new JsonArray();
                for (int i = 0; i < array.getLength(); i++) {
                    Object item = ScriptableObject.getProperty(array, i);
                    result.add(storageValue(item instanceof Function ? null : item, depth + 1, size, active));
                }
                return result;
            }
            Object[] ids = object.getIds();
            if (ids.length > 16384) throw new ScriptLimitException("Storage object exceeds limit");
            JsonObject result = new JsonObject();
            for (Object id : ids) {
                String key = Context.toString(id);
                if (key.length() > 1048576 - size[1]) throw new ScriptLimitException("Storage keys exceed limit");
                size[1] += key.length();
                Object item = id instanceof Number number
                        ? ScriptableObject.getProperty(object, number.intValue()) : ScriptableObject.getProperty(object, key);
                if (item == Undefined.instance || item == Scriptable.NOT_FOUND || item instanceof Function) continue;
                result.add(key, storageValue(item, depth + 1, size, active));
            }
            return result;
        } finally {
            active.remove(object);
        }
    }

    private void drawScoped(Runnable callback) {
        host.pushDraw();
        stateDepth = 0;
        transformed = false;
        originX = originY = 0;
        originScale = 1;
        transformScaleX = transformScaleY = 1;
        transformStack.clear();
        try {
            callback.run();
        } finally {
            try {
                while (stateDepth > 0) {
                    host.draw("restore", new Object[0]);
                    stateDepth--;
                }
            } finally {
                transformStack.clear();
                host.popDraw();
            }
        }
    }

    public boolean hasScreen(String id) { return screens.containsKey(id); }

    public String screenTitle(String id) {
        if (!screens.containsKey(id)) throw new IllegalArgumentException("Unknown screen");
        return run(cx -> {
            Object title = ScriptableObject.getProperty(screens.get(id), "title");
            return title == Scriptable.NOT_FOUND ? id : string(new Object[]{title}, 0);
        });
    }

    public void screenEvent(String id, String event) {
        if (!screens.containsKey(id) || closed) return;
        if (event.equals("onOpen") || event.equals("onClose")) buttons.clear();
        run(cx -> {
            Object callback = ScriptableObject.getProperty(screens.get(id), event);
            if (callback instanceof Function function) function.call(cx, scope, screens.get(id), new Object[0]);
            return null;
        });
    }

    public boolean screenInput(String id, String event, Object[] args) {
        if (closed || !screens.containsKey(id)) return false;
        return run(cx -> {
            Object callback = ScriptableObject.getProperty(screens.get(id), event);
            return callback instanceof Function function
                    && Context.toBoolean(function.call(cx, scope, screens.get(id), args));
        });
    }

    public void renderScreen(String id, int width, int height) {
        if (!screens.containsKey(id)) return;
        run(cx -> {
            buttons.clear();
            rendering = true;
            drawingScreen = id;
            drawCalls = 0;
            effectCalls = 0;
            screenWidth = renderWidth = width;
            screenHeight = renderHeight = height;
            try {
                if (renderContext == null) renderContext = drawingContext(cx);
                drawScoped(() -> ((Function) ScriptableObject.getProperty(screens.get(id), "render"))
                        .call(cx, scope, screens.get(id), new Object[]{renderContext}));
            } finally {
                rendering = false;
                drawingScreen = null;
            }
            return null;
        });
    }

    public boolean clickScreen(String id, double x, double y, int button) {
        if (closed || button != 0 || !screens.containsKey(id)) return false;
        return run(cx -> {
            for (int i = buttons.size() - 1; i >= 0; i--) {
                UiButton control = buttons.get(i);
                if (control.screen.equals(id) && control.enabled && x >= control.x && y >= control.y
                        && x < control.x + control.width && y < control.y + control.height) {
                    control.callback.call(cx, scope, scope, new Object[0]);
                    return true;
                }
            }
            return false;
        });
    }

    private Scriptable playerHandle(Context cx, String token) {
        Scriptable player = object(cx);
        for (String operation : List.of("snapshot", "target")) {
            function(player, operation, (current, args) -> playerCall(current, token, operation, args, false));
        }
        for (String operation : List.of("setRotation", "setVelocity", "setSprinting", "jump",
                "swing", "useItem", "stopUsingItem", "closeMenu", "attackTarget", "interactTarget")) {
            function(player, operation, (current, args) -> playerCall(current, token, operation, args, true));
        }
        Scriptable inventory = object(cx);
        property(player, "inventory", inventory);
        for (String operation : List.of("getItems", "getSlot", "getSelectedSlot")) {
            function(inventory, operation, (current, args) -> playerCall(current, token, operation, args, false));
        }
        function(inventory, "select", (current, args) -> playerCall(current, token, "select", args, true));
        function(inventory, "getMenu", (current, args) -> {
            Object data = playerCall(current, token, "menuSnapshot", new Object[0], false);
            if (!(data instanceof Scriptable snapshot)) return null;
            String menuToken = Context.toString(ScriptableObject.getProperty(snapshot, "token"));
            Scriptable menu = object(current);
            function(menu, "snapshot", (context, unused) ->
                    playerCall(context, token, "menuSnapshot", new Object[]{menuToken}, false));
            function(menu, "click", (context, values) -> {
                if (values.length != 3) throw new IllegalArgumentException("click requires slot, button and type");
                return playerCall(context, token, "click",
                        new Object[]{menuToken, values[0], values[1], values[2]}, true);
            });
            return menu;
        });
        return player;
    }

    private Object playerCall(Context cx, String token, String operation, Object[] arguments, boolean action) {
        if (action) {
            if (rendering) throw new IllegalStateException("Player actions are not available during render");
            if (++playerActions > 32) throw new ScriptLimitException();
            playerBudget.take();
        }
        return parse(cx, host.playerCall(token, operation, arguments));
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
                    factory.check(cx);
                    Object result = implementation.call(cx, args);
                    factory.check(cx);
                    return result;
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
        playerActions = 0;
        uiActions = 0;
        storageWrites = 0;
        try {
            return factory.call(cx -> {
                T result = action.run(cx);
                factory.check(cx);
                return result;
            });
        } catch (StackOverflowError error) {
            throw new ScriptLimitException("Plugin stack limit exceeded");
        }
    }

    private void consumeStorageWrite() {
        if (++storageWrites > 8) throw new ScriptLimitException("Too many storage writes");
        storageBudget.take();
    }

    @Override
    public void close() {
        if (closed) return;
        try {
            saveHudLayouts();
            if (scope != null) emit("unload");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to save HUD layouts", e);
        } finally {
            closed = true;
            listeners.clear();
            huds.clear();
            settings.clear();
            scope = null;
            parseJson = null;
            renderContext = null;
            screens.clear();
            buttons.clear();
            transformStack.clear();
            host.releasePlayer();
        }
    }

    public static String string(Object[] arguments, int index) {
        if (index >= arguments.length) throw new IllegalArgumentException("Missing argument");
        if (arguments[index] instanceof CharSequence text && text.length() > 8192) {
            throw new IllegalArgumentException("Text exceeds limit");
        }
        String value = Context.toString(arguments[index]);
        if (value.length() > 8192) throw new IllegalArgumentException("Text exceeds limit");
        return value;
    }

    private interface ApiFunction {
        Object call(Context cx, Object[] arguments) throws Exception;
    }

    public static final class HudLayout {
        private final String id;
        private final String label;
        private float x;
        private float y;
        private final float width;
        private final float height;
        private final boolean draggable;
        private float scale = 1;
        private final String keyX;
        private final String keyY;

        private HudLayout(String id, String label, float x, float y, float width, float height,
                          boolean draggable, String keyX, String keyY) {
            this.id = id;
            if (label.length() > 96) throw new IllegalArgumentException("HUD label exceeds 96 characters");
            this.label = label.isBlank() ? id : label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.draggable = draggable;
            this.keyX = keyX;
            this.keyY = keyY;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public float x() {
            return x;
        }

        public float y() {
            return y;
        }

        public float width() {
            return width;
        }

        public float height() {
            return height;
        }

        public boolean draggable() {
            return draggable;
        }

        public float scale() { return scale; }

        public void setScale(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid scale");
            scale = Math.max(0.5f, Math.min(2, value));
        }

        private String keyX() {
            return keyX;
        }

        private String keyY() {
            return keyY;
        }

        private void move(float x, float y, int guiWidth, int guiHeight) {
            if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Invalid HUD position");
            this.x = Math.max(0, Math.min(x, guiWidth - width * scale));
            this.y = Math.max(0, Math.min(y, guiHeight - height * scale));
        }
    }

    private record HudRegistration(String id, Scriptable definition, Function callback, HudLayout layout) {
    }

    private record UiButton(String screen, String id, float x, float y, float width, float height,
                            boolean enabled, Function callback) {}

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
            cx.setMaximumInterpreterStackDepth(128);
            cx.setClassShutter(name -> false);
            return cx;
        }

        @Override
        protected void observeInstructionCount(Context cx, int count) {
            BudgetContext budget = (BudgetContext) cx;
            budget.instructions += count;
            check(cx);
        }

        private void check(Context cx) {
            BudgetContext budget = (BudgetContext) cx;
            if (budget.instructions > budget.instructionLimit || System.nanoTime() - budget.deadline >= 0) {
                throw new ScriptLimitException();
            }
        }
    }

    private static final class RateBudget {
        private final int limit;
        private long window = System.nanoTime();
        private int used;

        private RateBudget(int limit) {
            this.limit = limit;
        }

        private void take() {
            long now = System.nanoTime();
            if (now - window >= 1_000_000_000L) {
                window = now;
                used = 0;
            }
            if (++used > limit) throw new ScriptLimitException("Plugin rate limit exceeded");
        }
    }

    public static final class ScriptLimitException extends Error {
        public ScriptLimitException() {
            super("Plugin execution budget exceeded");
        }

        public ScriptLimitException(String message) {
            super(message);
        }
    }
}
