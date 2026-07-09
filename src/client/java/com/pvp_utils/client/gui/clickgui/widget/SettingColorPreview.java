package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class SettingColorPreview extends SettingWidget {
    private final IntSupplier colorSupplier;
    private final IntSupplier secondColorSupplier;
    private final BooleanSupplier splitSupplier;
    private final Paint fillPaint = new Paint().setAntiAlias(true);
    private final Paint secondFillPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true);

    public SettingColorPreview(IntSupplier colorSupplier) {
        this(colorSupplier, colorSupplier, () -> false);
    }

    public SettingColorPreview(IntSupplier colorSupplier, IntSupplier secondColorSupplier, BooleanSupplier splitSupplier) {
        this.colorSupplier = colorSupplier;
        this.secondColorSupplier = secondColorSupplier;
        this.splitSupplier = splitSupplier;
        borderPaint.setMode(PaintMode.STROKE);
        borderPaint.setStrokeWidth(1.4f);
    }

    @Override public float getWidth() { return 100f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        int color = colorSupplier.getAsInt();
        fillPaint.setColor(withAlpha(color, alpha));
        borderPaint.setColor(withAlpha(0xFF111827, alpha * 0.24f));
        RRect rect = RRect.makeXYWH(x, y, getWidth(), getHeight(), 8f);
        if (splitSupplier.getAsBoolean()) {
            int secondColor = secondColorSupplier.getAsInt();
            secondFillPaint.setColor(withAlpha(secondColor, alpha));
            canvas.save();
            canvas.clipRRect(rect);
            canvas.drawRect(Rect.makeXYWH(x, y, getWidth() * 0.5f, getHeight()), fillPaint);
            canvas.drawRect(Rect.makeXYWH(x + getWidth() * 0.5f, y, getWidth() * 0.5f, getHeight()), secondFillPaint);
            canvas.restore();
        } else {
            canvas.drawRRect(rect, fillPaint);
        }
        canvas.drawRRect(rect, borderPaint);
    }
}
