package com.pvp_utils.client.onboarding;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.AccentColors;
import com.pvp_utils.client.gui.clickgui.theme.UiTheme;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaScreen;
import com.pvp_utils.client.render.chroma.effects.ChromaTransition;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class OnboardingScreen extends Screen implements ChromaScreen {
    private static final float DESIGN_WIDTH = 760f;
    private static final float DESIGN_HEIGHT = 520f;
    private static final UiTheme GUIDE_THEME = UiTheme.DARK;
    private static final int GUIDE_ACCENT = 0xFFF1F2F4;
    private static final long EXIT_NANOS = 450_000_000L;
    private static final long ENTER_NANOS = 600_000_000L;
    private static final String[] TIPS = {
            "Don't worry, it'll be done in just a moment~",
            "When you're feeling anxious, you might as well take a look at the sky outside the window.",
            "Maybe if you don't stare at it, it'll load a bit faster?",
            "Don't keep staring at me!"
    };
    private final OnboardingChoices choices = new OnboardingChoices();
    private final OnboardingPreview preview = new OnboardingPreview();
    private final Map<TextKey, TextLine> texts = new LinkedHashMap<>();
    private final long configuringNanos = ThreadLocalRandom.current().nextLong(8_000_000_000L, 12_000_000_001L);
    private Scene scene = Scene.FIRST_TIME;
    private Scene pendingScene;
    private long lastRenderNanos;
    private long clockNanos;
    private long sceneStartedNanos;
    private long transitionStartedNanos;
    private long nextTipNanos;
    private long tipStartedNanos;
    private int tipIndex = ThreadLocalRandom.current().nextInt(TIPS.length);
    private int previousTip = -1;
    private boolean closing;
    private long closingStartedNanos;
    private long fontRevision = -1L;
    private boolean textLanguage;
    private boolean fallbackLanguageChosen;
    private int fallbackAccentPage;
    private int background = 0xFF000000;
    private int primaryText = 0xFFFFFFFF;
    private int secondaryText = 0xFF888888;
    private Frame frame;

    public OnboardingScreen() {
        super(Component.literal("PVPUtils"));
    }

    @Override
    public void added() {
        lastRenderNanos = 0L;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        long delta = lastRenderNanos == 0L ? 0L : Math.max(0L, now - lastRenderNanos);
        lastRenderNanos = now;
        boolean visible = minecraft.getOverlay() == null && !minecraft.getWindow().isMinimized();
        if (visible) clockNanos += delta;
        if (pendingScene != null && clockNanos - transitionStartedNanos >= EXIT_NANOS) {
            scene = pendingScene;
            pendingScene = null;
            sceneStartedNanos = clockNanos;
            if (scene == Scene.CONFIGURING) {
                previousTip = -1;
                tipStartedNanos = clockNanos;
                nextTipNanos = clockNanos + randomTipInterval();
            }
        }
        long elapsed = Math.max(0L, clockNanos - sceneStartedNanos);
        if (visible && pendingScene == null && !closing) {
            if (scene == Scene.CONFIGURING && clockNanos >= nextTipNanos) {
                previousTip = tipIndex;
                tipIndex = (tipIndex + ThreadLocalRandom.current().nextInt(1, TIPS.length)) % TIPS.length;
                tipStartedNanos = clockNanos;
                nextTipNanos = clockNanos + randomTipInterval();
            }
            long duration = scene == Scene.CONFIGURING ? configuringNanos : scene.durationNanos;
            if (duration > 0L && elapsed >= duration) {
                if (scene == Scene.FINISH) {
                    closing = true;
                    closingStartedNanos = clockNanos;
                } else {
                    request(scene.next());
                }
            }
        }
        if (closing && clockNanos - closingStartedNanos >= 800_000_000L) {
            Config.onboardingCompleted = true;
            Config.save();
            minecraft.setScreen(new TitleScreen(true));
            return;
        }
        float opacity = pendingScene == null
                ? smooth((float) elapsed / ENTER_NANOS)
                : 1f - smooth((float) (clockNanos - transitionStartedNanos) / EXIT_NANOS);
        if (!visible && clockNanos == 0L) opacity = 0f;
        float blend = visible ? (float) (1.0 - Math.exp(-Math.min(delta * 1e-9, 0.1) * 8.0)) : 0f;
        boolean setup = scene.ordinal() >= Scene.APOLOGY.ordinal();
        background = AccentColors.mix(background, setup ? 0xFF101114 : 0xFF000000, blend);
        primaryText = AccentColors.mix(primaryText, 0xFFF4F4F5, blend);
        secondaryText = AccentColors.mix(secondaryText, setup ? 0xFF92959C : 0xFF888888, blend);
        float scale = Math.max(0.001f, Math.min(1f,
                Math.min(Math.max(1f, width - 32f) / DESIGN_WIDTH, Math.max(1f, height - 24f) / DESIGN_HEIGHT)));
        frame = new Frame(scene, opacity, scale, (width - DESIGN_WIDTH * scale) * 0.5f,
                (height - DESIGN_HEIGHT * scale) * 0.5f, mouseX, mouseY, elapsed * 1e-9,
                background, primaryText, secondaryText, GUIDE_ACCENT,
                smooth((float) (clockNanos - tipStartedNanos) / 450_000_000L),
                closing ? smooth((float) (clockNanos - closingStartedNanos) / 800_000_000L) : 0f,
                visible && pendingScene == null && opacity >= 0.999f && !closing);
        if (ChromaGraphics.hasFailed()) renderFallback(graphics);
    }

    @Override
    public void renderChroma(Canvas canvas) {
        Frame current = frame;
        if (current == null) return;
        ChromaDraw.fill(canvas, 0f, 0f, width, height, current.background());
        if (current.opacity() > 0f) {
            if (current.scene().isInteractive()) {
                drawSetup(canvas, current);
            } else {
                drawMessage(canvas, current);
            }
        }
        if (current.finishFade() > 0f) {
            ChromaDraw.fill(canvas, 0f, 0f, width, height, AccentColors.withAlpha(0xFF000000, current.finishFade()));
        }
    }

    private void drawMessage(Canvas canvas, Frame current) {
        float scale = current.scale();
        String[] lines = heading(current.scene()).split("\n");
        float firstY = height * 0.5f - lines.length * 18f * scale;
        for (int i = 0; i < lines.length; i++) {
            drawCentered(canvas, lines[i], 24f, width * 0.5f, firstY + i * 36f * scale,
                    708f * scale, scale, current.primaryText(), current.opacity());
        }
        if (current.scene() == Scene.CONFIGURING) {
            float tipY = height * 0.5f + 30f * scale;
            Rect clip = Rect.makeXYWH(current.x(18f), tipY - 2f * scale, 724f * scale, 22f * scale);
            int save = canvas.save();
            try {
                canvas.clipRect(clip, true);
                float progress = current.tipProgress();
                if (previousTip >= 0 && progress < 1f) {
                    drawCentered(canvas, TIPS[previousTip], 11f, width * 0.5f,
                            tipY - progress * 22f * scale, 708f * scale, scale,
                            current.secondaryText(), current.opacity() * (1f - progress));
                }
                drawCentered(canvas, TIPS[tipIndex], 11f, width * 0.5f,
                        tipY + (previousTip < 0 ? 0f : (1f - progress) * 22f * scale),
                        708f * scale, scale, current.secondaryText(),
                        current.opacity() * (previousTip < 0 ? 1f : progress));
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private void drawSetup(Canvas canvas, Frame current) {
        float scale = current.scale();
        drawCentered(canvas, "PVPUtils", 12f, width * 0.5f, current.y(8f), 708f * scale,
                scale, current.secondaryText(), current.opacity());
        drawCentered(canvas, heading(current.scene()), 24f, width * 0.5f, current.y(39f),
                708f * scale, scale, current.primaryText(), current.opacity());
        String subtitle = subtitle(current.scene());
        if (!subtitle.isEmpty()) {
            drawCentered(canvas, subtitle, 13f, width * 0.5f, current.y(76f), 708f * scale,
                    scale, current.secondaryText(), current.opacity());
        }
        Rect content = Rect.makeXYWH(current.x(44f), current.y(108f), 672f * scale,
                (current.scene().isChoice() ? 326f : 274f) * scale);
        ChromaTransition.draw(canvas, content, current.opacity(), 0f, () -> {
            if (current.scene().isChoice()) {
                choices.draw(canvas, content.getLeft(), content.getTop(), content.getWidth(), content.getHeight(),
                        current.scene().choiceMode(), current.mouseX(), current.mouseY(), current.seconds());
            } else {
                preview.draw(canvas, content.getLeft(), content.getTop(), content.getWidth(), content.getHeight(),
                        current.scene().previewMode(), current.seconds(), GUIDE_THEME, current.accent());
            }
        });
        if (!current.scene().isChoice()) {
            String[] details = detail(current.scene()).split("\n");
            for (int i = 0; i < details.length; i++) {
                drawCentered(canvas, details[i], 13f, width * 0.5f, current.y(398f + i * 22f),
                        708f * scale, scale, current.secondaryText(), current.opacity());
            }
        }
        if (current.scene() != Scene.LANGUAGE) {
            drawButton(canvas, current, 44f, 466f, 96f, UiText.t("上一步", "Back"), false, true);
        }
        drawButton(canvas, current, 592f, 466f, 124f,
                current.scene() == Scene.HUD ? UiText.t("完成引导", "Finish") : UiText.t("继续", "Continue"),
                true, canContinue());
        int selected = current.scene().ordinal() - Scene.LANGUAGE.ordinal();
        for (int i = 0; i < 6; i++) {
            ChromaDraw.rounded(canvas, current.x(345f + i * 14f), current.y(481f),
                    6f * scale, 6f * scale, 3f * scale,
                    AccentColors.withAlpha(i == selected ? current.accent() : current.secondaryText(),
                            current.opacity() * (i == selected ? 1f : 0.3f)));
        }
    }

    private void drawButton(Canvas canvas, Frame current, float x, float y, float buttonWidth,
                            String label, boolean filled, boolean enabled) {
        float scale = current.scale();
        Rect bounds = Rect.makeXYWH(current.x(x), current.y(y), buttonWidth * scale, 34f * scale);
        boolean hovered = current.interactive() && enabled && contains(bounds, current.mouseX(), current.mouseY());
        int color = filled ? current.accent() : 0xFF24262C;
        if (hovered) color = AccentColors.mix(color, current.primaryText(), 0.12f);
        ChromaDraw.rounded(canvas, bounds.getLeft(), bounds.getTop(), bounds.getWidth(), bounds.getHeight(),
                9f * scale, AccentColors.withAlpha(color, current.opacity() * (enabled ? 1f : 0.35f)));
        int text = filled ? UiTheme.readableText(0xFFFFFFFF, color) : current.primaryText();
        TextLine line = text(label, 12f);
        float textWidth = bounds.getWidth() - 10f * scale;
        float textScale = Math.min(scale, textWidth / Math.max(1f, line.text().width()));
        drawCentered(canvas, label, 12f, bounds.getLeft() + bounds.getWidth() * 0.5f,
                bounds.getTop() + (bounds.getHeight() - line.height() * textScale) * 0.5f, textWidth, textScale,
                text, current.opacity() * (enabled ? 1f : 0.5f));
    }

    private void drawCentered(Canvas canvas, String value, float size, float centerX, float top,
                              float maxWidth, float requestedScale, int color, float opacity) {
        TextLine line = text(value, size);
        float scale = Math.min(requestedScale, maxWidth / Math.max(1f, line.text().width()));
        Rect bounds = Rect.makeXYWH(centerX - maxWidth * 0.5f, top - 3f * scale,
                maxWidth, (line.height() + 6f) * scale);
        ChromaTransition.draw(canvas, bounds, opacity, 0f, () -> {
            int save = canvas.save();
            try {
                canvas.translate(centerX, top);
                canvas.scale(scale, scale);
                line.text().drawTop(canvas, -line.text().width() * 0.5f, 0f, color);
            } finally {
                canvas.restoreToCount(save);
            }
        });
    }

    private TextLine text(String value, float size) {
        long revision = ChromaDraw.textFontRevision();
        if (revision != fontRevision || textLanguage != Config.isChinese) {
            closeTexts();
            fontRevision = revision;
            textLanguage = Config.isChinese;
        }
        TextKey key = new TextKey(value, size);
        TextLine cached = texts.get(key);
        if (cached != null) return cached;
        if (texts.size() >= 96) {
            TextKey oldest = texts.keySet().iterator().next();
            texts.remove(oldest).text().close();
        }
        ChromaDraw.withFont(ChromaDraw.MISANS_REGULAR, () -> {
            FontMetrics metrics = ChromaDraw.textMetrics(size);
            texts.put(key, new TextLine(ChromaDraw.prepareText(value, size, false),
                    metrics.getDescent() - metrics.getAscent()));
        });
        return texts.get(key);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Frame current = frame;
        if (event.button() != 0 || current == null || !current.interactive() || !scene.isInteractive()) return false;
        float mouseX = (float) event.x();
        float mouseY = (float) event.y();
        if (contains(Rect.makeXYWH(current.x(592f), current.y(466f), 124f * current.scale(),
                34f * current.scale()), mouseX, mouseY) && canContinue()) {
            request(scene.next());
            return true;
        }
        if (scene != Scene.LANGUAGE && contains(Rect.makeXYWH(current.x(44f), current.y(466f),
                96f * current.scale(), 34f * current.scale()), mouseX, mouseY)) {
            request(Scene.values()[scene.ordinal() - 1]);
            return true;
        }
        if (scene.isChoice()) {
            return ChromaGraphics.hasFailed() ? fallbackClick(mouseX, mouseY) : choices.click(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (frame == null || !frame.interactive() || scene != Scene.ACCENT) return false;
        if (ChromaGraphics.hasFailed()) {
            fallbackAccentPage = Math.clamp(fallbackAccentPage + (vertical < 0 ? 1 : -1), 0,
                    (AccentColors.Preset.values().length - 1) / 12);
        } else {
            choices.scroll(vertical);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (frame == null || !frame.interactive() || !scene.isInteractive()) return true;
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && canContinue()) {
            request(scene.next());
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT && scene != Scene.LANGUAGE) {
            request(Scene.values()[scene.ordinal() - 1]);
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean canContinue() {
        return scene != Scene.LANGUAGE || choices.languageChosen() || fallbackLanguageChosen;
    }

    private void request(Scene next) {
        if (pendingScene != null || closing) return;
        pendingScene = next;
        transitionStartedNanos = clockNanos;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        closeTexts();
        choices.close();
        preview.close();
        frame = null;
    }

    private void closeTexts() {
        for (TextLine line : texts.values()) line.text().close();
        texts.clear();
    }

    private static long randomTipInterval() {
        return ThreadLocalRandom.current().nextLong(3_000_000_000L, 5_000_000_001L);
    }

    private static float smooth(float amount) {
        float t = Math.clamp(amount, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static boolean contains(Rect bounds, float x, float y) {
        return x >= bounds.getLeft() && x <= bounds.getRight() && y >= bounds.getTop() && y <= bounds.getBottom();
    }

    private static String heading(Scene scene) {
        return switch (scene) {
            case FIRST_TIME -> "I suppose this is your first time here.";
            case BASIC_SETUP -> "Let me walk you through some necessary basic setup.";
            case CONFIGURING -> "Configuring...";
            case APOLOGY -> "I'm sorry for keeping you waiting for so long.";
            case START -> "So let's get started right away!";
            case LANGUAGE -> "Choose your language.";
            case THEME -> UiText.t("选择主题", "Choose your theme");
            case ACCENT -> UiText.t("强调色选择", "Choose your accent color");
            case CONTROLS -> UiText.t("接下来，了解一些基本操作", "Let's learn the basics");
            case INVENTORY -> UiText.t("也可以从背包打开 ClickGUI", "Open ClickGUI from your inventory, too");
            case HUD -> UiText.t("调整 UI 组件", "Arrange your HUD");
            case FINISH -> UiText.t("基本引导已经结束，\n我已经迫不及待与你相见了！",
                    "That's all for the basics.\nI can't wait to see you in game!");
        };
    }

    private static String subtitle(Scene scene) {
        return switch (scene) {
            case THEME -> UiText.t("很好，接下来选一个你觉得好看的主题吧！", "Great! Pick a theme you like.");
            case ACCENT -> UiText.t("选择一个你最喜欢的颜色吧！", "Choose your favorite color!");
            case CONTROLS -> UiText.t("很好，在完成之前，我来教你一些基本的使用方法", "Before we finish, let me show you how things work.");
            default -> "";
        };
    }

    private static String detail(Scene scene) {
        return switch (scene) {
            case CONTROLS -> UiText.t("按下键盘上的右 Shift 即可呼出 ClickGUI", "Press Right Shift to open ClickGUI.");
            case INVENTORY -> UiText.t("在背包界面的右上角点击 PVPUtils 按钮也可以！",
                    "You can also click the PVPUtils button in the top-right corner of your inventory!");
            case HUD -> UiText.t("打开聊天栏可以配置各种 UI 组件的位置和大小。\n如果挡住视野，可在 ClickGUI 关闭；关闭后也只能从 ClickGUI 重新开启。",
                    "Open chat to move and resize your HUD components.\nHide them in ClickGUI when needed; use ClickGUI to turn them back on.");
            default -> "";
        };
    }

    private void renderFallback(GuiGraphics graphics) {
        Frame current = frame;
        graphics.fill(0, 0, width, height, current.background());
        String[] lines = heading(scene).split("\n");
        int top = scene.isInteractive() ? Math.round(current.y(39f)) : height / 2 - lines.length * 7;
        for (int i = 0; i < lines.length; i++) fallbackText(graphics, lines[i], width / 2, top + i * 16, current.primaryText());
        if (scene == Scene.CONFIGURING) fallbackText(graphics, TIPS[tipIndex], width / 2, top + 26, current.secondaryText());
        if (scene.isChoice()) {
            int count = fallbackCount();
            for (int i = 0; i < count; i++) {
                Rect bounds = fallbackBounds(current, i);
                graphics.fill((int) bounds.getLeft(), (int) bounds.getTop(), (int) bounds.getRight(),
                        (int) bounds.getBottom(), 0xFF24262C);
                fallbackText(graphics, fallbackLabel(i), Math.round(bounds.getLeft() + bounds.getWidth() * 0.5f),
                        Math.round(bounds.getTop() + 10f * current.scale()), current.primaryText());
            }
        } else if (scene.isInteractive()) {
            String[] details = detail(scene).split("\n");
            for (int i = 0; i < details.length; i++) fallbackText(graphics, details[i], width / 2,
                    Math.round(current.y(230f)) + i * 16, current.secondaryText());
        }
        if (scene.isInteractive()) {
            if (scene != Scene.LANGUAGE) fallbackText(graphics, UiText.t("上一步", "Back"), Math.round(current.x(92f)),
                    Math.round(current.y(476f)), current.primaryText());
            fallbackText(graphics, UiText.t("继续", "Continue"), Math.round(current.x(654f)),
                    Math.round(current.y(476f)), canContinue() ? current.primaryText() : current.secondaryText());
            if (scene == Scene.ACCENT) fallbackText(graphics, UiText.t("滚轮切换颜色页", "Scroll to change palette page"),
                    width / 2, Math.round(current.y(410f)), current.secondaryText());
        }
        if (current.finishFade() > 0f) graphics.fill(0, 0, width, height,
                AccentColors.withAlpha(0xFF000000, current.finishFade()));
    }

    private void fallbackText(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(minecraft.font, text, centerX - minecraft.font.width(text) / 2, y,
                AccentColors.withAlpha(color, frame.opacity()), false);
    }

    private int fallbackCount() {
        return switch (scene) {
            case LANGUAGE -> 2;
            case THEME -> UiTheme.values().length;
            case ACCENT -> Math.min(12, AccentColors.Preset.values().length - fallbackAccentPage * 12);
            default -> 0;
        };
    }

    private String fallbackLabel(int index) {
        return switch (scene) {
            case LANGUAGE -> index == 0 ? "简体中文" : "English";
            case THEME -> UiTheme.values()[index].label();
            case ACCENT -> AccentColors.Preset.values()[fallbackAccentPage * 12 + index].label;
            default -> "";
        };
    }

    private static Rect fallbackBounds(Frame current, int index) {
        return Rect.makeXYWH(current.x(60f + (index % 3) * 216f), current.y(140f + (index / 3) * 56f),
                204f * current.scale(), 46f * current.scale());
    }

    private boolean fallbackClick(float mouseX, float mouseY) {
        for (int i = 0; i < fallbackCount(); i++) {
            if (!contains(fallbackBounds(frame, i), mouseX, mouseY)) continue;
            switch (scene) {
                case LANGUAGE -> {
                    Config.isChinese = i == 0;
                    fallbackLanguageChosen = true;
                    Config.save();
                }
                case THEME -> UiTheme.values()[i].select();
                case ACCENT -> AccentColors.select(AccentColors.Preset.values()[fallbackAccentPage * 12 + i]);
                default -> { }
            }
            return true;
        }
        return false;
    }

    private enum Scene {
        FIRST_TIME(3_600_000_000L), BASIC_SETUP(3_600_000_000L), CONFIGURING(0L),
        APOLOGY(3_400_000_000L), START(2_800_000_000L), LANGUAGE(0L), THEME(0L),
        ACCENT(0L), CONTROLS(0L), INVENTORY(0L), HUD(0L), FINISH(3_800_000_000L);

        private final long durationNanos;

        Scene(long durationNanos) {
            this.durationNanos = durationNanos;
        }

        private Scene next() {
            return values()[Math.min(ordinal() + 1, values().length - 1)];
        }

        private boolean isInteractive() {
            return ordinal() >= LANGUAGE.ordinal() && ordinal() <= HUD.ordinal();
        }

        private boolean isChoice() {
            return this == LANGUAGE || this == THEME || this == ACCENT;
        }

        private OnboardingChoices.Mode choiceMode() {
            return switch (this) {
                case LANGUAGE -> OnboardingChoices.Mode.LANGUAGE;
                case THEME -> OnboardingChoices.Mode.THEME;
                case ACCENT -> OnboardingChoices.Mode.ACCENT;
                default -> throw new IllegalStateException();
            };
        }

        private OnboardingPreview.Mode previewMode() {
            return switch (this) {
                case CONTROLS -> OnboardingPreview.Mode.KEYBOARD;
                case INVENTORY -> OnboardingPreview.Mode.INVENTORY;
                case HUD -> OnboardingPreview.Mode.HUD;
                default -> throw new IllegalStateException();
            };
        }
    }

    private record TextKey(String value, float size) {
    }

    private record TextLine(ChromaDraw.PreparedText text, float height) {
    }

    private record Frame(Scene scene, float opacity, float scale, float originX, float originY,
                         float mouseX, float mouseY, double seconds, int background, int primaryText,
                         int secondaryText, int accent, float tipProgress, float finishFade, boolean interactive) {
        private float x(float local) {
            return originX + local * scale;
        }

        private float y(float local) {
            return originY + local * scale;
        }
    }
}
