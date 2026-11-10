package com.pvp_utils.compatibility;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Bytecode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class MixinConflictDetector {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String OVERWRITE = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String FINAL = "Lorg/spongepowered/asm/mixin/Final;";
    private static final String GROUP = "Lorg/spongepowered/asm/mixin/injection/Group;";
    private static final Set<String> INJECTORS = Set.of("Inject", "Redirect", "ModifyArg", "ModifyArgs",
            "ModifyConstant", "ModifyVariable", "ModifyExpressionValue", "ModifyReturnValue",
            "WrapOperation", "WrapWithCondition", "WrapWithConditionV2", "WrapMethod");

    private MixinConflictDetector() {
    }

    public static Report inspect(List<Definition> definitions, ClassProvider provider) {
        long started = System.nanoTime();
        Map<String, List<Definition>> targets = new LinkedHashMap<>();
        Set<String> ownTargets = new LinkedHashSet<>();
        for (Definition definition : definitions) {
            if (definition.mod().own()) {
                ownTargets.addAll(definition.targets());
            }
        }
        for (Definition definition : definitions) {
            for (String target : definition.targets()) {
                if (ownTargets.contains(target)) {
                    targets.computeIfAbsent(target, ignored -> new ArrayList<>()).add(definition);
                }
            }
        }
        List<Evidence> evidence = new ArrayList<>();
        Set<String> notes = new LinkedHashSet<>();
        int inspected = 0;
        for (Map.Entry<String, List<Definition>> entry : targets.entrySet()) {
            List<Definition> mixins = entry.getValue();
            if (mixins.stream().noneMatch(definition -> !definition.mod().own())) {
                continue;
            }
            String target = entry.getKey();
            if (mixins.stream().anyMatch(Definition::mutatesTarget)) {
                notes.add(target + ": a config plugin can modify the target bytecode; no automatic conflict verdict");
                continue;
            }
            try {
                ClassNode original = provider.read(target);
                if (original == null) {
                    notes.add(target + ": target bytecode unavailable; no automatic conflict verdict");
                    continue;
                }
                mixins.sort(Comparator.comparingInt(Definition::priority).thenComparingInt(Definition::order));
                inspectTarget(original, mixins, evidence, notes);
                inspected++;
            } catch (Exception | LinkageError exception) {
                notes.add(target + ": analysis incomplete (" + exception.getClass().getSimpleName() + ")");
            }
        }
        List<Evidence> distinct = evidence.stream().distinct().toList();
        return new Report(distinct, List.copyOf(notes), definitions.size(), ownTargets.size(), inspected,
                (System.nanoTime() - started) / 1_000_000L);
    }

    private static void inspectTarget(ClassNode target, List<Definition> mixins, List<Evidence> evidence,
                                      Set<String> notes) {
        for (Definition definition : mixins) {
            for (MethodNode method : definition.bytecode().methods) {
                boolean existing = target.methods.stream().anyMatch(original -> original.name.equals(method.name)
                        && original.desc.equals(method.desc));
                if (existing && annotation(method, OVERWRITE) == null && injectorAnnotation(method) == null
                        && annotation(method, "Lorg/spongepowered/asm/mixin/Shadow;") == null
                        && annotation(method, "Lorg/spongepowered/asm/mixin/Unique;") == null
                        && annotation(method, "Lorg/spongepowered/asm/mixin/Intrinsic;") == null
                        && annotation(method, "Lorg/spongepowered/asm/mixin/gen/Accessor;") == null
                        && annotation(method, "Lorg/spongepowered/asm/mixin/gen/Invoker;") == null
                        && !method.name.startsWith("<")) {
                    notes.add(target.name + ": an implicit method merge cannot be verified automatically");
                    return;
                }
            }
        }
        List<Body> originals = target.methods.stream().map(method -> new Body(method, null)).toList();
        List<Body> combined = effectiveBodies(target, originals, mixins, notes);
        List<Body> ownBodies = effectiveBodies(target, originals,
                mixins.stream().filter(definition -> definition.mod().own()).toList(), notes);
        List<Body> foreignBodies = effectiveBodies(target, originals,
                mixins.stream().filter(definition -> !definition.mod().own()).toList(), notes);
        if (combined == null || ownBodies == null || foreignBodies == null) return;
        List<ResolvedInjection> analyzed = new ArrayList<>();
        List<ResolvedInjection> exclusiveInjections = new ArrayList<>();
        for (Definition definition : mixins) {
            for (MethodNode method : definition.bytecode().methods) {
                AnnotationNode injector = injectorAnnotation(method);
                if (injector == null) continue;
                AnnotationNode group = annotation(method, GROUP);
                int required = integer(injector, "require", -1);
                if (required < 0) required = group == null ? definition.defaultRequire() : 0;
                if (group != null) {
                    notes.add(definition.className() + "#" + method.name + ": grouped injection counts are not verified");
                }
                if (value(injector, "expect") != null) {
                    notes.add(definition.className() + "#" + method.name + ": debug expect counts are not verified");
                }
                Injection injection = new Injection(definition, method, injector, Math.max(0, required));
                Resolution control = resolve(target, definition.mod().own() ? ownBodies : foreignBodies, injection);
                Resolution modified = resolve(target, combined, injection);
                if (!control.complete() || !modified.complete()) {
                    notes.add(definition.className() + "#" + method.name
                            + ": injection selector is outside the automatic verifier's supported subset");
                    if (exclusive(injector)) return;
                    continue;
                }
                ResolvedInjection resolved = new ResolvedInjection(injection, modified, control);
                analyzed.add(resolved);
                if (exclusive(injector)) exclusiveInjections.add(resolved);
            }
        }
        Ownership ownControls = redirectOwners(exclusiveInjections.stream()
                .filter(injection -> injection.injection().definition().mod().own()).toList(), true);
        Ownership foreignControls = redirectOwners(exclusiveInjections.stream()
                .filter(injection -> !injection.injection().definition().mod().own()).toList(), true);
        for (ResolvedInjection resolved : exclusiveInjections) {
            Ownership controls = resolved.injection().definition().mod().own() ? ownControls : foreignControls;
            if (!succeedsAlone(resolved, controls)) {
                notes.add(target.name + ": a required replacement injection already fails without the opposite side; no automatic conflict verdict");
                return;
            }
        }
        Ownership combinedOwners = redirectOwners(exclusiveInjections, false);
        for (ResolvedInjection resolved : analyzed) {
            Injection injection = resolved.injection();
            Ownership controls = injection.definition().mod().own() ? ownControls : foreignControls;
            if (!succeedsAlone(resolved, controls)) continue;
            for (Restriction restriction : restrictions(resolved.resolution(), injection)) {
                Definition writer = restriction.body().writer();
                if (injection.definition().requiredConfig() && writer.mod().own() != injection.definition().mod().own()) {
                    evidence.add(evidence(restriction.kind(), target.name, signature(restriction.body().method()),
                            injection, writer, 0, description(injection.annotation())));
                }
            }
            if (injection.required() > 0 && resolved.resolution().nodes().size() < injection.required()) {
                for (Body body : resolved.resolution().selectedBodies()) {
                    Definition writer = body.writer();
                    String method = signature(body.method());
                    long before = resolved.control().nodes().stream().filter(node -> node.method().equals(method)).count();
                    long after = resolved.resolution().nodes().stream().filter(node -> node.method().equals(method)).count();
                    if (writer != null && before > after && writer.mod().own() != injection.definition().mod().own()) {
                        evidence.add(evidence(Kind.OVERWRITE_REMOVES_REQUIRED_TARGET, target.name, method,
                                injection, writer, resolved.resolution().nodes().size(), description(injection.annotation())));
                    }
                }
            }
            if (!exclusive(injection.annotation()) || !restrictions(resolved.resolution(), injection).isEmpty()) continue;
            for (FinalCollision collision : combinedOwners.finalCollisions()) {
                if (collision.losing() == resolved && injection.definition().requiredConfig()
                        && collision.incumbent().injection().definition().mod().own() != injection.definition().mod().own()) {
                    evidence.add(evidence(Kind.FINAL_EXCLUSIVE_COLLISION, target.name, collision.node().method(),
                            injection, collision.incumbent().injection().definition(), 0, description(injection.annotation())));
                }
            }
            if (injection.required() == 0 || resolved.resolution().nodes().size() < injection.required()) continue;
            long surviving = resolved.resolution().nodes().stream()
                    .filter(node -> combinedOwners.owners().get(node) == resolved).count();
            if (surviving >= injection.required()) continue;
            for (Node node : resolved.resolution().nodes()) {
                ResolvedInjection owner = combinedOwners.owners().get(node);
                if (owner != null && owner != resolved
                        && owner.injection().definition().mod().own() != injection.definition().mod().own()) {
                    evidence.add(evidence(Kind.REQUIRED_EXCLUSIVE_COLLISION, target.name, node.method(), injection,
                            owner.injection().definition(), (int) surviving, description(injection.annotation())));
                }
            }
        }
    }

    private static List<Body> effectiveBodies(ClassNode target, List<Body> originals, List<Definition> mixins,
                                             Set<String> notes) {
        Map<String, Body> effective = new LinkedHashMap<>();
        originals.forEach(body -> effective.put(signature(body.method()), body));
        for (Definition definition : mixins) {
            for (MethodNode method : definition.bytecode().methods) {
                if (annotation(method, OVERWRITE) == null) {
                    continue;
                }
                if (hasAmbiguousShadows(definition.bytecode())) {
                    notes.add(target.name + ": an overwrite uses shadow aliases or prefixed members; no automatic conflict verdict");
                    return null;
                }
                String mapped = definition.remap().apply(method.name + method.desc);
                MemberInfo selector = parseMember(mapped, false);
                if (selector == null) {
                    notes.add(definition.className() + ": overwrite selector cannot be resolved");
                    continue;
                }
                List<String> matches = effective.entrySet().stream()
                        .filter(entry -> selector.matches(target.name, entry.getValue().method().name,
                                entry.getValue().method().desc).isExactMatch())
                        .map(Map.Entry::getKey).toList();
                if (matches.size() != 1) {
                    notes.add(definition.className() + ": overwrite target is ambiguous or unavailable");
                    continue;
                }
                String key = matches.getFirst();
                Body previous = effective.get(key);
                if (previous.writer() != null && (previous.writer().priority() >= definition.priority()
                        || annotation(previous.method(), FINAL) != null)) {
                    continue;
                }
                MethodNode copied = new MethodNode(Opcodes.ASM9, method.access, previous.method().name,
                        previous.method().desc, method.signature,
                        method.exceptions == null ? null : method.exceptions.toArray(String[]::new));
                method.accept(copied);
                copied.name = previous.method().name;
                copied.desc = previous.method().desc;
                for (AbstractInsnNode instruction : copied.instructions) {
                    if (instruction instanceof MethodInsnNode invocation) {
                        if (invocation.owner.equals(definition.bytecode().name)) invocation.owner = target.name;
                        invocation.desc = invocation.desc.replace("L" + definition.bytecode().name + ";", "L" + target.name + ";");
                    } else if (instruction instanceof FieldInsnNode field) {
                        if (field.owner.equals(definition.bytecode().name)) field.owner = target.name;
                        field.desc = field.desc.replace("L" + definition.bytecode().name + ";", "L" + target.name + ";");
                    }
                }
                effective.put(key, new Body(copied, definition));
            }
        }
        return List.copyOf(effective.values());
    }

    private static Ownership redirectOwners(List<ResolvedInjection> redirects, boolean control) {
        Map<Node, ResolvedInjection> owners = new HashMap<>();
        List<FinalCollision> finalCollisions = new ArrayList<>();
        for (ResolvedInjection current : redirects) {
            Resolution resolution = control ? current.control() : current.resolution();
            if (!restrictions(resolution, current.injection()).isEmpty()) continue;
            for (Node node : resolution.nodes()) {
                ResolvedInjection previous = owners.get(node);
                if (previous == null) {
                    owners.put(node, current);
                } else if (previous.injection().definition().priority() < current.injection().definition().priority()) {
                    if (annotation(previous.injection().method(), FINAL) != null) {
                        finalCollisions.add(new FinalCollision(current, previous, node));
                    } else {
                        owners.put(node, current);
                    }
                }
            }
        }
        return new Ownership(owners, List.copyOf(finalCollisions));
    }

    private static boolean succeedsAlone(ResolvedInjection resolved, Ownership controls) {
        Injection injection = resolved.injection();
        if (!restrictions(resolved.control(), injection).isEmpty()
                || resolved.control().nodes().size() < injection.required()) return false;
        if (!exclusive(injection.annotation())) return true;
        if (controls.finalCollisions().stream().anyMatch(collision -> collision.losing() == resolved)) return false;
        long surviving = resolved.control().nodes().stream().filter(node -> controls.owners().get(node) == resolved).count();
        return surviving >= injection.required();
    }

    private static List<Restriction> restrictions(Resolution resolution, Injection injection) {
        List<Restriction> restrictions = new ArrayList<>();
        for (Body body : resolution.selectedBodies()) {
            Definition writer = body.writer();
            if (writer == null) continue;
            if (annotation(body.method(), FINAL) != null) {
                restrictions.add(new Restriction(Kind.FINAL_METHOD_INJECTION, body));
                continue;
            }
            if (writer.className().equals(injection.definition().className())
                    || writer.priority() < injection.definition().priority()) continue;
            boolean constant = simpleName(injection.annotation().desc).equals("ModifyConstant");
            boolean restricted = constant || annotations(injection.annotation(), "at").stream()
                    .map(point -> string(point, "value", ""))
                    .anyMatch(point -> point.equals("INVOKE") || point.equals("FIELD") || point.equals("CONSTANT"));
            if (restricted) restrictions.add(new Restriction(Kind.OVERWRITE_PRIORITY_CONFLICT, body));
        }
        return List.copyOf(restrictions);
    }

    private static Evidence evidence(Kind kind, String target, String targetMethod, Injection losing,
                                     Definition other, int surviving, String point) {
        Definition own = losing.definition().mod().own() ? losing.definition() : other;
        Definition foreign = losing.definition().mod().own() ? other : losing.definition();
        return new Evidence(kind, foreign.mod(), own.className(), foreign.className(), target, targetMethod,
                losing.definition().className() + "#" + losing.method().name, losing.required(), surviving, point);
    }

    private static boolean hasAmbiguousShadows(ClassNode node) {
        String shadow = "Lorg/spongepowered/asm/mixin/Shadow;";
        for (MethodNode method : node.methods) {
            AnnotationNode annotation = annotation(method, shadow);
            if (annotation != null && (!strings(annotation, "aliases").isEmpty()
                    || method.name.startsWith(string(annotation, "prefix", "shadow$")))) return true;
        }
        return node.fields.stream().anyMatch(field -> {
            AnnotationNode annotation = annotation(field.visibleAnnotations, shadow);
            if (annotation == null) annotation = annotation(field.invisibleAnnotations, shadow);
            return annotation != null && !strings(annotation, "aliases").isEmpty();
        });
    }

    private static Resolution resolve(ClassNode target, List<Body> bodies, Injection injection) {
        Set<Node> nodes = new LinkedHashSet<>();
        Set<Body> selected = new LinkedHashSet<>();
        List<String> selectors = strings(injection.annotation(), "method");
        if (selectors.isEmpty() || value(injection.annotation(), "target") != null) {
            return new Resolution(nodes, List.of(), false);
        }
        for (String input : selectors) {
            MemberInfo selector = parseMember(injection.definition().remap().apply(input), true);
            if (selector == null || selector.getName() == null) {
                return new Resolution(nodes, List.of(), false);
            }
            int matched = 0;
            for (Body body : bodies) {
                if (selector.matches(target.name, body.method().name, body.method().desc).isExactMatch()) {
                    if (++matched > selector.getMaxMatchCount()) {
                        break;
                    }
                    selected.add(body);
                }
            }
        }
        String kind = simpleName(injection.annotation().desc);
        if (kind.equals("ModifyVariable") || kind.equals("WrapMethod")) {
            return new Resolution(nodes, List.copyOf(selected), false);
        }
        if (exclusive(injection.annotation()) && integer(injection.annotation(), "order", 10000) != 10000) {
            return new Resolution(nodes, List.copyOf(selected), false);
        }
        List<AnnotationNode> points = annotations(injection.annotation(), kind.equals("ModifyConstant") ? "constant" : "at");
        if (points.isEmpty()) {
            return new Resolution(nodes, List.copyOf(selected), false);
        }
        for (Body body : selected) {
            for (AnnotationNode point : points) {
                Selection selection = select(body.method(), point, injection, kind.equals("ModifyConstant"), false);
                if (!selection.complete()) {
                    return new Resolution(nodes, List.copyOf(selected), false);
                }
                for (Integer index : selection.indices()) {
                    if (exclusive(injection.annotation())
                            && !compatibleExclusiveHandler(target, body.method(), injection, body.method().instructions.get(index))) {
                        return new Resolution(nodes, List.copyOf(selected), false);
                    }
                    nodes.add(new Node(signature(body.method()), index));
                }
            }
        }
        return new Resolution(Set.copyOf(nodes), List.copyOf(selected), true);
    }

    private static boolean compatibleExclusiveHandler(ClassNode owner, MethodNode target, Injection injection,
                                                       AbstractInsnNode instruction) {
        boolean staticHandler = (injection.method().access & Opcodes.ACC_STATIC) != 0;
        if ((target.access & Opcodes.ACC_STATIC) != 0 && !staticHandler) return false;
        if (target.name.equals("<init>")) {
            Bytecode.DelegateInitialiser delegate = Bytecode.findDelegateInit(target, owner.superName, owner.name);
            if (!delegate.isPresent || (!staticHandler && target.instructions.indexOf(instruction)
                    <= target.instructions.indexOf(delegate.insn))) return false;
        }
        List<Type> expected = new ArrayList<>();
        Type result;
        if (simpleName(injection.annotation().desc).equals("ModifyConstant")) {
            Object value = instruction instanceof LdcInsnNode ldc ? ldc.cst : null;
            if (value instanceof String) result = Type.getObjectType("java/lang/String");
            else if (value instanceof Type) result = Type.getObjectType("java/lang/Class");
            else if (value instanceof Long || instruction.getOpcode() == Opcodes.LCONST_0
                    || instruction.getOpcode() == Opcodes.LCONST_1) result = Type.LONG_TYPE;
            else if (value instanceof Double || instruction.getOpcode() == Opcodes.DCONST_0
                    || instruction.getOpcode() == Opcodes.DCONST_1) result = Type.DOUBLE_TYPE;
            else if (value instanceof Float || (instruction.getOpcode() >= Opcodes.FCONST_0
                    && instruction.getOpcode() <= Opcodes.FCONST_2)) result = Type.FLOAT_TYPE;
            else if (instruction.getOpcode() == Opcodes.ACONST_NULL) result = Type.getReturnType(injection.method().desc);
            else result = Type.INT_TYPE;
            expected.add(result);
        } else if (instruction instanceof MethodInsnNode invocation && !invocation.name.startsWith("<")) {
            if (invocation.getOpcode() != Opcodes.INVOKESTATIC) expected.add(Type.getObjectType(invocation.owner));
            expected.addAll(List.of(Type.getArgumentTypes(invocation.desc)));
            result = Type.getReturnType(invocation.desc);
        } else if (instruction instanceof FieldInsnNode field) {
            boolean read = field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.GETSTATIC;
            boolean instance = field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.PUTFIELD;
            if (instance) expected.add(Type.getObjectType(field.owner));
            if (!read) expected.add(Type.getType(field.desc));
            result = read ? Type.getType(field.desc) : Type.VOID_TYPE;
        } else {
            return false;
        }
        Type[] parameters = Type.getArgumentTypes(injection.method().desc);
        if (!result.equals(Type.getReturnType(injection.method().desc)) || parameters.length < expected.size()) return false;
        for (int index = 0; index < expected.size(); index++) {
            if (!parameters[index].equals(expected.get(index))) return false;
        }
        Type[] captured = Type.getArgumentTypes(target.desc);
        int extra = parameters.length - expected.size();
        if (extra > captured.length) return false;
        for (int index = 0; index < extra; index++) {
            if (!parameters[index + expected.size()].equals(captured[index])) return false;
        }
        return true;
    }

    private static Selection select(MethodNode method, AnnotationNode point, Injection injection,
                                    boolean constant, boolean boundary) {
        String kind = constant ? "CONSTANT" : string(point, "value", "");
        if (simpleName(injection.annotation().desc).equals("Redirect")
                && !kind.equals("INVOKE") && !kind.equals("FIELD")) return Selection.unknown();
        String specifier = "";
        int colon = kind.indexOf(':');
        if (colon >= 0) {
            if (!boundary) {
                return Selection.unknown();
            }
            specifier = kind.substring(colon + 1);
            kind = kind.substring(0, colon);
        }
        if (!Set.of("HEAD", "TAIL", "RETURN", "INVOKE", "FIELD", "CONSTANT").contains(kind)) {
            return Selection.unknown();
        }
        int start = 0;
        int end = method.instructions.size() - 1;
        if (!boundary) {
            String sliceId = string(point, "slice", "");
            List<AnnotationNode> slices = annotations(injection.annotation(), "slice");
            AnnotationNode slice = slices.stream().filter(item -> string(item, "id", "").equals(sliceId)).findFirst().orElse(null);
            if (slice == null && !sliceId.isEmpty()) {
                return Selection.unknown();
            }
            if (slice != null) {
                AnnotationNode from = nested(slice, "from");
                AnnotationNode to = nested(slice, "to");
                if (from != null) {
                    Selection result = select(method, from, injection, false, true);
                    if (!result.complete()) {
                        return Selection.unknown();
                    }
                    start = result.indices().isEmpty() ? 0 : result.indices().getFirst();
                }
                if (to != null) {
                    Selection result = select(method, to, injection, false, true);
                    if (!result.complete()) {
                        return Selection.unknown();
                    }
                    end = result.indices().isEmpty() ? start : result.indices().getFirst();
                }
                if (start > end) {
                    return Selection.unknown();
                }
            }
        }
        String shift = enumValue(point, "shift", "NONE");
        if (simpleName(injection.annotation().desc).equals("Redirect") && !shift.equals("NONE")) {
            return Selection.unknown();
        }
        MemberInfo member = null;
        if (kind.equals("INVOKE") || kind.equals("FIELD")) {
            member = parseMember(injection.definition().remap().apply(string(point, "target", "")), false);
            if (member == null || !member.isFullyQualified()) {
                return Selection.unknown();
            }
        }
        Map<String, Object> constants = new HashMap<>();
        if (kind.equals("CONSTANT")) {
            if (constant) {
                if (point.values != null) {
                    for (int i = 0; i < point.values.size(); i += 2) {
                        constants.put((String) point.values.get(i), point.values.get(i + 1));
                    }
                }
            } else {
                for (String argument : strings(point, "args")) {
                    String[] parts = argument.split("=", 2);
                    if (parts.length != 2) {
                        return Selection.unknown();
                    }
                    try {
                        constants.put(parts[0], switch (parts[0]) {
                            case "intValue" -> Integer.valueOf(parts[1]);
                            case "longValue" -> Long.valueOf(parts[1]);
                            case "floatValue" -> Float.valueOf(parts[1]);
                            case "doubleValue" -> Double.valueOf(parts[1]);
                            case "stringValue" -> parts[1];
                            case "nullValue" -> Boolean.valueOf(parts[1]);
                            case "log" -> parts[1];
                            default -> throw new IllegalArgumentException();
                        });
                    } catch (IllegalArgumentException exception) {
                        return Selection.unknown();
                    }
                }
            }
            if (constants.containsKey("expandZeroConditions")
                    || constants.keySet().stream().filter(key -> key.endsWith("Value")
                    && (!key.equals("nullValue") || Boolean.TRUE.equals(constants.get(key)))).count() != 1) {
                return Selection.unknown();
            }
        } else if (!strings(point, "args").stream().allMatch(argument -> argument.startsWith("log="))) {
            return Selection.unknown();
        }
        List<Integer> indices = new ArrayList<>();
        int ordinal = integer(point, "ordinal", -1);
        int matched = 0;
        int opcode = integer(point, "opcode", -1);
        for (int index = start; index <= end; index++) {
            AbstractInsnNode instruction = method.instructions.get(index);
            boolean match = switch (kind) {
                case "HEAD" -> index == start;
                case "RETURN", "TAIL" -> instruction.getOpcode() >= Opcodes.IRETURN && instruction.getOpcode() <= Opcodes.RETURN;
                case "INVOKE" -> instruction instanceof MethodInsnNode invocation
                        && member.matches(invocation.owner, invocation.name, invocation.desc).isExactMatch();
                case "FIELD" -> instruction instanceof FieldInsnNode field
                        && member.matches(field.owner, field.name, field.desc).isExactMatch()
                        && (opcode < 0 || field.getOpcode() == opcode);
                case "CONSTANT" -> matchesConstant(instruction, constants);
                default -> false;
            };
            if (match) {
                if (ordinal < 0 || matched == ordinal || kind.equals("TAIL")) {
                    indices.add(index);
                }
                matched++;
            }
        }
        if ((kind.equals("TAIL") || specifier.equals("LAST")) && !indices.isEmpty()) {
            indices = new ArrayList<>(List.of(indices.getLast()));
        } else if (specifier.equals("FIRST") && !indices.isEmpty()) {
            indices = new ArrayList<>(List.of(indices.getFirst()));
        } else if (specifier.equals("ONE") && indices.size() != 1) {
            return Selection.unknown();
        }
        if (boundary && specifier.isEmpty() && !indices.isEmpty()) {
            indices = new ArrayList<>(List.of(indices.getFirst()));
        }
        int offset = switch (shift) {
            case "BEFORE" -> -1;
            case "AFTER" -> 1;
            case "BY" -> integer(point, "by", 0);
            default -> 0;
        };
        if (offset != 0) {
            List<Integer> shifted = new ArrayList<>();
            for (int index : indices) {
                int next = index + offset;
                if (next < 0 || next >= method.instructions.size()) {
                    return Selection.unknown();
                }
                shifted.add(next);
            }
            indices = shifted;
        }
        return new Selection(List.copyOf(indices), true);
    }

    private static boolean matchesConstant(AbstractInsnNode instruction, Map<String, Object> constants) {
        Object constant;
        int opcode = instruction.getOpcode();
        if (instruction instanceof LdcInsnNode ldc) {
            constant = ldc.cst;
        } else if (instruction instanceof IntInsnNode integer
                && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
            constant = integer.operand;
        } else if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            constant = opcode - Opcodes.ICONST_0;
        } else if (opcode == Opcodes.LCONST_0 || opcode == Opcodes.LCONST_1) {
            constant = (long) (opcode - Opcodes.LCONST_0);
        } else if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) {
            constant = (float) (opcode - Opcodes.FCONST_0);
        } else if (opcode == Opcodes.DCONST_0 || opcode == Opcodes.DCONST_1) {
            constant = (double) (opcode - Opcodes.DCONST_0);
        } else if (opcode == Opcodes.ACONST_NULL) {
            return Boolean.TRUE.equals(constants.get("nullValue"));
        } else {
            return false;
        }
        String key = switch (constant) {
            case Integer ignored -> "intValue";
            case Long ignored -> "longValue";
            case Float ignored -> "floatValue";
            case Double ignored -> "doubleValue";
            case String ignored -> "stringValue";
            case Type ignored -> "classValue";
            default -> "";
        };
        return constant.equals(constants.get(key));
    }

    private static MemberInfo parseMember(String input, boolean method) {
        if (input == null || input.isBlank() || input.startsWith("@") || input.contains("->") || input.startsWith("/")) {
            return null;
        }
        try {
            MemberInfo member = MemberInfo.parse(input, null).validate();
            return (MemberInfo) member.configure(method ? ITargetSelector.Configure.SELECT_MEMBER : ITargetSelector.Configure.SELECT_INSTRUCTION);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static List<String> declaredTargets(ClassNode node) {
        AnnotationNode mixin = annotation(node.visibleAnnotations, MIXIN);
        if (mixin == null) {
            mixin = annotation(node.invisibleAnnotations, MIXIN);
        }
        Set<String> targets = new LinkedHashSet<>();
        Object values = value(mixin, "value");
        if (values instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Type type) {
                    targets.add(type.getInternalName());
                }
            }
        }
        strings(mixin, "targets").forEach(target -> targets.add(target.replace('.', '/')));
        return List.copyOf(targets);
    }

    public static int declaredPriority(ClassNode node, int fallback) {
        AnnotationNode mixin = annotation(node.visibleAnnotations, MIXIN);
        return integer(mixin == null ? annotation(node.invisibleAnnotations, MIXIN) : mixin, "priority", fallback);
    }

    public static AnnotationNode annotation(MethodNode method, String descriptor) {
        AnnotationNode result = annotation(method.visibleAnnotations, descriptor);
        return result == null ? annotation(method.invisibleAnnotations, descriptor) : result;
    }

    public static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(descriptor)) {
                    return annotation;
                }
            }
        }
        return null;
    }

    private static AnnotationNode injectorAnnotation(MethodNode method) {
        List<AnnotationNode> all = new ArrayList<>();
        if (method.visibleAnnotations != null) all.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) all.addAll(method.invisibleAnnotations);
        for (AnnotationNode annotation : all) {
            if ((annotation.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/")
                    || annotation.desc.startsWith("Lcom/llamalad7/mixinextras/"))
                    && INJECTORS.contains(simpleName(annotation.desc))) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean exclusive(AnnotationNode annotation) {
        String name = simpleName(annotation.desc);
        return name.equals("Redirect") || name.equals("ModifyConstant");
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation != null && annotation.values != null) {
            for (int index = 0; index < annotation.values.size(); index += 2) {
                if (annotation.values.get(index).equals(key)) return annotation.values.get(index + 1);
            }
        }
        return null;
    }

    private static String string(AnnotationNode annotation, String key, String fallback) {
        Object result = value(annotation, key);
        return result instanceof String text ? text : fallback;
    }

    private static int integer(AnnotationNode annotation, String key, int fallback) {
        Object result = value(annotation, key);
        return result instanceof Integer number ? number : fallback;
    }

    private static List<String> strings(AnnotationNode annotation, String key) {
        Object result = value(annotation, key);
        if (result instanceof String text) return List.of(text);
        if (result instanceof List<?> list) return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return List.of();
    }

    private static List<AnnotationNode> annotations(AnnotationNode annotation, String key) {
        Object result = value(annotation, key);
        if (result instanceof AnnotationNode nested) return List.of(nested);
        if (result instanceof List<?> list) return list.stream().filter(AnnotationNode.class::isInstance).map(AnnotationNode.class::cast).toList();
        return List.of();
    }

    private static AnnotationNode nested(AnnotationNode annotation, String key) {
        Object result = value(annotation, key);
        return result instanceof AnnotationNode nested ? nested : null;
    }

    private static String enumValue(AnnotationNode annotation, String key, String fallback) {
        Object result = value(annotation, key);
        return result instanceof String[] enumeration && enumeration.length == 2 ? enumeration[1] : fallback;
    }

    private static String simpleName(String descriptor) {
        return descriptor.substring(descriptor.lastIndexOf('/') + 1, descriptor.length() - 1);
    }

    private static String signature(MethodNode method) {
        return method.name + method.desc;
    }

    private static String description(AnnotationNode annotation) {
        StringBuilder result = new StringBuilder("@").append(simpleName(annotation.desc));
        for (AnnotationNode at : annotations(annotation, "at")) {
            result.append(" ").append(string(at, "value", ""));
            String target = string(at, "target", "");
            if (!target.isBlank()) result.append(" ").append(target);
            int ordinal = integer(at, "ordinal", -1);
            if (ordinal >= 0) result.append(" ordinal=").append(ordinal);
        }
        for (AnnotationNode constant : annotations(annotation, "constant")) {
            if (constant.values == null) continue;
            for (int index = 0; index < constant.values.size(); index += 2) {
                result.append(" ").append(constant.values.get(index)).append('=').append(constant.values.get(index + 1));
            }
        }
        return result.toString();
    }

    public interface ClassProvider {
        ClassNode read(String className) throws Exception;
    }

    public record Mod(String id, String name, String version, boolean own) {
    }

    public record Definition(Mod mod, String className, ClassNode bytecode, List<String> targets,
                             int priority, int order, int defaultRequire, boolean requiredConfig,
                             boolean mutatesTarget, UnaryOperator<String> remap) {
        public Definition {
            targets = targets.stream().map(target -> target.replace('.', '/')).distinct().toList();
        }
    }

    public enum Kind {
        REQUIRED_EXCLUSIVE_COLLISION,
        FINAL_EXCLUSIVE_COLLISION,
        OVERWRITE_REMOVES_REQUIRED_TARGET,
        FINAL_METHOD_INJECTION,
        OVERWRITE_PRIORITY_CONFLICT
    }

    public record Evidence(Kind kind, Mod mod, String ownMixin, String otherMixin, String targetClass,
                           String targetMethod, String failedInjector, int required, int surviving,
                           String injectionPoint) {
    }

    public record Report(List<Evidence> conflicts, List<String> notes, int mixins, int ownTargets,
                         int inspectedTargets, long elapsedMillis) {
        public Report {
            conflicts = List.copyOf(conflicts);
            notes = List.copyOf(notes);
        }

        public static Report empty() {
            return new Report(List.of(), List.of(), 0, 0, 0, 0);
        }
    }

    private record Injection(Definition definition, MethodNode method, AnnotationNode annotation, int required) {
    }

    private record Body(MethodNode method, Definition writer) {
    }

    private record Node(String method, int index) {
    }

    private record Resolution(Set<Node> nodes, List<Body> selectedBodies, boolean complete) {
    }

    private record ResolvedInjection(Injection injection, Resolution resolution, Resolution control) {
    }

    private record Restriction(Kind kind, Body body) {
    }

    private record FinalCollision(ResolvedInjection losing, ResolvedInjection incumbent, Node node) {
    }

    private record Ownership(Map<Node, ResolvedInjection> owners, List<FinalCollision> finalCollisions) {
    }

    private record Selection(List<Integer> indices, boolean complete) {
        private static Selection unknown() {
            return new Selection(List.of(), false);
        }
    }
}
