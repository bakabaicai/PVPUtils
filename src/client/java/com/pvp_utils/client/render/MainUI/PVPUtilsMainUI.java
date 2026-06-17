package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class PVPUtilsMainUI extends Screen {
    private static final Identifier TEXT_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_text");
    private static final long HINT_DURATION_MS = 5000L;
    private static final long HINT_FADE_IN_MS = 400L;
    private static final long HINT_FADE_OUT_MS = 800L;

    private MainUIShader shader;
    private final boolean showEntryHint;
    private final List<MenuButton> buttons = new ArrayList<>();
    private TitleHitBox titleHitBox = new TitleHitBox(0f, 0f, 0f, 0f);
    private Surface textSurface;
    private DynamicTexture textTexture;
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

    public PVPUtilsMainUI(Screen parent) {
        this(parent, false);
    }

    public PVPUtilsMainUI(Screen parent, boolean showEntryHint) {
        super(Component.literal("Minecraft"));
        this.showEntryHint = showEntryHint;
    }

    @Override
    protected void init() {
        if (shader != null) shader.close();
        shader = MainUIShader.random();
        hintStartMs = showEntryHint ? System.currentTimeMillis() : 0L;
        invalidateTextTexture();
        buttons.clear();
        buttons.add(new MenuButton("Singleplayer", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(new PVPUtilsMainUI(null)));
        }));
        buttons.add(new MenuButton("Multiplayer", () -> {
            if (this.minecraft == null) return;
            Screen parent = new PVPUtilsMainUI(null);
            Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(parent) : new SafetyScreen(parent);
            this.minecraft.setScreen(screen);
        }));
        buttons.add(new MenuButton("Options", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(new PVPUtilsMainUI(null), this.minecraft.options));
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
        shader.render(graphics, mouseX, mouseY);
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
        FontRenderer.drawText(c, "Minecraft", titleHitBox.x, titleHitBox.y + titleHitBox.h * 0.82f, titleSize(), 0xFFFFFFFF);
        for (MenuButton button : buttons) {
            button.renderText(c);
        }
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
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private void refreshShader() {
        if (shader != null) shader.close();
        shader = MainUIShader.random();
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
        for (MenuButton button : buttons) {
            minX = Math.min(minX, button.x - 4f);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.w + 4f);
            maxY = Math.max(maxY, button.y + button.h + 10f);
        }
        textX = Math.max(0, (int) Math.floor(minX - 6f));
        textY = Math.max(0, (int) Math.floor(minY - 6f));
        int right = Math.min(this.width, (int) Math.ceil(maxX + 6f));
        int bottom = Math.min(this.height, (int) Math.ceil(maxY + 6f));
        textW = Math.max(1, right - textX);
        textH = Math.max(1, bottom - textY);
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
            FontRenderer.drawText(canvas, rest, x + FontRenderer.measureTextWidth(first, size), textY, size, 0xFFFFFFFF);
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
}
