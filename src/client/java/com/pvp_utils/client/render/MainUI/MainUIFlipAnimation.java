package com.pvp_utils.client.render.MainUI;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Picture;
import io.github.humbleui.skija.PictureRecorder;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.function.Consumer;

public final class MainUIFlipAnimation {
    public static final long DURATION_MS = 500L;

    private MainUIFlipAnimation() {
    }

    public static float ease(float progress) {
        float remaining = 1f - Math.max(0f, Math.min(1f, progress));
        return 1f - remaining * remaining * remaining;
    }

    public static float chromeAlpha(float progress, boolean returning) {
        float delay = returning ? 0.46f : 0.38f;
        return ease((progress - delay) / (1f - delay));
    }

    public static Rect interpolate(Rect from, Rect to, float amount) {
        return Rect.makeXYWH(
                from.getLeft() + (to.getLeft() - from.getLeft()) * amount,
                from.getTop() + (to.getTop() - from.getTop()) * amount,
                from.getWidth() + (to.getWidth() - from.getWidth()) * amount,
                from.getHeight() + (to.getHeight() - from.getHeight()) * amount);
    }

    public static Matrix33 project(Rect source, Rect destination, float angle, float distance) {
        float sourceX = source.getLeft() + source.getWidth() * 0.5f;
        float sourceY = source.getTop() + source.getHeight() * 0.5f;
        float centerX = destination.getLeft() + destination.getWidth() * 0.5f;
        float centerY = destination.getTop() + destination.getHeight() * 0.5f;
        float scaleX = destination.getWidth() / Math.max(1f, source.getWidth());
        float scaleY = destination.getHeight() / Math.max(1f, source.getHeight());
        float cosine = (float) Math.cos(angle);
        if (Math.abs(cosine) < 0.0001f) {
            cosine = Math.copySign(0.0001f, cosine);
        }
        float perspective = (float) Math.sin(angle) * scaleX / Math.max(destination.getWidth(), distance);
        float denominator = 1f - perspective * sourceX;
        float horizontal = cosine * scaleX;
        return new Matrix33(
                horizontal + centerX * perspective, 0f, centerX * denominator - horizontal * sourceX,
                centerY * perspective, scaleY, centerY * denominator - scaleY * sourceY,
                perspective, 0f, denominator);
    }

    public static void drawPage(Canvas canvas, Matrix33 cardTransform, RRect cardBounds,
                                float chromeAlpha, Consumer<Canvas> painter) {
        Rect viewport = canvas.getLocalClipBounds();
        Rect recordingBounds = Rect.makeLTRB(
                Math.min(viewport.getLeft(), cardBounds.getLeft()) - 1f,
                Math.min(viewport.getTop(), cardBounds.getTop()) - 1f,
                Math.max(viewport.getRight(), cardBounds.getRight()) + 1f,
                Math.max(viewport.getBottom(), cardBounds.getBottom()) + 1f);
        try (PictureRecorder recorder = new PictureRecorder()) {
            painter.accept(recorder.beginRecording(recordingBounds));
            try (Picture picture = recorder.finishRecordingAsPicture()) {
                drawRecordedPage(canvas, picture, cardTransform, cardBounds, chromeAlpha);
            }
        }
    }

    private static void drawRecordedPage(Canvas canvas, Picture picture, Matrix33 cardTransform,
                                         RRect cardBounds, float chromeAlpha) {
        if (chromeAlpha > 0.001f) {
            try (Paint opacity = new Paint().setAlphaf(Math.min(1f, chromeAlpha))) {
                int save = canvas.saveLayer(null, opacity);
                try {
                    canvas.clipRect(Rect.makeLTRB(cardBounds.getLeft() - 1f, cardBounds.getTop() - 1f,
                            cardBounds.getRight() + 1f, cardBounds.getBottom() + 1f), ClipMode.DIFFERENCE, false);
                    canvas.drawPicture(picture);
                } finally {
                    canvas.restoreToCount(save);
                }
            }
        }
        int save = canvas.save();
        try {
            canvas.concat(cardTransform);
            canvas.clipRRect(cardBounds, true);
            canvas.drawPicture(picture);
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
