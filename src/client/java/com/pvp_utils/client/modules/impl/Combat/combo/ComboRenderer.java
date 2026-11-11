package com.pvp_utils.client.modules.impl.Combat.combo;

import com.pvp_utils.PVPUtils;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.pvp_utils.client.modules.impl.Combat.combo.ComboTimeline.*;

final class ComboRenderer implements AutoCloseable {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "pvp-utils-combo-images");
        thread.setDaemon(true);
        return thread;
    });
    private CompletableFuture<Map<Texture, PreparedSheet>> pending = CompletableFuture.supplyAsync(this::prepare, worker);
    private final Map<Texture, Sheet> sheets = new EnumMap<>(Texture.class);
    private final Point[] haloPositions = new Point[(HALO_SEGMENTS + 1) * 2];
    private final int[] haloColors = new int[haloPositions.length];
    private Paint paint;
    private Paint haloPaint;
    private ColorFilter particleTint;
    private ColorFilter flashTint;
    private volatile boolean closed;
    private Throwable failure;

    private record PreparedSheet(ComboTextureAtlas atlas, byte[] pixels) {
    }

    private record Sheet(ComboTextureAtlas atlas, Image image) {
    }

    Throwable failure() {
        return failure;
    }

    void prepareRenderResources() {
        if (!closed) publish();
    }

    void render(Canvas canvas, Frame frame, int guiWidth, int guiHeight) {
        if (closed || !publish() || !frame.visible() || frame.alpha() <= 0f) return;
        int save = canvas.save();
        try {
            canvas.translate(centerX(guiWidth), centerY(guiHeight) + frame.entryOffsetY());
            canvas.scale(SCALE, SCALE);
            baseParticles(canvas, frame);
            halo(canvas, frame);
            bars(canvas, frame);
            draw(canvas, Texture.FRAME, frame.elapsedMillis(), FRAME_WIDTH, FRAME_HEIGHT,
                    0.5f, 0.5f, frame.alpha(), null, false, false);
            draw(canvas, Texture.EMBLEM, frame.elapsedMillis(), EMBLEM_SIZE, EMBLEM_SIZE,
                    0.5f, 0.5f, frame.alpha(), null, false, false);
            if (frame.flashAlpha() > 0f) {
                draw(canvas, Texture.EMBLEM, frame.elapsedMillis(), EMBLEM_SIZE, EMBLEM_SIZE,
                        0.5f, 0.5f, frame.alpha() * frame.flashAlpha(), flashTint, false, false);
            }
            overlay(canvas, Texture.HERO, frame, heroWidth(), heroHeight(), 0.4934375f, 0.49079242f, -30f, false);
            if (frame.largeSparks()) {
                overlay(canvas, Texture.SPARKS, frame, sparksSize(), sparksSize(), 0.49052733f, 0.5185547f, 2f, true);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private Map<Texture, PreparedSheet> prepare() {
        Map<Texture, PreparedSheet> result = new EnumMap<>(Texture.class);
        try {
            for (Texture texture : Texture.values()) {
                if (closed || Thread.currentThread().isInterrupted()) throw new IOException("Combo image loading cancelled");
                String path = "/assets/pvp_utils/combo/textures/" + texture.fileName;
                try (InputStream input = ComboRenderer.class.getResourceAsStream(path)) {
                    if (input == null) throw new IOException("Missing combo texture: " + path);
                    BufferedImage image = ImageIO.read(input);
                    if (image == null || image.getWidth() != texture.width || image.getHeight() != texture.height) {
                        throw new IOException("Unexpected combo texture dimensions: " + path);
                    }
                    ComboTextureAtlas atlas = ComboTextureAtlas.of(texture);
                    byte[] pixels = new byte[Math.multiplyExact(Math.multiplyExact(atlas.width(), atlas.height()), 4)];
                    int[] original = image.getRGB(0, 0, texture.width, texture.height, null, 0, texture.width);
                    for (int frame = 0; frame < texture.frames; frame++) {
                        if (closed || Thread.currentThread().isInterrupted()) throw new IOException("Combo image loading cancelled");
                        for (int y = -1; y <= texture.frameHeight(); y++) {
                            for (int x = -1; x <= texture.width; x++) {
                                int argb = original[atlas.sourceY(frame, y) * texture.width + atlas.sourceX(x)];
                                int offset = ((atlas.y(frame) + y) * atlas.width() + atlas.x(frame) + x) * 4;
                                pixels[offset] = (byte) (argb >> 16);
                                pixels[offset + 1] = (byte) (argb >> 8);
                                pixels[offset + 2] = (byte) argb;
                                pixels[offset + 3] = (byte) (argb >> 24);
                            }
                        }
                    }
                    image.flush();
                    result.put(texture, new PreparedSheet(atlas, pixels));
                }
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new CompletionException(exception);
        } finally {
            worker.shutdown();
        }
    }

    private boolean publish() {
        if (failure != null) return false;
        if (!sheets.isEmpty()) return true;
        if (pending == null || !pending.isDone()) return false;
        Map<Texture, Sheet> next = new EnumMap<>(Texture.class);
        try {
            for (Map.Entry<Texture, PreparedSheet> entry : pending.join().entrySet()) {
                PreparedSheet data = entry.getValue();
                ComboTextureAtlas atlas = data.atlas();
                Image image = Image.makeRaster(new ImageInfo(atlas.width(), atlas.height(),
                        ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), data.pixels(), atlas.width() * 4L);
                next.put(entry.getKey(), new Sheet(atlas, image));
            }
            paint = new Paint().setAntiAlias(true);
            haloPaint = new Paint().setAntiAlias(true);
            particleTint = ColorFilter.makeBlend(0xFF000000 | PARTICLE_COLOR, BlendMode.MODULATE);
            flashTint = ColorFilter.makeBlend(0xFF000000 | FLASH_COLOR, BlendMode.MODULATE);
            sheets.putAll(next);
            return true;
        } catch (RuntimeException | LinkageError exception) {
            next.values().forEach(sheet -> sheet.image().close());
            failure = exception;
            releasePaints();
            PVPUtils.LOGGER.warn("Unable to prepare combo effect textures", exception);
            return false;
        } finally {
            pending = null;
        }
    }

    private void baseParticles(Canvas canvas, Frame frame) {
        int save = canvas.save();
        try {
            canvas.translate(0f, 45f);
            draw(canvas, Texture.BASE, frame.elapsedMillis(), BASE_PARTICLE_SIZE, BASE_PARTICLE_SIZE,
                    0f, BASE_PARTICLE_ANCHOR_Y, frame.alpha(), particleTint, false, false);
            draw(canvas, Texture.BASE, frame.elapsedMillis(), BASE_PARTICLE_SIZE, BASE_PARTICLE_SIZE,
                    1f, BASE_PARTICLE_ANCHOR_Y, frame.alpha(), particleTint, false, true);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void bars(Canvas canvas, Frame frame) {
        for (int index = 0; index < frame.graphicTier(); index++) {
            int save = canvas.save();
            try {
                canvas.rotate(layoutAngle(frame.graphicTier(), index) + frame.barRotation());
                canvas.translate(0f, -(BAR_RADIUS + frame.barTravel()));
                canvas.scale(frame.barScale(), frame.barScale());
                draw(canvas, Texture.BAR, frame.elapsedMillis(), BAR_SIZE, BAR_SIZE,
                        0.5f, 0.5f, frame.alpha(), null, false, false);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private void overlay(Canvas canvas, Texture texture, Frame frame, float width, float height,
                         float anchorX, float anchorY, float offsetY, boolean additive) {
        int save = canvas.save();
        try {
            canvas.translate(0f, offsetY);
            draw(canvas, texture, frame.elapsedMillis(), width, height, anchorX, anchorY,
                    frame.alpha(), particleTint, additive, false);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void draw(Canvas canvas, Texture texture, long elapsed, float width, float height,
                      float anchorX, float anchorY, float alpha, ColorFilter tint, boolean additive, boolean flipX) {
        Sheet sheet = sheets.get(texture);
        int frame = texture.frameIndex(elapsed);
        ComboTextureAtlas atlas = sheet.atlas();
        Rect source = Rect.makeXYWH(atlas.x(frame), atlas.y(frame), texture.width, texture.frameHeight());
        float x = -width * anchorX;
        Rect destination = Rect.makeXYWH(x, -height * anchorY, width, height);
        paint.setColor(0xFFFFFFFF).setAlpha(Math.clamp(Math.round(alpha * 255f), 0, 255))
                .setBlendMode(additive ? BlendMode.PLUS : BlendMode.SRC_OVER).setColorFilter(tint);
        int save = canvas.save();
        try {
            if (flipX) {
                canvas.translate(2f * x + width, 0f);
                canvas.scale(-1f, 1f);
            }
            canvas.drawImageRect(sheet.image(), source, destination, SamplingMode.LINEAR, paint, false);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void halo(Canvas canvas, Frame frame) {
        float outer = HALO_RADIUS + HALO_WIDTH * 0.5f;
        float inner = HALO_RADIUS - HALO_WIDTH * 0.5f;
        float offset = haloAngle(frame.elapsedMillis());
        for (int index = 0; index <= HALO_SEGMENTS; index++) {
            float angle = (float) (Math.PI * 2.0) * index / HALO_SEGMENTS + offset;
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            float outerY = sine * outer;
            float innerY = sine * inner;
            int outerAlpha = Math.clamp((int) (frame.alpha() * Math.clamp((outer - outerY) / (2f * outer), 0f, 1f) * 255f), 0, 255);
            int innerAlpha = Math.clamp((int) (frame.alpha() * Math.clamp((outer - innerY) / (2f * outer), 0f, 1f) * 255f), 0, 255);
            int vertex = index * 2;
            haloPositions[vertex] = new Point(cosine * outer, outerY);
            haloPositions[vertex + 1] = new Point(cosine * inner, innerY);
            haloColors[vertex] = outerAlpha << 24 | 0xFFFFFF;
            haloColors[vertex + 1] = innerAlpha << 24 | 0xFFFFFF;
        }
        haloPaint.setColor(0xFFFFFFFF).setAlpha(255).setBlendMode(BlendMode.SRC_OVER);
        canvas.drawTriangleStrip(haloPositions, haloColors, haloPaint);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (pending != null) pending.cancel(true);
        worker.shutdownNow();
        Minecraft client = Minecraft.getInstance();
        Runnable release = () -> {
            sheets.values().forEach(sheet -> sheet.image().close());
            sheets.clear();
            releasePaints();
            pending = null;
        };
        if (client.isSameThread()) release.run(); else client.execute(release);
    }

    private void releasePaints() {
        if (paint != null) paint.close();
        if (haloPaint != null) haloPaint.close();
        if (particleTint != null) particleTint.close();
        if (flashTint != null) flashTint.close();
        paint = null;
        haloPaint = null;
        particleTint = null;
        flashTint = null;
    }
}
