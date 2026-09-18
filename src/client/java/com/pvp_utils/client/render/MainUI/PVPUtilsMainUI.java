package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.alt.AltManagerScreen;
import com.pvp_utils.client.via.ViaFabricPlusBridge;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class PVPUtilsMainUI extends Screen {
    private static final Identifier TEXT_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_text");
    private static final long HINT_DURATION_MS = 5000L;
    private static final long HINT_FADE_IN_MS = 400L;
    private static final long HINT_FADE_OUT_MS = 800L;
    private static final long ENTRY_FADE_DELAY_MS = 2100L;
    private static final long ENTRY_FADE_IN_MS = 850L;
    private static final float SETTINGS_SIZE = 36f;
    private static final float SETTINGS_MARGIN = 24f;
    private static final float MAIN_LAYOUT_BASE_SCALE = 0.80f;
    private static final float MAIN_LAYOUT_BASE_GUI_SCALE = 2f;
    private static final float MAIN_LAYOUT_MARGIN_PIXELS = 48f;
    private static final float MAIN_LAYOUT_BASE_WIDTH = 246f;
    private static final float MAIN_LAYOUT_BASE_HEIGHT = 360f;
    private static final Identifier BACKGROUND_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_custom_background");

    private MainUIShader shader;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final boolean showEntryHint;
    private final String fixedShaderPath;
    private final boolean entryFade;
    private final boolean entryFadeDelay;
    private boolean returnTransition;
    private final List<MenuButton> buttons = new ArrayList<>();
    private TitleHitBox titleHitBox = new TitleHitBox(0f, 0f, 0f, 0f);
    private Surface textSurface;
    private DynamicTexture textTexture;
    private DynamicTexture backgroundTexture;
    private MainUIVideoBackground videoBackground;
    private int textX;
    private int textY;
    private int textW;
    private int textH;
    private int textPixelW = -1;
    private int textPixelH = -1;
    private int textGuiW = -1;
    private int textGuiH = -1;
    private boolean nativeLoaded;
    private int pressedIndex = -1;
    private boolean titlePressed;
    private long hintStartMs;
    private boolean settingsOpen;
    private boolean settingsHover;
    private float settingsPanelProgress;
    private float settingsHoverProgress;
    private float backgroundOffsetX;
    private float backgroundOffsetY;
    private int backgroundTextureW = -1;
    private int backgroundTextureH = -1;
    private int lastWindowPixelW = -1;
    private int lastWindowPixelH = -1;
    private String loadedBackground = "";
    private boolean lightSettingsTheme;
    private long lastRenderMs;
    private long entryFadeStartMs;
    private boolean pendingGpuUi;
    private float pendingGpuAlpha = 1f;
    private long returnTransitionStartMs;
    private PVPUtilsSingleplayerScreen embeddedSingleplayer;
    private PVPUtilsMultiplayerScreen embeddedMultiplayer;
    private AltManagerScreen embeddedAltManager;
    private PVPUtilsViaFabricPlusScreen embeddedViaFabricPlus;

    public PVPUtilsMainUI(Screen parent) {
        this(parent, false);
    }

    public PVPUtilsMainUI(Screen parent, boolean showEntryHint) {
        this(parent, showEntryHint, false, null);
    }

    public PVPUtilsMainUI(Screen parent, boolean showEntryHint, boolean entryFadeDelay) {
        this(parent, showEntryHint, entryFadeDelay, null);
    }

    private PVPUtilsMainUI(Screen parent, boolean showEntryHint, boolean entryFadeDelay, String fixedShaderPath) {
        this(parent, showEntryHint, entryFadeDelay, fixedShaderPath, false);
    }

    private PVPUtilsMainUI(Screen parent, boolean showEntryHint, boolean entryFadeDelay, String fixedShaderPath, boolean returnTransition) {
        super(Component.literal("Minecraft"));
        this.showEntryHint = showEntryHint;
        this.fixedShaderPath = fixedShaderPath;
        this.entryFade = showEntryHint || parent instanceof TitleScreen || entryFadeDelay;
        this.entryFadeDelay = entryFadeDelay;
        this.returnTransition = returnTransition;
    }

    static PVPUtilsMainUI returningFromSingleplayer(String shaderPath) {
        return new PVPUtilsMainUI(null, false, false, shaderPath, true);
    }

    @Override
    protected void init() {
        if (shader != null) shader.close();
        String sharedShaderPath = initialShaderPath();
        shader = sharedShaderPath == null ? MainUIShader.random() : MainUIShader.named(sharedShaderPath);
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
        hintStartMs = showEntryHint ? System.currentTimeMillis() : 0L;
        entryFadeStartMs = entryFade ? System.currentTimeMillis() : 0L;
        returnTransitionStartMs = returnTransition ? animationNowNanos() : 0L;
        invalidateTextTexture();
        refreshThemeFromBackground();
        buttons.clear();
        buttons.add(new MenuButton("Single player", "\uE7FD", this::startSingleplayerTransition));
        buttons.add(new MenuButton("Multi player", "\uE7EF", () -> {
            if (this.minecraft == null) return;
            startMultiplayerTransition();
        }));
        buttons.add(new MenuButton("Alt Manager", "\uE853", () -> {
            startAltManagerTransition();
        }));
        if (ViaFabricPlusBridge.isInstalled()) {
            buttons.add(new MenuButton("ViaFabricPlus", "\uE64C", this::startViaFabricPlusTransition));
        }
        if (ViaFabricPlusBridge.isModMenuInstalled()) {
            buttons.add(new MenuButton("Mod Menu", "\uE241", () -> {
                if (this.minecraft != null) {
                    ViaFabricPlusBridge.openModMenu(returnParent());
                }
            }));
        }
        buttons.add(new MenuButton("Options", "\uE8B8", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(returnParent(), this.minecraft.options));
        }));
        buttons.add(new MenuButton("Shutdown", "\uE8AC", () -> {
            if (this.minecraft != null) this.minecraft.stop();
        }));
        updateButtonPositions();
    }

    @Override
    protected void repositionElements() {
        updateButtonPositions();
        if (embeddedSingleplayer != null) embeddedSingleplayer.resize(this.width, this.height);
        if (embeddedMultiplayer != null) embeddedMultiplayer.resize(this.width, this.height);
        if (embeddedAltManager != null) embeddedAltManager.resize(this.width, this.height);
        if (embeddedViaFabricPlus != null) embeddedViaFabricPlus.resize(this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (renderEmbeddedPage(graphics, mouseX, mouseY, delta)) return;
        float layoutScale = mainLayoutScale();
        updateSettingsPanel(
                MainUiScale.topRightX(mouseX, this.width, this.height, layoutScale),
                MainUiScale.topRightY(mouseY, this.width, this.height, layoutScale)
        );
        if (returnTransition && returnTransitionProgress() >= 1f) {
            returnTransition = false;
            returnTransitionStartMs = 0L;
        }
        updateSingleplayerTransition();
        updateButtonPositions();
        if (renderEmbeddedPage(graphics, mouseX, mouseY, delta)) return;
        renderMainBackground(graphics, mouseX, mouseY);
        float entryAlpha = entryAlpha();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).render(graphics, mouseX, mouseY, i == pressedIndex, entryAlpha);
        }
        pendingGpuAlpha = entryAlpha;
        pendingGpuUi = true;
        renderEntryHint(graphics, entryAlpha);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void tick() {
        super.tick();
        if (embeddedMultiplayer != null) embeddedMultiplayer.tick();
        if (embeddedAltManager != null) embeddedAltManager.tick();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (embeddedAltManager != null) return embeddedAltManager.keyPressed(event);
        if (embeddedViaFabricPlus != null) return embeddedViaFabricPlus.keyPressed(event);
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (embeddedAltManager != null) return embeddedAltManager.charTyped(event);
        if (embeddedViaFabricPlus != null) return embeddedViaFabricPlus.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (embeddedSingleplayer != null) return embeddedSingleplayer.mouseClicked(event, consumed);
        if (embeddedMultiplayer != null) return embeddedMultiplayer.mouseClicked(event, consumed);
        if (embeddedAltManager != null) return embeddedAltManager.mouseClicked(event, consumed);
        if (embeddedViaFabricPlus != null) return embeddedViaFabricPlus.mouseClicked(event, consumed);
        if (returnTransition || singleplayerTransitioning) {
            return true;
        }
        float layoutScale = mainLayoutScale();
        float settingsMouseX = MainUiScale.topRightX((int) event.x(), this.width, this.height, layoutScale);
        float settingsMouseY = MainUiScale.topRightY((int) event.y(), this.width, this.height, layoutScale);
        if (isInsideSettings(settingsMouseX, settingsMouseY)) {
            if (event.button() == 0) {
                settingsOpen = true;
                playClickSound();
                invalidateTextTexture();
            }
            return true;
        }
        if (settingsOpen && event.button() == 0) {
            if (isInsideBackgroundModeCustom(settingsMouseX, settingsMouseY)) {
                setBackgroundMode(Config.MainUIBackgroundMode.IMAGE);
                Config.save();
                refreshThemeFromBackground();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isInsideBackgroundModeBuiltin(settingsMouseX, settingsMouseY)) {
                setBackgroundMode(Config.MainUIBackgroundMode.GLSL);
                Config.save();
                lightSettingsTheme = true;
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isGlslBackground() && isInsideGlslModeRandom(settingsMouseX, settingsMouseY)) {
                Config.mainUIGlslMode = Config.MainUIGlslMode.RANDOM;
                Config.save();
                refreshShader();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isGlslBackground() && isInsideGlslModeFixed(settingsMouseX, settingsMouseY)) {
                Config.mainUIGlslMode = Config.MainUIGlslMode.FIXED;
                Config.mainUIGlslShader = MainUIShader.normalizeShader(shader == null ? Config.mainUIGlslShader : shader.fragmentPath());
                Config.save();
                reloadConfiguredShader();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isGlslBackground() && Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED && isInsideGlslShaderSelect(settingsMouseX, settingsMouseY)) {
                cycleGlslShader();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isInsideBackgroundModeVideo(settingsMouseX, settingsMouseY)) {
                setBackgroundMode(Config.MainUIBackgroundMode.VIDEO);
                ensureSelectedVideo();
                Config.save();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if ((isImageBackground() || isVideoBackground()) && isInsideOpenBackgroundFolder(settingsMouseX, settingsMouseY)) {
                MainUIBackgrounds.openFolder();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isImageBackground() && isInsideBackgroundImageSelect(settingsMouseX, settingsMouseY)) {
                cycleBackgroundImage();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isVideoBackground() && isInsideBackgroundVideoSelect(settingsMouseX, settingsMouseY)) {
                cycleBackgroundVideo();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isImageBackground() && isInsideMouseEffectToggle(settingsMouseX, settingsMouseY)) {
                Config.mainUIMouseEffect = !Config.mainUIMouseEffect;
                Config.save();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
        }
        if (titleHitBox.contains((float) event.x(), (float) event.y())) {
            if (event.button() == 0) {
                titlePressed = true;
            } else if (event.button() == 1) {
                refreshShader();
            }
            return true;
        }
        if (event.button() != 0) return false;
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains((float) event.x(), (float) event.y())) {
                pressedIndex = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (embeddedSingleplayer != null) return embeddedSingleplayer.mouseReleased(event);
        if (embeddedMultiplayer != null) return embeddedMultiplayer.mouseReleased(event);
        if (embeddedAltManager != null) return embeddedAltManager.mouseReleased(event);
        if (embeddedViaFabricPlus != null) return embeddedViaFabricPlus.mouseReleased(event);
        if (event.button() != 0) return false;
        if (titlePressed) {
            titlePressed = false;
            if (titleHitBox.contains((float) event.x(), (float) event.y())) {
                playClickSound();
                Config.useMainUI = false;
                Config.save();
                if (this.minecraft != null) this.minecraft.setScreen(new TitleScreen());
                return true;
            }
        }
        int index = pressedIndex;
        pressedIndex = -1;
        if (index >= 0 && index < buttons.size() && buttons.get(index).contains((float) event.x(), (float) event.y())) {
            playClickSound();
            buttons.get(index).action.run();
            return true;
        }
        return false;
    }

    private void updateButtonPositions() {
        if (buttons.isEmpty()) return;
        float cardW = animatedMenuCardWidth();
        float cardY = animatedMenuCardY();
        float buttonH = compactButtonHeight();
        float gap = compactButtonGap();
        float scale = mainLayoutScale();
        float startX = (this.width - cardW) * 0.5f + 16f * scale;
        float startY = cardY + 18f * scale;
        float buttonW = cardW - 32f * scale;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBounds(startX, startY + i * (buttonH + gap), buttonW, buttonH);
        }
        float titleSize = titleSize();
        float titleW = FontRenderer.measureTextWidth("PVPUtils", titleSize);
        float titleH = FontRenderer.getLineHeight(titleSize);
        float titleX = (this.width - titleW) * 0.5f;
        float titleY = compactCardY() - titleH - 32f * scale;
        titleHitBox = new TitleHitBox(titleX, titleY, titleW, titleH);
        updateTextRegion();
        invalidateTextTexture();
    }

    private float animatedMenuCardWidth() {
        if (!singleplayerTransitioning && !returnTransition) return compactCardWidth();
        float progress = returnTransition ? 1f - easeOutCubic(returnTransitionProgress())
                : easeOutCubic(singleplayerTransitionProgress());
        float targetW = openingViaFabricPlus
                ? Math.max(620f, Math.min(900f, this.width * 0.84f))
                : Math.max(320f, Math.min(500f, this.width * 0.52f));
        return compactCardWidth() + (targetW - compactCardWidth()) * progress;
    }

    private float animatedMenuCardY() {
        if (!singleplayerTransitioning && !returnTransition) return compactCardY();
        float progress = returnTransition ? 1f - easeOutCubic(returnTransitionProgress())
                : easeOutCubic(singleplayerTransitionProgress());
        return compactCardY() + (76f - compactCardY()) * progress;
    }

    private float titleSize() {
        return 32f * mainLayoutScale();
    }

    private float compactCardWidth() {
        return MAIN_LAYOUT_BASE_WIDTH * mainLayoutScale();
    }

    private float compactButtonHeight() {
        return 34f * mainLayoutScale();
    }

    private float compactButtonGap() {
        return 2f * mainLayoutScale();
    }

    private float compactCardHeight() {
        return 36f * mainLayoutScale() + buttons.size() * compactButtonHeight() + Math.max(0, buttons.size() - 1) * compactButtonGap();
    }

    private float compactCardY() {
        return this.height * 0.5f - compactCardHeight() * 0.5f + 36f * mainLayoutScale();
    }

    private float mainLayoutScale() {
        if (this.minecraft == null) return MAIN_LAYOUT_BASE_SCALE;
        var window = this.minecraft.getWindow();
        float guiScale = Math.max(1f, (float) window.getGuiScale());
        float availableW = Math.max(1f, window.getWidth() - MAIN_LAYOUT_MARGIN_PIXELS * 2f);
        float availableH = Math.max(1f, window.getHeight() - MAIN_LAYOUT_MARGIN_PIXELS * 2f);
        float baselineWidth = MAIN_LAYOUT_BASE_WIDTH * MAIN_LAYOUT_BASE_GUI_SCALE * MAIN_LAYOUT_BASE_SCALE;
        float baselineHeight = MAIN_LAYOUT_BASE_HEIGHT * MAIN_LAYOUT_BASE_GUI_SCALE * MAIN_LAYOUT_BASE_SCALE;
        float fit = Math.min(1f, Math.min(availableW / baselineWidth, availableH / baselineHeight));
        return Math.max(0.35f, MAIN_LAYOUT_BASE_SCALE * MAIN_LAYOUT_BASE_GUI_SCALE / guiScale * fit);
    }

    private void renderText(GuiGraphics graphics, float alpha) {
        ensureTextTexture();
        if (textTexture == null) return;
        int color = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24 | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXT_TEXTURE_ID, textX, textY, 0f, 0f, textW, textH, textPixelW, textPixelH, textPixelW, textPixelH, color);
    }

    public void renderFrameEnd() {
        if (embeddedSingleplayer != null) {
            embeddedSingleplayer.renderFrameEnd();
            return;
        }
        if (embeddedMultiplayer != null) {
            embeddedMultiplayer.renderFrameEnd();
            return;
        }
        if (embeddedAltManager != null) {
            embeddedAltManager.renderFrameEnd();
            return;
        }
        if (embeddedViaFabricPlus != null) {
            embeddedViaFabricPlus.renderFrameEnd();
            return;
        }
        if (!pendingGpuUi || this.minecraft == null || this.minecraft.screen != this) {
            pendingGpuUi = false;
            return;
        }
        renderGpuUi(pendingGpuAlpha);
        pendingGpuUi = false;
    }

    private void renderGpuUi(float alpha) {
        Canvas c = glBackend.begin(mainFramebufferId());
        if (c == null) return;
        try {
            if (pendingGpuAlpha > 0.001f && !singleplayerTransitioning && !returnTransition) {
                renderMainCardBlur(c);
            }
            renderCompactMenuCard(c, alpha);
            float singleT = singleplayerTransitionProgress();
            float returnT = returnTransitionProgress();
            float controlsFade;
            float titleFade;
            float titleMove;
            if (returnTransition) {
                titleFade = easeOutCubic(returnT);
                controlsFade = easeOutCubic(Math.max(0f, (returnT - 0.48f) / 0.52f));
                titleMove = (1f - titleFade) * 34f;
            } else {
                controlsFade = singleplayerTransitioning ? 1f - easeOutCubic(Math.min(1f, singleT * 1.55f)) : 1f;
                titleFade = controlsFade;
                titleMove = easeOutCubic(singleT) * 34f;
            }
            int titleAlpha = Math.round(255f * Math.max(0f, Math.min(1f, alpha * titleFade)));
            FontRenderer.drawText(c, "PVPUtils", titleHitBox.x, titleHitBox.y + titleHitBox.h * 0.82f + titleMove, titleSize(), (titleAlpha << 24) | 0xFFFFFF);
            if (controlsFade > 0.08f) {
                c.save();
                MainUiScale.applyTopRight(c, this.width, this.height, mainLayoutScale());
                renderSettingsPlaceholder(c);
                renderSettingsPanel(c);
                c.restore();
            }
            if (controlsFade > 0.001f && alpha > 0.001f) {
                for (MenuButton button : buttons) {
                    button.renderText(c, alpha * controlsFade);
                }
            }
            if (controlsFade > 0.08f) {
                c.save();
                MainUiScale.applyBottomLeft(c, this.width, this.height, mainLayoutScale());
                renderVersionText(c);
                c.restore();
            }
        } finally {
            glBackend.end();
        }
    }

    private void renderMainCardBlur(Canvas canvas) {
        float progress = returnTransition ? returnTransitionProgress() : singleplayerTransitionProgress();
        TransitionCard target = transitionTarget();
        float t = returnTransition
                ? 1f - easeOutCubic(progress)
                : easeOutCubic(progress);
        float cardW = compactCardWidth() + (target.width - compactCardWidth()) * t;
        float cardH = compactCardHeight() + (target.height - compactCardHeight()) * t;
        float cardY = compactCardY() + (target.y - compactCardY()) * t;
        float angle = (returnTransition || singleplayerTransitioning)
                ? easeInOutCubic(progress) * (float) Math.PI
                : 0f;
        float visibleW = cardW * Math.max(0.065f, Math.abs((float) Math.cos(angle)));
        float x = (this.width - visibleW) * 0.5f;
        float strength = Math.max(0.65f, Math.min(1.25f, 0.65f + Math.max(visibleW, cardH) / 520f * 0.25f));
        SkiaBlurRenderer.getInstance().render(
                canvas,
                glBackend.getContext(),
                Minecraft.getInstance(),
                mainFramebufferId(),
                x,
                cardY,
                visibleW,
                cardH,
                18f * mainLayoutScale(),
                0x12000000,
                strength
        );
    }

    private int mainFramebufferId() {
        Minecraft client = Minecraft.getInstance();
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private void renderMainBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isVideoBackground()) {
            ensureSelectedVideo();
            ensureVideoBackground();
            if (videoBackground.render(graphics, Config.mainUIVideoBackground)) {
                return;
            }
            renderVideoUnavailable(graphics);
            return;
        }
        if (!isImageBackground()) {
            ensureShaderReady();
            shader.render(graphics, mouseX, mouseY);
            return;
        }
        ensureBackgroundTexture();
        if (backgroundTexture == null || backgroundTextureW <= 0 || backgroundTextureH <= 0) {
            ensureShaderReady();
            shader.render(graphics, mouseX, mouseY);
            return;
        }

        float coverScale = Math.max(this.width / (float) backgroundTextureW, this.height / (float) backgroundTextureH);
        if (Config.mainUIMouseEffect) {
            coverScale *= 1.18f;
            float minW = this.width * 1.16f;
            float minH = this.height * 1.16f;
            coverScale = Math.max(coverScale, minW / backgroundTextureW);
            coverScale = Math.max(coverScale, minH / backgroundTextureH);
        } else {
            coverScale *= 1.08f;
        }
        float drawW = backgroundTextureW * coverScale;
        float drawH = backgroundTextureH * coverScale;
        float targetOffsetX = 0f;
        float targetOffsetY = 0f;
        float maxOffsetX = Math.max(0f, (drawW - this.width) * 0.5f);
        float maxOffsetY = Math.max(0f, (drawH - this.height) * 0.5f);
        if (Config.mainUIMouseEffect) {
            float overflowX = Math.max(0f, drawW - this.width);
            float overflowY = Math.max(0f, drawH - this.height);
            float dragX = Math.max(overflowX * 0.62f, this.width * 0.06f);
            float dragY = Math.max(overflowY * 0.62f, this.height * 0.06f);
            targetOffsetX = ((mouseX / Math.max(1f, (float) this.width)) - 0.5f) * -dragX;
            targetOffsetY = ((mouseY / Math.max(1f, (float) this.height)) - 0.5f) * -dragY;
        }
        targetOffsetX = clamp(targetOffsetX, -maxOffsetX, maxOffsetX);
        targetOffsetY = clamp(targetOffsetY, -maxOffsetY, maxOffsetY);
        backgroundOffsetX += (targetOffsetX - backgroundOffsetX) * 0.08f;
        backgroundOffsetY += (targetOffsetY - backgroundOffsetY) * 0.08f;
        backgroundOffsetX = clamp(backgroundOffsetX, -maxOffsetX, maxOffsetX);
        backgroundOffsetY = clamp(backgroundOffsetY, -maxOffsetY, maxOffsetY);

        int x = Math.round((this.width - drawW) * 0.5f + backgroundOffsetX);
        int y = Math.round((this.height - drawH) * 0.5f + backgroundOffsetY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE_ID, x, y, 0f, 0f, Math.round(drawW), Math.round(drawH), backgroundTextureW, backgroundTextureH, backgroundTextureW, backgroundTextureH);
    }

    private void ensureVideoBackground() {
        if (videoBackground == null) {
            videoBackground = new MainUIVideoBackground();
        }
    }

    private void renderVideoUnavailable(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xFF05070A);
        String title = Config.isChinese ? "视频背景不可用" : "Video background unavailable";
        String reason = videoBackground == null || videoBackground.getLastError().isBlank()
                ? (Config.isChinese ? "视频文件无法解码" : "Video file could not be decoded")
                : videoBackground.getLastError();
        int titleW = this.minecraft.font.width(title);
        int reasonW = this.minecraft.font.width(reason);
        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.drawString(this.minecraft.font, title, cx - titleW / 2, cy - 12, 0xFFFFD176, true);
        graphics.drawString(this.minecraft.font, reason, cx - reasonW / 2, cy + 4, 0xFFE5E7EB, true);
    }

    private void ensureBackgroundTexture() {
        Minecraft client = Minecraft.getInstance();
        int windowPixelW = client.getWindow().getWidth();
        int windowPixelH = client.getWindow().getHeight();
        if (windowPixelW <= 0 || windowPixelH <= 0) return;
        if (lastWindowPixelW != -1 && (lastWindowPixelW != windowPixelW || lastWindowPixelH != windowPixelH)) {
            destroyBackgroundTexture();
            backgroundOffsetX = 0f;
            backgroundOffsetY = 0f;
        }
        lastWindowPixelW = windowPixelW;
        lastWindowPixelH = windowPixelH;

        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        if (backgroundTexture != null && selected.equals(loadedBackground)) return;
        destroyBackgroundTexture();

        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) {
            List<String> files = MainUIBackgrounds.listPngs();
            selected = files.isEmpty() ? "1.png" : files.get(0);
            Config.mainUIBackgroundImage = selected;
            Config.save();
            path = MainUIBackgrounds.resolve(selected);
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) return;
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int argb = image.getRGB(px, py);
                    buffer.put((byte) ((argb >> 16) & 255));
                    buffer.put((byte) ((argb >> 8) & 255));
                    buffer.put((byte) (argb & 255));
                    buffer.put((byte) ((argb >>> 24) & 255));
                }
            }
            buffer.flip();
            backgroundTexture = new DynamicTexture("pvp_utils:mainui_custom_background", width, height, false);
            client.getTextureManager().register(BACKGROUND_TEXTURE_ID, backgroundTexture);
            GpuTexture gpuTexture = backgroundTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buffer, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
            MemoryUtil.memFree(buffer);
            backgroundTextureW = width;
            backgroundTextureH = height;
            loadedBackground = selected;
            refreshThemeFromImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderEntryHint(GuiGraphics graphics, float entryAlpha) {
        if (hintStartMs <= 0L) return;
        long elapsed = System.currentTimeMillis() - hintStartMs;
        if (elapsed >= HINT_DURATION_MS) return;

        float alpha;
        if (elapsed < HINT_FADE_IN_MS) {
            alpha = elapsed / (float) HINT_FADE_IN_MS;
        } else if (elapsed > HINT_DURATION_MS - HINT_FADE_OUT_MS) {
            alpha = (HINT_DURATION_MS - elapsed) / (float) HINT_FADE_OUT_MS;
        } else {
            alpha = 1f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha)) * Math.max(0f, Math.min(1f, entryAlpha));
        int a = Math.round(alpha * 255f);
        if (a <= 0) return;

        String text = Config.isChinese
                ? "点击 PVPUtils 标题返回原版主界面 · 右键随机切换背景"
                : "Click the PVPUtils title for the vanilla menu · Right-click to change the background";
        int textW = this.font.width(text);
        int x = (this.width - textW) / 2;
        int y = Math.max(12, Math.round(titleHitBox.y - 24f));
        int bgW = textW + 28;
        int bgH = 24;
        int bgX = (this.width - bgW) / 2;
        int bgY = y - 8;
        graphics.fill(bgX, bgY, bgX + bgW, bgY + bgH, (Math.round(alpha * 150f) << 24));
        graphics.drawString(this.font, text, x, y, (a << 24) | 0xFFFFFF, false);
    }

    private float entryAlpha() {
        if (!entryFade || entryFadeStartMs <= 0L) {
            return 1f;
        }
        long elapsed = System.currentTimeMillis() - entryFadeStartMs;
        long delay = entryFadeDelay ? ENTRY_FADE_DELAY_MS : 0L;
        if (elapsed <= delay) {
            return 0f;
        }
        elapsed -= delay;
        if (elapsed >= ENTRY_FADE_IN_MS) {
            return 1f;
        }
        return easeOutCubic(elapsed / (float) ENTRY_FADE_IN_MS);
    }

    private void ensureTextTexture() {
        Minecraft client = Minecraft.getInstance();
        float scale = (float) client.getWindow().getGuiScale();
        int targetW = Math.max(1, (int) Math.ceil(textW * scale));
        int targetH = Math.max(1, (int) Math.ceil(textH * scale));
        if (textTexture != null && textPixelW == targetW && textPixelH == targetH && textGuiW == this.width && textGuiH == this.height) return;

        ensureNativeLoaded();
        destroyTextTexture();
        SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);
        textSurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, props);
        textTexture = new DynamicTexture("pvp_utils:mainui_text", targetW, targetH, false);
        client.getTextureManager().register(TEXT_TEXTURE_ID, textTexture);
        textPixelW = targetW;
        textPixelH = targetH;
        textGuiW = this.width;
        textGuiH = this.height;

        Canvas c = textSurface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(scale, scale);
        c.translate(-textX, -textY);
        renderCompactMenuCard(c, 1f);
        FontRenderer.drawText(c, "PVPUtils", titleHitBox.x, titleHitBox.y + titleHitBox.h * 0.82f, titleSize(), 0xFFFFFFFF);
        renderSettingsPlaceholder(c);
        renderSettingsPanel(c);
        for (MenuButton button : buttons) {
            button.renderText(c, 1f);
        }
        renderVersionText(c);
        c.restore();

        Pixmap pixmap = new Pixmap();
        if (!textSurface.peekPixels(pixmap)) {
            pixmap.close();
            return;
        }
        long addr = pixmap.getAddr();
        int byteSize = textPixelH * pixmap.getRowBytes();
        GpuTexture gpuTexture = textTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, MemoryUtil.memByteBuffer(addr, byteSize), NativeImage.Format.RGBA, 0, 0, 0, 0, textPixelW, textPixelH);
        pixmap.close();
    }

    @Override
    public void onClose() {
        if (embeddedSingleplayer != null) {
            embeddedSingleplayer.onClose();
            return;
        }
        if (embeddedMultiplayer != null) {
            embeddedMultiplayer.onClose();
            return;
        }
        if (embeddedAltManager != null) {
            embeddedAltManager.onClose();
            return;
        }
        if (embeddedViaFabricPlus != null) {
            embeddedViaFabricPlus.onClose();
            return;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TitleScreen());
        }
    }

    @Override
    public void removed() {
        if (preserveEmbeddedPages) {
            preserveEmbeddedPages = false;
        } else {
            disposeEmbeddedPages();
        }
        if (shader != null) {
            shader.close();
            shader = null;
        }
        destroyTextTexture();
        destroyBackgroundTexture();
        closeVideoBackground();
        glBackend.destroy();
        lastWindowPixelW = -1;
        lastWindowPixelH = -1;
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private boolean singleplayerTransitioning;
    private boolean openingMultiplayer;
    private boolean openingAltManager;
    private boolean openingViaFabricPlus;
    private boolean returningMultiplayer;
    private boolean returningFixedPage;
    private boolean preserveEmbeddedPages;
    private long singleplayerTransitionStartMs;
    private static final long SINGLEPLAYER_TRANSITION_MS = 520L;
    private static final long RETURN_TRANSITION_MS = 440L;

    private void startSingleplayerTransition() {
        if (singleplayerTransitioning || returnTransition) return;
        pressedIndex = -1;
        titlePressed = false;
        settingsOpen = false;
        singleplayerTransitioning = true;
        singleplayerTransitionStartMs = animationNowNanos();
        openingMultiplayer = false;
        openingAltManager = false;
        openingViaFabricPlus = false;
        returningMultiplayer = false;
        returningFixedPage = false;
    }

    private void startMultiplayerTransition() {
        if (singleplayerTransitioning || returnTransition) return;
        pressedIndex = -1;
        titlePressed = false;
        settingsOpen = false;
        singleplayerTransitioning = true;
        singleplayerTransitionStartMs = animationNowNanos();
        openingMultiplayer = true;
        openingAltManager = false;
        openingViaFabricPlus = false;
        returningMultiplayer = false;
        returningFixedPage = false;
    }

    private void startAltManagerTransition() {
        if (singleplayerTransitioning || returnTransition) return;
        pressedIndex = -1;
        titlePressed = false;
        settingsOpen = false;
        singleplayerTransitioning = true;
        singleplayerTransitionStartMs = animationNowNanos();
        openingMultiplayer = false;
        openingAltManager = true;
        openingViaFabricPlus = false;
        returningMultiplayer = false;
        returningFixedPage = false;
    }

    private void startViaFabricPlusTransition() {
        if (singleplayerTransitioning || returnTransition || !ViaFabricPlusBridge.isInstalled()) return;
        pressedIndex = -1;
        titlePressed = false;
        settingsOpen = false;
        singleplayerTransitioning = true;
        singleplayerTransitionStartMs = animationNowNanos();
        openingMultiplayer = false;
        openingAltManager = false;
        openingViaFabricPlus = true;
        returningMultiplayer = false;
        returningFixedPage = false;
    }

    private void updateSingleplayerTransition() {
        if (!singleplayerTransitioning) return;
        if (animationNowNanos() - singleplayerTransitionStartMs < SINGLEPLAYER_TRANSITION_MS * 1_000_000L) return;
        singleplayerTransitioning = false;
        if (this.minecraft != null) {
            String path = shader == null ? null : shader.fragmentPath();
            if (openingViaFabricPlus) {
                embeddedViaFabricPlus = new PVPUtilsViaFabricPlusScreen(this, path, this::beginEmbeddedReturn);
                embeddedViaFabricPlus.initEmbedded(this.minecraft, this.width, this.height);
            } else if (openingAltManager) {
                embeddedAltManager = new AltManagerScreen(this, path, this::beginEmbeddedReturn);
                embeddedAltManager.initEmbedded(this.minecraft, this.width, this.height);
            } else if (openingMultiplayer) {
                embeddedMultiplayer = new PVPUtilsMultiplayerScreen(this, path, this::beginEmbeddedReturn);
                embeddedMultiplayer.initEmbedded(this.minecraft, this.width, this.height);
            } else {
                embeddedSingleplayer = new PVPUtilsSingleplayerScreen(this, path, this::beginEmbeddedReturn);
                embeddedSingleplayer.initEmbedded(this.minecraft, this.width, this.height);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (embeddedSingleplayer != null) {
            return embeddedSingleplayer.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (embeddedMultiplayer != null) {
            return embeddedMultiplayer.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (embeddedAltManager != null) {
            return embeddedAltManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (embeddedViaFabricPlus != null) {
            return embeddedViaFabricPlus.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean renderEmbeddedPage(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (embeddedSingleplayer != null) {
            renderMainBackground(graphics, mouseX, mouseY);
            embeddedSingleplayer.render(graphics, mouseX, mouseY, delta);
            return true;
        }
        if (embeddedMultiplayer != null) {
            renderMainBackground(graphics, mouseX, mouseY);
            embeddedMultiplayer.render(graphics, mouseX, mouseY, delta);
            return true;
        }
        if (embeddedAltManager != null) {
            renderMainBackground(graphics, mouseX, mouseY);
            embeddedAltManager.render(graphics, mouseX, mouseY, delta);
            return true;
        }
        if (embeddedViaFabricPlus != null) {
            renderMainBackground(graphics, mouseX, mouseY);
            embeddedViaFabricPlus.render(graphics, mouseX, mouseY, delta);
            return true;
        }
        return false;
    }

    private void beginEmbeddedReturn() {
        returningMultiplayer = embeddedMultiplayer != null;
        returningFixedPage = embeddedSingleplayer != null || embeddedMultiplayer != null;
        disposeEmbeddedPages();
        returnTransition = true;
        returnTransitionStartMs = animationNowNanos();
        singleplayerTransitioning = false;
        openingMultiplayer = false;
        openingAltManager = false;
        openingViaFabricPlus = false;
        pendingGpuAlpha = 1f;
        pendingGpuUi = true;
    }

    void preserveEmbeddedPagesForOverlay() {
        preserveEmbeddedPages = true;
    }

    private void disposeEmbeddedPages() {
        if (embeddedSingleplayer != null) {
            embeddedSingleplayer.removed();
            embeddedSingleplayer = null;
        }
        if (embeddedMultiplayer != null) {
            embeddedMultiplayer.removed();
            embeddedMultiplayer = null;
        }
        if (embeddedAltManager != null) {
            embeddedAltManager.removed();
            embeddedAltManager = null;
        }
        if (embeddedViaFabricPlus != null) {
            embeddedViaFabricPlus.removed();
            embeddedViaFabricPlus = null;
        }
    }

    private float singleplayerTransitionProgress() {
        if (!singleplayerTransitioning || singleplayerTransitionStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f,
                (animationNowNanos() - singleplayerTransitionStartMs) / (SINGLEPLAYER_TRANSITION_MS * 1_000_000f)));
    }

    private float returnTransitionProgress() {
        if (!returnTransition || returnTransitionStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f,
                (animationNowNanos() - returnTransitionStartMs) / (RETURN_TRANSITION_MS * 1_000_000f)));
    }

    private long animationNowNanos() {
        return System.nanoTime();
    }

    private void refreshShader() {
        if (shader != null) shader.close();
        shader = MainUIShader.random();
        if (Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED) {
            Config.mainUIGlslShader = shader.fragmentPath();
            Config.save();
        }
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
    }

    private String initialShaderPath() {
        if (fixedShaderPath != null) {
            return fixedShaderPath;
        }
        if (Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED) {
            Config.mainUIGlslShader = MainUIShader.normalizeShader(Config.mainUIGlslShader);
            return Config.mainUIGlslShader;
        }
        return MainUISharedBackground.activeShaderPath();
    }

    private void reloadConfiguredShader() {
        if (Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED) {
            Config.mainUIGlslShader = MainUIShader.normalizeShader(Config.mainUIGlslShader);
            if (shader != null && Config.mainUIGlslShader.equals(shader.fragmentPath())) {
                MainUISharedBackground.setActiveShader(shader.fragmentPath());
                return;
            }
            if (shader != null) shader.close();
            shader = MainUIShader.named(Config.mainUIGlslShader);
            MainUISharedBackground.setActiveShader(shader.fragmentPath());
            return;
        }
        if (shader == null) {
            shader = MainUIShader.random();
        }
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
    }

    private void ensureShaderReady() {
        if (shader != null) return;
        String sharedShaderPath = initialShaderPath();
        shader = sharedShaderPath == null ? MainUIShader.random() : MainUIShader.named(sharedShaderPath);
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
    }

    private void cycleGlslShader() {
        List<String> shaders = MainUIShader.shaderFiles();
        if (shaders.isEmpty()) {
            return;
        }
        String selected = MainUIShader.normalizeShader(Config.mainUIGlslShader);
        int index = shaders.indexOf(selected);
        Config.mainUIGlslShader = shaders.get((index + 1 + shaders.size()) % shaders.size());
        Config.save();
        reloadConfiguredShader();
    }

    PVPUtilsMainUI returnParent() {
        return new PVPUtilsMainUI(null, false, false, shader == null ? null : shader.fragmentPath());
    }

    private void cycleBackgroundImage() {
        List<String> files = MainUIBackgrounds.listPngs();
        if (files.isEmpty()) return;
        int index = files.indexOf(Config.mainUIBackgroundImage);
        Config.mainUIBackgroundImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        destroyBackgroundTexture();
        refreshThemeFromBackground();
    }

    private void cycleBackgroundVideo() {
        List<String> files = MainUIBackgrounds.listMp4s();
        if (files.isEmpty()) {
            Config.mainUIVideoBackground = "";
            Config.save();
            closeVideoBackground();
            return;
        }
        int index = files.indexOf(Config.mainUIVideoBackground);
        Config.mainUIVideoBackground = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        closeVideoBackground();
    }

    private void ensureSelectedVideo() {
        List<String> files = MainUIBackgrounds.listMp4s();
        if (files.isEmpty()) {
            Config.mainUIVideoBackground = "";
            return;
        }
        if (Config.mainUIVideoBackground == null || Config.mainUIVideoBackground.isBlank() || !files.contains(Config.mainUIVideoBackground)) {
            Config.mainUIVideoBackground = files.get(0);
            Config.save();
        }
    }

    private void refreshThemeFromBackground() {
        lightSettingsTheme = true;
        if (!isImageBackground()) return;
        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) return;
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image != null) refreshThemeFromImage(image);
        } catch (IOException ignored) {
        }
    }

    private void refreshThemeFromImage(BufferedImage image) {
        long total = 0L;
        int samples = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int stepX = Math.max(1, width / 96);
        int stepY = Math.max(1, height / 96);
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                total += (r * 299L + g * 587L + b * 114L) / 1000L;
                samples++;
            }
        }
        if (samples > 0) {
            lightSettingsTheme = total / (float) samples >= 140f;
        }
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void invalidateTextTexture() {
        textGuiW = -1;
        textGuiH = -1;
    }

    private void updateTextRegion() {
        float cardX = (this.width - compactCardWidth()) * 0.5f;
        float cardY = compactCardY();
        float minX = Math.min(titleHitBox.x, cardX);
        float minY = Math.min(titleHitBox.y, cardY);
        float maxX = titleHitBox.x + titleHitBox.w;
        float maxY = titleHitBox.y + titleHitBox.h;
        maxX = Math.max(maxX, cardX + compactCardWidth());
        maxY = Math.max(maxY, cardY + compactCardHeight());
        float settingsX = getSettingsX();
        float settingsY = getSettingsY();
        float panelW = getSettingsPanelWidth();
        float panelH = getSettingsPanelMaxHeight();
        minX = Math.min(minX, settingsX);
        minY = Math.min(minY, settingsY);
        maxX = Math.max(maxX, settingsX + SETTINGS_SIZE);
        maxY = Math.max(maxY, settingsY + SETTINGS_SIZE);
        minX = Math.min(minX, settingsX + SETTINGS_SIZE - panelW);
        maxX = Math.max(maxX, settingsX + SETTINGS_SIZE);
        maxY = Math.max(maxY, settingsY + SETTINGS_SIZE + panelH);
        for (MenuButton button : buttons) {
            minX = Math.min(minX, button.x - 4f);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.w + 4f);
            maxY = Math.max(maxY, button.y + button.h + 10f);
        }
        String version = Version.displayName();
        if (Version.DEBUG) {
            minY = Math.min(minY, this.height - 12f - 11f - 10f);
            maxX = Math.max(maxX, 12f + FontRenderer.measureTextWidth("DEBUG", 11f));
        }
        minX = Math.min(minX, 12f);
        maxX = Math.max(maxX, 12f + FontRenderer.measureTextWidth(version, 11f));
        maxY = Math.max(maxY, this.height - 12f + FontRenderer.getLineHeight(11f));
        textX = Math.max(0, (int) Math.floor(minX - 6f));
        textY = Math.max(0, (int) Math.floor(minY - 6f));
        int right = Math.min(this.width, (int) Math.ceil(maxX + 6f));
        int bottom = Math.min(this.height, (int) Math.ceil(maxY + 6f));
        textW = Math.max(1, right - textX);
        textH = Math.max(1, bottom - textY);
    }

    private float getSettingsX() {
        return this.width - SETTINGS_MARGIN - SETTINGS_SIZE;
    }

    private float getSettingsY() {
        return SETTINGS_MARGIN;
    }

    private void renderSettingsPlaceholder(Canvas canvas) {
        float x = getSettingsX();
        float y = getSettingsY();
        float fade = 1f - easeOutCubic(settingsPanelProgress);
        if (fade <= 0.01f) return;
        int alpha = Math.round((isImageBackground() ? 47f : 190f) + 40f * settingsHoverProgress);
        boolean lightTheme = isLightTheme();
        int baseColor = lightTheme ? 0x111111 : 0xFFFFFF;
        int accentColor = lightTheme ? 0xD17600 : 0xFFD176;
        int bgColor = (Math.round(alpha * fade) << 24) | (lightTheme ? 0xF7F7F7 : 0xFFFFFF);
        int iconColor = (Math.round((230f + 25f * settingsHoverProgress) * fade) << 24) | lerpRgb(baseColor, accentColor, settingsHoverProgress);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(bgColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, SETTINGS_SIZE, SETTINGS_SIZE, 10f), bg);
        }
        String icon = "\uE8B8";
        float size = 22f;
        float iconW = FontRenderer.measureTextWidth(icon, size, FontRenderer.MATERIAL_SYMBOLS);
        float iconH = FontRenderer.getLineHeight(size, FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, icon, x + (SETTINGS_SIZE - iconW) * 0.5f, y + (SETTINGS_SIZE + iconH) * 0.5f - 2f, size, iconColor, FontRenderer.MATERIAL_SYMBOLS);
    }

    private void renderSettingsPanel(Canvas canvas) {
        if (settingsPanelProgress <= 0.001f) return;
        float t = easeOutCubic(settingsPanelProgress);
        float fullW = getSettingsPanelWidth();
        float fullH = getSettingsPanelHeight();
        float w = fullW * t;
        float h = fullH * t;
        float x = getSettingsX() + SETTINGS_SIZE - w;
        float y = getSettingsY();
        boolean lightTheme = isLightTheme();
        int alpha = Math.round((!isImageBackground() ? 196f : (lightTheme ? 118f : 70f)) * t);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((alpha << 24) | (lightTheme ? 0xF7F7F7 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 16f), bg);
        }
        if (t <= 0.45f) return;

        int textAlpha = Math.round(255f * Math.min(1f, (t - 0.45f) / 0.55f));
        int primary = (textAlpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF);
        int secondary = (Math.round(textAlpha * 0.72f) << 24) | (lightTheme ? 0x444444 : 0xFFFFFF);
        float contentX = x + 22f;
        float contentY = y + 34f;
        FontRenderer.drawText(canvas, Config.isChinese ? "UI \u8bbe\u7f6e" : "UI Settings", contentX, contentY, 18f, primary);

        float rowY = y + 72f;
        FontRenderer.drawText(canvas, Config.isChinese ? "\u5f53\u524d\u80cc\u666f\u6a21\u5f0f" : "Background Mode", contentX, rowY, 12f, secondary);
        renderChoice(canvas, contentX, rowY + 18f, 82f, Config.isChinese ? "\u5185\u7f6eGLSL" : "GLSL", isGlslBackground(), textAlpha);
        renderChoice(canvas, contentX + 92f, rowY + 18f, 92f, Config.isChinese ? "\u56fe\u7247" : "Image", isImageBackground(), textAlpha);
        renderChoice(canvas, contentX + 194f, rowY + 18f, 76f, Config.isChinese ? "\u89c6\u9891" : "Video", isVideoBackground(), textAlpha);

        if (isImageBackground() || isVideoBackground()) {
            float folderY = y + 148f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u6253\u5f00\u76ee\u5f55" : "Open Folder", contentX, folderY, 12f, secondary);
            renderButton(canvas, contentX + 198f, folderY - 16f, 72f, Config.isChinese ? "\u6253\u5f00" : "Open", textAlpha);
        }

        if (isImageBackground()) {
            float imageY = y + 184f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u9009\u62e9\u56fe\u7247" : "Select Image", contentX, imageY, 12f, secondary);
            renderButton(canvas, contentX + 112f, imageY - 16f, 158f, Config.mainUIBackgroundImage, textAlpha);

            float effectY = y + 220f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u80cc\u666f\u6548\u679c" : "Background Effects", contentX, effectY, 12f, secondary);
            renderToggle(canvas, contentX, effectY + 18f, Config.isChinese ? "\u9f20\u6807\u4ea4\u4e92\u6548\u679c" : "Mouse Interaction", Config.mainUIMouseEffect, textAlpha);
        } else if (isVideoBackground()) {
            float videoY = y + 184f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u9009\u62e9\u89c6\u9891" : "Select Video", contentX, videoY, 12f, secondary);
            String label = Config.mainUIVideoBackground == null || Config.mainUIVideoBackground.isBlank()
                    ? (Config.isChinese ? "\u65e0 MP4" : "No MP4")
                    : Config.mainUIVideoBackground;
            renderButton(canvas, contentX + 112f, videoY - 16f, 158f, label, textAlpha);
        } else {
            float glslY = y + 148f;
            FontRenderer.drawText(canvas, Config.isChinese ? "GLSL \u6a21\u5f0f" : "GLSL Mode", contentX, glslY, 12f, secondary);
            renderChoice(canvas, contentX + 112f, glslY - 16f, 72f, Config.isChinese ? "\u968f\u673a" : "Random", Config.mainUIGlslMode == Config.MainUIGlslMode.RANDOM, textAlpha);
            renderChoice(canvas, contentX + 194f, glslY - 16f, 76f, Config.isChinese ? "\u56fa\u5b9a" : "Fixed", Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED, textAlpha);
            if (Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED) {
                float shaderY = y + 184f;
                FontRenderer.drawText(canvas, Config.isChinese ? "\u9009\u62e9 GLSL" : "Select GLSL", contentX, shaderY, 12f, secondary);
                renderButton(canvas, contentX + 112f, shaderY - 16f, 158f, MainUIShader.normalizeShader(Config.mainUIGlslShader), textAlpha);
            }
        }
    }

    private void renderVersionText(Canvas canvas) {
        float versionX = 12f;
        float versionY = this.height - 12f;
        if (Version.DEBUG) {
            float debugY = versionY - FontRenderer.getLineHeight(11f) - 4f;
            FontRenderer.drawText(canvas, "DEBUG", versionX, debugY, 11f, 0xFFFFD34D);
        }
        drawVersionText(canvas, versionX, versionY, 11f, 0xE6FFFFFF);
    }

    private void renderCompactMenuCard(Canvas canvas, float alpha) {
        int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
        float t = returnTransition
                ? 1f - easeOutCubic(returnTransitionProgress())
                : easeOutCubic(singleplayerTransitionProgress());
        float baseW = compactCardWidth();
        float baseH = compactCardHeight();
        TransitionCard target = transitionTarget();
        float cardW = baseW + (target.width - baseW) * t;
        float cardH = baseH + (target.height - baseH) * t;
        float cardCx = this.width * 0.5f;
        float cardY = compactCardY() + (target.y - compactCardY()) * t;
        float transitionProgress = returnTransition
                ? returnTransitionProgress()
                : singleplayerTransitionProgress();
        float angle = returnTransition
                ? easeInOutCubic(transitionProgress) * (float) Math.PI
                : singleplayerTransitioning ? easeInOutCubic(transitionProgress) * (float) Math.PI : 0f;
        try (Paint bg = new Paint(); Paint stroke = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(a * (0x32 / 255f)) << 24) | 0x101010);
            drawBookPageCard(canvas, cardCx, cardY, cardW, cardH, angle, bg);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor((Math.round(a * 0.14f) << 24) | 0xFFFFFF);
            drawBookPageCard(canvas, cardCx, cardY + 0.5f, cardW - 1f, cardH - 1f, angle, stroke);
        }
    }

    private TransitionCard transitionTarget() {
        if (openingViaFabricPlus) {
            return new TransitionCard(
                    Math.max(620f, Math.min(900f, this.width * 0.84f)),
                    Math.max(370f, Math.min(this.height - 100f, this.height * 0.78f)),
                    76f
            );
        }
        boolean multiplayer = openingMultiplayer || (returnTransition && returningMultiplayer);
        boolean fixedPage = (!returnTransition && !openingAltManager) || (returnTransition && returningFixedPage);
        int layoutWidth = fixedPage ? MainUiScale.pageWidth() : this.width;
        int layoutHeight = fixedPage ? MainUiScale.pageHeight() : this.height;
        float rawWidth = Math.max(320f, Math.min(500f, layoutWidth * 0.52f));
        float rawHeight = multiplayer
                ? Math.max(280f, Math.min(layoutHeight - 150f, layoutHeight * 0.70f))
                : Math.max(260f, Math.min(layoutHeight - 154f, layoutHeight * 0.72f));
        float rawY = multiplayer ? 72f : 76f;
        if (fixedPage) {
            float rawX = (layoutWidth - rawWidth) * 0.5f;
            float minY = multiplayer ? 20f : 18f;
            float maxY = rawY + rawHeight + (multiplayer ? 44f : 76f);
            float scale = MainUiScale.pageScale(
                    rawX,
                    minY,
                    rawX + rawWidth,
                    maxY
            );
            float centerY = (minY + maxY) * 0.5f;
            return new TransitionCard(
                    rawWidth * scale,
                    rawHeight * scale,
                    this.height * 0.5f + (rawY - centerY) * scale
            );
        }
        return new TransitionCard(
                rawWidth,
                rawHeight,
                rawY
        );
    }

    private record TransitionCard(float width, float height, float y) {
    }

    private void drawBookPageCard(Canvas canvas, float cx, float y, float w, float h, float angle, Paint paint) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        if (Math.abs(sin) < 0.03f && cos > 0.99f) {
            canvas.drawRRect(RRect.makeXYWH(cx - w * 0.5f, y, w, h, 18f * mainLayoutScale()), paint);
            return;
        }
        float scaleX = Math.copySign(Math.max(0.065f, Math.abs(cos)), cos);
        canvas.save();
        canvas.translate(cx, y + h * 0.5f);
        canvas.skew(sin * 0.10f, 0f);
        canvas.scale(scaleX, 1f);
        canvas.drawRRect(RRect.makeXYWH(-w * 0.5f, -h * 0.5f, w, h, 18f * mainLayoutScale()), paint);
        canvas.restore();
    }

    private void drawVersionText(Canvas canvas, float x, float y, float size, int baseColor) {
        String version = Version.displayName();
        String type = Version.typeName();
        if (type.isEmpty()) {
            FontRenderer.drawText(canvas, version, x, y, size, baseColor);
            return;
        }

        String marker = "-" + type;
        int typeStart = version.indexOf(marker);
        if (typeStart < 0) {
            FontRenderer.drawText(canvas, version, x, y, size, baseColor);
            return;
        }

        typeStart += 1;
        int typeEnd = typeStart + type.length();
        String before = version.substring(0, typeStart);
        String typed = version.substring(typeStart, typeEnd);
        String after = version.substring(typeEnd);
        int typeColor = Version.TYPE == 1 ? 0xFFFF4444 : 0xFFFFD34D;
        FontRenderer.drawText(canvas, before, x, y, size, baseColor);
        float tx = x + FontRenderer.measureTextWidth(before, size);
        FontRenderer.drawText(canvas, typed, tx, y, size, typeColor);
        FontRenderer.drawText(canvas, after, tx + FontRenderer.measureTextWidth(typed, size), y, size, baseColor);
    }

    private void renderChoice(Canvas canvas, float x, float y, float w, String text, boolean selected, int alpha) {
        boolean lightTheme = isLightTheme();
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(((selected ? Math.round(alpha * 0.28f) : Math.round(alpha * 0.12f)) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 28f, 9f), bg);
        }
        int color = (alpha << 24) | (selected ? 0xFFD176 : (lightTheme ? 0x111111 : 0xFFFFFF));
        float tw = FontRenderer.measureTextWidth(text, 11f);
        FontRenderer.drawText(canvas, text, x + (w - tw) * 0.5f, y + 18f, 11f, color);
    }

    private void renderButton(Canvas canvas, float x, float y, float w, String text, int alpha) {
        boolean lightTheme = isLightTheme();
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * 0.14f) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 28f, 9f), bg);
        }
        String label = text == null ? "" : text;
        if (FontRenderer.measureTextWidth(label, 11f) > w - 16f) {
            while (label.length() > 1 && FontRenderer.measureTextWidth(label + "...", 11f) > w - 16f) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        float tw = FontRenderer.measureTextWidth(label, 11f);
        FontRenderer.drawText(canvas, label, x + (w - tw) * 0.5f, y + 18f, 11f, (alpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
    }

    private void renderToggle(Canvas canvas, float x, float y, String text, boolean selected, int alpha) {
        boolean lightTheme = isLightTheme();
        FontRenderer.drawText(canvas, text, x, y + 18f, 12f, (alpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
        float tx = x + 218f;
        try (Paint track = new Paint()) {
            track.setAntiAlias(true);
            track.setColor((Math.round(alpha * 0.22f) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(tx, y + 3f, 44f, 24f, 12f), track);
        }
        try (Paint knob = new Paint()) {
            knob.setAntiAlias(true);
            knob.setColor((alpha << 24) | (selected ? 0xFFD176 : (lightTheme ? 0x111111 : 0xFFFFFF)));
            canvas.drawCircle(tx + (selected ? 32f : 12f), y + 15f, 8f, knob);
        }
    }

    private void updateSettingsPanel(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        if (lastRenderMs <= 0L) lastRenderMs = now;
        float dt = Math.min(0.05f, (now - lastRenderMs) / 1000f);
        lastRenderMs = now;

        boolean oldHover = settingsHover;
        settingsHover = isInsideSettings(mouseX, mouseY);
        if (settingsOpen && !isInsideSettingsArea(mouseX, mouseY)) {
            settingsOpen = false;
        }

        float target = settingsOpen ? 1f : 0f;
        float hoverTarget = settingsHover ? 1f : 0f;
        float oldProgress = settingsPanelProgress;
        float oldHoverProgress = settingsHoverProgress;
        settingsPanelProgress += (target - settingsPanelProgress) * Math.min(1f, dt * 10f);
        settingsHoverProgress += (hoverTarget - settingsHoverProgress) * Math.min(1f, dt * 12f);
        if (Math.abs(settingsPanelProgress - target) < 0.002f) settingsPanelProgress = target;
        if (Math.abs(settingsHoverProgress - hoverTarget) < 0.002f) settingsHoverProgress = hoverTarget;
        if (oldHover != settingsHover || Math.abs(oldProgress - settingsPanelProgress) > 0.0005f || Math.abs(oldHoverProgress - settingsHoverProgress) > 0.0005f) {
            invalidateTextTexture();
        }
    }

    private boolean isInsideSettings(float mx, float my) {
        float x = getSettingsX();
        float y = getSettingsY();
        return mx >= x && mx <= x + SETTINGS_SIZE && my >= y && my <= y + SETTINGS_SIZE;
    }

    private boolean isInsideSettingsArea(float mx, float my) {
        if (isInsideSettings(mx, my)) return true;
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth();
        float y = getSettingsY();
        return mx >= x && mx <= x + getSettingsPanelWidth() && my >= y && my <= y + getSettingsPanelHeight();
    }

    private boolean isInsideBackgroundModeBuiltin(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 22f;
        float y = getSettingsY() + 90f;
        return mx >= x && mx <= x + 82f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundModeCustom(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 114f;
        float y = getSettingsY() + 90f;
        return mx >= x && mx <= x + 92f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundModeVideo(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 216f;
        float y = getSettingsY() + 90f;
        return mx >= x && mx <= x + 76f && my >= y && my <= y + 28f;
    }

    private boolean isInsideGlslModeRandom(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 134f;
        float y = getSettingsY() + 132f;
        return mx >= x && mx <= x + 72f && my >= y && my <= y + 28f;
    }

    private boolean isInsideGlslModeFixed(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 216f;
        float y = getSettingsY() + 132f;
        return mx >= x && mx <= x + 76f && my >= y && my <= y + 28f;
    }

    private boolean isInsideGlslShaderSelect(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 134f;
        float y = getSettingsY() + 168f;
        return mx >= x && mx <= x + 158f && my >= y && my <= y + 28f;
    }

    private boolean isInsideOpenBackgroundFolder(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 198f;
        float y = getSettingsY() + 132f;
        return mx >= x && mx <= x + 72f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundImageSelect(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 112f;
        float y = getSettingsY() + 168f;
        return mx >= x && mx <= x + 158f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundVideoSelect(float mx, float my) {
        return isInsideBackgroundImageSelect(mx, my);
    }

    private boolean isInsideMouseEffectToggle(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 22f;
        float y = getSettingsY() + 238f;
        return mx >= x && mx <= x + 270f && my >= y && my <= y + 30f;
    }

    private float getSettingsPanelWidth() {
        return 340f;
    }

    private float getSettingsPanelHeight() {
        return isImageBackground() ? 276f : isVideoBackground() ? 220f : Config.mainUIGlslMode == Config.MainUIGlslMode.FIXED ? 220f : 184f;
    }

    private float getSettingsPanelMaxHeight() {
        return 276f;
    }

    private void setBackgroundMode(Config.MainUIBackgroundMode mode) {
        Config.mainUIBackgroundMode = mode == null ? Config.MainUIBackgroundMode.GLSL : mode;
        Config.mainUICustomBackground = Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.IMAGE;
        if (isGlslBackground()) {
            reloadConfiguredShader();
        }
        if (!isVideoBackground()) {
            closeVideoBackground();
        }
        if (!isImageBackground()) {
            destroyBackgroundTexture();
        }
    }

    private boolean isGlslBackground() {
        return !isImageBackground() && !isVideoBackground();
    }

    private boolean isImageBackground() {
        return Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.IMAGE || Config.mainUICustomBackground;
    }

    private boolean isVideoBackground() {
        return Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.VIDEO;
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private float easeInOutCubic(float value) {
        float t = Math.max(0f, Math.min(1f, value));
        return t < 0.5f
                ? 4f * t * t * t
                : cubicInverted(2f - 2f * t);
    }

    private float cubicInverted(float value) {
        return 1f - value * value * value * 0.5f;
    }

    private void destroyTextTexture() {
        if (textSurface != null) {
            textSurface.close();
            textSurface = null;
        }
        if (textTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXT_TEXTURE_ID);
            textTexture = null;
        }
        textPixelW = -1;
        textPixelH = -1;
    }

    private void destroyBackgroundTexture() {
        if (backgroundTexture != null) {
            Minecraft.getInstance().getTextureManager().release(BACKGROUND_TEXTURE_ID);
            backgroundTexture = null;
        }
        backgroundTextureW = -1;
        backgroundTextureH = -1;
        loadedBackground = "";
    }

    private void closeVideoBackground() {
        if (videoBackground != null) {
            videoBackground.close();
            videoBackground = null;
        }
    }

    private class MenuButton {
        private final String text;
        private final String icon;
        private final Runnable action;
        private float x;
        private float y;
        private float w;
        private float h;
        private float hover;

        private MenuButton(String text, String icon, Runnable action) {
            this.text = text;
            this.icon = icon;
            this.action = action;
        }

        private void setBounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY, boolean pressed, float alpha) {
            float oldHover = hover;
            hover += ((contains(mouseX, mouseY) ? 1f : 0f) - hover) * 0.18f;
            if (pressed) hover = Math.min(1f, hover + 0.08f);
            if (Math.abs(oldHover - hover) > 0.002f) {
                invalidateTextTexture();
            }
        }

        private void renderText(Canvas canvas, float alpha) {
            boolean pressed = buttons.indexOf(this) == pressedIndex;
            float t = easeOutCubic(hover);
            int drawAlpha = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
            float scale = pressed ? 0.985f : 1f + t * 0.018f;
            float drawW = w * scale;
            float drawH = h * scale;
            float drawX = x + (w - drawW) * 0.5f;
            float drawY = y + (h - drawH) * 0.5f;
            try (Paint bg = new Paint()) {
                bg.setAntiAlias(true);
                bg.setColor((Math.round(drawAlpha * (0.07f + 0.15f * t) * (pressed ? 1.25f : 1f)) << 24) | 0xFFFFFF);
                canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, 12f), bg);
            }
            float uiScale = mainLayoutScale();
            float iconSize = 18f * uiScale;
            float textSize = 15f * uiScale;
            int color = withAlpha(0xFFFFFFFF, drawAlpha);
            float iconX = drawX + 22f * uiScale;
            float centerY = drawY + drawH * 0.5f;
            float iconH = FontRenderer.getLineHeight(iconSize, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, icon, iconX, centerY + iconH * 0.35f, iconSize, color, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, text, drawX + 54f * uiScale + t * 3f * uiScale, centerY + FontRenderer.getLineHeight(textSize) * 0.36f, textSize, color);
        }
    }

    private record TitleHitBox(float x, float y, float w, float h) {
        private boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = from >>> 24;
        int rr = (from >> 16) & 255;
        int gr = (from >> 8) & 255;
        int br = from & 255;
        int at = to >>> 24;
        int rt = (to >> 16) & 255;
        int gt = (to >> 8) & 255;
        int bt = to & 255;
        return ((int) (ar + (at - ar) * t) << 24)
                | ((int) (rr + (rt - rr) * t) << 16)
                | ((int) (gr + (gt - gr) * t) << 8)
                | (int) (br + (bt - br) * t);
    }

    private static int lerpRgb(int from, int to, float t) {
        return lerpColor(0xFF000000 | from, 0xFF000000 | to, t) & 0xFFFFFF;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    private int themedTextColor(int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (isLightTheme() ? 0x111111 : 0xFFFFFF);
    }

    private int mainTextColor(int alpha) {
        boolean darkText = isImageBackground() && lightSettingsTheme;
        return (Math.max(0, Math.min(255, alpha)) << 24) | (darkText ? 0x111111 : 0xFFFFFF);
    }

    private boolean isLightTheme() {
        return !isImageBackground() || lightSettingsTheme;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
