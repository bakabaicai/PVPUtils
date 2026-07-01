package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Picture;
import io.github.humbleui.skija.PictureRecorder;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class SettingModule {

    public final String title;
    public final String subtitle;
    public final SettingWidget mainWidget;
    private final List<SubEntry> subEntries = new ArrayList<>();
    private BooleanSupplier visibleSupplier = () -> true;

    private boolean expanded = false;
    private float expandProgress = 0f;

    private static final float MODULE_H = 56f;
    private static final float SUB_H = 44f;
    private static final float PAD_X = 20f;
    private static final String ARROW_EXPANDED = "\uE5CF";
    private static final String ARROW_COLLAPSED = "\uE5CC";
    private final Paint modulePaint = new Paint();
    private final Paint subPaint = new Paint();
    private Picture staticPicture;
    private float staticPictureWidth = -1f;
    private boolean staticPictureExpanded = false;

    public SettingModule(String title, String subtitle, SettingWidget mainWidget) {
        this.title = title;
        this.subtitle = subtitle;
        this.mainWidget = mainWidget;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, () -> true, false));
        disposeStaticPicture();
        return this;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier, true));
        disposeStaticPicture();
        return this;
    }

    public SettingModule addSubWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier, true));
        disposeStaticPicture();
        return this;
    }

    public SettingModule visibleWhen(BooleanSupplier supplier) {
        this.visibleSupplier = supplier;
        disposeStaticPicture();
        return this;
    }

    public boolean isVisible() {
        return visibleSupplier.getAsBoolean();
    }

    public float getTotalHeight() {
        float base = MODULE_H;
        if (expandProgress > 0f) {
            base += expandProgress * getVisibleSubCount() * SUB_H;
        }
        return base;
    }

    public boolean isAnimating() {
        if (Math.abs((expanded ? 1f : 0f) - expandProgress) > 0.01f) return true;
        if (mainWidget != null && mainWidget.isAnimating()) return true;
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            if (sub.widget != null && sub.widget.isAnimating()) return true;
        }
        return false;
    }

    public void update(float dt) {
        float target = expanded ? 1f : 0f;
        expandProgress += (target - expandProgress) * Math.min(1f, dt * 14f);
        if (expandProgress < 0.001f) expandProgress = 0f;
        if (expandProgress > 0.999f) expandProgress = 1f;
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float alpha, float viewportTop, float viewportBottom) {
        boolean stableExpanded = expandProgress >= 0.99f;
        boolean stableCollapsed = expandProgress <= 0.01f;
        boolean canUseStaticPicture = alpha >= 0.999f && (stableExpanded || stableCollapsed) && !hasConditionalSubEntries();

        if (canUseStaticPicture) {
            boolean expandedState = stableExpanded && hasVisibleSubEntries();
            ensureStaticPicture(contentW, expandedState);
            if (staticPicture != null) {
                canvas.save();
                canvas.translate(x, y);
                canvas.drawPicture(staticPicture);
                canvas.restore();
            }
        } else {
            drawStaticContent(canvas, x, y, contentW, alpha, viewportTop, viewportBottom, expandProgress);
        }

        if (mainWidget != null) {
            float wx = x + contentW - PAD_X - mainWidget.getWidth();
            float wy = y + (MODULE_H - 8f - mainWidget.getHeight()) / 2f;
            mainWidget.draw(canvas, wx, wy, alpha);
        }

        if (expandProgress > 0.01f) {
            float subAlpha = alpha * expandProgress;
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (subBottom > viewportTop && sy < viewportBottom && sub.widget != null) {
                    float wx = x + contentW - PAD_X - sub.widget.getWidth();
                    float wy = sy + (SUB_H - 6f - sub.widget.getHeight()) / 2f;
                    sub.widget.draw(canvas, wx, wy, subAlpha);
                }
                sy += SUB_H;
            }
        }
    }

    private void ensureStaticPicture(float contentW, boolean expandedState) {
        if (staticPicture != null && Math.abs(staticPictureWidth - contentW) < 0.01f && staticPictureExpanded == expandedState) return;
        disposeStaticPicture();
        PictureRecorder recorder = new PictureRecorder();
        float totalHeight = MODULE_H + (expandedState ? getVisibleSubCount() * SUB_H : 0f);
        Canvas pictureCanvas = recorder.beginRecording(Rect.makeXYWH(0f, 0f, contentW, totalHeight));
        drawStaticContent(pictureCanvas, 0f, 0f, contentW, 1f, 0f, totalHeight, expandedState ? 1f : 0f);
        staticPicture = recorder.finishRecordingAsPicture();
        staticPictureWidth = contentW;
        staticPictureExpanded = expandedState;
    }

    private void drawStaticContent(Canvas canvas, float x, float y, float contentW, float alpha, float viewportTop, float viewportBottom, float progress) {
        modulePaint.setColor(withAlpha(0xFFFFFF, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, contentW, MODULE_H - 8f, 10f), modulePaint);

        FontRenderer.drawText(canvas, title, x + PAD_X, y + 22f, 13f, withAlpha(0x111111, alpha));
        FontRenderer.drawText(canvas, subtitle, x + PAD_X, y + 38f, 10f, withAlpha(0xAAAAAA, alpha));

        if (progress > 0.01f) {
            float subAlpha = alpha * progress;
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (subBottom > viewportTop && sy < viewportBottom) {
                    subPaint.setColor(withAlpha(0xF8F8FF, subAlpha));
                    canvas.drawRRect(RRect.makeXYWH(x + 8f, sy, contentW - 8f, SUB_H - 6f, 8f), subPaint);
                    if (sub.subtitle == null || sub.subtitle.isEmpty()) {
                        FontRenderer.drawText(canvas, sub.title, x + PAD_X + 8f, sy + (SUB_H - 6f) / 2f + 4.5f, 12f, withAlpha(0x333333, subAlpha));
                    } else {
                        FontRenderer.drawText(canvas, sub.title, x + PAD_X + 8f, sy + 16f, 12f, withAlpha(0x333333, subAlpha));
                        FontRenderer.drawText(canvas, sub.subtitle, x + PAD_X + 8f, sy + 30f, 10f, withAlpha(0xAAAAAA, subAlpha));
                    }
                }
                sy += SUB_H;
            }
        }

        if (hasVisibleSubEntries()) {
            String arrow = progress > 0.5f ? ARROW_EXPANDED : ARROW_COLLAPSED;
            float aw = FontRenderer.measureTextWidth(arrow, 12f, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, arrow, x + contentW - 5f - aw, y + (MODULE_H - 8f) / 2f + 5.5f, 12f, withAlpha(0xBBBBBB, alpha), FontRenderer.MATERIAL_SYMBOLS);
        }
    }

    public boolean onClick(float mx, float my, float x, float y, float contentW, int button) {
        float moduleBottom = y + MODULE_H - 8f;

        if (my >= y && my <= moduleBottom) {
            if (button == 1 && hasVisibleSubEntries()) {
                expanded = !expanded;
                disposeStaticPicture();
                return true;
            }
            if (button == 0 && mainWidget != null) {
                float wx = x + contentW - PAD_X - mainWidget.getWidth();
                float wy = y + (MODULE_H - 8f - mainWidget.getHeight()) / 2f;
                return mainWidget.onClick(mx, my, wx, wy, button);
            }
        }

        if (expanded && expandProgress > 0.5f) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (my >= sy && my <= subBottom && sub.widget != null) {
                    float wx = x + contentW - PAD_X - sub.widget.getWidth();
                    float wy = sy + (SUB_H - 6f - sub.widget.getHeight()) / 2f;
                    return sub.widget.onClick(mx, my, wx, wy, button);
                }
                sy += SUB_H;
            }
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float x, float y, float contentW) {
        if (mainWidget instanceof SettingSlider s && s.isDragging()) {
            float wx = x + contentW - PAD_X - s.getWidth();
            float wy = y + (MODULE_H - 8f - s.getHeight()) / 2f;
            return s.onDrag(mx, my, wx, wy);
        }
        if (expanded) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                if (sub.widget instanceof SettingSlider s && s.isDragging()) {
                    float wx = x + contentW - PAD_X - s.getWidth();
                    float wy = sy + (SUB_H - 6f - s.getHeight()) / 2f;
                    return s.onDrag(mx, my, wx, wy);
                }
                sy += SUB_H;
            }
        }
        return false;
    }

    public void releaseDrag() {
        if (mainWidget instanceof SettingSlider s) s.releaseDrag();
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            if (sub.widget instanceof SettingSlider s) s.releaseDrag();
        }
    }

    private void disposeStaticPicture() {
        if (staticPicture != null) {
            staticPicture.close();
            staticPicture = null;
        }
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    private int getVisibleSubCount() {
        int count = 0;
        for (SubEntry sub : subEntries) {
            if (sub.isVisible()) count++;
        }
        return count;
    }

    private boolean hasVisibleSubEntries() {
        for (SubEntry sub : subEntries) {
            if (sub.isVisible()) return true;
        }
        return false;
    }

    private boolean hasConditionalSubEntries() {
        for (SubEntry sub : subEntries) {
            if (sub.conditional) return true;
        }
        return false;
    }

    private record SubEntry(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier, boolean conditional) {
        private boolean isVisible() {
            return visibleSupplier.getAsBoolean();
        }
    }
}
