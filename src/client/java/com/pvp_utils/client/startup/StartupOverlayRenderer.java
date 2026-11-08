package com.pvp_utils.client.startup;

import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FontMetrics;

public final class StartupOverlayRenderer implements AutoCloseable {
    public static final String TITLE = "Starting PVPUtils";
    public static final String WELCOME = "Champion, Welcome back!";
    private static final boolean TEST_SLOW_STARTUP = false;
    private static final long TEST_STAGE_DURATION_NANOS = 1_200_000_000L;
    private static final long TEST_DOT_INTERVAL_NANOS = 500_000_000L;
    private static final float TEST_TRANSITION_SLOWDOWN = 2f;
    private static final StartupProgress.Stage[] STAGES = StartupProgress.Stage.values();
    private static final int DOT_COUNT = 6;
    private static final long DOT_INTERVAL_NANOS = 350_000_000L;
    private static final float TITLE_SIZE = 22f;
    private static final float STATUS_SIZE = 11f;
    private final SkijaUi.PreparedText[] titles = new SkijaUi.PreparedText[DOT_COUNT];
    private SkijaUi.PreparedText welcome;
    private SkijaUi.PreparedText status;
    private StartupProgress.Stage preparedStage;
    private long fontRevision = -1L;
    private float titleHeight;
    private float statusHeight;
    private boolean started;
    private long startedNanos;
    private StartupProgress.Stage displayedStage;
    private long stageStartedNanos;
    private boolean ready;
    private long readyNanos;
    private Frame frame;

    public void updateFrame(int width, int height, long nowNanos, boolean resourcesReady) {
        if (!started) {
            started = true;
            startedNanos = nowNanos;
            displayedStage = TEST_SLOW_STARTUP ? StartupProgress.Stage.RUNTIME : StartupProgress.stage();
            stageStartedNanos = nowNanos;
        }
        StartupProgress.Stage actualStage = resourcesReady ? StartupProgress.Stage.READY : StartupProgress.stage();
        if (TEST_SLOW_STARTUP) {
            if (actualStage.ordinal() < displayedStage.ordinal()) {
                displayedStage = actualStage;
                stageStartedNanos = nowNanos;
            } else if (displayedStage.ordinal() < actualStage.ordinal()
                    && nowNanos - stageStartedNanos >= TEST_STAGE_DURATION_NANOS) {
                displayedStage = STAGES[displayedStage.ordinal() + 1];
                stageStartedNanos = nowNanos;
            }
        } else {
            displayedStage = actualStage;
        }
        boolean stagesFinished = !TEST_SLOW_STARTUP || displayedStage == StartupProgress.Stage.READY
                && nowNanos - stageStartedNanos >= TEST_STAGE_DURATION_NANOS;
        if (resourcesReady && stagesFinished && !ready) {
            ready = true;
            readyNanos = nowNanos;
        }
        long dotInterval = TEST_SLOW_STARTUP ? TEST_DOT_INTERVAL_NANOS : DOT_INTERVAL_NANOS;
        int dots = 1 + (int) (Math.max(0L, nowNanos - startedNanos) / dotInterval % DOT_COUNT);
        float elapsed = ready ? Math.max(0f, (nowNanos - readyNanos) / 1_000_000f) : 0f;
        if (TEST_SLOW_STARTUP) elapsed /= TEST_TRANSITION_SLOWDOWN;
        float loadingAlpha = ready ? 1f - smooth((elapsed - 250f) / 200f) : 1f;
        float welcomeAlpha = ready ? smooth((elapsed - 450f) / 350f) : 0f;
        float backgroundAlpha = ready ? 1f - smooth((elapsed - 1450f) / 850f) : 1f;
        frame = new Frame(width, height, dots, displayedStage,
                loadingAlpha * backgroundAlpha, welcomeAlpha * backgroundAlpha,
                backgroundAlpha, ready, ready && elapsed >= 2300f);
    }

    public Frame frame() {
        return frame;
    }

    public void renderSkija(Canvas canvas) {
        Frame current = frame;
        if (current == null || current.width() <= 0 || current.height() <= 0 || current.finished()) return;
        if (current.backgroundAlpha() > 0f) {
            SkijaUi.fill(canvas, 0f, 0f, current.width(), current.height(), color(0, current.backgroundAlpha()));
        }
        if (current.loadingAlpha() <= 0f && current.welcomeAlpha() <= 0f) return;
        prepareTexts(current.stage());
        float textWidth = Math.max(Math.max(titles[DOT_COUNT - 1].width(), welcome.width()), status.width());
        float textHeight = titleHeight + 2f * (9f + statusHeight);
        float scale = Math.min(1f, Math.min(Math.max(0f, current.width() - 32f) / textWidth,
                Math.max(0f, current.height() - 24f) / textHeight));
        if (scale <= 0f) return;
        int save = canvas.save();
        try {
            canvas.translate(current.width() * 0.5f, current.height() * 0.5f);
            canvas.scale(scale, scale);
            float top = -titleHeight * 0.5f;
            if (current.loadingAlpha() > 0f) {
                titles[current.dots() - 1].drawTop(canvas, -titles[DOT_COUNT - 1].width() * 0.5f,
                        top, color(0xFFFFFF, current.loadingAlpha()));
                status.drawTop(canvas, -status.width() * 0.5f, top + titleHeight + 9f,
                        color(0x888888, current.loadingAlpha()));
            }
            if (current.welcomeAlpha() > 0f) {
                welcome.drawTop(canvas, -welcome.width() * 0.5f, top,
                        color(0xFFFFFF, current.welcomeAlpha()));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void prepareTexts(StartupProgress.Stage stage) {
        long revision = SkijaUi.textFontRevision();
        if (welcome == null || fontRevision != revision) {
            closeTexts();
            SkijaUi.withFont(SkijaUi.MISANS_REGULAR, () -> {
                for (int i = 0; i < DOT_COUNT; i++) {
                    titles[i] = SkijaUi.prepareText(TITLE + ".".repeat(i + 1), TITLE_SIZE, false);
                }
                welcome = SkijaUi.prepareText(WELCOME, TITLE_SIZE, false);
                FontMetrics titleMetrics = SkijaUi.textMetrics(TITLE_SIZE);
                titleHeight = titleMetrics.getDescent() - titleMetrics.getAscent();
            });
            fontRevision = revision;
        }
        if (status == null || preparedStage != stage) {
            if (status != null) status.close();
            SkijaUi.withFont(SkijaUi.MISANS_REGULAR, () -> {
                status = SkijaUi.prepareText(stage.label(), STATUS_SIZE, false);
                FontMetrics statusMetrics = SkijaUi.textMetrics(STATUS_SIZE);
                statusHeight = statusMetrics.getDescent() - statusMetrics.getAscent();
                preparedStage = stage;
            });
        }
    }

    private static float smooth(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static int color(int rgb, float opacity) {
        return Math.round(Math.clamp(opacity, 0f, 1f) * 255f) << 24 | rgb;
    }

    private void closeTexts() {
        for (int i = 0; i < titles.length; i++) {
            if (titles[i] != null) titles[i].close();
            titles[i] = null;
        }
        if (welcome != null) welcome.close();
        welcome = null;
        if (status != null) status.close();
        status = null;
        preparedStage = null;
    }

    @Override
    public void close() {
        closeTexts();
        frame = null;
    }

    public record Frame(int width, int height, int dots, StartupProgress.Stage stage,
                        float loadingAlpha, float welcomeAlpha, float backgroundAlpha,
                        boolean rendersScreenBehind, boolean finished) {
    }
}
