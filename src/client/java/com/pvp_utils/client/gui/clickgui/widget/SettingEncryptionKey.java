package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.Config;
import com.pvp_utils.client.communication.EncryptedChatManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;

public final class SettingEncryptionKey extends SettingTextBox {
    private static final float INPUT_WIDTH = 150f;
    private static final float BUTTON_WIDTH = 56f;
    private static final float GAP = 6f;
    private final SettingButton randomButton;
    private final SettingButton copyButton;
    private float copiedSeconds;

    public SettingEncryptionKey() {
        super(() -> Config.encryptedChatKey, value -> {
            Config.encryptedChatKey = value;
            Config.save();
        }, Integer.MAX_VALUE);
        randomButton = new SettingButton(() -> UiText.t("随机", "Random"), () -> {
            Config.encryptedChatKey = EncryptedChatManager.generateKey();
            Config.save();
            copiedSeconds = 0f;
        }) {
            @Override public float getWidth() { return BUTTON_WIDTH; }
        };
        copyButton = new SettingButton(() -> copiedSeconds > 0f ? UiText.t("已复制", "Copied") : UiText.t("复制", "Copy"), () -> {
            if (Config.encryptedChatKey.isEmpty()) return;
            Minecraft.getInstance().keyboardHandler.setClipboard(Config.encryptedChatKey);
            copiedSeconds = 1.5f;
        }) {
            @Override public float getWidth() { return BUTTON_WIDTH; }
        };
    }

    @Override
    public float getWidth() {
        return INPUT_WIDTH + GAP * 2f + BUTTON_WIDTH * 2f;
    }

    @Override
    protected float getInputWidth() {
        return INPUT_WIDTH;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        super.draw(canvas, x, y, alpha);
        randomButton.draw(canvas, x + INPUT_WIDTH + GAP, y, alpha);
        copyButton.draw(canvas, x + INPUT_WIDTH + GAP * 2f + BUTTON_WIDTH, y, alpha);
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (randomButton.onClick(mx, my, x + INPUT_WIDTH + GAP, y, button)
                || copyButton.onClick(mx, my, x + INPUT_WIDTH + GAP * 2f + BUTTON_WIDTH, y, button)) {
            clearFocus();
            return true;
        }
        return super.onClick(mx, my, x, y, button);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        randomButton.update(dt);
        copyButton.update(dt);
        copiedSeconds = Math.max(0f, copiedSeconds - dt);
    }

    @Override
    public boolean isAnimating() {
        return super.isAnimating() || randomButton.isAnimating() || copyButton.isAnimating() || copiedSeconds > 0f;
    }
}
