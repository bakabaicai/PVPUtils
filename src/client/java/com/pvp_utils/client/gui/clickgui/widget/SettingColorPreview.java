package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;

import java.util.function.IntSupplier;

public class SettingColorPreview extends SettingWidget {
    private final IntSupplier colorSupplier;
    private final Paint fillPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true);

    public SettingColorPreview(IntSupplier colorSupplier) {
        this.colorSupplier = colorSupplier;
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
        canvas.drawRRect(rect, fillPaint);
        canvas.drawRRect(rect, borderPaint);
    }
}
