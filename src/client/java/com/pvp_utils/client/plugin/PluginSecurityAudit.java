package com.pvp_utils.client.plugin;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PluginSecurityAudit {
    public enum Risk {
        PLAYER_CONTROL, UI_CONTROL, RESOURCE_USAGE, HOST_ACCESS, DYNAMIC_CODE, AUDIT_FAILURE
    }

    public record Finding(String ruleId, Risk risk, int line, String descriptionZh, String descriptionEn) {}

    public record Report(List<Finding> findings, boolean completed) {
        public Report {
            findings = List.copyOf(findings);
        }

        public boolean requiresApproval() {
            return !completed || !findings.isEmpty();
        }

        public boolean failed() {
            return !completed;
        }
    }

    private enum Phase {
        TOP_LEVEL, LOAD, UNLOAD, TICK, RENDER, JOIN, LEAVE, SCREEN_RENDER, ON_OPEN, ON_CLOSE, INPUT, UNKNOWN
    }

    private static final int MAX_SOURCE = 262144;
    private static final int MAX_NODES = 20000;
    private static final int MAX_DEPTH = 128;
    private static final int MAX_FUNCTIONS = 256;
    private static final int MAX_FINDINGS = 128;
    private static final Set<String> BUILTINS = Set.of("Math", "JSON", "String", "Number", "Boolean", "Object",
            "Array", "Date", "RegExp", "Error", "TypeError", "RangeError", "Map", "Set", "WeakMap", "WeakSet",
            "parseInt", "parseFloat", "isNaN", "isFinite", "undefined", "NaN", "Infinity", "decodeURI",
            "decodeURIComponent", "encodeURI", "encodeURIComponent", "Int8Array", "Uint8Array", "Uint8ClampedArray",
            "Int16Array", "Uint16Array", "Int32Array", "Uint32Array", "Float32Array", "Float64Array", "ArrayBuffer",
            "DataView", "pvputils");
    private static final Set<String> ACTIONS = Set.of("setRotation", "setVelocity", "setSprinting", "jump", "swing",
            "useItem", "stopUsingItem", "closeMenu", "attackTarget", "interactTarget", "select", "click");
    private static final Set<String> DRAWS = Set.of("text", "textWidth", "textShadow", "rect", "roundedRect", "line",
            "outline", "gradient", "gradientDiagonal", "shadow", "circle", "icon", "texture", "blur", "glass",
            "save", "restore", "translate", "scale", "rotate", "clip", "clipRounded", "button");
    private static final Set<String> DATA_METHODS = Set.of("toString", "valueOf", "toFixed", "toPrecision",
            "toExponential", "toLocaleString", "toUpperCase", "toLowerCase", "trim", "trimStart", "trimEnd",
            "charAt", "charCodeAt", "codePointAt", "substring", "substr", "slice", "split", "indexOf", "lastIndexOf",
            "includes", "startsWith", "endsWith", "replace", "replaceAll", "match", "search", "concat", "join",
            "filter", "map", "reduce", "reduceRight", "forEach", "some", "every", "find", "findIndex", "sort",
            "reverse", "push", "pop", "shift", "unshift", "splice", "fill", "copyWithin", "entries", "keys",
            "values", "has", "get", "set", "add", "delete", "clear", "test", "exec", "repeat", "padStart", "padEnd");
    private static final Set<String> HOST_ROOTS = Set.of("java", "javax", "Packages", "Java", "JavaAdapter",
            "JavaImporter", "org", "com", "net", "sun", "jdk", "System", "Runtime", "ProcessBuilder", "Class",
            "File", "Files", "Paths", "URL", "Socket", "HttpClient", "process", "require", "fetch", "XMLHttpRequest",
            "WebSocket", "load", "readFile", "readUrl", "quit", "exit", "print", "importClass", "importPackage");
    private static final Set<String> HOST_METHODS = Set.of("disconnect", "disconnectFromWorld", "clearLevel",
            "setLevel", "setScreen", "stop", "halt", "exit", "destroy", "destroyForcibly", "forName", "getClass",
            "getDeclaredMethod", "getDeclaredMethods", "getDeclaredField", "getDeclaredFields", "getClassLoader",
            "setAccessible", "invoke", "newInstance", "exec", "getRuntime", "getInstance", "sendChat", "sendCommand");

    private PluginSecurityAudit() {}

    public static Report audit(String source, String sourceName) {
        if (source == null || source.length() > MAX_SOURCE) {
            return failure("AUDIT_SOURCE_LIMIT", 1, "脚本为空或超过静态审计大小限制。", "Source is missing or exceeds the static audit size limit.");
        }
        try {
            long deadline = System.nanoTime() + 500_000_000L;
            preflight(source);
            CompilerEnvirons environment = new CompilerEnvirons();
            environment.setLanguageVersion(Context.VERSION_ES6);
            environment.setRecoverFromErrors(false);
            environment.setRecordingComments(false);
            environment.setXmlAvailable(false);
            ErrorReporter reporter = new ErrorReporter() {
                @Override
                public void warning(String message, String name, int line, String text, int offset) {}

                @Override
                public void error(String message, String name, int line, String text, int offset) {
                    throw new EvaluatorException("Plugin audit parse failed", name, line);
                }

                @Override
                public EvaluatorException runtimeError(String message, String name, int line, String text, int offset) {
                    return new EvaluatorException("Plugin audit parse failed", name, line);
                }
            };
            AstRoot root = new Parser(environment, reporter).parse(source, sourceName == null ? "plugin.js" : sourceName, 1);
            return new Analysis(root, deadline).run();
        } catch (EvaluatorException exception) {
            return failure("AUDIT_PARSE_FAILED", Math.max(1, exception.lineNumber()),
                    "脚本语法无法完成静态审计，已阻止执行。", "Static parsing failed; execution must be blocked.");
        } catch (AuditLimitException | StackOverflowError exception) {
            return failure("AUDIT_COMPLEXITY_LIMIT", 1, "脚本复杂度超过静态审计限制，已阻止执行。",
                    "Source complexity exceeds the static audit limit; execution must be blocked.");
        } catch (RuntimeException exception) {
            return failure("AUDIT_FAILED", 1, "静态审计未完成，已阻止执行。", "Static audit did not complete; execution must be blocked.");
        }
    }

    private static Report failure(String id, int line, String zh, String en) {
        return new Report(List.of(new Finding(id, Risk.AUDIT_FAILURE, line, zh, en)), false);
    }

    private static void preflight(String source) {
        int depth = 0;
        int punctuation = 0;
        char quote = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : 0;
            if (lineComment) {
                if (current == '\n' || current == '\r') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    index++;
                }
                continue;
            }
            if (quote != 0) {
                if (current == '\\') index++;
                else if (current == quote) quote = 0;
                continue;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
                index++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                index++;
            } else if (current == '\'' || current == '"' || current == '`') {
                quote = current;
            } else {
                if (current == '(' || current == '[' || current == '{') {
                    if (++depth > MAX_DEPTH) throw new AuditLimitException();
                } else if (current == ')' || current == ']' || current == '}') {
                    depth = Math.max(0, depth - 1);
                }
                if (!Character.isJavaIdentifierPart(current) && !Character.isWhitespace(current)
                        && ++punctuation > MAX_NODES) throw new AuditLimitException();
            }
        }
    }

    private static final class Analysis {
        private final List<AstNode> nodes = new ArrayList<>();
        private final List<FunctionCall> calls = new ArrayList<>();
        private final List<FunctionNode> functions = new ArrayList<>();
        private final Map<Binding, List<Reference>> bindings = new HashMap<>();
        private final Map<Binding, Value> seeds = new HashMap<>();
        private final Map<FunctionNode, List<AstNode>> returns = new IdentityHashMap<>();
        private final Map<FunctionNode, EnumSet<Phase>> phases = new IdentityHashMap<>();
        private final Map<FunctionNode, Set<FunctionNode>> edges = new IdentityHashMap<>();
        private final Set<FunctionNode> repeatedFunctions = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<String, Finding> findings = new LinkedHashMap<>();
        private final long deadline;
        private long work = 2000000;
        private boolean changed;

        private Analysis(AstRoot root, long deadline) {
            this.deadline = deadline;
            if (System.nanoTime() > deadline) throw new AuditLimitException();
            root.visit(node -> {
                checkpoint();
                if (nodes.size() >= MAX_NODES || node.depth() > MAX_DEPTH) throw new AuditLimitException();
                nodes.add(node);
                if (node instanceof FunctionCall call) calls.add(call);
                if (node instanceof FunctionNode function) {
                    if (functions.size() >= MAX_FUNCTIONS) throw new AuditLimitException();
                    functions.add(function);
                    phases.put(function, EnumSet.noneOf(Phase.class));
                    if (!function.getName().isEmpty()) {
                        bind(function.getEnclosingFunction(), function.getName(), new Reference(function, List.of()));
                    }
                    for (AstNode parameter : function.getParams()) declarePattern(parameter, function);
                }
                if (node instanceof VariableInitializer variable) {
                    declarePattern(variable.getTarget(), node.getEnclosingFunction());
                    if (variable.getInitializer() != null) {
                        bindPattern(variable.getTarget(), variable.getInitializer(), List.of(), node.getEnclosingFunction());
                    }
                }
                if (node instanceof Assignment assignment) {
                    bindPattern(assignment.getLeft(), assignment.getRight(), List.of(), node.getEnclosingFunction());
                }
                if (node instanceof ReturnStatement statement && statement.getReturnValue() != null) {
                    returns.computeIfAbsent(node.getEnclosingFunction(), key -> new ArrayList<>()).add(statement.getReturnValue());
                }
                return true;
            });
        }

        private Report run() {
            for (int pass = 0; pass < MAX_FUNCTIONS + 2; pass++) {
                changed = false;
                for (FunctionCall call : calls) propagate(call);
                if (!changed) break;
                if (pass == MAX_FUNCTIONS + 1) throw new AuditLimitException();
            }
            for (AstNode node : nodes) inspect(node);
            for (FunctionCall call : calls) inspectCall(call);
            inspectRecursion();
            if (System.nanoTime() > deadline) throw new AuditLimitException();
            return new Report(new ArrayList<>(findings.values()), true);
        }

        private void checkpoint() {
            if (--work < 0 || (work & 255) == 0 && System.nanoTime() > deadline) throw new AuditLimitException();
        }

        private void bind(FunctionNode owner, String name, Reference reference) {
            if (name != null && !name.isEmpty()) {
                bindings.computeIfAbsent(new Binding(owner, name), key -> new ArrayList<>()).add(reference);
            }
        }

        private void declarePattern(AstNode target, FunctionNode owner) {
            if (target instanceof Name name) bindings.computeIfAbsent(new Binding(owner, name.getIdentifier()), key -> new ArrayList<>());
            else if (target instanceof ObjectLiteral object) for (ObjectProperty property : object.getElements()) declarePattern(property.getRight(), owner);
            else if (target instanceof ArrayLiteral array) for (AstNode element : array.getElements()) declarePattern(element, owner);
        }

        private void bindPattern(AstNode target, AstNode source, List<String> projection, FunctionNode owner) {
            if (target instanceof Name name) {
                bind(owner, name.getIdentifier(), new Reference(source, projection));
            } else if (target instanceof PropertyGet property) {
                String name = rawName(property);
                if (name != null) bind(owner, name, new Reference(source, projection));
            } else if (target instanceof ElementGet element) {
                String root = rawName(element.getTarget());
                String key = literalKey(element.getElement());
                if (root != null && key != null) bind(owner, root + "." + key, new Reference(source, projection));
            } else if (target instanceof ObjectLiteral object) {
                for (ObjectProperty property : object.getElements()) {
                    String key = literalKey(property.getLeft());
                    if (key == null) {
                        add("DYNAMIC_DESTRUCTURING", Risk.DYNAMIC_CODE, target,
                                "解构属性无法静态解析，可能隐藏受控接口调用。", "A destructured property cannot be resolved and may hide controlled API calls.");
                    } else {
                        List<String> path = new ArrayList<>(projection);
                        path.add(key);
                        bindPattern(property.getRight(), source, path, owner);
                    }
                }
            } else if (target instanceof ArrayLiteral array) {
                for (int index = 0; index < array.getElements().size(); index++) {
                    List<String> path = new ArrayList<>(projection);
                    path.add(Integer.toString(index));
                    bindPattern(array.getElements().get(index), source, path, owner);
                }
            }
        }

        private Value resolve(AstNode node) {
            return resolve(node, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        private Value resolve(AstNode node, int depth, Set<AstNode> active) {
            checkpoint();
            if (node == null) return new Value();
            if (depth > 24 || !active.add(node)) return Value.unknown();
            try {
                if (node instanceof ParenthesizedExpression expression) return resolve(expression.getExpression(), depth + 1, active);
                if (node instanceof Name name) return resolveName(name, depth, active);
                if (node instanceof StringLiteral string) return Value.text(string.getValue());
                if (node instanceof NumberLiteral number) return Value.number(number.getNumber());
                if (node instanceof FunctionNode function) return Value.function(function);
                if (node instanceof ObjectLiteral || node instanceof ArrayLiteral) return Value.object(node);
                if (node instanceof PropertyGet property) {
                    Value value = project(resolve(property.getTarget(), depth + 1, active), property.getProperty().getIdentifier(), depth + 1, active);
                    String raw = rawName(property);
                    if (raw != null && hasBinding(node.getEnclosingFunction(), raw)) {
                        value.merge(resolveBinding(node.getEnclosingFunction(), raw, depth, active));
                    }
                    return value;
                }
                if (node instanceof ElementGet element) {
                    Value target = resolve(element.getTarget(), depth + 1, active);
                    Value key = resolve(element.getElement(), depth + 1, active);
                    Value value = new Value();
                    for (String text : key.keys()) value.merge(project(target, text, depth + 1, active));
                    if (key.keys().isEmpty() || key.unknown) value.unknown = true;
                    return value;
                }
                if (node instanceof Assignment assignment) return resolve(assignment.getRight(), depth + 1, active);
                if (node instanceof ConditionalExpression conditional) {
                    Value value = resolve(conditional.getTrueExpression(), depth + 1, active);
                    value.merge(resolve(conditional.getFalseExpression(), depth + 1, active));
                    return value;
                }
                if (node instanceof UnaryExpression unary) {
                    Value value = resolve(unary.getOperand(), depth + 1, active);
                    if (unary.getOperator() == Token.NEG || unary.getOperator() == Token.POS) {
                        Value numeric = new Value();
                        for (double number : value.numbers) numeric.numbers.add(unary.getOperator() == Token.NEG ? -number : number);
                        numeric.paths.addAll(value.paths);
                        numeric.unknown = value.unknown;
                        return numeric;
                    }
                    return value;
                }
                if (node instanceof InfixExpression infix) {
                    Value left = resolve(infix.getLeft(), depth + 1, active);
                    Value right = resolve(infix.getRight(), depth + 1, active);
                    if (infix.getOperator() == Token.ADD) {
                        Value value = new Value();
                        if (!left.texts.isEmpty() || !right.texts.isEmpty()) {
                            for (String a : left.keys()) for (String b : right.keys()) {
                                if (a.length() + b.length() > MAX_SOURCE) throw new AuditLimitException();
                                value.texts.add(a + b);
                                if (value.texts.size() > 128) throw new AuditLimitException();
                            }
                        }
                        numeric(value, left, right, Token.ADD);
                        value.paths.addAll(left.paths);
                        value.paths.addAll(right.paths);
                        value.unknown = left.unknown || right.unknown;
                        return value;
                    }
                    if (Set.of(Token.SUB, Token.MUL, Token.DIV, Token.MOD).contains(infix.getOperator())) {
                        Value value = new Value();
                        numeric(value, left, right, infix.getOperator());
                        value.paths.addAll(left.paths);
                        value.paths.addAll(right.paths);
                        value.unknown = left.unknown || right.unknown;
                        return value;
                    }
                    left.merge(right);
                    return left;
                }
                if (node instanceof FunctionCall call) return callValue(call, depth, active);
                if (node instanceof KeywordLiteral keyword && keyword.getType() == Token.NULL) return new Value();
                return Value.unknown();
            } finally {
                active.remove(node);
            }
        }

        private Value resolveName(Name name, int depth, Set<AstNode> active) {
            String text = name.getIdentifier();
            if (text.equals("Infinity")) return Value.number(Double.POSITIVE_INFINITY);
            if (text.equals("NaN")) return Value.number(Double.NaN);
            if (hasBinding(name.getEnclosingFunction(), text)) return resolveBinding(name.getEnclosingFunction(), text, depth, active);
            return Value.path(text, !BUILTINS.contains(text));
        }

        private boolean hasBinding(FunctionNode owner, String name) {
            for (FunctionNode current = owner;; current = current == null ? null : current.getEnclosingFunction()) {
                Binding key = new Binding(current, name);
                if (bindings.containsKey(key) || seeds.containsKey(key)) return true;
                if (current == null) return false;
            }
        }

        private Value resolveBinding(FunctionNode owner, String name, int depth, Set<AstNode> active) {
            for (FunctionNode current = owner;; current = current == null ? null : current.getEnclosingFunction()) {
                Binding key = new Binding(current, name);
                if (bindings.containsKey(key) || seeds.containsKey(key)) {
                    Value result = new Value();
                    Value seed = seeds.get(key);
                    if (seed != null) result.merge(seed);
                    for (Reference reference : bindings.getOrDefault(key, List.of())) {
                        Value value = resolve(reference.node(), depth + 1, active);
                        for (String property : reference.projection()) value = project(value, property, depth + 1, active);
                        result.merge(value);
                    }
                    if (bindings.getOrDefault(key, List.of()).isEmpty() && seed == null) result.unknown = true;
                    return result;
                }
                if (current == null) return Value.unknown();
            }
        }

        private Value project(Value target, String key, int depth, Set<AstNode> active) {
            Value value = new Value();
            for (String path : target.paths) {
                if (path.length() + key.length() > 4096) throw new AuditLimitException();
                value.paths.add(path + "." + key);
                if (path.equals("Number")) {
                    switch (key) {
                        case "NaN" -> value.numbers.add(Double.NaN);
                        case "POSITIVE_INFINITY" -> value.numbers.add(Double.POSITIVE_INFINITY);
                        case "NEGATIVE_INFINITY" -> value.numbers.add(Double.NEGATIVE_INFINITY);
                        case "MAX_VALUE" -> value.numbers.add(Double.MAX_VALUE);
                        case "MIN_VALUE" -> value.numbers.add(Double.MIN_VALUE);
                        case "MAX_SAFE_INTEGER" -> value.numbers.add(9007199254740991d);
                        default -> {}
                    }
                }
                if (path.equals("Math") && key.equals("PI")) value.numbers.add(Math.PI);
                if (path.equals("Math") && key.equals("E")) value.numbers.add(Math.E);
            }
            for (AstNode object : target.objects) {
                if (object instanceof ObjectLiteral literal) {
                    for (ObjectProperty property : literal.getElements()) {
                        if (key.equals(literalKey(property.getLeft()))) value.merge(resolve(property.getRight(), depth + 1, active));
                    }
                } else if (object instanceof ArrayLiteral array) {
                    if (key.equals("length")) value.numbers.add((double) array.getElements().size());
                    else {
                        try {
                            int index = Integer.parseInt(key);
                            if (index >= 0 && index < array.getElements().size()) value.merge(resolve(array.getElements().get(index), depth + 1, active));
                        } catch (NumberFormatException ignored) {
                            value.paths.add("$data." + key);
                        }
                    }
                }
            }
            if (Set.of("call", "apply", "bind").contains(key)
                    && (!target.functions.isEmpty() || !target.paths.isEmpty())) {
                value.functions.addAll(target.functions);
                value.invocations.add(key);
                value.boundArguments.addAll(target.boundArguments);
            }
            if (!target.texts.isEmpty() || !target.numbers.isEmpty()) value.paths.add("$data." + key);
            if (key.equals("length")) {
                if (target.arrayLength >= 0) value.numbers.add((double) target.arrayLength);
                if (target.textLength >= 0) value.numbers.add((double) target.textLength);
            }
            value.unknown = target.unknown;
            return value;
        }

        private Value callValue(FunctionCall call, int depth, Set<AstNode> active) {
            Value target = resolve(call.getTarget(), depth + 1, active);
            Value result = new Value();
            for (String raw : target.paths) {
                String path = stripInvocation(raw);
                if (target.invocations.contains("bind")) result.paths.add(path);
                else if (path.equals("pvputils.player.get")) result.paths.add("$player");
                else if (path.equals("$player.inventory.getMenu")) result.paths.add("$menu");
                else if (path.startsWith("pvputils.settings.")) result.paths.add("$setting");
                else if (path.startsWith("pvputils.player.") || path.startsWith("$player.")
                        || path.startsWith("$menu.") || path.equals("pvputils.storage.get")) result.paths.add("$data");
                else if (path.equals("Number") && call.getArguments().size() == 1) {
                    Value argument = resolve(call.getArguments().get(0), depth + 1, active);
                    result.numbers.addAll(argument.numbers);
                    for (String text : argument.texts) {
                        try {
                            result.numbers.add(Double.parseDouble(text));
                        } catch (NumberFormatException exception) {
                            result.numbers.add(Double.NaN);
                        }
                    }
                    if (result.numbers.isEmpty()) result.paths.add("$data");
                } else if (path.startsWith("Math.") && !call.getArguments().isEmpty()) {
                    List<Double> numeric = new ArrayList<>();
                    for (AstNode argument : call.getArguments()) {
                        Value value = resolve(argument, depth + 1, active);
                        if (value.numbers.size() != 1) break;
                        numeric.add(value.numbers.iterator().next());
                    }
                    if (numeric.size() == call.getArguments().size()) {
                        double a = numeric.get(0);
                        double b = numeric.size() > 1 ? numeric.get(1) : 0;
                        switch (last(path)) {
                            case "abs" -> result.numbers.add(Math.abs(a));
                            case "floor" -> result.numbers.add(Math.floor(a));
                            case "ceil" -> result.numbers.add(Math.ceil(a));
                            case "round" -> result.numbers.add(Math.floor(a + 0.5));
                            case "sqrt" -> result.numbers.add(Math.sqrt(a));
                            case "pow" -> result.numbers.add(Math.pow(a, b));
                            case "min" -> result.numbers.add(numeric.stream().mapToDouble(Double::doubleValue).min().orElse(a));
                            case "max" -> result.numbers.add(numeric.stream().mapToDouble(Double::doubleValue).max().orElse(a));
                            default -> result.paths.add("$data");
                        }
                    } else result.paths.add("$data");
                }
                else if (path.equals("String.fromCharCode") || path.equals("String.fromCodePoint")) {
                    StringBuilder decoded = new StringBuilder();
                    boolean constant = call.getArguments().size() <= 128;
                    for (AstNode argument : call.getArguments()) {
                        Value number = resolve(argument, depth + 1, active);
                        if (number.numbers.size() != 1) {
                            constant = false;
                            break;
                        }
                        double code = number.numbers.iterator().next();
                        if (!Double.isFinite(code) || code < 0 || code > 65535) {
                            constant = false;
                            break;
                        }
                        decoded.append((char) code);
                    }
                    if (constant) result.texts.add(decoded.toString());
                    else result.unknown = true;
                } else if (safeCall(path)) {
                    result.paths.add("$data");
                    estimateResult(call, path, result, depth, active);
                }
                else result.unknown = true;
            }
            if (target.invocations.contains("bind")) {
                result.functions.addAll(target.functions);
                result.boundArguments.addAll(arguments(call, target));
            }
            else for (FunctionNode function : target.functions) {
                for (AstNode returned : returns.getOrDefault(function, List.of())) result.merge(resolve(returned, depth + 1, active));
            }
            if (target.paths.isEmpty() && target.functions.isEmpty()) result.unknown = true;
            return result;
        }

        private void estimateResult(FunctionCall call, String path, Value result, int depth, Set<AstNode> active) {
            List<AstNode> arguments = call.getArguments();
            if (path.equals("Array") || path.equals("ArrayBuffer") || path.endsWith("Array")) {
                result.objects.add(call);
                if (arguments.size() == 1) {
                    Value size = resolve(arguments.get(0), depth + 1, active);
                    result.arrayLength = upperLength(size.numbers);
                } else result.arrayLength = arguments.size();
                return;
            }
            if (path.equals("Map") || path.equals("Set")) {
                result.objects.add(call);
                return;
            }
            AstNode receiverNode = call.getTarget() instanceof PropertyGet property ? property.getTarget()
                    : call.getTarget() instanceof ElementGet element ? element.getTarget() : null;
            if (receiverNode == null) return;
            Value receiver = resolve(receiverNode, depth + 1, active);
            String method = last(path);
            if (method.equals("fill")) {
                result.arrayLength = receiver.arrayLength;
                if (!arguments.isEmpty()) result.elementLength = resolve(arguments.get(0), depth + 1, active).textLength;
            } else if (method.equals("join")) {
                long separator = arguments.isEmpty() ? 1 : resolve(arguments.get(0), depth + 1, active).textLength;
                if (receiver.arrayLength >= 0 && receiver.elementLength >= 0 && separator >= 0) {
                    result.textLength = Math.min(1_000_000_000L, receiver.arrayLength * receiver.elementLength
                            + Math.max(0, receiver.arrayLength - 1) * separator);
                }
            } else if (method.equals("repeat") && !arguments.isEmpty()) {
                long count = upperLength(resolve(arguments.get(0), depth + 1, active).numbers);
                if (count >= 0 && receiver.textLength >= 0) result.textLength = Math.min(1_000_000_000L, count * receiver.textLength);
            } else if (Set.of("padStart", "padEnd").contains(method) && !arguments.isEmpty()) {
                result.textLength = Math.max(receiver.textLength, upperLength(resolve(arguments.get(0), depth + 1, active).numbers));
            } else if (Set.of("filter", "slice", "map", "sort", "reverse").contains(method)) {
                result.arrayLength = receiver.arrayLength;
                if (!method.equals("map")) result.elementLength = receiver.elementLength;
                result.textLength = receiver.textLength;
            } else if (Set.of("trim", "trimStart", "trimEnd", "toLowerCase", "toUpperCase", "substring", "substr").contains(method)) {
                result.textLength = receiver.textLength;
            }
        }

        private long upperLength(Set<Double> values) {
            return values.stream().filter(number -> Double.isFinite(number) && number >= 0)
                    .mapToLong(number -> (long) Math.min(1_000_000_000d, number)).max().orElse(-1);
        }

        private void numeric(Value result, Value left, Value right, int operator) {
            for (double a : left.numbers) for (double b : right.numbers) {
                result.numbers.add(switch (operator) {
                    case Token.ADD -> a + b;
                    case Token.SUB -> a - b;
                    case Token.MUL -> a * b;
                    case Token.DIV -> a / b;
                    default -> a % b;
                });
                if (result.numbers.size() > 128) throw new AuditLimitException();
            }
        }

        private void propagate(FunctionCall call) {
            Value target = resolve(call.getTarget());
            List<Value> arguments = arguments(call, target);
            FunctionNode caller = call.getEnclosingFunction();
            EnumSet<Phase> current = context(call);
            boolean loop = inLoop(call) || repeatedFunctions.contains(caller);
            boolean binding = target.invocations.contains("bind");
            for (FunctionNode function : target.functions) {
                if (!binding) {
                    changed |= phases.get(function).addAll(current);
                    edges.computeIfAbsent(caller, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(function);
                    if (loop) changed |= repeatedFunctions.add(function);
                }
                for (int index = 0; index < function.getParams().size() && index < arguments.size(); index++) {
                    seedPattern(function.getParams().get(index), arguments.get(index), function);
                }
            }
            if (binding) return;
            for (String raw : target.paths) {
                String path = stripInvocation(raw);
                if (path.equals("pvputils.events.on") && arguments.size() >= 2) {
                    Value event = arguments.get(0);
                    Value callback = arguments.get(1);
                    for (String name : event.texts) {
                        Phase phase;
                        try {
                            phase = Phase.valueOf(name.toUpperCase(java.util.Locale.ROOT));
                        } catch (IllegalArgumentException exception) {
                            phase = Phase.UNKNOWN;
                        }
                        register(callback, phase, phase == Phase.RENDER ? "$ctx" : null, call);
                    }
                    if (event.texts.isEmpty() || event.unknown || callback.functions.isEmpty()) {
                        add("DYNAMIC_CALLBACK", Risk.DYNAMIC_CODE, call, "事件或回调无法静态解析，无法验证其控制行为。",
                                "The event or callback cannot be resolved; its control behavior cannot be verified.");
                        register(callback, Phase.UNKNOWN, null, call);
                    }
                } else if ((path.equals("pvputils.hud.register") || path.equals("pvputils.ui.screen")) && !arguments.isEmpty()) {
                    Value definition = arguments.get(0);
                    boolean hud = path.equals("pvputils.hud.register");
                    Value layout = property(definition, "layout");
                    boolean local = !layout.objects.isEmpty();
                    if (hud && local) {
                        Value width = property(layout, "width");
                        Value height = property(layout, "height");
                        if (width.numbers.stream().anyMatch(number -> number >= 1024)
                                && height.numbers.stream().anyMatch(number -> number >= 512)) {
                            add("UI_LARGE_HUD_LAYOUT", Risk.UI_CONTROL, call, "HUD 布局占用大面积区域，局部坐标绘制也可能遮挡游戏画面。",
                                    "The HUD layout spans a large area; drawing in local coordinates may obscure the game view.");
                        }
                    }
                    register(property(definition, "render"), hud ? Phase.RENDER : Phase.SCREEN_RENDER, local ? "$ctxLocal" : "$ctx", call);
                    if (definition.objects.isEmpty() || property(definition, "render").functions.isEmpty()) {
                        add("DYNAMIC_CALLBACK", Risk.DYNAMIC_CODE, call, "HUD 或界面定义无法静态解析，需人工检查。",
                                "The HUD or screen definition cannot be resolved and needs manual review.");
                    }
                    if (!hud) {
                        register(property(definition, "onOpen"), Phase.ON_OPEN, null, call);
                        register(property(definition, "onClose"), Phase.ON_CLOSE, null, call);
                        for (String key : List.of("onMouseDown", "onMouseUp", "onDrag", "onScroll", "onKey")) register(property(definition, key), Phase.INPUT, null, call);
                    }
                } else if (path.startsWith("$ctx") && path.endsWith(".button") && arguments.size() > 6) {
                    register(arguments.get(6), Phase.INPUT, null, call);
                } else if (Set.of("forEach", "map", "filter", "reduce", "some", "every", "find", "sort").contains(last(path))) {
                    for (Value callback : arguments) {
                        for (FunctionNode function : callback.functions) {
                            changed |= phases.get(function).addAll(current);
                            changed |= repeatedFunctions.add(function);
                            edges.computeIfAbsent(caller, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(function);
                            if (!function.getParams().isEmpty()) seedPattern(function.getParams().get(0), Value.path("$data", false), function);
                        }
                    }
                }
            }
        }

        private Value property(Value target, String key) {
            return project(target, key, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        private List<Value> arguments(FunctionCall call, Value target) {
            List<Value> result = new ArrayList<>(target.boundArguments);
            if (target.invocations.contains("apply")) {
                if (call.getArguments().size() < 2) return result;
                Value array = resolve(call.getArguments().get(1));
                Value length = property(array, "length");
                int count = (int) length.numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                if (count < 0 || count > 128 || length.unknown || length.numbers.isEmpty()) {
                    add("DYNAMIC_ARGUMENTS", Risk.DYNAMIC_CODE, call, "apply 参数列表无法静态确认，可能隐藏受控接口调用。",
                            "The apply argument list cannot be resolved and may hide controlled API calls.");
                    return result;
                }
                for (int index = 0; index < count; index++) result.add(property(array, Integer.toString(index)));
            } else {
                int offset = target.invocations.contains("call") || target.invocations.contains("bind") ? 1 : 0;
                for (int index = offset; index < call.getArguments().size(); index++) result.add(resolve(call.getArguments().get(index)));
            }
            return result;
        }

        private void register(Value callback, Phase phase, String parameter, AstNode site) {
            for (FunctionNode function : callback.functions) {
                changed |= phases.get(function).add(phase);
                if (parameter != null && !function.getParams().isEmpty()) seedPattern(function.getParams().get(0), Value.path(parameter, false), function);
            }
            for (String raw : callback.paths) {
                String path = stripInvocation(raw);
                if ((path.startsWith("$player.") || path.startsWith("$menu.")) && ACTIONS.contains(last(path))) {
                    add("PLAYER_CONTROL_CALLBACK", Risk.PLAYER_CONTROL, site, "玩家或背包操作被直接注册为回调，可能持续控制玩家。",
                            "A player or inventory operation is registered as a callback and may repeatedly control the player.");
                }
                if ((path.equals("pvputils.ui.open") || path.equals("pvputils.ui.close")) && phase != Phase.INPUT) {
                    add("UI_LOCKOUT", Risk.UI_CONTROL, site, "界面切换被注册为自动回调，可能反复打开界面或干扰关闭。",
                            "UI switching is registered as an automatic callback and may repeatedly open screens or prevent closing.");
                }
                if (HOST_ROOTS.contains(first(path))) hostFinding(site);
            }
        }

        private void seedPattern(AstNode parameter, Value value, FunctionNode owner) {
            if (parameter instanceof Name name) {
                Value stored = seeds.computeIfAbsent(new Binding(owner, name.getIdentifier()), key -> new Value());
                changed |= stored.merge(value);
            } else if (parameter instanceof ObjectLiteral object) {
                for (ObjectProperty property : object.getElements()) {
                    String key = literalKey(property.getLeft());
                    if (key != null) seedPattern(property.getRight(), property(value, key), owner);
                }
            } else if (parameter instanceof ArrayLiteral array) {
                for (int index = 0; index < array.getElements().size(); index++) {
                    seedPattern(array.getElements().get(index), property(value, Integer.toString(index)), owner);
                }
            }
        }

        private EnumSet<Phase> context(AstNode node) {
            FunctionNode owner = node.getEnclosingFunction();
            if (owner == null) return EnumSet.of(Phase.TOP_LEVEL);
            EnumSet<Phase> value = phases.get(owner);
            return value == null ? EnumSet.noneOf(Phase.class) : EnumSet.copyOf(value);
        }

        private boolean frequent(AstNode node) {
            EnumSet<Phase> context = context(node);
            return inLoop(node) || repeatedFunctions.contains(node.getEnclosingFunction())
                    || context.contains(Phase.TICK) || context.contains(Phase.RENDER) || context.contains(Phase.SCREEN_RENDER);
        }

        private void inspect(AstNode node) {
            if (node instanceof ElementGet element) {
                Value key = resolve(element.getElement());
                Value target = resolve(element.getTarget());
                if (key.keys().isEmpty() || key.unknown) {
                    if (target.paths.stream().anyMatch(this::controlled) || isCalledTarget(node)) {
                        add("DYNAMIC_PROPERTY", Risk.DYNAMIC_CODE, node, "动态属性访问可能隐藏玩家、界面或宿主调用。",
                                "A dynamic property access may hide player, UI, or host calls.");
                    }
                }
                for (String name : key.keys()) inspectProperty(node, name);
            } else if (node instanceof PropertyGet property) {
                inspectProperty(node, property.getProperty().getIdentifier());
            } else if (node instanceof ObjectProperty property && (property.isGetterMethod() || property.isSetterMethod())) {
                add("HIDDEN_ACCESSOR", Risk.DYNAMIC_CODE, node, "属性访问器会在读取或赋值时隐式执行逻辑，需要检查其控制行为。",
                        "An accessor executes implicitly during reads or writes and requires review of its behavior.");
            } else if (node instanceof Loop loop && !bounded(loop)) {
                add("UNBOUNDED_LOOP", Risk.RESOURCE_USAGE, node, "循环次数无法静态确认，可能阻塞客户端线程。",
                        "The loop bound cannot be verified and may block the client thread.");
            } else if (node instanceof Loop loop && nestedIterations(loop) > 65536) {
                add("NESTED_LOOP_COST", Risk.RESOURCE_USAGE, node, "嵌套循环的组合次数过大，可能阻塞客户端线程。",
                        "The combined iteration count of nested loops is excessive and may block the client thread.");
            } else if (node instanceof StringLiteral literal && literal.getValue().length() > 16384) {
                add("LARGE_LITERAL", Risk.RESOURCE_USAGE, node, "超长字符串可能增加内存或内置运算消耗。",
                        "A large string may increase memory or native operation costs.");
            } else if (node instanceof ArrayLiteral literal && literal.getElements().size() > 4096) {
                add("LARGE_LITERAL", Risk.RESOURCE_USAGE, node, "大型数组可能增加内存与回调执行消耗。",
                        "A large array may increase memory and callback execution costs.");
            } else if (node instanceof RegExpLiteral literal) {
                inspectRegex(node, literal.getValue());
            } else if (node instanceof TaggedTemplateLiteral) {
                add("DYNAMIC_TEMPLATE", Risk.DYNAMIC_CODE, node, "带标签模板可间接执行函数，需检查其动态行为。",
                        "A tagged template invokes a function indirectly and needs review.");
            } else if (node.getType() == Token.WITH) {
                add("DYNAMIC_SCOPE", Risk.DYNAMIC_CODE, node, "with 动态作用域会隐藏接口和函数来源。",
                        "A with scope obscures API and function provenance.");
            }
            if (node instanceof Assignment assignment) {
                Value target = resolve(assignment.getLeft());
                if (frequent(node) && target.paths.contains("$setting.value")) {
                    add("PERSISTENCE_SPAM", Risk.RESOURCE_USAGE, node, "在循环或高频回调中修改设置会反复写入磁盘。",
                            "Changing a setting in a loop or frequent callback repeatedly writes to disk.");
                }
            }
            if (node instanceof UpdateExpression update && frequent(node)) {
                Value target = resolve(update.getOperand());
                if (target.paths.contains("$setting.value")) add("PERSISTENCE_SPAM", Risk.RESOURCE_USAGE, node,
                        "在循环或高频回调中修改设置会反复写入磁盘。", "Changing a setting in a loop or frequent callback repeatedly writes to disk.");
            }
            if (node instanceof Name name && !(name.getParent() instanceof PropertyGet property && property.getProperty() == name)
                    && HOST_ROOTS.contains(name.getIdentifier())) {
                Value value = resolve(name);
                if (value.paths.stream().anyMatch(path -> HOST_ROOTS.contains(first(path)))) hostFinding(node);
            }
        }

        private void inspectProperty(AstNode node, String key) {
            if (key.equals("constructor") || key.equals("__proto__") || key.equals("__parent__")) {
                add("PROTOTYPE_ESCAPE", Risk.DYNAMIC_CODE, node, "构造器或原型链访问可能用于动态执行或逃逸尝试。",
                        "Constructor or prototype traversal may attempt dynamic execution or escape.");
            }
        }

        private void inspectCall(FunctionCall call) {
            Value target = resolve(call.getTarget());
            if (target.invocations.contains("bind")) {
                for (String path : target.paths) inspectAllocation(call, stripInvocation(path));
                return;
            }
            boolean recognized = !target.functions.isEmpty();
            boolean dangerous = false;
            for (String raw : target.paths) {
                String path = stripInvocation(raw);
                String name = last(path);
                if (HOST_ROOTS.contains(first(path)) || HOST_METHODS.contains(name) && !safeCall(path)) {
                    hostFinding(call);
                    dangerous = true;
                }
                if (path.equals("eval") || path.equals("Function") || name.equals("constructor")
                        || path.equals("Reflect.construct")) {
                    add("DYNAMIC_EXECUTION", Risk.DYNAMIC_CODE, call, "动态代码执行无法通过静态审计确认其行为。",
                            "Dynamic code execution cannot be verified by static analysis.");
                    dangerous = true;
                }
                if (Set.of("atob", "unescape", "String.fromCharCode", "String.fromCodePoint").contains(path)) {
                    if (frequent(call) || call.getArguments().size() > 128 || resolve(call).texts.isEmpty()) {
                        add("OBFUSCATED_CODE", Risk.DYNAMIC_CODE, call, "动态解码或字符拼装可能隐藏接口调用或代码。",
                                "Dynamic decoding or character assembly may conceal API calls or code.");
                    }
                    recognized = true;
                }
                if (path.equals("Object.defineProperty") || path.equals("Object.defineProperties")
                        || path.equals("Object.setPrototypeOf") || name.equals("__defineGetter__") || name.equals("__defineSetter__")
                        || path.equals("Proxy")) {
                    add("HIDDEN_ACCESSOR", Risk.DYNAMIC_CODE, call, "动态属性或原型定义可能隐藏执行行为。",
                            "Dynamic property or prototype definitions may hide execution.");
                    recognized = true;
                }
                if ((path.startsWith("$player.") || path.startsWith("$menu.")) && ACTIONS.contains(name)) {
                    add(frequent(call) ? "CONTINUOUS_PLAYER_CONTROL" : "PLAYER_MUTATION", Risk.PLAYER_CONTROL, call,
                            frequent(call) ? "循环或高频回调可持续控制玩家、快捷栏或容器操作。" : "脚本会修改玩家状态、使用物品、交互或操作背包。",
                            frequent(call) ? "A loop or frequent callback can continuously control the player, hotbar, or inventory."
                                    : "The script changes player state, uses items, interacts, or manipulates inventory.");
                    if (name.equals("click") && call.getArguments().size() >= 3 && resolve(call.getArguments().get(2)).texts.contains("throw")) {
                        add("INVENTORY_DISCARD", Risk.PLAYER_CONTROL, call, "容器 throw 操作会尝试丢弃物品，循环调用可丢弃大量背包物品。",
                                "The inventory throw operation attempts to discard items; repeated calls can discard many items.");
                    }
                    recognized = true;
                }
                if (path.equals("pvputils.ui.open") || path.equals("pvputils.ui.close")) {
                    EnumSet<Phase> context = context(call);
                    boolean risky = frequent(call) || context.contains(Phase.JOIN) || context.contains(Phase.LEAVE)
                            || context.contains(Phase.ON_CLOSE) || context.contains(Phase.ON_OPEN) || context.contains(Phase.UNLOAD);
                    if (risky) {
                        add("UI_LOCKOUT", Risk.UI_CONTROL, call, "界面切换位于循环、高频或生命周期回调，可能反复打开界面或干扰关闭。",
                                "UI switching in a loop, frequent callback, or lifecycle callback may repeatedly open screens or interfere with closing.");
                    } else if (path.endsWith(".open") && !context.equals(EnumSet.of(Phase.INPUT))) {
                        add("AUTOMATIC_UI_OPEN", Risk.UI_CONTROL, call, "脚本可自动打开界面；无法静态确认所有触发路径。",
                                "The script can open a screen automatically; all triggering paths cannot be statically verified.");
                    }
                    recognized = true;
                }
                if (path.equals("pvputils.storage.set") && frequent(call)) {
                    add("PERSISTENCE_SPAM", Risk.RESOURCE_USAGE, call, "循环或高频回调中的持久化写入可能刷盘并阻塞客户端。",
                            "Persistent writes in a loop or frequent callback may thrash disk and block the client.");
                }
                if ((path.equals("pvputils.notify.show") || path.equals("pvputils.log")) && frequent(call)) {
                    add("MESSAGE_SPAM", Risk.RESOURCE_USAGE, call, "循环或高频回调可能持续刷通知或日志。",
                            "A loop or frequent callback may continuously spam notifications or logs.");
                }
                if (path.startsWith("$ctx") && DRAWS.contains(name)) {
                    inspectDraw(call, name, path.startsWith("$ctxLocal"));
                    recognized = true;
                }
                if (path.equals("RegExp")) {
                    if (call.getArguments().isEmpty()) recognized = true;
                    else {
                        Value pattern = resolve(call.getArguments().get(0));
                        for (String text : pattern.texts) inspectRegex(call, text);
                        if (pattern.texts.isEmpty() || pattern.unknown) add("DYNAMIC_REGEX", Risk.RESOURCE_USAGE, call,
                                "动态正则表达式可能触发高开销回溯，需人工检查。", "A dynamic regular expression may cause costly backtracking and needs review.");
                    }
                }
                if (path.startsWith("$data") && Set.of("match", "matchAll", "search").contains(name) && !call.getArguments().isEmpty()) {
                    Value pattern = resolve(call.getArguments().get(0));
                    for (String text : pattern.texts) inspectRegex(call, text);
                    if (pattern.unknown || pattern.texts.isEmpty() && !(call.getArguments().get(0) instanceof RegExpLiteral)) {
                        add("DYNAMIC_REGEX", Risk.RESOURCE_USAGE, call, "字符串匹配的动态正则表达式可能触发高开销回溯。",
                                "A dynamic pattern used for string matching may cause costly regex backtracking.");
                    }
                }
                if (Set.of("push", "unshift", "splice", "set", "add").contains(name) && frequent(call)) {
                    Value receiver = receiver(call.getTarget());
                    if (receiver.objects.stream().anyMatch(object -> object.getEnclosingFunction() != call.getEnclosingFunction())) {
                        add("PERSISTENT_COLLECTION_GROWTH", Risk.RESOURCE_USAGE, call, "高频回调持续扩充外部集合，可能不断累积内存。",
                                "A frequent callback grows an outer collection and may accumulate memory indefinitely.");
                    }
                }
                inspectAllocation(call, path);
                recognized |= safeCall(path) || path.startsWith("pvputils.") || path.startsWith("$player.") || path.startsWith("$menu.");
            }
            if ((!recognized || target.unknown) && !dangerous && !terminal(call.getTarget()).equals("bind")) {
                add("UNRESOLVED_CALL", Risk.DYNAMIC_CODE, call, "调用目标无法完整静态解析，可能包含隐藏的控制或动态行为。",
                        "The call target cannot be fully resolved and may contain hidden control or dynamic behavior.");
            }
        }

        private void inspectAllocation(FunctionCall call, String path) {
            String name = last(path);
            List<Value> arguments = arguments(call, resolve(call.getTarget()));
            if ((path.equals("Array") || path.endsWith("Array") || path.equals("ArrayBuffer")) && arguments.size() == 1) {
                Value size = arguments.get(0);
                if (size.numbers.stream().anyMatch(value -> !Double.isFinite(value) || value > 65536)
                        || size.unknown || size.numbers.isEmpty() && size.objects.isEmpty()) {
                    add("LARGE_ALLOCATION", Risk.RESOURCE_USAGE, call, "数组或缓冲区分配大小很大或无法确认，可能耗尽内存。",
                            "An array or buffer allocation is large or unverified and may exhaust memory.");
                }
            }
            if (Set.of("repeat", "padStart", "padEnd").contains(name) && !arguments.isEmpty()) {
                Value size = arguments.get(0);
                Value receiver = receiver(call.getTarget());
                long length = Math.max(1, receiver.textLength);
                if (size.numbers.isEmpty() || size.unknown || size.numbers.stream().anyMatch(value -> !Double.isFinite(value)
                        || value > 16384 || name.equals("repeat") && value * length > 65536)) {
                    add("LARGE_ALLOCATION", Risk.RESOURCE_USAGE, call, "字符串扩张大小很大或无法确认，内置操作可能绕过指令预算。",
                            "String expansion is large or unverified; native operations may bypass instruction accounting.");
                }
            }
            Value result = resolve(call);
            if (result.textLength > 65536 || result.arrayLength > 65536
                    || result.arrayLength >= 0 && result.elementLength >= 0 && result.arrayLength * result.elementLength > 1048576) {
                add("LARGE_ALLOCATION", Risk.RESOURCE_USAGE, call, "数组、字符串或连续扩张操作可能分配大量内存。",
                        "Array, string, or chained expansion operations may allocate excessive memory.");
            }
            if (path.equals("JSON.parse") && !call.getArguments().isEmpty()) {
                Value text = resolve(call.getArguments().get(0));
                if (text.texts.stream().anyMatch(value -> value.length() > 65536) || frequent(call) && (text.unknown || text.texts.isEmpty())) {
                    add("NATIVE_PROCESSING_COST", Risk.RESOURCE_USAGE, call, "大型或高频 JSON 解析可能在内置操作中消耗大量时间与内存。",
                            "Large or frequent JSON parsing may consume substantial time and memory in native operations.");
                }
            }
        }

        private Value receiver(AstNode target) {
            if (target instanceof PropertyGet property) return resolve(property.getTarget());
            if (target instanceof ElementGet element) return resolve(element.getTarget());
            return new Value();
        }

        private void inspectDraw(FunctionCall call, String method, boolean local) {
            List<Value> arguments = arguments(call, resolve(call.getTarget()));
            for (Value value : arguments) {
                if (value.numbers.stream().anyMatch(number -> !Double.isFinite(number) || Math.abs(number) > 100000)) {
                    add("INVALID_UI_BOUNDS", Risk.UI_CONTROL, call, "绘制参数包含非有限值或超大坐标，可能导致异常或干扰显示。",
                            "Drawing arguments contain non-finite values or excessive coordinates and may fail or disrupt display.");
                }
            }
            int width = method.equals("texture") ? 3 : method.equals("button") ? 4 : 2;
            int height = width + 1;
            boolean rectangular = Set.of("rect", "roundedRect", "outline", "gradient", "gradientDiagonal", "shadow", "texture", "blur", "glass", "button").contains(method);
            if (rectangular && arguments.size() > height) {
                Value w = arguments.get(width);
                Value h = arguments.get(height);
                if (w.numbers.stream().anyMatch(number -> number > 4096 || number < 0)
                        || h.numbers.stream().anyMatch(number -> number > 4096 || number < 0)) {
                    add("INVALID_UI_BOUNDS", Risk.UI_CONTROL, call, "图形或按钮尺寸超出受控绘制范围。", "Graphic or button dimensions exceed controlled drawing bounds.");
                }
                boolean fullWidth = w.paths.stream().anyMatch(path -> path.endsWith(".screenWidth") || !local && path.endsWith(".width"));
                boolean fullHeight = h.paths.stream().anyMatch(path -> path.endsWith(".screenHeight") || !local && path.endsWith(".height"));
                double scale = drawScale(call);
                boolean large = w.numbers.stream().anyMatch(number -> number * scale >= 1024)
                        && h.numbers.stream().anyMatch(number -> number * scale >= 512);
                boolean screenRender = context(call).contains(Phase.SCREEN_RENDER);
                if ((fullWidth && fullHeight || large) && (method.equals("button")
                        || !screenRender && (opaque(arguments) || method.equals("texture")))) {
                    add(method.equals("button") ? "UI_INPUT_BLOCK" : "UI_SCREEN_COVER", Risk.UI_CONTROL, call,
                            method.equals("button") ? "大面积按钮可覆盖其他控件并消费鼠标输入。" : "大面积不透明 HUD 可能遮挡游戏画面。",
                            method.equals("button") ? "A large button may cover controls and consume mouse input." : "A large opaque HUD may obscure the game view.");
                }
            }
            if (method.equals("circle") && arguments.size() > 2 && !context(call).contains(Phase.SCREEN_RENDER)
                    && arguments.get(2).numbers.stream().anyMatch(radius -> radius * drawScale(call) >= 512) && opaque(arguments)) {
                add("UI_SCREEN_COVER", Risk.UI_CONTROL, call, "大面积不透明圆形 HUD 可能遮挡游戏画面。",
                        "A large opaque circular HUD may obscure the game view.");
            }
            if (method.equals("scale") && arguments.stream().anyMatch(value -> value.unknown || value.numbers.isEmpty()
                    || value.numbers.stream().anyMatch(number -> number > 8 || number <= 0
                    || number != 1 && (inLoop(call) || repeatedFunctions.contains(call.getEnclosingFunction()))))) {
                add("INVALID_UI_BOUNDS", Risk.UI_CONTROL, call, "极端、重复或无法确认的缩放可放大遮挡区域或造成异常绘制。",
                        "Extreme, repeated, or unresolved scaling may amplify screen coverage or disrupt drawing.");
            }
            if (Set.of("blur", "glass", "shadow").contains(method) && (inLoop(call) || repeatedFunctions.contains(call.getEnclosingFunction()))) {
                add("EXPENSIVE_DRAW_LOOP", Risk.RESOURCE_USAGE, call, "循环中的滤镜或液态玻璃可能反复执行高开销原生绘制。",
                        "Filters or glass in a loop may repeatedly invoke expensive native drawing.");
            }
        }

        private double drawScale(FunctionCall target) {
            double scale = 1;
            ArrayDeque<Double> saved = new ArrayDeque<>();
            for (FunctionCall call : calls) {
                checkpoint();
                if (call == target) break;
                if (call.getEnclosingFunction() != target.getEnclosingFunction()) continue;
                Value value = resolve(call.getTarget());
                for (String raw : value.paths) {
                    String path = stripInvocation(raw);
                    if (!path.startsWith("$ctx")) continue;
                    if (last(path).equals("save")) saved.push(scale);
                    else if (last(path).equals("restore") && !saved.isEmpty()) scale = saved.pop();
                    else if (last(path).equals("scale")) {
                        double multiplier = 1;
                        for (Value argument : arguments(call, value)) {
                            multiplier = Math.max(multiplier, argument.numbers.stream().mapToDouble(Math::abs).max().orElse(1));
                        }
                        scale = Math.min(1_000_000, scale * multiplier);
                    }
                }
            }
            return scale;
        }

        private boolean opaque(List<Value> arguments) {
            for (Value argument : arguments) for (String text : argument.texts) {
                if (text.length() == 7 && text.charAt(0) == '#') return true;
                if (text.length() == 9 && text.charAt(0) == '#') {
                    try {
                        if (Integer.parseInt(text.substring(1, 3), 16) >= 128) return true;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return false;
        }

        private boolean bounded(Loop loop) {
            if (loop instanceof ForInLoop iteration) {
                Value value = resolve(iteration.getIteratedObject());
                return !value.unknown && (value.paths.stream().anyMatch(path -> path.startsWith("$data")) || !value.objects.isEmpty())
                        && !mutatesCollection(iteration.getBody(), value);
            }
            if (!(loop instanceof ForLoop iteration) || !(iteration.getCondition() instanceof InfixExpression condition)) return false;
            if (!Set.of(Token.LT, Token.LE, Token.GT, Token.GE).contains(condition.getOperator()) || !(condition.getLeft() instanceof Name variable)) return false;
            Value bound = resolve(condition.getRight());
            Value start = loopStart(iteration.getInitializer(), variable.getIdentifier());
            if (start.unknown || start.numbers.isEmpty() || start.numbers.stream().anyMatch(number -> !Double.isFinite(number) || Math.abs(number) > 4096)
                    || writesName(iteration.getBody(), variable.getIdentifier())) return false;
            boolean small = !bound.unknown && !bound.numbers.isEmpty()
                    && bound.numbers.stream().allMatch(number -> Double.isFinite(number) && Math.abs(number) <= 4096
                    && start.numbers.stream().allMatch(initial -> Math.abs(number - initial) <= 4096));
            boolean dataBound = bound.paths.stream().anyMatch(path -> path.startsWith("$data") && path.endsWith(".length"));
            if (condition.getRight() instanceof Name name && writesName(iteration.getBody(), name.getIdentifier())) return false;
            if (dataBound && mutatesCollection(iteration.getBody(), receiver(condition.getRight()))) return false;
            AstNode increment = iteration.getIncrement();
            boolean increasing = condition.getOperator() == Token.LT || condition.getOperator() == Token.LE;
            if (increment instanceof UpdateExpression update && update.getOperand() instanceof Name name
                    && name.getIdentifier().equals(variable.getIdentifier())) {
                return (small || dataBound) && (increasing ? update.getOperator() == Token.INC : update.getOperator() == Token.DEC);
            }
            return false;
        }

        private Value loopStart(AstNode initializer, String name) {
            if (initializer instanceof VariableDeclaration declaration) {
                for (VariableInitializer variable : declaration.getVariables()) {
                    if (variable.getTarget() instanceof Name target && target.getIdentifier().equals(name)) return resolve(variable.getInitializer());
                }
            }
            if (initializer instanceof Assignment assignment && assignment.getLeft() instanceof Name target
                    && target.getIdentifier().equals(name)) return resolve(assignment.getRight());
            return Value.unknown();
        }

        private double nestedIterations(Loop loop) {
            double count = 1;
            for (AstNode node = loop; node != null && !(node instanceof FunctionNode); node = node.getParent()) {
                if (node instanceof ForLoop iteration && iteration.getCondition() instanceof InfixExpression condition
                        && condition.getLeft() instanceof Name variable) {
                    Value start = loopStart(iteration.getInitializer(), variable.getIdentifier());
                    Value end = resolve(condition.getRight());
                    double maximum = 1;
                    for (double a : start.numbers) for (double b : end.numbers) maximum = Math.max(maximum, Math.abs(a - b) + 1);
                    count = Math.min(1_000_000_000, count * maximum);
                }
            }
            return count;
        }

        private boolean writesName(AstNode body, String name) {
            boolean[] writes = {false};
            body.visit(node -> {
                checkpoint();
                AstNode target = node instanceof Assignment assignment ? assignment.getLeft()
                        : node instanceof UpdateExpression update ? update.getOperand() : null;
                if (target instanceof Name variable && variable.getIdentifier().equals(name)) writes[0] = true;
                return !writes[0];
            });
            return writes[0];
        }

        private boolean mutatesCollection(AstNode body, Value collection) {
            boolean[] mutates = {false};
            body.visit(node -> {
                checkpoint();
                Value receiver = null;
                if (node instanceof FunctionCall call && Set.of("push", "unshift", "splice").contains(terminal(call.getTarget()))) {
                    receiver = receiver(call.getTarget());
                } else if (node instanceof Assignment assignment) {
                    receiver = receiver(assignment.getLeft());
                }
                if (receiver != null && (receiver.objects.stream().anyMatch(collection.objects::contains)
                        || receiver.paths.stream().anyMatch(collection.paths::contains))) mutates[0] = true;
                return !mutates[0];
            });
            return mutates[0];
        }

        private void inspectRegex(AstNode node, String pattern) {
            if (pattern.length() > 4096) {
                add("REGEX_BACKTRACKING", Risk.RESOURCE_USAGE, node, "大型正则表达式可能触发高开销编译或匹配。", "A large regular expression may require costly compilation or matching.");
                return;
            }
            ArrayDeque<RegexGroup> groups = new ArrayDeque<>();
            groups.push(new RegexGroup());
            boolean characterClass = false;
            boolean risky = false;
            for (int index = 0; index < pattern.length(); index++) {
                char current = pattern.charAt(index);
                if (current == '\\') {
                    if (index + 1 < pattern.length() && Character.isDigit(pattern.charAt(index + 1))) risky = true;
                    index++;
                    continue;
                }
                if (current == '[') characterClass = true;
                if (current == ']') characterClass = false;
                if (characterClass) continue;
                if (current == '(') groups.push(new RegexGroup());
                else if (current == ')' && groups.size() > 1) {
                    RegexGroup closed = groups.pop();
                    boolean quantified = index + 1 < pattern.length() && "+*{?".indexOf(pattern.charAt(index + 1)) >= 0;
                    if (quantified && (closed.quantified || closed.alternative)) risky = true;
                    groups.peek().quantified |= closed.quantified || quantified;
                } else if (current == '|') groups.peek().alternative = true;
                else if (current == '+' || current == '*' || current == '{') groups.peek().quantified = true;
            }
            if (risky) add("REGEX_BACKTRACKING", Risk.RESOURCE_USAGE, node, "嵌套重复、重复分支或反向引用可能触发正则高开销回溯。",
                    "Nested repetition, repeated alternatives, or backreferences may cause costly regex backtracking.");
        }

        private void inspectRecursion() {
            for (FunctionNode function : functions) {
                Set<FunctionNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                ArrayDeque<FunctionNode> pending = new ArrayDeque<>(edges.getOrDefault(function, Set.of()));
                while (!pending.isEmpty()) {
                    checkpoint();
                    FunctionNode next = pending.removeFirst();
                    if (next == function) {
                        add("RECURSIVE_CALL", Risk.RESOURCE_USAGE, function, "直接或间接递归可能耗尽调用栈或阻塞客户端线程。",
                                "Direct or indirect recursion may exhaust the stack or block the client thread.");
                        break;
                    }
                    if (seen.add(next)) pending.addAll(edges.getOrDefault(next, Set.of()));
                }
            }
        }

        private boolean controlled(String path) {
            return path.startsWith("pvputils") || path.startsWith("$player") || path.startsWith("$menu") || path.startsWith("$ctx") || HOST_ROOTS.contains(first(path));
        }

        private void hostFinding(AstNode node) {
            add("HOST_OR_DISCONNECT_ATTEMPT", Risk.HOST_ACCESS, node, "发现断连、退出、Java/反射、进程、文件或网络访问尝试；这些不是公开插件接口。",
                    "A disconnect, exit, Java/reflection, process, file, or network access attempt was found; these are not public plugin APIs.");
        }

        private void add(String id, Risk risk, AstNode node, String zh, String en) {
            int line = Math.max(1, node.getLineno());
            String key = id + ":" + line;
            if (!findings.containsKey(key)) {
                if (findings.size() >= MAX_FINDINGS) throw new AuditLimitException();
                findings.put(key, new Finding(id, risk, line, zh, en));
            }
        }
    }

    private static boolean safeCall(String path) {
        if (BUILTINS.contains(first(path)) && !path.startsWith("pvputils")) return true;
        if (path.startsWith("$data") && DATA_METHODS.contains(last(path))) return true;
        return path.startsWith("$setting") && last(path).equals("value");
    }

    private static boolean inLoop(AstNode node) {
        for (AstNode parent = node.getParent(); parent != null && !(parent instanceof FunctionNode); parent = parent.getParent()) {
            if (parent instanceof Loop) return true;
        }
        return false;
    }

    private static boolean isCalledTarget(AstNode node) {
        AstNode current = node;
        while (current.getParent() instanceof ParenthesizedExpression) current = current.getParent();
        return current.getParent() instanceof FunctionCall call && call.getTarget() == current;
    }

    private static String rawName(AstNode node) {
        if (node instanceof Name name) return name.getIdentifier();
        if (node instanceof PropertyGet property) {
            String target = rawName(property.getTarget());
            return target == null ? null : target + "." + property.getProperty().getIdentifier();
        }
        return null;
    }

    private static String literalKey(AstNode node) {
        if (node instanceof Name name) return name.getIdentifier();
        if (node instanceof StringLiteral text) return text.getValue();
        if (node instanceof NumberLiteral number) return Long.toString((long) number.getNumber());
        return null;
    }

    private static String terminal(AstNode node) {
        if (node instanceof PropertyGet property) return property.getProperty().getIdentifier();
        if (node instanceof Name name) return name.getIdentifier();
        return "";
    }

    private static String stripInvocation(String path) {
        while (path.endsWith(".call") || path.endsWith(".apply") || path.endsWith(".bind")) path = path.substring(0, path.lastIndexOf('.'));
        return path;
    }

    private static String first(String path) {
        int split = path.indexOf('.');
        return split < 0 ? path : path.substring(0, split);
    }

    private static String last(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private record Binding(FunctionNode owner, String name) {}
    private record Reference(AstNode node, List<String> projection) {}

    private static final class Value {
        private final Set<String> paths = new LinkedHashSet<>();
        private final Set<String> texts = new LinkedHashSet<>();
        private final Set<Double> numbers = new LinkedHashSet<>();
        private final Set<FunctionNode> functions = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<AstNode> objects = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<String> invocations = new LinkedHashSet<>();
        private final List<Value> boundArguments = new ArrayList<>();
        private long textLength = -1;
        private long arrayLength = -1;
        private long elementLength = -1;
        private boolean unknown;

        private static Value unknown() {
            Value value = new Value();
            value.unknown = true;
            return value;
        }

        private static Value path(String path, boolean unknown) {
            Value value = new Value();
            value.paths.add(path);
            value.unknown = unknown;
            return value;
        }

        private static Value text(String text) {
            Value value = new Value();
            value.texts.add(text);
            value.textLength = text.length();
            return value;
        }

        private static Value number(double number) {
            Value value = new Value();
            value.numbers.add(number);
            return value;
        }

        private static Value function(FunctionNode function) {
            Value value = new Value();
            value.functions.add(function);
            return value;
        }

        private static Value object(AstNode object) {
            Value value = new Value();
            value.objects.add(object);
            if (object instanceof ArrayLiteral array) value.arrayLength = array.getElements().size();
            return value;
        }

        private boolean merge(Value value) {
            boolean changed = paths.addAll(value.paths);
            changed |= texts.addAll(value.texts);
            changed |= numbers.addAll(value.numbers);
            changed |= functions.addAll(value.functions);
            changed |= objects.addAll(value.objects);
            changed |= invocations.addAll(value.invocations);
            if (value.boundArguments.size() > 128) throw new AuditLimitException();
            for (int index = 0; index < value.boundArguments.size(); index++) {
                if (index >= boundArguments.size()) {
                    boundArguments.add(new Value());
                    changed = true;
                }
                changed |= boundArguments.get(index).merge(value.boundArguments.get(index));
            }
            changed |= value.textLength > textLength || value.arrayLength > arrayLength || value.elementLength > elementLength;
            textLength = Math.max(textLength, value.textLength);
            arrayLength = Math.max(arrayLength, value.arrayLength);
            elementLength = Math.max(elementLength, value.elementLength);
            changed |= !unknown && value.unknown;
            unknown |= value.unknown;
            if (paths.size() + texts.size() + numbers.size() + functions.size() + objects.size() > 128) throw new AuditLimitException();
            return changed;
        }

        private Set<String> keys() {
            Set<String> keys = new LinkedHashSet<>(texts);
            for (double number : numbers) if (Double.isFinite(number) && number == Math.rint(number)) keys.add(Long.toString((long) number));
            return keys;
        }
    }

    private static final class RegexGroup {
        private boolean quantified;
        private boolean alternative;
    }

    private static final class AuditLimitException extends RuntimeException {}
}
