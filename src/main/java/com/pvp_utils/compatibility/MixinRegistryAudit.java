package com.pvp_utils.compatibility;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MixinRegistryAudit {
    private static final String FUNCTIONAL_CONFIG = "pvp-utils.client.mixins.json";
    private static final String FUNCTIONAL_PACKAGE = "com.pvp_utils.mixin.client.";

    private MixinRegistryAudit() {
    }

    public static MixinConflictDetector.Report inspect(List<String> functionalMixins) throws Exception {
        List<Config> configurations = registeredConfigurations();
        IMixinConfig ownConfig = configurations.stream().filter(config -> config.getName().equals(FUNCTIONAL_CONFIG))
                .findFirst().orElseThrow().getConfig();
        List<String> lifecycleNotes = new ArrayList<>();
        lifecycleNotes.add("PVPUtils functional target names are registered after acceptTargets callbacks; plugins may not observe them in that callback");
        for (Config configuration : configurations) {
            IMixinConfig config = configuration.getConfig();
            if (config == ownConfig || config.getPlugin() == null) continue;
            int order = (Integer) invoke(config, "compareTo", new Class<?>[]{ownConfig.getClass()}, ownConfig);
            if (order > 0) {
                lifecycleNotes.add(config.getName() + ": companion plugin Mixins may be registered after this audit; full dynamic coverage is unavailable");
            }
        }
        FabricLoader loader = FabricLoader.getInstance();
        ModContainer ownContainer = loader.getModContainer("pvp-utils").orElseThrow();
        MixinConflictDetector.Mod own = mod(ownContainer, true);
        IClassBytecodeProvider bytecode = MixinService.getService().getBytecodeProvider();
        List<MixinConflictDetector.Definition> definitions = new ArrayList<>();
        Map<String, ClassNode> originals = new HashMap<>();
        Set<String> ownTargets = new LinkedHashSet<>();
        List<ClassNode> ownNodes = new ArrayList<>();
        for (String name : functionalMixins) {
            ClassNode node = bytecode.getClassNode(FUNCTIONAL_PACKAGE + name, true, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            ownNodes.add(node);
            ownTargets.addAll(MixinConflictDetector.declaredTargets(node));
        }
        Set<IMixinInfo> active = new LinkedHashSet<>();
        for (Config configuration : configurations) {
            IMixinConfig config = configuration.getConfig();
            if (config.getName().startsWith("pvp-utils.") || config.getTargets().isEmpty()) {
                continue;
            }
            for (String target : ownTargets) {
                String dotted = target.replace('/', '.');
                if (config.getTargets().contains(dotted)) {
                    Object result = invoke(config, "getMixinsFor", new Class<?>[]{String.class}, dotted);
                    if (result instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof IMixinInfo info) active.add(info);
                        }
                    }
                }
            }
        }
        int maximumOrder = 0;
        Map<IMixinConfig, Boolean> mutationCache = new HashMap<>();
        for (IMixinInfo info : active) {
            IMixinConfig config = info.getConfig();
            String id = FabricUtil.getModId(config);
            ModContainer container = id == null ? null : loader.getModContainer(id).orElse(null);
            MixinConflictDetector.Mod foreign = container == null
                    ? new MixinConflictDetector.Mod(id == null ? config.getName() : id,
                    id == null ? config.getName() : id, "unknown", false) : mod(container, false);
            IReferenceMapper mapper = referenceMapper(config);
            int order = mixinOrder(info);
            maximumOrder = Math.max(maximumOrder, order);
            boolean mutation = mutationCache.computeIfAbsent(config, ignored -> canMutateTargets(config.getPlugin(), bytecode));
            definitions.add(new MixinConflictDetector.Definition(foreign, info.getClassName(),
                    info.getClassNode(ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES), info.getTargetClasses(),
                    info.getPriority(), order, defaultRequire(config), config.isRequired(), mutation,
                    selector -> mapper.remap(info.getClassRef(), selector)));
        }
        IReferenceMapper ownMapper = referenceMapper(ownConfig);
        int defaultPriority = (Integer) invoke(ownConfig, "getDefaultMixinPriority", new Class<?>[0]);
        int ownRequire = defaultRequire(ownConfig);
        for (int index = 0; index < ownNodes.size(); index++) {
            ClassNode node = ownNodes.get(index);
            definitions.add(new MixinConflictDetector.Definition(own, node.name.replace('/', '.'), node,
                    MixinConflictDetector.declaredTargets(node), MixinConflictDetector.declaredPriority(node, defaultPriority),
                    maximumOrder + index + 1, ownRequire, ownConfig.isRequired(), false,
                    selector -> ownMapper.remap(node.name, selector)));
        }
        MixinConflictDetector.Report report = MixinConflictDetector.inspect(definitions, target -> {
            ClassNode cached = originals.get(target);
            if (cached != null) return cached;
            ClassNode node = bytecode.getClassNode(target, true, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            originals.put(target, node);
            return node;
        });
        List<String> notes = new ArrayList<>(report.notes());
        notes.addAll(lifecycleNotes);
        return new MixinConflictDetector.Report(report.conflicts(), notes, report.mixins(), report.ownTargets(),
                report.inspectedTargets(), report.elapsedMillis());
    }

    private static List<Config> registeredConfigurations() throws ReflectiveOperationException {
        Field field = Config.class.getDeclaredField("allConfigs");
        if (!field.trySetAccessible()) throw new IllegalAccessException("Mixin config registry is inaccessible");
        Object value = field.get(null);
        if (!(value instanceof Map<?, ?> map)) throw new IllegalStateException("Unexpected Mixin config registry");
        return map.values().stream().filter(Config.class::isInstance).map(Config.class::cast).toList();
    }

    private static int mixinOrder(IMixinInfo info) throws ReflectiveOperationException {
        Field field = info.getClass().getDeclaredField("order");
        if (!field.trySetAccessible()) throw new IllegalAccessException("Mixin registration order is inaccessible");
        return field.getInt(info);
    }

    private static int defaultRequire(IMixinConfig config) throws ReflectiveOperationException {
        return (Integer) invoke(config, "getDefaultRequiredInjections", new Class<?>[0]);
    }

    private static IReferenceMapper referenceMapper(IMixinConfig config) throws ReflectiveOperationException {
        return (IReferenceMapper) invoke(config, "getReferenceMapper", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments)
            throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(name, parameters);
        if (!method.trySetAccessible()) throw new IllegalAccessException("Mixin metadata is inaccessible: " + name);
        return method.invoke(target, arguments);
    }

    private static MixinConflictDetector.Mod mod(ModContainer container, boolean own) {
        return new MixinConflictDetector.Mod(container.getMetadata().getId(), container.getMetadata().getName(),
                container.getMetadata().getVersion().getFriendlyString(), own);
    }

    private static boolean canMutateTargets(IMixinConfigPlugin plugin, IClassBytecodeProvider bytecode) {
        if (plugin == null) return false;
        try {
            String className = plugin.getClass().getName();
            Set<String> resolved = new LinkedHashSet<>();
            while (className != null && !className.equals("java/lang/Object") && !className.equals("java.lang.Object")) {
                ClassNode node = bytecode.getClassNode(className, false, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : node.methods) {
                    if ((!method.name.equals("preApply") && !method.name.equals("postApply")) || !resolved.add(method.name)) {
                        continue;
                    }
                    for (AbstractInsnNode instruction : method.instructions) {
                        int opcode = instruction.getOpcode();
                        if (opcode >= 0 && opcode != Opcodes.RETURN && opcode != Opcodes.NOP) return true;
                    }
                }
                if (resolved.size() == 2) break;
                className = node.superName;
            }
            return resolved.size() != 2;
        } catch (Exception | LinkageError exception) {
            return true;
        }
    }
}
