package com.pvp_utils.client.alt;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import com.pvp_utils.client.render.MainUI.MainUiScale;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AltManagerScreen extends Screen {
    private static final long OPEN_MS = 440L;
    private static final long CLOSE_MS = 440L;
    private final Screen parent;
    private final String shaderPath;
    private final Runnable embeddedBack;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final List<Float> accountHover = new ArrayList<>();
    private final List<Float> accountSelection = new ArrayList<>();
    private final Map<UUID, Image> accountAvatars = new HashMap<>();
    private final Map<UUID, CompletableFuture<byte[]>> pendingAvatars = new HashMap<>();
    private Image offlineAvatar;
    private AltManager.Account selected;
    private AltManager.Account deleteTarget;
    private String input = "";
    private String status = "";
    private String deviceCode = "";
    private String microsoftStatus = "Preparing Microsoft login...";
    private int microsoftStatusColor = 0xFFFFFFFF;
    private long deviceExpiresAt;
    private long deviceCodeCopiedAt;
    private long microsoftSuccessAt;
    private long statusExpiresAt;
    private long lastAccountClickMs;
    private int lastAccountClick = -1;
    private boolean offlineOpen;
    private boolean microsoftWaiting;
    private boolean deleteConfirmOpen;
    private boolean closingToMain;
    private boolean backDispatched;
    private boolean pendingFrame;
    private int mouseX;
    private int mouseY;
    private float scroll;
    private float targetScroll;
    private long openStartMs;
    private long closeStartMs;
    private float offlineDialogProgress;
    private float microsoftDialogProgress;
    private float deleteDialogProgress;

    public AltManagerScreen(Screen parent) {
        this(parent, MainUISharedBackground.activeShaderPath(), null);
    }

    public AltManagerScreen(Screen parent, String shaderPath, Runnable embeddedBack) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
        this.shaderPath = shaderPath;
        this.embeddedBack = embeddedBack;
    }

    public void initEmbedded(Minecraft client, int width, int height) {
        init(width, height);
    }

    @Override
    protected void init() {
        AltManager.init();
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        backDispatched = false;
        offlineOpen = false;
        microsoftWaiting = false;
        input = "";
        status = "";
        deviceCode = "";
        microsoftStatus = "Preparing Microsoft login...";
        microsoftStatusColor = 0xFFFFFFFF;
        deviceCodeCopiedAt = 0L;
        microsoftSuccessAt = 0L;
        statusExpiresAt = 0L;
        lastAccountClickMs = 0L;
        lastAccountClick = -1;
        deleteTarget = null;
        deleteConfirmOpen = false;
        offlineDialogProgress = 0f;
        microsoftDialogProgress = 0f;
        deleteDialogProgress = 0f;
        scroll = 0f;
        targetScroll = 0f;
        selected = currentAccount();
        syncAccountHover();
    }

    @Override
    public void tick() {
        super.tick();
        if (microsoftWaiting && deviceExpiresAt > 0L && System.currentTimeMillis() >= deviceExpiresAt) {
            deviceExpiresAt = 0L;
            microsoftStatus = "Microsoft login expired.";
            microsoftStatusColor = 0xFFFF7777;
        }
        if (microsoftWaiting && microsoftSuccessAt > 0L && System.currentTimeMillis() - microsoftSuccessAt >= 1800L) {
            microsoftWaiting = false;
            microsoftSuccessAt = 0L;
        }
        if (!status.isBlank() && statusExpiresAt > 0L && System.currentTimeMillis() >= statusExpiresAt) {
            status = "";
            statusExpiresAt = 0L;
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        clampScroll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (embeddedBack == null && minecraft.screen == this) {
            MainUISharedBackground.render(graphics, mouseX, mouseY);
        }
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        scroll += (targetScroll - scroll) * 0.20f;
        offlineDialogProgress = animate(offlineDialogProgress, offlineOpen ? 1f : 0f);
        microsoftDialogProgress = animate(microsoftDialogProgress, microsoftWaiting ? 1f : 0f);
        deleteDialogProgress = animate(deleteDialogProgress, deleteConfirmOpen ? 1f : 0f);
        if (!deleteConfirmOpen && deleteDialogProgress < .01f) deleteTarget = null;
        pendingFrame = true;
        if (closingToMain && closeProgress() >= 1f && minecraft != null && !backDispatched) {
            backDispatched = true;
            if (embeddedBack != null && minecraft.screen != this) {
                embeddedBack.run();
            } else {
                minecraft.setScreen(parent);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public void renderFrameEnd() {
        if (!pendingFrame || minecraft == null || (embeddedBack == null && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            if (!closingToMain) {
                SkiaBlurRenderer.getInstance().render(
                        canvas,
                        glBackend.getContext(),
                        minecraft,
                        mainFramebufferId(),
                        cardX(),
                        cardY(),
                        cardW(),
                        cardH(),
                        20f,
                        0x12000000,
                        0.95f
                );
            }
            draw(canvas);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    private void draw(Canvas canvas) {
        float contentAlpha = closingToMain ? 1f - ease(closeProgress()) : ease(openProgress());
        int alpha = Math.round(255f * contentAlpha);
        float x = cardX();
        float y = cardY();
        float w = cardW();
        float h = cardH();
        drawMainCard(canvas, x, y, w, h);
        String title = "Alt Manager";
        float pageW = MainUiScale.pageWidth();
        FontRenderer.drawText(
                canvas,
                title,
                pageW * .5f - FontRenderer.measureTextWidth(title, 30f) * .5f,
                titleY(),
                30f,
                (alpha << 24) | 0xFFFFFF
        );
        drawAccounts(canvas, x + 16f, y + 16f, w - 32f, h - 82f, alpha);
        if (!status.isBlank()) {
            drawCentered(canvas, status, pageW * .5f, y + h - 40f, 10f, (Math.round(alpha * .62f) << 24) | 0xFFFFFF);
        }
        drawCentered(canvas, currentLoginText(), pageW * .5f, y + h - 20f, 11f,
                (Math.round(alpha * .82f) << 24) | 0xFFFFFF);
        float buttonY = y + h + 12f;
        float buttonW = (w - 32f) / 5f;
        drawButton(canvas, bottomButtonX(x, buttonW, 0), buttonY, buttonW, 32f, "Login", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 1), buttonY, buttonW, 32f, "Delete", alpha,
                selected == null || !selected.isCurrent());
        drawButton(canvas, bottomButtonX(x, buttonW, 2), buttonY, buttonW, 32f, "Add", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 3), buttonY, buttonW, 32f, "Offline", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 4), buttonY, buttonW, 32f, "Back", alpha);
        float modalProgress = Math.max(offlineDialogProgress, Math.max(microsoftDialogProgress, deleteDialogProgress));
        if (modalProgress > .01f) {
            int tintAlpha = Math.round(0x26 * ease(modalProgress));
            SkiaBlurRenderer.getInstance().render(
                    canvas,
                    glBackend.getContext(),
                    minecraft,
                    mainFramebufferId(),
                    0f,
                    0f,
                    pageW,
                    MainUiScale.pageHeight(),
                    0f,
                    tintAlpha << 24,
                    0.45f + 0.50f * ease(modalProgress)
            );
        }
        if (offlineDialogProgress > .01f) drawAnimatedDialog(canvas, offlineDialogProgress, () -> drawOfflineDialog(canvas));
        if (microsoftDialogProgress > .01f) drawAnimatedDialog(canvas, microsoftDialogProgress, () -> drawMicrosoftDialog(canvas));
        if (deleteDialogProgress > .01f) drawAnimatedDialog(canvas, deleteDialogProgress, () -> drawDeleteDialog(canvas));
    }

    private void drawMainCard(Canvas canvas, float x, float y, float w, float h) {
        try (Paint card = new Paint(); Paint stroke = new Paint()) {
            card.setAntiAlias(true);
            card.setColor(0x32101010);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 20f), card);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, w - 1f, h - 1f, 20f), stroke);
        }
    }

    private void drawAccounts(Canvas canvas, float x, float y, float w, float h, int alpha) {
        List<AltManager.Account> accounts = AltManager.accounts();
        syncAccountHover();
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(x, y, w, h));
        if (accounts.isEmpty()) {
            drawCentered(canvas, "No accounts", x + w * .5f, y + h * .5f, 14f, (Math.round(alpha * .8f) << 24) | 0xFFFFFF);
        } else {
            float itemY = y - scroll;
            for (int i = 0; i < accounts.size(); i++) {
                drawAccount(canvas, accounts.get(i), i, x, itemY, w, 64f, alpha);
                itemY += 72f;
            }
        }
        canvas.restore();
    }

    private void drawAccount(Canvas canvas, AltManager.Account account, int index, float x, float y, float w, float h, int alpha) {
        if (y + h < cardY() + 16f || y > cardY() + cardH() - 66f) return;
        boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float hover = accountHover.get(index);
        hover += ((over ? 1f : 0f) - hover) * .16f;
        accountHover.set(index, hover);
        float curve = hover * hover * (3f - 2f * hover);
        boolean selectedNow = selected != null && selected.uuid().equals(account.uuid());
        float selection = accountSelection.get(index);
        selection += ((selectedNow ? 1f : 0f) - selection) * .16f;
        accountSelection.set(index, selection);
        float selectionCurve = selection * selection * (3f - 2f * selection);
        boolean current = account.isCurrent();
        try (Paint bg = new Paint(); Paint icon = new Paint()) {
            bg.setAntiAlias(true);
            float opacity = current
                    ? .15f + curve * .08f + selectionCurve * .08f
                    : .06f + curve * .13f + selectionCurve * .18f;
            int backgroundColor = current
                    ? lerpRgb(0x8DE8AE, 0xB8F4CB, selectionCurve)
                    : lerpRgb(0xFFFFFF, 0x86D4FF, selectionCurve);
            bg.setColor((Math.round(alpha * opacity) << 24) | backgroundColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 13f), bg);
            icon.setAntiAlias(true);
            icon.setColor((Math.round(alpha * (.10f + .12f * curve)) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + 10f, y + 10f, 44f, 44f, 10f), icon);
        }
        if (!drawAvatar(canvas, account, x + 10f, y + 10f, 44f, alpha)) {
            String symbol = account.typeName().equals("Microsoft") ? "\uE853" : "\uE7FD";
            float symbolSize = 24f;
            float symbolWidth = FontRenderer.measureTextWidth(symbol, symbolSize, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(
                    canvas,
                    symbol,
                    x + 32f - symbolWidth * .5f,
                    y + 39f,
                    symbolSize,
                    (Math.round(alpha * .86f) << 24) | 0xFFFFFF,
                    FontRenderer.MATERIAL_SYMBOLS
            );
        }
        FontRenderer.drawText(canvas, account.name(), x + 66f, y + 26f, 15f, (alpha << 24) | 0xFFFFFF);
        String detail = account.typeName() + (account.isCurrent() ? "  |  Active" : "");
        FontRenderer.drawText(canvas, detail, x + 66f, y + 46f, 11f, (Math.round(alpha * .72f) << 24) | 0xFFFFFF);
    }

    private void drawOfflineDialog(Canvas canvas) {
        float w = Math.min(380f, MainUiScale.pageWidth() - 40f);
        float h = 190f;
        float x = (MainUiScale.pageWidth() - w) * .5f;
        float y = (MainUiScale.pageHeight() - h) * .5f;
        drawDialogCard(canvas, x, y, w, h);
        drawCentered(canvas, "Add Offline", MainUiScale.pageWidth() * .5f, y + 38f, 18f, 0xFFFFFFFF);
        FontRenderer.drawText(canvas, "Username", x + 24f, y + 72f, 12f, 0xB8FFFFFF);
        drawInput(canvas, x + 24f, y + 84f, w - 48f, 34f);
        float buttonW = (w - 56f) * .5f;
        drawButton(canvas, x + 24f, y + 136f, buttonW, 30f, "Confirm", 255);
        drawButton(canvas, x + 32f + buttonW, y + 136f, buttonW, 30f, "Cancel", 255);
    }

    private void drawDeleteDialog(Canvas canvas) {
        float w = Math.min(380f, MainUiScale.pageWidth() - 40f);
        float h = 166f;
        float x = (MainUiScale.pageWidth() - w) * .5f;
        float y = (MainUiScale.pageHeight() - h) * .5f;
        drawDialogCard(canvas, x, y, w, h);
        drawCentered(canvas, "Delete Account", MainUiScale.pageWidth() * .5f, y + 38f, 18f, 0xFFFFFFFF);
        String accountName = deleteTarget == null ? "this account" : deleteTarget.name();
        drawCentered(canvas, "Delete " + accountName + "?", MainUiScale.pageWidth() * .5f, y + 82f, 13f, 0xCFFFFFFF);
        float buttonW = (w - 56f) * .5f;
        drawButton(canvas, x + 24f, y + 112f, buttonW, 30f, "Delete", 255);
        drawButton(canvas, x + 32f + buttonW, y + 112f, buttonW, 30f, "Cancel", 255);
    }

    private void drawMicrosoftDialog(Canvas canvas) {
        float w = Math.min(380f, MainUiScale.pageWidth() - 40f);
        float h = 166f;
        float x = (MainUiScale.pageWidth() - w) * .5f;
        float y = (MainUiScale.pageHeight() - h) * .5f;
        drawDialogCard(canvas, x, y, w, h);
        drawCentered(canvas, "Microsoft Login", MainUiScale.pageWidth() * .5f, y + 38f, 18f, 0xFFFFFFFF);
        if (microsoftSuccessAt > 0L) {
            FontRenderer.drawText(canvas, "\uE876", x + 24f, y + 83f, 24f, 0xFF62E58B, FontRenderer.MATERIAL_SYMBOLS);
        } else {
            drawSpinner(canvas, x + 38f, y + 76f);
        }
        FontRenderer.drawText(canvas, microsoftStatus, x + 66f, y + 80f, 13f, microsoftStatusColor);
        if (!deviceCode.isBlank()) {
            FontRenderer.drawText(canvas, "Code: " + deviceCode, x + 24f, y + 112f, 11f, 0xB8FFFFFF);
        }
        float buttonW = (w - 56f) * .5f;
        String copyLabel = System.currentTimeMillis() - deviceCodeCopiedAt < 1500L ? "Copied" : "Copy Code";
        drawButton(canvas, x + 24f, y + 124f, buttonW, 26f, copyLabel, 255);
        drawButton(canvas, x + 32f + buttonW, y + 124f, buttonW, 26f, "Cancel", 255);
    }

    private void drawDialogCard(Canvas canvas, float x, float y, float w, float h) {
        SkiaBlurRenderer.getInstance().render(
                canvas,
                glBackend.getContext(),
                minecraft,
                mainFramebufferId(),
                x,
                y,
                w,
                h,
                20f,
                0x12000000,
                0.95f
        );
        drawMainCard(canvas, x, y, w, h);
    }

    private void drawAnimatedDialog(Canvas canvas, float progress, Runnable renderer) {
        float eased = ease(progress);
        float scale = .90f + .10f * eased;
        canvas.saveLayerAlpha(Rect.makeXYWH(0f, 0f, width, height), Math.round(255f * eased));
        canvas.translate(width * .5f, height * .5f);
        canvas.scale(scale, scale);
        canvas.translate(-width * .5f, -height * .5f);
        renderer.run();
        canvas.restore();
    }

    private void drawInput(Canvas canvas, float x, float y, float w, float h) {
        boolean over = inside(mouseX, mouseY, x, y, w, h);
        try (Paint bg = new Paint(); Paint stroke = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(((over ? 0x18 : 0x0E) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, w - 1f, h - 1f, 10f), stroke);
        }
        String shown = input.isBlank() ? "Enter username" : input;
        int color = input.isBlank() ? 0x8AFFFFFF : 0xFFFFFFFF;
        FontRenderer.drawText(canvas, shown, x + 12f, y + 22f, 12f, color);
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cursorX = x + 12f + FontRenderer.measureTextWidth(input, 12f);
            try (Paint cursor = new Paint()) {
                cursor.setColor(0xFFFFFFFF);
                canvas.drawRect(Rect.makeXYWH(cursorX, y + 8f, 1f, h - 16f), cursor);
            }
        }
    }

    private void drawButton(Canvas canvas, float x, float y, float w, float h, String label, int alpha) {
        drawButton(canvas, x, y, w, h, label, alpha, true);
    }

    private void drawButton(Canvas canvas, float x, float y, float w, float h, String label, int alpha, boolean enabled) {
        boolean over = enabled && inside(mouseX, mouseY, x, y, w, h);
        float hover = over ? 1f : 0f;
        int color = enabled ? lerpRgb(0x67B9EA, 0xA7E0FF, hover) : 0x7D8790;
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * (enabled ? .20f + .12f * hover : .12f)) << 24) | color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
        }
        float fontSize = h <= 28f ? 12f : 13f;
        FontRenderer.drawText(
                canvas,
                label,
                x + (w - FontRenderer.measureTextWidth(label, fontSize)) * .5f,
                y + (h <= 28f ? 18f : 21f),
                fontSize,
                (Math.round(alpha * (enabled ? 1f : .48f)) << 24) | 0xFFFFFF
        );
    }

    private void drawSpinner(Canvas canvas, float x, float y) {
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(0xFFA7E0FF);
            canvas.drawArc(
                    x - 14f,
                    y - 14f,
                    x + 14f,
                    y + 14f,
                    (System.currentTimeMillis() % 1200L) / 1200f * 360f,
                    270f,
                    false,
                    paint
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0 || closingToMain) return true;
        float mx = (float) event.x();
        float my = (float) event.y();
        if (deleteConfirmOpen) {
            float w = Math.min(380f, width - 40f);
            float h = 166f;
            float x = (width - w) * .5f;
            float y = (height - h) * .5f;
            float buttonW = (w - 56f) * .5f;
            if (inside(mx, my, x + 24f, y + 112f, buttonW, 30f)) {
                playClick();
                if (deleteTarget != null && !deleteTarget.isCurrent()) {
                    AltManager.remove(deleteTarget);
                    selected = currentAccount();
                    showStatus("Account deleted.");
                    syncAccountHover();
                    clampScroll();
                }
                deleteConfirmOpen = false;
                return true;
            }
            if (inside(mx, my, x + 32f + buttonW, y + 112f, buttonW, 30f)) {
                playClick();
                deleteConfirmOpen = false;
            }
            return true;
        }
        if (microsoftWaiting) {
            float w = Math.min(380f, width - 40f);
            float h = 166f;
            float x = (width - w) * .5f;
            float y = (height - h) * .5f;
            float buttonW = (w - 56f) * .5f;
            if (inside(mx, my, x + 24f, y + 124f, buttonW, 26f)) {
                playClick();
                if (!deviceCode.isBlank() && minecraft != null) {
                    minecraft.keyboardHandler.setClipboard(deviceCode);
                    deviceCodeCopiedAt = System.currentTimeMillis();
                }
                return true;
            }
            if (inside(mx, my, x + 32f + buttonW, y + 124f, buttonW, 26f)) {
                playClick();
                microsoftWaiting = false;
                showStatus("Microsoft login cancelled.");
            }
            return true;
        }
        if (offlineOpen) {
            handleOfflineDialogClick(mx, my);
            return true;
        }
        float x = cardX();
        float buttonY = cardY() + cardH() + 12f;
        float buttonW = (cardW() - 32f) / 5f;
        if (inside(mx, my, bottomButtonX(x, buttonW, 0), buttonY, buttonW, 32f)) {
            playClick();
            if (selected != null) {
                showStatus(AltManager.login(selected) ? "Logged in as " + selected.name() : "Login failed.");
            }
            return true;
        }
        if (inside(mx, my, bottomButtonX(x, buttonW, 1), buttonY, buttonW, 32f)) {
            playClick();
            if (selected != null) {
                if (selected.isCurrent()) {
                    showStatus("The current account cannot be deleted.");
                } else {
                    deleteTarget = selected;
                    deleteConfirmOpen = true;
                }
            }
            return true;
        }
        if (inside(mx, my, bottomButtonX(x, buttonW, 2), buttonY, buttonW, 32f)) {
            playClick();
            startMicrosoftLogin();
            return true;
        }
        if (inside(mx, my, bottomButtonX(x, buttonW, 3), buttonY, buttonW, 32f)) {
            playClick();
            offlineOpen = true;
            input = "";
            return true;
        }
        if (inside(mx, my, bottomButtonX(x, buttonW, 4), buttonY, buttonW, 32f)) {
            playClick();
            startClose();
            return true;
        }
        int hit = accountAt(mx, my);
        if (hit >= 0) {
            long now = System.currentTimeMillis();
            AltManager.Account account = AltManager.accounts().get(hit);
            selected = account;
            if (lastAccountClick == hit && now - lastAccountClickMs <= 350L) {
                playClick();
                showStatus(AltManager.login(account) ? "Logged in as " + account.name() : "Login failed.");
                lastAccountClick = -1;
                lastAccountClickMs = 0L;
            } else {
                lastAccountClick = hit;
                lastAccountClickMs = now;
            }
        }
        return true;
    }

    private void handleOfflineDialogClick(float mx, float my) {
        float w = Math.min(380f, MainUiScale.pageWidth() - 40f);
        float h = 190f;
        float x = (MainUiScale.pageWidth() - w) * .5f;
        float y = (MainUiScale.pageHeight() - h) * .5f;
        float buttonW = (w - 56f) * .5f;
        if (inside(mx, my, x + 24f, y + 136f, buttonW, 30f)) {
            playClick();
            AltManager.Account account = AltManager.addOffline(input);
            if (account == null) {
                showStatus("Invalid account name.");
            } else {
                selected = account;
                showStatus("Account added.");
                offlineOpen = false;
                input = "";
                syncAccountHover();
                clampScroll();
            }
            return;
        }
        if (inside(mx, my, x + 32f + buttonW, y + 136f, buttonW, 30f)) {
            playClick();
            offlineOpen = false;
            input = "";
        }
    }

    private void startMicrosoftLogin() {
        microsoftWaiting = true;
        showStatus("Waiting for Microsoft authorization.");
        deviceCode = "";
        microsoftStatus = "Requesting device code...";
        microsoftStatusColor = 0xFFFFFFFF;
        deviceExpiresAt = 0L;
        deviceCodeCopiedAt = 0L;
        microsoftSuccessAt = 0L;
        MicrosoftAuth.login(info -> {
            String[] parts = info.split("\\n", 3);
            String code = parts.length > 0 ? parts[0] : "";
            long seconds;
            try {
                seconds = parts.length > 2 ? Long.parseLong(parts[2]) : 900L;
            } catch (NumberFormatException ignored) {
                seconds = 900L;
            }
            long expiresAt = System.currentTimeMillis() + seconds * 1000L;
            deviceCode = code;
            deviceExpiresAt = expiresAt;
            if (!code.isBlank() && minecraft != null) {
                minecraft.keyboardHandler.setClipboard(code);
                deviceCodeCopiedAt = System.currentTimeMillis();
            }
        }, value -> {
            microsoftStatus = value;
            microsoftStatusColor = 0xFFFFFFFF;
        }, account -> Minecraft.getInstance().execute(() -> {
            AltManager.add(account);
            selected = account;
            microsoftStatus = "Microsoft login successful.";
            microsoftStatusColor = 0xFF62E58B;
            microsoftSuccessAt = System.currentTimeMillis();
            showStatus("Microsoft login succeeded: " + account.name());
            syncAccountHover();
            clampScroll();
        }), error -> Minecraft.getInstance().execute(() -> {
            microsoftStatus = error == null || error.isBlank() ? "Microsoft login failed." : error;
            microsoftStatusColor = 0xFFFF7777;
            showStatus(microsoftStatus);
        }));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (offlineOpen || microsoftWaiting) return true;
        targetScroll -= (float) verticalAmount * 48f;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (offlineOpen) {
                offlineOpen = false;
                input = "";
            } else if (microsoftWaiting) {
                microsoftWaiting = false;
                showStatus("Microsoft login cancelled.");
            } else if (deleteConfirmOpen) {
                deleteConfirmOpen = false;
            } else {
                startClose();
            }
            return true;
        }
        if (offlineOpen && event.key() == 259 && !input.isEmpty()) {
            input = input.substring(0, input.length() - 1);
            return true;
        }
        if (offlineOpen && event.key() == 257) {
            AltManager.Account account = AltManager.addOffline(input);
            if (account == null) {
                showStatus("Invalid account name.");
            } else {
                selected = account;
                showStatus("Account added.");
                offlineOpen = false;
                input = "";
                syncAccountHover();
                clampScroll();
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (offlineOpen && event.isAllowedChatCharacter() && input.length() < 16) {
            input += event.codepointAsString();
        }
        return true;
    }

    @Override
    public void onClose() {
        if (offlineOpen) {
            offlineOpen = false;
            input = "";
            return;
        }
        if (microsoftWaiting) {
            microsoftWaiting = false;
            showStatus("Microsoft login cancelled.");
            return;
        }
        if (deleteConfirmOpen) {
            deleteConfirmOpen = false;
            return;
        }
        startClose();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        for (Image image : accountAvatars.values()) {
            if (image != null) image.close();
        }
        accountAvatars.clear();
        pendingAvatars.clear();
        if (offlineAvatar != null) {
            offlineAvatar.close();
            offlineAvatar = null;
        }
        glBackend.destroy();
        super.removed();
    }

    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private int accountAt(float mx, float my) {
        float x = cardX() + 16f;
        float y = cardY() + 16f;
        float w = cardW() - 32f;
        float h = cardH() - 82f;
        if (mx < x || mx > x + w || my < y || my > y + h) return -1;
        int index = (int) ((my - y + scroll) / 72f);
        float inside = (my - y + scroll) - index * 72f;
        return index >= 0 && index < AltManager.accounts().size() && inside <= 64f ? index : -1;
    }

    private void syncAccountHover() {
        int size = AltManager.accounts().size();
        while (accountHover.size() < size) accountHover.add(0f);
        while (accountHover.size() > size) accountHover.remove(accountHover.size() - 1);
        while (accountSelection.size() < size) accountSelection.add(0f);
        while (accountSelection.size() > size) accountSelection.remove(accountSelection.size() - 1);
    }

    private boolean drawAvatar(Canvas canvas, AltManager.Account account, float x, float y, float size, int alpha) {
        Image image = avatar(account);
        if (image == null) {
            image = offlineAvatar();
            if (image == null) return false;
            try (Paint paint = new Paint()) {
                paint.setAntiAlias(true);
                paint.setColor((alpha << 24) | 0xFFFFFF);
                canvas.save();
                canvas.clipRRect(RRect.makeXYWH(x, y, size, size, 10f), true);
                canvas.drawImageRect(
                        image,
                        Rect.makeXYWH(0f, 0f, image.getWidth(), image.getHeight()),
                        Rect.makeXYWH(x, y, size, size),
                        SamplingMode.DEFAULT,
                        paint,
                        true
                );
                canvas.restore();
            }
            return true;
        }
        float scaleX = image.getWidth() / 64f;
        float scaleY = image.getHeight() / 64f;
        Rect destination = Rect.makeXYWH(x, y, size, size);
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setColor((alpha << 24) | 0xFFFFFF);
            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(x, y, size, size, 10f), true);
            canvas.drawImageRect(
                    image,
                    Rect.makeXYWH(8f * scaleX, 8f * scaleY, 8f * scaleX, 8f * scaleY),
                    destination,
                    SamplingMode.DEFAULT,
                    paint,
                    true
            );
            canvas.drawImageRect(
                    image,
                    Rect.makeXYWH(40f * scaleX, 8f * scaleY, 8f * scaleX, 8f * scaleY),
                    destination,
                    SamplingMode.DEFAULT,
                    paint,
                    true
            );
            canvas.restore();
        }
        return true;
    }

    private Image avatar(AltManager.Account account) {
        UUID uuid = account.uuid();
        if (accountAvatars.containsKey(uuid)) return accountAvatars.get(uuid);
        CompletableFuture<byte[]> pending = pendingAvatars.get(uuid);
        if (pending == null) {
            pendingAvatars.put(uuid, AltAvatarLoader.load(account));
            return null;
        }
        if (!pending.isDone()) return null;
        pendingAvatars.remove(uuid);
        try {
            byte[] bytes = pending.join();
            Image image = bytes == null || bytes.length == 0 ? null : Image.makeFromEncoded(bytes);
            accountAvatars.put(uuid, image);
            return image;
        } catch (RuntimeException ignored) {
            accountAvatars.put(uuid, null);
            return null;
        }
    }

    private Image offlineAvatar() {
        if (offlineAvatar != null) return offlineAvatar;
        try (InputStream stream = AltManagerScreen.class.getResourceAsStream("/assets/pvp_utils/textures/Offline.png")) {
            if (stream != null) offlineAvatar = Image.makeFromEncoded(stream.readAllBytes());
        } catch (Exception ignored) {
            offlineAvatar = null;
        }
        return offlineAvatar;
    }

    private AltManager.Account currentAccount() {
        for (AltManager.Account account : AltManager.accounts()) {
            if (account.isCurrent()) return account;
        }
        return null;
    }

    private String currentLoginText() {
        AltManager.Account account = currentAccount();
        if (account != null) {
            return "Logged in as " + account.name() + " (" + account.typeName() + ")";
        }
        if (minecraft != null && minecraft.getUser() != null) {
            String type = minecraft.getUser().getXuid().isPresent() || minecraft.getUser().getClientId().isPresent()
                    ? "Microsoft"
                    : "Offline";
            return "Logged in as " + minecraft.getUser().getName() + " (" + type + ")";
        }
        return "No account is currently logged in";
    }

    private void clampScroll() {
        float max = Math.max(0f, AltManager.accounts().size() * 72f - (cardH() - 82f));
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        scroll = Math.max(0f, Math.min(max, scroll));
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    private void showStatus(String value) {
        status = value == null ? "" : value;
        statusExpiresAt = status.isBlank() ? 0L : System.currentTimeMillis() + 2600L;
    }

    private float animate(float current, float target) {
        float next = current + (target - current) * .18f;
        return Math.abs(next - target) < .002f ? target : next;
    }

    private void drawCentered(Canvas canvas, String text, float centerX, float y, float size, int color) {
        FontRenderer.drawText(canvas, text, centerX - FontRenderer.measureTextWidth(text, size) * .5f, y, size, color);
    }

    private boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float bottomButtonX(float x, float buttonW, int index) {
        return x + index * (buttonW + 8f);
    }

    private float openProgress() {
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openStartMs) / (float) OPEN_MS));
    }

    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / (float) CLOSE_MS));
    }

    private float ease(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private int lerpRgb(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private float cardW() {
        return Math.max(320f, Math.min(500f, width * .52f));
    }

    private float cardH() {
        return Math.max(260f, Math.min(height - 154f, height * .72f));
    }

    private float cardX() {
        return (width - cardW()) * .5f;
    }

    private float cardY() {
        return 76f;
    }

    private float titleY() {
        return Math.min(50f, cardY() + 12f);
    }
}
