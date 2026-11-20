package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatNewStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class BetterChatEditBoxMixin {
    @Shadow @Final private Font font;
    @Shadow private int displayPos;
    @Shadow private int highlightPos;
    @Shadow private int textX;
    @Shadow private int textY;
    @Shadow private int textColor;
    @Shadow private int textColorUneditable;
    @Shadow private boolean isEditable;
    @Shadow private FormattedCharSequence applyFormat(String text, int offset) { throw new AssertionError(); }

    @Unique private boolean pvp_utils$cursorInitialized;
    @Unique private float pvp_utils$cursorFrom;
    @Unique private float pvp_utils$cursorTarget;
    @Unique private long pvp_utils$cursorStarted;
    @Unique private long pvp_utils$cursorLastInteraction;
    @Unique private int pvp_utils$previousCursor;
    @Unique private int pvp_utils$previousDisplayPos;
    @Unique private int pvp_utils$previousTextX;
    @Unique private int pvp_utils$previousInnerWidth;
    @Unique private String pvp_utils$previousValue = "";

    @Unique
    private boolean pvp_utils$customCursorEnabled() {
        if (!BetterChatNewStyle.enabled()) return false;
        return Minecraft.getInstance().screen instanceof ChatScreen screen
                && ((ChatScreenAccessor) screen).pvp_utils$getInput() == (Object) this;
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void pvp_utils$hideVanillaInsertCursor(GuiGraphics graphics, int left, int top, int right, int bottom,
                                                  int color, Operation<Void> original) {
        if (!pvp_utils$customCursorEnabled()) original.call(graphics, left, top, right, bottom, color);
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", ordinal = 1))
    private void pvp_utils$hideVanillaAppendCursor(GuiGraphics graphics, Font textFont, String text, int x, int y,
                                                  int color, boolean shadow, Operation<Void> original) {
        if (!pvp_utils$customCursorEnabled()) original.call(graphics, textFont, text, x, y, color, shadow);
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void pvp_utils$drawAnimatedCursor(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                             CallbackInfo ci) {
        EditBox input = (EditBox) (Object) this;
        if (!pvp_utils$customCursorEnabled() || !input.isVisible() || !input.isFocused()) {
            this.pvp_utils$cursorInitialized = false;
            return;
        }
        String value = input.getValue();
        int cursor = input.getCursorPosition();
        int innerWidth = input.getInnerWidth();
        String visible = this.font.plainSubstrByWidth(value.substring(this.displayPos), innerWidth);
        int visibleCursor = cursor - this.displayPos;
        if (innerWidth <= 1 || visibleCursor < 0 || visibleCursor > visible.length()) {
            this.pvp_utils$cursorInitialized = false;
            return;
        }

        long now = System.nanoTime();
        float target = this.textX + this.font.width(applyFormat(visible.substring(0, visibleCursor), this.displayPos));
        boolean selection = cursor != this.highlightPos;
        boolean changed = !this.pvp_utils$cursorInitialized || cursor != this.pvp_utils$previousCursor
                || !value.equals(this.pvp_utils$previousValue);
        boolean snap = !this.pvp_utils$cursorInitialized || this.displayPos != this.pvp_utils$previousDisplayPos
                || this.textX != this.pvp_utils$previousTextX || innerWidth != this.pvp_utils$previousInnerWidth
                || selection || Math.abs(target - this.pvp_utils$cursorTarget) > innerWidth * 0.5f;
        if (snap) {
            this.pvp_utils$cursorFrom = target;
            this.pvp_utils$cursorTarget = target;
            this.pvp_utils$cursorStarted = now;
        } else if (target != this.pvp_utils$cursorTarget) {
            this.pvp_utils$cursorFrom = pvp_utils$cursorPosition(now);
            this.pvp_utils$cursorTarget = target;
            this.pvp_utils$cursorStarted = now;
        }
        if (changed || selection) this.pvp_utils$cursorLastInteraction = now;
        this.pvp_utils$cursorInitialized = true;
        this.pvp_utils$previousValue = value;
        this.pvp_utils$previousCursor = cursor;
        this.pvp_utils$previousDisplayPos = this.displayPos;
        this.pvp_utils$previousTextX = this.textX;
        this.pvp_utils$previousInnerWidth = innerWidth;

        long idle = now - this.pvp_utils$cursorLastInteraction;
        float opacity = idle <= 450_000_000L ? 1f
                : (float) (0.5 + 0.5 * Math.cos((idle - 450_000_000L) / 600_000_000.0 * Math.PI * 2.0));
        int textColor = this.isEditable ? this.textColor : this.textColorUneditable;
        int textAlpha = textColor >>> 24;
        int alpha = Math.round(opacity * (textAlpha == 0 ? 255 : textAlpha));
        if (alpha == 0) return;
        float x = Math.clamp(pvp_utils$cursorPosition(now), this.textX, this.textX + innerWidth - 1.25f);
        int color = (alpha << 24) | (textColor & 0xFFFFFF);
        graphics.enableScissor(this.textX, this.textY - 1, this.textX + innerWidth, this.textY + this.font.lineHeight + 1);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, this.textY - 1f);
            graphics.pose().scale(1.25f, 1f);
            graphics.fill(0, 0, 1, this.font.lineHeight + 1, color);
        } finally {
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
    }

    @Unique
    private float pvp_utils$cursorPosition(long now) {
        float progress = Math.clamp((now - this.pvp_utils$cursorStarted) / 120_000_000f, 0f, 1f);
        float inverse = 1f - progress;
        return this.pvp_utils$cursorTarget + (this.pvp_utils$cursorFrom - this.pvp_utils$cursorTarget)
                * inverse * inverse * inverse;
    }
}
