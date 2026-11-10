package com.pvp_utils.compatibility;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CompatibilityGate {
    public static final String FIX_MOD_ID = "pvputils-skija-patch";
    public static final String FIX_URL = "https://modrinth.com/mod/pvputils-fix";
    private static final Logger LOGGER = LoggerFactory.getLogger("PVPUtils-Compatibility");
    private static CompatibilityStateStore store;
    private static CompatibilityStateStore.State state = CompatibilityStateStore.State.defaults();
    private static PlatformEnvironment platform;
    private static MixinConflictDetector.Report report = MixinConflictDetector.Report.empty();
    private static List<String> functionalMixins = List.of();
    private static boolean prepared;
    private static boolean decided;
    private static boolean oneTimeOverride;
    private static boolean patchPresent;
    private static boolean preferencesReadFailed;
    private static boolean disabledForCurrentLaunch;
    private static volatile boolean enabled;
    private static volatile WarningKind warning = WarningKind.NONE;

    private CompatibilityGate() {
    }

    public static synchronized void prepare() {
        if (prepared) return;
        prepared = true;
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.getEnvironmentType() != EnvType.CLIENT) return;
        platform = PlatformEnvironment.current();
        patchPresent = loader.isModLoaded(FIX_MOD_ID);
        store = new CompatibilityStateStore(loader.getConfigDir());
        if (System.getProperty("pvp-utils.compatibility.restart.cleanup") != null) {
            CompatibilityRestart.cleanupAfterBoot(loader.getConfigDir());
        }
        try {
            state = store.load();
        } catch (IOException exception) {
            preferencesReadFailed = true;
            LOGGER.error("Cannot read compatibility preferences; the original file was preserved", exception);
            return;
        }
        try {
            oneTimeOverride = store.consumeNextLaunch(ProcessHandle.current().pid());
        } catch (IOException exception) {
            LOGGER.error("Cannot consume the one-time compatibility override; the original file was preserved", exception);
        }
    }

    public static synchronized List<String> functionalMixins() {
        prepare();
        if (decided) return enabled ? functionalMixins : List.of();
        decided = true;
        if (platform == null) return List.of();
        if (preferencesReadFailed) {
            warning = WarningKind.DETECTION_FAILURE;
            writeReport();
            return List.of();
        }
        try (InputStream resource = MixinService.getService().getResourceAsStream("pvp-utils.functional-mixins.json")) {
            if (resource == null) throw new IOException("Functional Mixin manifest is missing");
            JsonElement manifest = JsonParser.parseReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            List<String> names = new ArrayList<>();
            for (JsonElement entry : manifest.getAsJsonObject().getAsJsonArray("mixins")) {
                String name = entry.getAsString();
                if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
                    throw new IOException("Invalid functional Mixin name");
                }
                names.add(name);
            }
            if (names.isEmpty() || names.stream().distinct().count() != names.size()) {
                throw new IOException("Functional Mixin manifest is empty or contains duplicates");
            }
            functionalMixins = List.copyOf(names);
        } catch (Exception exception) {
            LOGGER.error("PVPUtils functional Mixins could not be read; normal initialization is disabled", exception);
            warning = WarningKind.DETECTION_FAILURE;
            writeReport();
            return List.of();
        }
        try {
            report = MixinRegistryAudit.inspect(functionalMixins);
        } catch (Exception | LinkageError exception) {
            LOGGER.error("Compatibility analysis is incomplete; no mod is classified as conflicting", exception);
            report = new MixinConflictDetector.Report(List.of(),
                    List.of("Mixin registry analysis unavailable: " + exception.getClass().getSimpleName()), 0, 0, 0, 0);
        }
        Decision decision = decide(state, platform, patchPresent, oneTimeOverride, !blockingConflicts().isEmpty());
        enabled = decision.enabled();
        warning = decision.warning();
        LOGGER.info("Compatibility check: {} loaded mods, {} inspected targets, {} confirmed findings, {} ms; PVPUtils {}",
                FabricLoader.getInstance().getAllMods().size(), report.inspectedTargets(), report.conflicts().size(),
                report.elapsedMillis(), enabled ? "enabled" : "disabled");
        writeReport();
        return enabled ? functionalMixins : List.of();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean hasPendingWarning() {
        return warning != WarningKind.NONE;
    }

    public static WarningKind warning() {
        return warning;
    }

    public static PlatformEnvironment platform() {
        return platform;
    }

    public static synchronized List<MixinConflictDetector.Evidence> blockingConflicts() {
        return report.conflicts().stream().filter(evidence -> !state.ignoredModIds().contains(evidence.mod().id())).toList();
    }

    public static List<MixinConflictDetector.Mod> conflictingMods() {
        return blockingConflicts().stream().map(MixinConflictDetector.Evidence::mod).distinct()
                .sorted(Comparator.comparing(MixinConflictDetector.Mod::id)).toList();
    }

    public static synchronized void disableForCurrentLaunch() {
        disabledForCurrentLaunch = true;
        enabled = false;
        warning = WarningKind.NONE;
        writeReport();
    }

    public static synchronized void acknowledgePatchNotice() throws IOException {
        state = store.update(CompatibilityStateStore.State::acknowledgeFixNotice);
        refreshWarning();
        writeReport();
    }

    public static synchronized void ignoreModPermanently(String id) throws IOException {
        if (conflictingMods().stream().noneMatch(mod -> mod.id().equals(id))) {
            throw new IOException("The selected mod has no current compatibility finding");
        }
        state = store.update(current -> current.ignoreMod(id));
        refreshWarning();
        writeReport();
    }

    public static synchronized void grantNextLaunch() throws IOException {
        store.grantNextLaunch(ProcessHandle.current().pid());
    }

    public static Path stateFile() {
        return store.stateFile();
    }

    private static void refreshWarning() {
        if (disabledForCurrentLaunch) {
            warning = WarningKind.NONE;
            return;
        }
        warning = decide(state, platform, patchPresent, oneTimeOverride, !blockingConflicts().isEmpty()).warning();
    }

    public static Decision decide(CompatibilityStateStore.State state, PlatformEnvironment platform,
                                   boolean patchPresent, boolean oneTimeOverride, boolean conflicts) {
        if (!oneTimeOverride && conflicts) return new Decision(false, WarningKind.MOD_CONFLICT);
        if (!oneTimeOverride && !platform.supported() && !patchPresent) {
            return new Decision(false, WarningKind.UNSUPPORTED_PLATFORM);
        }
        if (!platform.supported() && patchPresent && !state.fixNoticeAcknowledged()) {
            return new Decision(true, WarningKind.PATCH_NOTICE);
        }
        return new Decision(true, WarningKind.NONE);
    }

    private static void writeReport() {
        if (store == null) return;
        StringBuilder contents = new StringBuilder("PVPUtils compatibility report\n");
        contents.append("Time: ").append(Instant.now()).append('\n');
        contents.append("Platform: ").append(platform == null ? "unavailable" : platform.description()).append('\n');
        contents.append("PVPUtils-Fix: ").append(patchPresent).append('\n');
        contents.append("Enabled this launch: ").append(enabled).append('\n');
        contents.append("One-time override consumed: ").append(oneTimeOverride).append('\n');
        contents.append("Disabled by user this launch: ").append(disabledForCurrentLaunch).append('\n');
        contents.append("Preferences read failed: ").append(preferencesReadFailed).append('\n');
        contents.append("Ignored mod IDs: ").append(state.ignoredModIds().stream().sorted().toList()).append('\n');
        contents.append("Warning: ").append(warning).append('\n');
        contents.append("Analyzed Mixins: ").append(report.mixins()).append('\n');
        contents.append("Shared targets inspected: ").append(report.inspectedTargets()).append('\n');
        contents.append("Analysis time: ").append(report.elapsedMillis()).append(" ms\n");
        contents.append("\nLoaded mods:\n");
        FabricLoader.getInstance().getAllMods().stream().sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                .forEach(mod -> contents.append(mod.getMetadata().getId()).append(" ")
                        .append(mod.getMetadata().getVersion().getFriendlyString()).append('\n'));
        contents.append("\nConfirmed injection findings:\n");
        for (MixinConflictDetector.Evidence evidence : report.conflicts()) {
            contents.append(evidence.mod().id()).append(" ").append(evidence.mod().version()).append(" / ")
                    .append(evidence.kind()).append('\n');
            contents.append("  Target: ").append(evidence.targetClass()).append('#').append(evidence.targetMethod()).append('\n');
            contents.append("  PVPUtils: ").append(evidence.ownMixin()).append('\n');
            contents.append("  Other Mixin: ").append(evidence.otherMixin()).append('\n');
            contents.append("  Failed injector: ").append(evidence.failedInjector()).append('\n');
            contents.append("  Injection point: ").append(evidence.injectionPoint()).append('\n');
            contents.append("  Remaining/required: ").append(evidence.surviving()).append('/').append(evidence.required()).append('\n');
        }
        contents.append("\nUnverified cases:\n");
        report.notes().forEach(note -> contents.append(note).append('\n'));
        contents.append("\nShared target names alone are not incompatibility evidence. This report does not certify runtime, resource or dependency compatibility.\n");
        try {
            store.writeReport(contents.toString());
        } catch (IOException exception) {
            LOGGER.error("Cannot save the compatibility report", exception);
        }
    }

    public enum WarningKind {
        NONE,
        MOD_CONFLICT,
        UNSUPPORTED_PLATFORM,
        PATCH_NOTICE,
        DETECTION_FAILURE
    }

    public record Decision(boolean enabled, WarningKind warning) {
    }
}
