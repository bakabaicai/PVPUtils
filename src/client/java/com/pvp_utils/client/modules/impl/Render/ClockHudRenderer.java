package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.HudAppearance;
import com.pvp_utils.client.render.skia.SkijaUi;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SoftShadow;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ClockHudRenderer extends SkiaTextHudRenderer implements AutoCloseable {
    private static final ClockHudRenderer INSTANCE = new ClockHudRenderer();
    private static final int DIAL_SIZE = 128;
    private static final float DATE_SIZE = 10.5f;
    private static final float DATE_PADDING = 2f;
    private static final float SHADOW_PADDING = 28f;
    private static final int SHADOW_RASTER_SCALE = 2;
    private static final double FULL_TURN = Math.PI * 2.0;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_CHINESE = DateTimeFormatter.ofPattern("EEE d", Locale.CHINA);
    private static final DateTimeFormatter DATE_ENGLISH = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);
    private long cachedSecond = Long.MIN_VALUE;
    private boolean cachedChinese;
    private String time = "";
    private String date = "";
    private LocalDateTime localTime;
    private double secondAngle;
    private double minuteAngle;
    private double hourAngle;
    private Path dialPath;
    private SkijaUi.PreparedText dateText;
    private String preparedDate;
    private long preparedFontRevision = -1;
    private Image dateImage;
    private int dateRasterScale;
    private Paint datePaint;
    private ColorFilter dateTint;
    private int dateTintColor;
    private Image shadowImage;
    private Paint shadowPaint;
    private float shadowStrength;
    private Path screenDialPath;
    private float screenDialX;
    private float screenDialY;
    private float screenDialWidth;
    private float screenDialHeight;

    private void updateClock() {
        long millis = System.currentTimeMillis();
        long second = Math.floorDiv(millis, 1000L);
        if (cachedSecond != second || cachedChinese != Config.isChinese) {
            localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            time = localTime.format(TIME_FORMAT);
            date = localTime.format(Config.isChinese ? DATE_CHINESE : DATE_ENGLISH);
            cachedSecond = second;
            cachedChinese = Config.isChinese;
        }
        double seconds = localTime.getSecond() + Math.floorMod(millis, 1000L) / 1000.0;
        double minutes = localTime.getMinute() + seconds / 60.0;
        secondAngle = seconds * FULL_TURN / 60.0;
        minuteAngle = minutes * FULL_TURN / 60.0;
        hourAngle = (localTime.getHour() % 12 + minutes / 60.0) * FULL_TURN / 12.0;
    }

    private ClockHudRenderer() {
    }

    public static ClockHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected HudAppearance appearance() {
        return Config.clockAppearance;
    }

    @Override
    protected boolean enabled() {
        return Config.clockHud;
    }

    @Override
    protected Config.HudStyle style() {
        return Config.clockHudStyle;
    }

    @Override
    protected boolean backgroundEnabled() {
        return Config.clockHudBackground;
    }

    @Override
    protected float configX() {
        return Config.clockHudX;
    }

    @Override
    protected void setConfigX(float value) {
        Config.clockHudX = value;
    }

    @Override
    protected float configY() {
        return Config.clockHudY;
    }

    @Override
    protected void setConfigY(float value) {
        Config.clockHudY = value;
    }

    @Override
    protected float configScale() {
        return Config.clockHudScale;
    }

    @Override
    protected void setConfigScale(float value) {
        Config.clockHudScale = value;
    }

    @Override
    protected List<Line> lines() {
        updateClock();
        return List.of(new Line(time, Config.clockAppearance.accent()));
    }

    @Override
    protected int skiaPanelWidth(List<Line> lines) {
        return style() == Config.HudStyle.MATERIAL ? DIAL_SIZE : super.skiaPanelWidth(lines);
    }

    @Override
    protected int panelHeight(int lineCount) {
        return style() == Config.HudStyle.MATERIAL ? DIAL_SIZE : super.panelHeight(lineCount);
    }

    @Override
    protected void drawBackground(Canvas canvas, float x, float y, float width, float height, float scale) {
        if (style() != Config.HudStyle.MATERIAL) {
            super.drawBackground(canvas, x, y, width, height, scale);
            return;
        }
        if (!backgroundEnabled()) return;
        int color = materialBackgroundColor();
        float opacity = (color >>> 24) / 255f;
        boolean blurred = Config.clockHudBlurBackground && opacity < 0.999f;
        if (blurred) {
            SkijaRenderer.drawBlurredBackdrop(canvas, screenDialPath(x, y, width, height),
                    x, y, width, height, Math.clamp(Config.skiaBlurStrength, 0f, 2f) * 10.5f);
        }
        if (opacity <= 0f && !blurred) return;
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(width / DIAL_SIZE, height / DIAL_SIZE);
            Path path = dialPath();
            HudAppearance appearance = appearance();
            if (appearance.shadow) {
                prepareShadow(appearance.shadowStrength);
                shadowPaint.setAlphaf(blurred ? 1f : opacity);
                canvas.drawImageRect(shadowImage, Rect.makeWH(shadowImage.getWidth(), shadowImage.getHeight()),
                        Rect.makeXYWH(-SHADOW_PADDING, -SHADOW_PADDING,
                                shadowImage.getWidth() / (float) SHADOW_RASTER_SCALE,
                                shadowImage.getHeight() / (float) SHADOW_RASTER_SCALE),
                        SamplingMode.LINEAR, shadowPaint, true);
            }
            if (opacity > 0f) SkijaUi.fillPath(canvas, path, color);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    @Override
    protected void drawContent(Canvas canvas, List<Line> lines,
                               float width, float height) {
        if (style() != Config.HudStyle.MATERIAL) {
            super.drawContent(canvas, lines, width, height);
            return;
        }
        prepareDateImage(canvas);
        HudAppearance appearance = appearance();
        int accent = appearance.accent();
        int background = materialBackgroundColor();
        float luminance = ((background >> 16) & 255) * 0.2126f
                + ((background >> 8) & 255) * 0.7152f + (background & 255) * 0.0722f;
        int foreground = appearance.customText ? appearance.text()
                : luminance > 150f ? 0xFF1D2417 : 0xFFF3F6E9;
        int minuteColor = appearance.customText ? foreground : mixColor(accent, foreground, 0.64f);
        int hourColor = mixColor(minuteColor, background, 0.44f);
        int dotColor = mixColor(accent, luminance > 150f ? 0xFF746019 : 0xFFFFE9AD, 0.86f);
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        drawHand(canvas, centerX, centerY, hourAngle, 26f, 10.5f, hourColor);
        drawHand(canvas, centerX, centerY, minuteAngle, 37f, 10.5f, minuteColor);
        float dotX = centerX + (float) Math.sin(secondAngle) * 49f;
        float dotY = centerY - (float) Math.cos(secondAngle) * 49f;
        SkijaUi.rounded(canvas, dotX - 4.5f, dotY - 4.5f, 9f, 9f, 4.5f, dotColor);
        int save = canvas.save();
        try {
            canvas.translate(centerX, centerY);
            canvas.rotate((float) Math.toDegrees(secondAngle) + 180f);
            tintDate(foreground);
            canvas.drawImageRect(dateImage, Rect.makeWH(dateImage.getWidth(), dateImage.getHeight()),
                    Rect.makeXYWH(-dateText.width() * 0.5f - DATE_PADDING, -54f - DATE_PADDING,
                            dateImage.getWidth() / (float) dateRasterScale,
                            dateImage.getHeight() / (float) dateRasterScale),
                    SamplingMode.LINEAR, datePaint, true);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private static void drawHand(Canvas canvas, float x, float y, double angle,
                                 float length, float thickness, int color) {
        SkijaUi.line(canvas, x, y, x + (float) Math.sin(angle) * length,
                y - (float) Math.cos(angle) * length, thickness, color, PaintStrokeCap.ROUND);
    }

    private void prepareDate() {
        long revision = SkijaUi.textFontRevision();
        if (dateText != null && date.equals(preparedDate) && revision == preparedFontRevision) return;
        if (dateText != null) dateText.close();
        if (dateImage != null) dateImage.close();
        dateImage = null;
        dateText = SkijaUi.prepareText(date, DATE_SIZE, false);
        preparedDate = date;
        preparedFontRevision = revision;
    }

    private void prepareDateImage(Canvas canvas) {
        prepareDate();
        float[] matrix = canvas.getLocalToDeviceAsMatrix33().getMat();
        double scale = Math.max(Math.hypot(matrix[0], matrix[3]), Math.hypot(matrix[1], matrix[4]));
        int density = Math.clamp((int) Math.ceil(scale), 1, 12);
        if (dateImage != null && dateRasterScale == density) return;
        float height = SkijaUi.textMetrics(DATE_SIZE).getDescent() - SkijaUi.textMetrics(DATE_SIZE).getAscent();
        int pixelWidth = Math.max(1, (int) Math.ceil((dateText.width() + DATE_PADDING * 2f) * density));
        int pixelHeight = Math.max(1, (int) Math.ceil((height + DATE_PADDING * 2f) * density));
        Image generated;
        try (Surface surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)) {
            Canvas raster = surface.getCanvas();
            raster.clear(0);
            raster.scale(density, density);
            dateText.drawTop(raster, DATE_PADDING, DATE_PADDING, 0xFFFFFFFF);
            generated = surface.makeImageSnapshot();
        }
        if (dateImage != null) dateImage.close();
        dateImage = generated;
        dateRasterScale = density;
    }

    private void tintDate(int color) {
        if (datePaint == null) datePaint = new Paint().setAntiAlias(true);
        int rgb = color | 0xFF000000;
        if (dateTint == null || dateTintColor != rgb) {
            ColorFilter generated = ColorFilter.makeBlend(rgb, BlendMode.SRC_IN);
            datePaint.setColorFilter(generated);
            if (dateTint != null) dateTint.close();
            dateTint = generated;
            dateTintColor = rgb;
        }
        datePaint.setAlphaf((color >>> 24) / 255f);
    }

    private void prepareShadow(float strength) {
        if (shadowImage != null && shadowStrength == strength) return;
        int size = Math.round((DIAL_SIZE + SHADOW_PADDING * 2f) * SHADOW_RASTER_SCALE);
        Image generated;
        try (Surface surface = Surface.makeRasterN32Premul(size, size)) {
            Canvas raster = surface.getCanvas();
            raster.clear(0);
            raster.scale(SHADOW_RASTER_SCALE, SHADOW_RASTER_SCALE);
            raster.translate(SHADOW_PADDING, SHADOW_PADDING);
            SoftShadow.drawPath(raster, dialPath(), strength, 0xB0000000);
            generated = surface.makeImageSnapshot();
        }
        if (shadowImage != null) shadowImage.close();
        shadowImage = generated;
        shadowStrength = strength;
        if (shadowPaint == null) shadowPaint = new Paint().setAntiAlias(true);
    }

    private Path screenDialPath(float x, float y, float width, float height) {
        if (screenDialPath != null && screenDialX == x && screenDialY == y
                && screenDialWidth == width && screenDialHeight == height) return screenDialPath;
        Path generated = dialPath().makeTransform(new Matrix33(width / DIAL_SIZE, 0f, x,
                0f, height / DIAL_SIZE, y, 0f, 0f, 1f));
        if (screenDialPath != null) screenDialPath.close();
        screenDialPath = generated;
        screenDialX = x;
        screenDialY = y;
        screenDialWidth = width;
        screenDialHeight = height;
        return screenDialPath;
    }

    private int materialBackgroundColor() {
        HudAppearance appearance = appearance();
        int color = appearance.background();
        if (!appearance.customBackground) color = mixColor(color, appearance.accent(), 0.13f);
        float opacity = Config.clockHudOpaqueBackground
                ? Math.clamp(Config.clockHudBackgroundOpacity, 0f, 1f) : 0f;
        return Math.round(opacity * 255f) << 24 | color & 0xFFFFFF;
    }

    private Path dialPath() {
        if (dialPath != null) return dialPath;
        try (PathBuilder builder = new PathBuilder()) {
            double step = FULL_TURN / 48.0;
            float center = DIAL_SIZE * 0.5f;
            for (int i = 0; i < 48; i++) {
                double from = i * step;
                double to = (i + 1) * step;
                double fromRadius = 61.8 + 2.2 * Math.cos(from * 12.0);
                double toRadius = 61.8 + 2.2 * Math.cos(to * 12.0);
                double fromSlope = -26.4 * Math.sin(from * 12.0);
                double toSlope = -26.4 * Math.sin(to * 12.0);
                double fromX = Math.sin(from) * fromRadius;
                double fromY = -Math.cos(from) * fromRadius;
                double toX = Math.sin(to) * toRadius;
                double toY = -Math.cos(to) * toRadius;
                double fromDx = Math.cos(from) * fromRadius + Math.sin(from) * fromSlope;
                double fromDy = Math.sin(from) * fromRadius - Math.cos(from) * fromSlope;
                double toDx = Math.cos(to) * toRadius + Math.sin(to) * toSlope;
                double toDy = Math.sin(to) * toRadius - Math.cos(to) * toSlope;
                if (i == 0) builder.moveTo(center + (float) fromX, center + (float) fromY);
                builder.cubicTo(center + (float) (fromX + fromDx * step / 3.0),
                        center + (float) (fromY + fromDy * step / 3.0),
                        center + (float) (toX - toDx * step / 3.0),
                        center + (float) (toY - toDy * step / 3.0),
                        center + (float) toX, center + (float) toY);
            }
            dialPath = builder.closePath().detach();
        }
        return dialPath;
    }

    private static int mixColor(int first, int second, float amount) {
        int red = Math.round(((first >> 16) & 255) * (1f - amount) + ((second >> 16) & 255) * amount);
        int green = Math.round(((first >> 8) & 255) * (1f - amount) + ((second >> 8) & 255) * amount);
        int blue = Math.round((first & 255) * (1f - amount) + (second & 255) * amount);
        return (first & 0xFF000000) | red << 16 | green << 8 | blue;
    }

    @Override
    public void close() {
        if (dialPath != null) dialPath.close();
        if (dateText != null) dateText.close();
        if (dateImage != null) dateImage.close();
        if (shadowImage != null) shadowImage.close();
        if (screenDialPath != null) screenDialPath.close();
        if (datePaint != null) {
            datePaint.setColorFilter(null);
            datePaint.close();
        }
        if (dateTint != null) dateTint.close();
        if (shadowPaint != null) shadowPaint.close();
        dialPath = null;
        dateText = null;
        preparedDate = null;
        preparedFontRevision = -1;
        dateImage = null;
        datePaint = null;
        dateTint = null;
        shadowImage = null;
        shadowPaint = null;
        screenDialPath = null;
    }
}
