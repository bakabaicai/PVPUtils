package com.pvp_utils.client.startup;

import java.util.concurrent.atomic.AtomicReference;

public final class StartupProgress {
    private static final AtomicReference<State> STATE = new AtomicReference<>(new State(Stage.RUNTIME, 0, false));

    private StartupProgress() {
    }

    public static Stage stage() {
        return STATE.get().stage();
    }

    public static boolean isRunning() {
        return !STATE.get().finished();
    }

    public static void advance(Stage stage) {
        STATE.updateAndGet(current -> current.finished() || stage.ordinal() <= current.stage().ordinal()
                ? current : new State(stage, current.reloadGeneration(), false));
    }

    public static void beginResourceReload() {
        STATE.updateAndGet(current -> current.finished()
                ? current : new State(Stage.ASSETS, current.reloadGeneration() + 1, false));
    }

    public static int reloadGeneration() {
        return STATE.get().reloadGeneration();
    }

    public static void beginApplying(int generation) {
        STATE.updateAndGet(current -> current.finished() || current.reloadGeneration() != generation
                || current.stage().ordinal() >= Stage.APPLYING.ordinal()
                ? current : new State(Stage.APPLYING, generation, false));
    }

    public static void complete() {
        STATE.updateAndGet(current -> current.finished()
                ? current : new State(current.stage(), current.reloadGeneration(), true));
    }

    public enum Stage {
        RUNTIME("Starting the runtime..."),
        MIXINS("Preparing mixins..."),
        DATA_FIXER("Optimizing data fixes..."),
        REGISTRIES("Building registries and caches..."),
        MODULES("Loading modules..."),
        GRAPHICS("Setting up graphics..."),
        SYSTEMS("Starting client systems..."),
        PACKS("Scanning resource packs..."),
        ASSETS("Loading game assets..."),
        ATLASES("Building texture atlases..."),
        MODELS("Baking models..."),
        APPLYING("Applying resources..."),
        READY("About to complete...");

        private final String label;

        Stage(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private record State(Stage stage, int reloadGeneration, boolean finished) {
    }
}
