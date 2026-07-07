package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.render.font.FontRenderer;
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
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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
    private static final float SETTINGS_SIZE = 36f;
    private static final float SETTINGS_MARGIN = 24f;
    private static final Identifier BACKGROUND_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_custom_background");

    private MainUIShader shader;
    private final boolean showEntryHint;
    private final String fixedShaderPath;
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

    public PVPUtilsMainUI(Screen parent) {
        this(parent, false);
    }

    public PVPUtilsMainUI(Screen parent, boolean showEntryHint) {
        this(parent, showEntryHint, null);
    }

    private PVPUtilsMainUI(Screen parent, boolean showEntryHint, String fixedShaderPath) {
        super(Component.literal("Minecraft"));
        this.showEntryHint = showEntryHint;
        this.fixedShaderPath = fixedShaderPath;
    }

    @Override
    protected void init() {
        if (shader != null) shader.close();
        shader = fixedShaderPath == null ? MainUIShader.random() : MainUIShader.named(fixedShaderPath);
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
        hintStartMs = showEntryHint ? System.currentTimeMillis() : 0L;
        invalidateTextTexture();
        refreshThemeFromBackground();
        buttons.clear();
        buttons.add(new MenuButton("Singleplayer", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(returnParent()));
        }));
        buttons.add(new MenuButton("Multiplayer", () -> {
            if (this.minecraft == null) return;
            Screen parent = returnParent();
            Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(parent) : new SafetyScreen(parent);
            this.minecraft.setScreen(screen);
        }));
        buttons.add(new MenuButton("Options", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(returnParent(), this.minecraft.options));
        }));
        buttons.add(new MenuButton("Exit", () -> {
            if (this.minecraft != null) this.minecraft.stop();
        }));
        updateButtonPositions();
    }

    @Override
    protected void repositionElements() {
        updateButtonPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateSettingsPanel(mouseX, mouseY);
        renderMainBackground(graphics, mouseX, mouseY);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).render(graphics, mouseX, mouseY, i == pressedIndex);
        }
        renderText(graphics);
        renderEntryHint(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (isInsideSettings((float) event.x(), (float) event.y())) {
            if (event.button() == 0) {
                settingsOpen = true;
                playClickSound();
                invalidateTextTexture();
            }
            return true;
        }
        if (settingsOpen && event.button() == 0) {
            if (isInsideBackgroundModeCustom((float) event.x(), (float) event.y())) {
                setBackgroundMode(Config.MainUIBackgroundMode.IMAGE);
                Config.save();
                refreshThemeFromBackground();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isInsideBackgroundModeBuiltin((float) event.x(), (float) event.y())) {
                setBackgroundMode(Config.MainUIBackgroundMode.GLSL);
                Config.save();
                lightSettingsTheme = true;
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isInsideBackgroundModeVideo((float) event.x(), (float) event.y())) {
                setBackgroundMode(Config.MainUIBackgroundMode.VIDEO);
                ensureSelectedVideo();
                Config.save();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if ((isImageBackground() || isVideoBackground()) && isInsideOpenBackgroundFolder((float) event.x(), (float) event.y())) {
                MainUIBackgrounds.openFolder();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isImageBackground() && isInsideBackgroundImageSelect((float) event.x(), (float) event.y())) {
                cycleBackgroundImage();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isVideoBackground() && isInsideBackgroundVideoSelect((float) event.x(), (float) event.y())) {
                cycleBackgroundVideo();
                playClickSound();
                invalidateTextTexture();
                return true;
            }
            if (isImageBackground() && isInsideMouseEffectToggle((float) event.x(), (float) event.y())) {
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
        float buttonW = Math.min(this.width / 6f, 300f);
        float buttonH = 30f;
        float gap = 10f;
        float y = this.height - Math.min((this.width + this.height * 2f) / 25f, 150f);
        float startX = this.width / 2f - (buttonW * 4f + gap * 3f) / 2f;
        for (int i = 0; i < 4; i++) {
            buttons.get(i).setBounds(startX + i * (buttonW + gap), y, buttonW, buttonH);
        }
        float titleSize = titleSize();
        float titleX = this.width / 15f;
        float titleY = this.width / 30f;
        float titleW = FontRenderer.measureTextWidth("Minecraft", titleSize);
        float titleH = FontRenderer.getLineHeight(titleSize);
        titleHitBox = new TitleHitBox(titleX, titleY, titleW, titleH);
        updateTextRegion();
        invalidateTextTexture();
    }

    private float titleSize() {
        float scale = Math.max(0.5f, Math.min(1.0f, (this.width * 2f + this.height) / 6000f + 0.1f));
        return 48f * scale;
    }

    private void renderText(GuiGraphics graphics) {
        ensureTextTexture();
        if (textTexture == null) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXT_TEXTURE_ID, textX, textY, 0f, 0f, textW, textH, textPixelW, textPixelH, textPixelW, textPixelH);
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
            shader.render(graphics, mouseX, mouseY);
            return;
        }
        ensureBackgroundTexture();
        if (backgroundTexture == null || backgroundTextureW <= 0 || backgroundTextureH <= 0) {
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

    private void renderEntryHint(GuiGraphics graphics) {
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
        alpha = Math.max(0f, Math.min(1f, alpha));
        int a = Math.round(alpha * 255f);
        if (a <= 0) return;

        String text = Config.isChinese
                ? "点击左上角“Minecraft”标题可返回原版UI，右键则可以切换风格。"
                : "Click the \"Minecraft\" title in the top-left to return to the vanilla UI. Right-click it to switch styles.";
        int textW = this.font.width(text);
        int x = (this.width - textW) / 2;
        int y = this.height / 2;
        int bgW = textW + 28;
        int bgH = 28;
        int bgX = (this.width - bgW) / 2;
        int bgY = y - 15;
        graphics.fill(bgX, bgY, bgX + bgW, bgY + bgH, (Math.round(alpha * 150f) << 24));
        graphics.drawString(this.font, text, x, y - 4, (a << 24) | 0xFFFFFF, false);
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
        FontRenderer.drawText(c, "Minecraft", titleHitBox.x, titleHitBox.y + titleHitBox.h * 0.82f, titleSize(), mainTextColor(255));
        renderSettingsPlaceholder(c);
        renderSettingsPanel(c);
        for (MenuButton button : buttons) {
            button.renderText(c);
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
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TitleScreen());
        }
    }

    @Override
    public void removed() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        destroyTextTexture();
        destroyBackgroundTexture();
        closeVideoBackground();
        lastWindowPixelW = -1;
        lastWindowPixelH = -1;
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private void refreshShader() {
        if (shader != null) shader.close();
        shader = MainUIShader.random();
        MainUISharedBackground.setActiveShader(shader.fragmentPath());
    }

    private PVPUtilsMainUI returnParent() {
        return new PVPUtilsMainUI(null, false, shader == null ? null : shader.fragmentPath());
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
        float minX = titleHitBox.x;
        float minY = titleHitBox.y;
        float maxX = titleHitBox.x + titleHitBox.w;
        float maxY = titleHitBox.y + titleHitBox.h;
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
        return isImageBackground() ? 276f : isVideoBackground() ? 220f : 146f;
    }

    private float getSettingsPanelMaxHeight() {
        return 276f;
    }

    private void setBackgroundMode(Config.MainUIBackgroundMode mode) {
        Config.mainUIBackgroundMode = mode == null ? Config.MainUIBackgroundMode.GLSL : mode;
        Config.mainUICustomBackground = Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.IMAGE;
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
        private final Runnable action;
        private float x;
        private float y;
        private float w;
        private float h;
        private float hover;

        private MenuButton(String text, Runnable action) {
            this.text = text;
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

        private void render(GuiGraphics graphics, int mouseX, int mouseY, boolean pressed) {
            hover += ((contains(mouseX, mouseY) ? 1f : 0f) - hover) * 0.2f;
            int lineColor = lerpColor(0xFFB7B7B7, pressed ? 0xFFAC6120 : 0xFFD77927, hover);
            graphics.fill(Math.round(x + 1f), Math.round(y + 1f), Math.round(x + w + 2f), Math.round(y + 5f), 0xA0404040);
            graphics.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + 4f), lineColor);
        }

        private void renderText(Canvas canvas) {
            String first = text.substring(0, 1);
            String rest = text.length() > 1 ? text.substring(1) : "";
            float textScale = Math.min((PVPUtilsMainUI.this.width * 2f + PVPUtilsMainUI.this.height) / 5000f + 1.25f, 3f);
            float size = 10f * textScale;
            float textY = y + 20f;
            FontRenderer.drawText(canvas, first, x, textY, size, 0xFFE69E2A);
            FontRenderer.drawText(canvas, rest, x + FontRenderer.measureTextWidth(first, size), textY, size, mainTextColor(255));
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
