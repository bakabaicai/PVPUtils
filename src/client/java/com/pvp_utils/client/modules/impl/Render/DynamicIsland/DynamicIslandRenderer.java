package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.pvp_utils.Config;
import com.pvp_utils.client.AntiCheat;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicCovers;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.ItemUseStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.LowHealthHandler;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.render.chroma.effects.ChromaStatusIcon;
import com.pvp_utils.client.render.chroma.effects.ChromaTransition;
import com.pvp_utils.client.render.chroma.effects.ChromaSeparation;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.effects.ChromaShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DynamicIslandRenderer implements AutoCloseable {
    private static final DynamicIslandRenderer INSTANCE = new DynamicIslandRenderer();
    private static final float HEIGHT = 40f;
    private static final float MUSIC_HEIGHT = 32f;
    private static final float MUSIC_MIN_WIDTH = MUSIC_HEIGHT * 3f;
    private static final float MIN_WIDTH = 246f;
    private static final float TOP = 10f;
    private static final float TEXT_SIZE = 11.5f;
    private static final float SIZE_EASE_SPEED = 13.5f;
    private static final float SATELLITE_RADIUS = 20f;
    private static final float SATELLITE_GAP = 7f;
    private static final float IDLE_PADDING = 14f;
    private static final float IDLE_ICON_SIZE = 12f;
    private static final float IDLE_ICON_GAP = 7f;
    private static final float FPS_SIZE = 11.5f;
    private static final float FPS_UNIT_SIZE = 8f;
    private static final long TAB_REQUEST_TTL_MS = 120L;
    private static final float TAB_TOP_PADDING = 16f;
    private static final float TAB_BOTTOM_PADDING = 16f;
    private static final float TAB_SIDE_PADDING = 18f;
    private static final float TAB_ROW_HEIGHT = 14f;
    private static final float TAB_COLUMN_GAP = 16f;
    private static final float TAB_NAME_SIZE = 11.5f;
    private static final int TAB_MAX_ROWS = 20;
    private static final int TAB_SELF_NAME_COLOR = 0xFFFF7777;
    private static final int TAB_DEFAULT_NAME_COLOR = 0xFFFFFFFF;
    private static final int TAB_SPECTATOR_NAME_COLOR = 0xFFAAAAAA;
    private static final String ICON_MUSIC = "\uE405";
    private static final String ICON_BLOCK = "\uF720";
    private static final String ICON_BLOCK_ALT = "\uE934";
    private static final String ICON_ITEM_USE = "\uE425";
    private static final String ICON_SWORDS = "\uF889";
    private static final String ICON_PERSON = "\uE7FD";
    private static final String ICON_GLOBE = "\uE894";
    private final IslandTextCache textCache = new IslandTextCache();
    private final float[] musicLevels = new float[7];
    private DisplayFrame displayed;
    private Phase phase = Phase.STEADY;
    private float visibility = 1f;
    private float animatedWidth = -1f;
    private float animatedHeight = HEIGHT;
    private float separation;
    private float satelliteVisibility;
    private float leftSeparation;
    private float leftSatelliteVisibility;
    private float renderedWidth = MIN_WIDTH;
    private long lastFrameNs;
    private long lastTabRequestTime;
    private long contentShownMs;
    private long resizeStartedMs;
    private Song satelliteSong;
    private Song leftSatelliteSong;
    private String satelliteFps = "0";
    private boolean satelliteMusic;
    private String lyricText = "";
    private String previousLyric = "";
    private long lyricSongId = -1L;
    private long lyricTime = -1L;
    private float lyricProgress = 1f;
    private ChromaSeparation.Geometry geometry;
    private GeometryKey geometryKey;

    public static DynamicIslandRenderer getInstance() { return INSTANCE; }
    public float getEditWidth() { return renderedWidth * getScale(); }
    public float getEditHeight() { return animatedHeight * getScale(); }
    public float getDefaultY() { return TOP; }

    public float getRenderX(int screenW) {
        return clamp((screenW - getEditWidth()) * 0.5f + Config.dynamicIslandX, 0f, Math.max(0f, screenW - getEditWidth()));
    }

    public float getRenderY(int screenH) {
        return clamp(TOP + Config.dynamicIslandY, 0f, Math.max(0f, screenH - getEditHeight()));
    }

    public void requestTabListFrame() { lastTabRequestTime = System.currentTimeMillis(); }

    public void renderChroma(Canvas canvas) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.dynamicIsland || client.player == null || client.level == null || client.options.hideGui) {
            close();
            return;
        }
        if (client.screen != null && !(client.screen instanceof ChatScreen)
                && !HudEditOverlay.getInstance().isActive()) {
            lastFrameNs = 0L;
            lastTabRequestTime = 0L;
            return;
        }
        long nowNs = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        float dt = lastFrameNs == 0L ? 1f / 60f : clamp((nowNs - lastFrameNs) / 1_000_000_000f, 0f, 0.05f);
        lastFrameNs = nowNs;
        DynamicIslandLyrics.LyricsCard lyrics = Config.dynamicIslandLyrics ? DynamicIslandLyrics.snapshot() : DynamicIslandLyrics.HIDDEN;
        DisplayFrame desired = snapshot(client, lyrics);
        boolean musicActive = lyrics.visible() && desired.song != null;
        boolean musicOnLeft = desired.content.split && musicActive;
        boolean detached = desired.content.split || musicActive && (desired.kind != Kind.MUSIC || Config.dynamicIslandMusicMode == Config.DynamicIslandMusicMode.MINIMIZED);
        separation = snap(easeTo(separation, detached ? 1f : 0f, SIZE_EASE_SPEED, dt));
        satelliteVisibility = snap(easeTo(satelliteVisibility, detached ? 1f : 0f, 18f, dt));
        leftSeparation = snap(easeTo(leftSeparation, musicOnLeft ? 1f : 0f, SIZE_EASE_SPEED, dt));
        leftSatelliteVisibility = snap(easeTo(leftSatelliteVisibility, musicOnLeft ? 1f : 0f, 18f, dt));
        if (musicOnLeft) leftSatelliteSong = desired.song;
        if (detached) {
            satelliteMusic = musicActive && !musicOnLeft;
            satelliteSong = desired.song;
            satelliteFps = desired.content.fps;
        }
        updatePresentation(desired, dt, nowMs);
        updateLyrics(lyrics, dt);
        updateMusicLevels(dt, nowMs);
        float scale = getScale();
        float radius = Math.min(animatedHeight * 0.5f, 20f);
        float leftExtension = satelliteExtension(animatedHeight, radius, leftSeparation);
        float extension = leftExtension + satelliteExtension(animatedHeight, radius, separation);
        animatedWidth = Math.min(animatedWidth, availableWidth(client, extension));
        renderedWidth = animatedWidth + extension;
        float x = getRenderX(client.getWindow().getGuiScaledWidth()) + leftExtension * scale;
        float y = getRenderY(client.getWindow().getGuiScaledHeight());
        GeometryKey next = new GeometryKey(x, y, animatedWidth * scale, animatedHeight * scale, radius * scale, leftSeparation, separation, scale);
        if (!next.equals(geometryKey)) {
            if (geometry != null) geometry.close();
            geometry = ChromaSeparation.create(x, y, next.width, next.height, next.radius,
                    SATELLITE_RADIUS * scale, SATELLITE_GAP * scale, leftSeparation, separation, true);
            geometryKey = next;
        }
        Rect bounds = geometry.bounds();
        if (Config.dynamicIslandShadow) ChromaShadow.drawPath(canvas, geometry.path(), Config.dynamicIslandShadowStrength, 0xA0000000);
        if (Config.dynamicIslandBackground == Config.DynamicIslandBackground.BLUR) {
            ChromaGraphics.drawBlurredBackdrop(canvas, geometry.path(), bounds.getLeft(), bounds.getTop(),
                    bounds.getWidth(), bounds.getHeight(), Math.clamp(Config.skiaBlurStrength, 0f, 5f) * 10.5f);
        }
        ChromaDraw.fillPath(canvas, geometry.path(), Config.dynamicIslandColor);
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            int clip = canvas.save();
            try {
                canvas.clipRRect(RRect.makeXYWH(0f, 0f, animatedWidth, animatedHeight, radius), true);
                ChromaTransition.draw(canvas, Rect.makeXYWH(0f, 0f, animatedWidth, animatedHeight), visibility, 0f,
                        () -> drawContent(canvas, displayed, new IslandLayout(animatedWidth, animatedHeight, radius, displayed.kind == Kind.TAB), nowMs));
            } finally { canvas.restoreToCount(clip); }
            if (leftSatelliteVisibility > 0.001f && leftSeparation > 0.08f && leftSatelliteSong != null) {
                float sx = (geometry.leftSatelliteCenterX() - x) / scale;
                float sy = (geometry.leftSatelliteCenterY() - y) / scale;
                float r = geometry.leftSatelliteRadius() / scale;
                float outside = clamp((r - sx) / Math.max(1f, r * 2f), 0f, 1f);
                drawMusicLogo(canvas, leftSatelliteSong, sx - 10f, sy - 10f, 20f,
                        leftSatelliteVisibility * outside, true);
            }
            if (satelliteVisibility > 0.001f && separation > 0.08f) {
                float sx = (geometry.satelliteCenterX() - x) / scale;
                float sy = (geometry.satelliteCenterY() - y) / scale;
                float r = geometry.satelliteRadius() / scale;
                float outside = clamp((sx - animatedWidth + r) / Math.max(1f, r * 2f), 0f, 1f);
                float alpha = satelliteVisibility * outside;
                if (satelliteMusic) {
                    drawMusicLogo(canvas, satelliteSong, sx - 10f, sy - 10f, 20f, alpha, true);
                } else {
                    float numberWidth = r * 2f - 6f;
                    float valueWidth = textCache.width(satelliteFps, 10f);
                    float numberScale = Math.min(1f, Math.max(0f, numberWidth) / Math.max(0.01f, valueWidth));
                    int numberSave = canvas.save();
                    try {
                        canvas.translate(sx, sy - 4f);
                        canvas.scale(numberScale, numberScale);
                        textCache.draw(canvas, satelliteFps, -valueWidth * 0.5f, 0f, 10f,
                                withAlpha(primaryColor(), Math.round(alpha * 255f)), valueWidth, 0f);
                    } finally {
                        canvas.restoreToCount(numberSave);
                    }
                    textCache.draw(canvas, "FPS", sx - r + 3f, sy + 8f, 6.5f,
                            withAlpha(secondaryColor(), Math.round(alpha * 170f)), r * 2f - 6f, 0.5f);
                }
            }
        } finally { canvas.restoreToCount(save); }
    }

    private DisplayFrame snapshot(Minecraft client, DynamicIslandLyrics.LyricsCard lyrics) {
        IslandContent content = buildContent(client);
        boolean tab = client.options.keyPlayerList.isDown() || System.currentTimeMillis() - lastTabRequestTime <= TAB_REQUEST_TTL_MS;
        List<PlayerInfo> players = tab ? getTabPlayers(client) : List.of();
        DynamicIslandNotificationCard notification = DynamicIslandNotifications.snapshot();
        LowHealthHandler.Snapshot alert = LowHealthHandler.snapshot();
        ItemUseStatusRenderer.Snapshot item = Config.dynamicIslandItemUseStatus ? ItemUseStatusRenderer.getInstance().snapshot(client) : ItemUseStatusRenderer.Snapshot.EMPTY;
        BlockCountDisplayRenderer.Snapshot block = Config.dynamicIslandBlockCount ? BlockCountDisplayRenderer.getInstance().snapshot(client) : BlockCountDisplayRenderer.Snapshot.EMPTY;
        Kind kind = tab ? Kind.TAB : notification.visible() ? Kind.NOTIFICATION : alert.visible() ? Kind.ALERT : item.visible() ? Kind.ITEM : block.visible() ? Kind.BLOCK
                : lyrics.visible() && Config.dynamicIslandMusicMode == Config.DynamicIslandMusicMode.LYRICS ? Kind.MUSIC : Kind.IDLE;
        String key = switch (kind) {
            case NOTIFICATION -> kind + ":" + notification.createdAtMs();
            case ALERT -> kind + ":" + alert.createdAtMs();
            case IDLE -> kind + ":" + content.doubleRow;
            default -> kind.name();
        };
        return new DisplayFrame(kind, key + ":" + content.split, content, players, block, item, alert, notification, lyrics, MusicPlaybackService.INSTANCE.currentSong());
    }

    private void updatePresentation(DisplayFrame desired, float dt, long nowMs) {
        if (displayed == null) {
            displayed = desired;
            IslandLayout first = measureLayout(desired);
            animatedWidth = first.width;
            animatedHeight = first.height;
            visibility = 1f;
            phase = Phase.STEADY;
            contentShownMs = nowMs;
            return;
        }
        boolean changed = !displayed.key.equals(desired.key);
        if (changed && phase != Phase.EXIT) phase = Phase.EXIT;
        if (!changed) {
            displayed = desired;
            if (phase == Phase.EXIT) phase = Phase.ENTER;
        }
        if (phase == Phase.EXIT) {
            visibility = easeTo(visibility, 0f, 24f, dt);
            if (visibility < 0.025f) {
                visibility = 0f;
                displayed = desired;
                phase = Phase.RESIZE;
                resizeStartedMs = nowMs;
            }
        }
        IslandLayout target = measureLayout(displayed);
        animatedWidth = easeTo(animatedWidth, target.width, SIZE_EASE_SPEED, dt);
        animatedHeight = easeTo(animatedHeight, target.height, SIZE_EASE_SPEED, dt);
        boolean settled = Math.abs(animatedWidth - target.width) < 0.65f && Math.abs(animatedHeight - target.height) < 0.35f;
        if (settled) {
            animatedWidth = target.width;
            animatedHeight = target.height;
            if (phase == Phase.RESIZE) { phase = Phase.ENTER; contentShownMs = nowMs; }
        }
        if (phase == Phase.RESIZE && nowMs - resizeStartedMs >= 420L) {
            phase = Phase.ENTER;
            contentShownMs = nowMs;
        }
        if (phase == Phase.ENTER) {
            visibility = easeTo(visibility, 1f, 18f, dt);
            if (visibility > 0.99f) { visibility = 1f; phase = Phase.STEADY; }
        }
    }

    private IslandLayout measureLayout(DisplayFrame frame) {
        Minecraft client = Minecraft.getInstance();
        float width;
        float height;
        switch (frame.kind) {
            case MUSIC -> { width = Math.min(390f, Math.max(MUSIC_MIN_WIDTH, textCache.width(frame.lyrics.text(), TEXT_SIZE) + (frame.content.split ? 67f : 92f))); height = MUSIC_HEIGHT; }
            case IDLE -> {
                float brand = iconTextWidth(frame.content.brand, TEXT_SIZE);
                float server = textCache.width(frame.content.location, 10f);
                float fps = fpsSlotWidth(frame.content.fps);
                if (frame.content.doubleRow) {
                    float user = iconTextWidth(frame.content.username, 10f);
                    float bottom = user + IDLE_ICON_SIZE + IDLE_ICON_GAP + server + 24f;
                    width = Math.max(264f, IDLE_PADDING * 2f + Math.max(brand + fps + 24f, bottom));
                } else {
                    width = Math.max(frame.content.split ? 210f : MIN_WIDTH,
                            IDLE_PADDING * 2f + brand + 24f + server + (frame.content.split ? 0f : fps + 16f));
                }
                height = frame.content.doubleRow ? 64f : HEIGHT;
            }
            case TAB -> {
                int count = Math.max(1, frame.players.size());
                int columns = Math.max(1, (count + TAB_MAX_ROWS - 1) / TAB_MAX_ROWS);
                int rows = Math.max(1, (count + columns - 1) / columns);
                width = Math.max(320f, TAB_SIDE_PADDING * 2f + columns * 170f + (columns - 1) * TAB_COLUMN_GAP);
                height = TAB_TOP_PADDING + TAB_BOTTOM_PADDING + rows * TAB_ROW_HEIGHT;
            }
            default -> {
                width = Math.min(410f, Math.max(230f, 65f + Math.max(textCache.width(title(frame), 12f), textCache.width(detail(frame), 10f))));
                height = frame.kind == Kind.ITEM || frame.kind == Kind.BLOCK ? 59f : 54f;
            }
        }
        float radius = Math.min(height * 0.5f, 20f);
        float maxWidth = availableWidth(client, satelliteExtension(height, radius, leftSeparation)
                + satelliteExtension(height, radius, separation));
        float maxHeight = Math.max(HEIGHT, (client.getWindow().getGuiScaledHeight() - 20f) / getScale());
        return new IslandLayout(Math.max(1f, Math.min(width, maxWidth)), Math.min(height, maxHeight), Math.min(height * 0.5f, 20f), frame.kind == Kind.TAB);
    }

    private float availableWidth(Minecraft client, float extension) {
        return Math.max(1f, Math.max(1f, client.getWindow().getGuiScaledWidth() - 20f) / getScale() - extension);
    }

    private float satelliteExtension(float height, float radius, float progress) {
        float r = Math.min(SATELLITE_RADIUS, height * 0.5f);
        float center = -Math.max(radius, r) + (Math.max(radius, r) + SATELLITE_GAP + r) * progress;
        float growth = clamp(progress / 0.4f, 0f, 1f);
        float circle = r * (0.72f + 0.28f * growth * growth * (3f - 2f * growth));
        return progress <= 0f ? 0f : Math.max(0f, center + circle);
    }

    private IslandContent buildContent(Minecraft client) {
        String username = Config.nickHider && Config.nickHiderTab && Config.nickHiderNickname != null && !Config.nickHiderNickname.isBlank() ? Config.nickHiderNickname : client.getUser().getName();
        ServerData server = client.getCurrentServer();
        String location = server != null && server.ip != null && !server.ip.isBlank() ? server.ip : UiText.t("单人游戏", "Singleplayer");
        String brand = Config.clientName == null || Config.clientName.isBlank() ? "PVPUtils" : Config.clientName;
        boolean split = Config.dynamicIslandShape == Config.DynamicIslandShape.SPLIT;
        return new IslandContent(brand, username, location, String.valueOf(client.getFps()), Config.dynamicIslandDoubleRow && !split, split);
    }

    private void drawContent(Canvas canvas, DisplayFrame frame, IslandLayout layout, long nowMs) {
        switch (frame.kind) {
            case IDLE -> drawIdle(canvas, frame.content, layout);
            case MUSIC -> drawMusic(canvas, frame.song, layout, frame.content.split);
            case TAB -> drawTabContent(canvas, frame.players, layout, 1f);
            default -> drawStatus(canvas, frame, layout, nowMs);
        }
    }

    private void drawIdle(Canvas canvas, IslandContent content, IslandLayout layout) {
        float left = IDLE_PADDING;
        float right = Math.max(left, layout.width - IDLE_PADDING);
        float innerWidth = right - left;
        float centerY = content.doubleRow ? 20f : layout.height * 0.5f;
        float fpsWidth = content.split ? 0f : fpsSlotWidth(content.fps);
        int brandColor = Config.dynamicIslandAppearance.accent();
        if (!content.split) drawFps(canvas, content.fps, right, centerY, innerWidth);
        if (content.doubleRow) {
            drawIconLabel(canvas, ICON_SWORDS, content.brand, left, centerY, TEXT_SIZE,
                    primaryColor(), brandColor, Math.max(0f, innerWidth - fpsWidth - 24f));
            float usable = Math.max(0f, innerWidth - 24f);
            float serverWidth = iconTextWidth(content.location, 10f);
            float userWidth = Math.min(iconTextWidth(content.username, 10f),
                    Math.max(0f, usable - Math.min(serverWidth, usable * 0.55f)));
            float serverBudget = Math.min(serverWidth, Math.max(0f, usable - userWidth));
            drawIconLabel(canvas, ICON_PERSON, content.username, left, 45f, 10f,
                    secondaryColor(), secondaryColor(), userWidth);
            drawIconLabel(canvas, ICON_GLOBE, content.location, right - serverBudget, 45f, 10f,
                    secondaryColor(), secondaryColor(), serverBudget);
            return;
        }
        float serverRight = content.split ? right : right - fpsWidth - 16f;
        float pairWidth = Math.max(0f, serverRight - left - 24f);
        float serverWidth = textCache.width(content.location, 10f);
        float brandWidth = Math.min(iconTextWidth(content.brand, TEXT_SIZE), pairWidth);
        float serverBudget = Math.min(serverWidth, Math.max(0f, pairWidth - brandWidth));
        drawIconLabel(canvas, ICON_SWORDS, content.brand, left, centerY, TEXT_SIZE,
                primaryColor(), brandColor, brandWidth);
        textCache.draw(canvas, content.location, serverRight - serverBudget, centerY, 10f,
                secondaryColor(), serverBudget, 1f);
    }

    private float iconTextWidth(String text, float size) {
        return IDLE_ICON_SIZE + IDLE_ICON_GAP + textCache.width(text, size);
    }

    private void drawIconLabel(Canvas canvas, String icon, String text, float x, float centerY, float size,
                               int color, int iconColor, float width) {
        if (width < IDLE_ICON_SIZE + IDLE_ICON_GAP) return;
        float glyphWidth = ChromaDraw.iconWidth(icon, IDLE_ICON_SIZE, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        ChromaDraw.icon(canvas, icon, x + (IDLE_ICON_SIZE - glyphWidth) * 0.5f, centerY - IDLE_ICON_SIZE * 0.5f,
                IDLE_ICON_SIZE, iconColor, IDLE_ICON_SIZE, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        textCache.draw(canvas, text, x + IDLE_ICON_SIZE + IDLE_ICON_GAP, centerY, size,
                color, width - IDLE_ICON_SIZE - IDLE_ICON_GAP, 0f);
    }

    private float fpsSlotWidth(String fps) {
        return Math.max(textCache.width("88888", FPS_SIZE), textCache.width(fps, FPS_SIZE))
                + 4f + textCache.width("FPS", FPS_UNIT_SIZE);
    }

    private void drawFps(Canvas canvas, String fps, float right, float centerY, float availableWidth) {
        if (availableWidth <= 0f) return;
        float valueWidth = textCache.width(fps, FPS_SIZE);
        float unitWidth = textCache.width("FPS", FPS_UNIT_SIZE);
        float width = valueWidth + 4f + unitWidth;
        float scale = Math.min(1f, availableWidth / width);
        int save = canvas.save();
        try {
            canvas.translate(right - width * scale, centerY);
            canvas.scale(scale, scale);
            textCache.draw(canvas, fps, 0f, 0f, FPS_SIZE, primaryColor(), valueWidth, 0f);
            textCache.draw(canvas, "FPS", valueWidth + 4f, 1f, FPS_UNIT_SIZE, secondaryColor(), unitWidth, 0f);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void updateLyrics(DynamicIslandLyrics.LyricsCard lyrics, float dt) {
        if (lyrics.visible() && (lyrics.songId() != lyricSongId || lyrics.lineTimeMs() != lyricTime || !lyrics.text().equals(lyricText))) {
            previousLyric = lyricText;
            lyricText = lyrics.text();
            lyricSongId = lyrics.songId();
            lyricTime = lyrics.lineTimeMs();
            lyricProgress = previousLyric.isEmpty() || displayed.kind != Kind.MUSIC ? 1f : 0f;
        }
        lyricProgress = snap(easeTo(lyricProgress, 1f, SIZE_EASE_SPEED, dt));
        if (lyricProgress >= 1f) previousLyric = "";
    }

    private void updateMusicLevels(float dt, long nowMs) {
        var levels = MusicPlaybackService.INSTANCE.visualizerSnapshot();
        boolean fresh = MusicPlaybackService.INSTANCE.isPlaying() && nowMs - levels.timestampMs() < 350L;
        for (int i = 0; i < musicLevels.length; i++) {
            float target = fresh ? levels.level(i) : 0f;
            musicLevels[i] = easeTo(musicLevels[i], target, target > musicLevels[i] ? 26f : 12f, dt);
        }
    }

    private void drawMusic(Canvas canvas, Song song, IslandLayout layout, boolean split) {
        if (!split) drawMusicLogo(canvas, song, 9f, (layout.height - 20f) * 0.5f, 20f, 1f, false);
        float lyricX = split ? 14f : 39f;
        float lyricW = Math.max(0f, layout.width - lyricX - 43f);
        float centerY = layout.height * 0.5f;
        Rect lyricBounds = Rect.makeXYWH(lyricX, 4f, lyricW, layout.height - 8f);
        int save = canvas.save();
        try {
            canvas.clipRect(lyricBounds);
            if (!previousLyric.isEmpty() && lyricProgress < 1f) {
                ChromaTransition.draw(canvas, lyricBounds, 1f - lyricProgress, -19f * lyricProgress,
                        () -> textCache.drawScrolling(canvas, previousLyric, lyricX, centerY, TEXT_SIZE, primaryColor(), lyricW, 0L, 6000L));
            }
            ChromaTransition.draw(canvas, lyricBounds, lyricProgress, 19f * (1f - lyricProgress),
                    () -> textCache.drawScrolling(canvas, lyricText, lyricX, centerY, TEXT_SIZE, primaryColor(), lyricW,
                            Math.max(0L, MusicPlaybackService.INSTANCE.positionMs() - lyricTime),
                            DynamicIslandLyrics.snapshot().lineDurationMs(), DynamicIslandLyrics.snapshot().line()));
        } finally { canvas.restoreToCount(save); }
        float barsX = layout.width - 30f;
        int barColor = musicAccentColor(song);
        for (int i = 0; i < musicLevels.length; i++) {
            float envelope = 1f - Math.abs(i - 3f) / 4f;
            float height = 2.5f + envelope * 6.5f + musicLevels[i] * (5f + envelope * 4f);
            ChromaDraw.rounded(canvas, barsX + i * 3f, centerY - height * 0.5f, 1.8f, height, 0.9f, barColor);
        }
    }

    private int musicAccentColor(Song song) {
        int fallback = primaryColor();
        int color = NeteaseMusicCovers.accentColor(song == null ? null : song.image(), fallback);
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;
        float luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f;
        boolean lightBackground = fallback == 0xFF20242D;
        float target = lightBackground ? Math.min(luminance, 105f) : Math.max(luminance, 145f);
        if (Math.abs(target - luminance) < 0.01f) return color;
        float endpoint = lightBackground ? 0f : 255f;
        float amount = (target - luminance) / (endpoint - luminance);
        return 0xFF000000 | (Math.round(red + (endpoint - red) * amount) << 16)
                | (Math.round(green + (endpoint - green) * amount) << 8)
                | Math.round(blue + (endpoint - blue) * amount);
    }

    private void drawMusicLogo(Canvas canvas, Song song, float x, float y, float size, float alpha, boolean circle) {
        if (alpha <= 0f) return;
        Image cover = song == null ? null : NeteaseMusicCovers.skiaImage(song.image());
        if (cover == null) {
            float glyphWidth = ChromaDraw.iconWidth(ICON_MUSIC, size * 0.8f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
            ChromaDraw.icon(canvas, ICON_MUSIC, x + (size - glyphWidth) * 0.5f, y, size, withAlpha(primaryColor(), Math.round(alpha * 255f)), size * 0.8f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
            return;
        }
        int save = canvas.save();
        try (Paint paint = new Paint().setAntiAlias(true).setAlpha(Math.round(alpha * 255f))) {
            canvas.clipRRect(RRect.makeXYWH(x, y, size, size, circle ? size * 0.5f : 6f), true);
            float crop = Math.min(cover.getWidth(), cover.getHeight());
            Rect source = Rect.makeXYWH((cover.getWidth() - crop) * 0.5f, (cover.getHeight() - crop) * 0.5f, crop, crop);
            canvas.drawImageRect(cover, source, Rect.makeXYWH(x, y, size, size), SamplingMode.LINEAR, paint, true);
        } finally { canvas.restoreToCount(save); }
    }

    private void drawStatus(Canvas canvas, DisplayFrame frame, IslandLayout layout, long nowMs) {
        float iconSize = 21f;
        float iconX = 14f;
        float iconY = (layout.height - iconSize) * 0.5f - (frame.kind == Kind.ITEM || frame.kind == Kind.BLOCK ? 4f : 0f);
        String icon = switch (frame.kind) {
            case ITEM -> ICON_ITEM_USE;
            case BLOCK -> AntiCheat.isDetected() && Config.dynamicIslandBlockCountAltIcon ? ICON_BLOCK_ALT : ICON_BLOCK;
            case ALERT -> frame.alert.icon();
            default -> frame.notification.icon();
        };
        int accent = frame.kind == Kind.NOTIFICATION ? frame.notification.accentColor() : primaryColor();
        float iconProgress = clamp((nowMs - contentShownMs) / 520f, 0f, 1f);
        if (icon.equals("\uE5CA") || icon.equals("\uE5CD")) {
            ChromaStatusIcon.draw(canvas, icon.equals("\uE5CA"), iconX, iconY, iconSize, iconProgress, accent);
        } else {
            float width = ChromaDraw.iconWidth(icon, iconSize, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
            ChromaDraw.icon(canvas, icon, iconX + (iconSize - width) * 0.5f, iconY, iconSize, accent, iconSize, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        }
        float contentX = 47f;
        float contentWidth = Math.max(0f, layout.width - contentX - 15f);
        textCache.draw(canvas, title(frame), contentX, 19f, 12f, primaryColor(), contentWidth, 0f);
        textCache.draw(canvas, detail(frame), contentX, 35f, 10f, secondaryColor(), contentWidth, 0f);
        if (frame.kind == Kind.ITEM || frame.kind == Kind.BLOCK) {
            float progress = frame.kind == Kind.ITEM ? frame.item.progress() : frame.block.progress();
            float width = Math.max(0f, layout.width - 30f);
            ChromaDraw.rounded(canvas, 15f, layout.height - 10f, width, 2.5f, 1.25f, withAlpha(primaryColor(), 36));
            float filled = width * clamp(progress, 0f, 1f);
            if (filled > 0f) ChromaDraw.rounded(canvas, 15f, layout.height - 10f, filled, 2.5f, 1.25f, withAlpha(primaryColor(), 220));
        }
    }

    private String title(DisplayFrame frame) {
        return switch (frame.kind) {
            case ITEM -> frame.item.itemName();
            case BLOCK -> frame.block.itemName();
            case ALERT -> frame.alert.title();
            default -> frame.notification.title();
        };
    }

    private String detail(DisplayFrame frame) {
        return switch (frame.kind) {
            case ITEM -> UiText.t("使用中", "Using") + " · " + Math.round(clamp(frame.item.progress(), 0f, 1f) * 100f) + "%";
            case BLOCK -> frame.block.blocksLeft() + UiText.t(" 个剩余", " remaining") + " · " + String.format(Locale.ROOT, "%.1f/s", frame.block.blocksPerSecond());
            case ALERT -> frame.alert.message();
            default -> frame.notification.message();
        };
    }

    private int primaryColor() {
        if (Config.dynamicIslandAppearance.customText) return Config.dynamicIslandAppearance.text();
        int color = Config.dynamicIslandColor;
        double luminance = ((color >>> 16) & 255) * 0.2126 + ((color >>> 8) & 255) * 0.7152 + (color & 255) * 0.0722;
        return luminance > 155d ? 0xFF20242D : 0xFFF3F5FA;
    }

    private int secondaryColor() { return withAlpha(primaryColor(), 170); }
    private float easeTo(float current, float target, float speed, float dt) { return current + (target - current) * (1f - (float) Math.exp(-speed * dt)); }
    private float snap(float value) { return value < 0.001f ? 0f : value > 0.999f ? 1f : value; }

    private List<PlayerInfo> getTabPlayers(Minecraft client) {
        if (client.getConnection() == null) return List.of();
        List<PlayerInfo> players = new ArrayList<>(client.getConnection().getListedOnlinePlayers());
        players.sort(Comparator
        .comparingInt(PlayerInfo::getTabListOrder)
        .thenComparing(info -> getPlayerName(info).toLowerCase(Locale.ROOT)));
        return players;
    }

    private void drawTabContent(Canvas canvas, List<PlayerInfo> players, IslandLayout layout, float fade) {
        int alpha = Math.round(clamp(fade, 0f, 1f) * 255f);
        if (alpha <= 0 || players.isEmpty()) return;

        int count = players.size();
        int columns = Math.max(1, (count + TAB_MAX_ROWS - 1) / TAB_MAX_ROWS);
        int rows = Math.max(1, (count + columns - 1) / columns);
        float usableW = layout.width - TAB_SIDE_PADDING * 2f - (columns - 1) * TAB_COLUMN_GAP;
        float columnW = usableW / columns;
        float totalContentH = rows * TAB_ROW_HEIGHT;
        float startY = TAB_TOP_PADDING + Math.max(0f, layout.height - TAB_TOP_PADDING - TAB_BOTTOM_PADDING - totalContentH) * 0.5f + TAB_ROW_HEIGHT * 0.5f;

        for (int i = 0; i < count; i++) {
            int column = i / rows;
            int row = i % rows;
            float x = TAB_SIDE_PADDING + column * (columnW + TAB_COLUMN_GAP);
            float y = startY + row * TAB_ROW_HEIGHT;
            PlayerInfo player = players.get(i);
            if (y - TAB_ROW_HEIGHT * 0.5f >= layout.height - 4f) continue;
            int baseColor = player.getGameMode() == GameType.SPECTATOR ? secondaryColor() : primaryColor();
            Map.Entry<String, Integer> formattedName = resolveTabName(player, baseColor);
            int color = isLocalPlayer(player) ? TAB_SELF_NAME_COLOR : formattedName.getValue();
            textCache.draw(canvas, formattedName.getKey(), x, y, TAB_NAME_SIZE, withAlpha(color, alpha), columnW - 46f, 0f);

            String latency = formatLatency(player.getLatency());
            float latencyW = Math.min(42f, columnW);
            textCache.draw(canvas, latency, x + columnW - latencyW, y, 9f, withAlpha(latencyColor(player.getLatency()), alpha), latencyW, 1f);
        }
    }

    private Map.Entry<String, Integer> resolveTabName(PlayerInfo player, int fallbackColor) {
        Component displayName = player.getTabListDisplayName();
        int teamColor = teamColor(player, fallbackColor);
        if (displayName != null) {
            Map.Entry<String, Integer> componentName = parseComponentTabName(displayName, teamColor);
            if (!componentName.getKey().isBlank()) {
                return componentName;
            }
        }

        PlayerTeam team = player.getTeam();
        if (team != null) {
            Component formatted = team.getFormattedName(Component.literal(player.getProfile().name()));
            Map.Entry<String, Integer> teamName = parseComponentTabName(formatted, teamColor);
            if (!teamName.getKey().isBlank()) {
                return teamName;
            }
        }

        return parseLegacyTabName(player.getProfile().name(), teamColor);
    }

    private Map.Entry<String, Integer> parseComponentTabName(Component component, int fallbackColor) {
        if (component == null) {
            return Map.entry("", fallbackColor);
        }

        StringBuilder cleanName = new StringBuilder();
        int[] color = { fallbackColor };
        boolean[] sawExplicitColor = { false };
        component.visit((style, text) -> {
            Integer parsedColor = styleColor(style);
            if (parsedColor != null && !sawExplicitColor[0] && containsVisibleText(text)) {
                color[0] = parsedColor;
                sawExplicitColor[0] = true;
            }
            Map.Entry<String, Integer> parsedText = parseLegacyTabName(text, sawExplicitColor[0] ? color[0] : fallbackColor);
            if (!sawExplicitColor[0] && parsedText.getValue() != fallbackColor && containsVisibleText(parsedText.getKey())) {
                color[0] = parsedText.getValue();
                sawExplicitColor[0] = true;
            }
            cleanName.append(parsedText.getKey());
            return java.util.Optional.empty();
        }, component.getStyle() == null ? Style.EMPTY : component.getStyle());
        if (cleanName.isEmpty()) {
            return parseLegacyTabName(component.getString(), fallbackColor);
        }
        return Map.entry(cleanName.toString(), color[0]);
    }

    private Map.Entry<String, Integer> parseLegacyTabName(String rawName, int fallbackColor) {
        if (rawName == null || rawName.isEmpty()) {
            return Map.entry("", fallbackColor);
        }

        StringBuilder cleanName = new StringBuilder(rawName.length());
        int color = fallbackColor;
        for (int i = 0; i < rawName.length(); i++) {
            char current = rawName.charAt(i);
            if ((current == '\u00A7' || current == '&') && i + 1 < rawName.length()) {
                if (Character.toLowerCase(rawName.charAt(i + 1)) == 'x') {
                    Integer hexColor = parseHexColor(rawName, i);
                    if (hexColor != null) {
                        color = hexColor;
                        i += 13;
                        continue;
                    }
                }
                Integer parsedColor = minecraftColor(rawName.charAt(++i), fallbackColor);
                if (parsedColor != null) {
                    color = parsedColor;
                }
                continue;
            }
            cleanName.append(current);
        }
        return Map.entry(cleanName.toString(), color);
    }

    private Integer parseHexColor(String rawName, int sectionIndex) {
        if (sectionIndex + 13 >= rawName.length()) return null;
        char marker = rawName.charAt(sectionIndex);
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int prefix = sectionIndex + 2 + i * 2;
            int value = prefix + 1;
            if (rawName.charAt(prefix) != marker || !isHex(rawName.charAt(value))) {
                return null;
            }
            hex.append(rawName.charAt(value));
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex.toString(), 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isHex(char value) {
        return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }

    private boolean containsVisibleText(String text) {
        if (text == null || text.isEmpty()) return false;
        return !parseLegacyTabName(text, TAB_DEFAULT_NAME_COLOR).getKey().isBlank();
    }

    private Integer styleColor(Style style) {
        if (style == null || style.getColor() == null) {
            return null;
        }
        return 0xFF000000 | (style.getColor().getValue() & 0x00FFFFFF);
    }

    private int teamColor(PlayerInfo player, int fallbackColor) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            return fallbackColor;
        }
        Integer prefixColor = firstComponentColor(team.getPlayerPrefix());
        if (prefixColor != null) {
            return prefixColor;
        }
        ChatFormatting formatting = team.getColor();
        if (formatting != null && formatting.isColor() && formatting.getColor() != null) {
            return 0xFF000000 | (formatting.getColor() & 0x00FFFFFF);
        }
        return fallbackColor;
    }

    private Integer firstComponentColor(Component component) {
        if (component == null) {
            return null;
        }
        int[] color = { 0 };
        boolean[] found = { false };
        component.visit((style, text) -> {
            if (!found[0] && containsVisibleText(text)) {
                Integer styleColor = styleColor(style);
                if (styleColor != null) {
                    color[0] = styleColor;
                    found[0] = true;
                } else {
                    Map.Entry<String, Integer> parsed = parseLegacyTabName(text, TAB_DEFAULT_NAME_COLOR);
                    if (parsed.getValue() != TAB_DEFAULT_NAME_COLOR) {
                        color[0] = parsed.getValue();
                        found[0] = true;
                    }
                }
            }
            return java.util.Optional.empty();
        }, component.getStyle() == null ? Style.EMPTY : component.getStyle());
        return found[0] ? color[0] : null;
    }

    private Integer minecraftColor(char code, int fallbackColor) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            case 'r' -> fallbackColor;
            default -> null;
        };
    }

    private boolean isLocalPlayer(PlayerInfo player) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && player.getProfile().id().equals(client.player.getUUID());
    }

    private String getPlayerName(PlayerInfo player) {
        if (player.getTabListDisplayName() != null) {
            return player.getTabListDisplayName().getString();
        }
        return player.getProfile().name();
    }

    private String formatLatency(int latency) { return latency < 0 ? "?" : latency + "ms"; }

    private int latencyColor(int latency) {
        if (latency < 0) return secondaryColor();
        if (latency < 150) return 0xFF53D86A;
        if (latency < 300) return 0xFFFFD166;
        return 0xFFFF6B6B;
    }

    private int withAlpha(int argb, int alpha) { return (argb & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24); }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private float getScale() {
        float requested = Float.isFinite(Config.dynamicIslandScale) ? Math.clamp(Config.dynamicIslandScale, 0.5f, 3f) : 1f;
        if (displayed == null || displayed.kind != Kind.MUSIC) return requested;
        float extension = satelliteExtension(MUSIC_HEIGHT, MUSIC_HEIGHT * 0.5f, leftSeparation)
                + satelliteExtension(MUSIC_HEIGHT, MUSIC_HEIGHT * 0.5f, separation);
        float available = Math.max(1f, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 20f);
        return Math.min(requested, available / (MUSIC_MIN_WIDTH + extension));
    }

    @Override
    public void close() {
        textCache.close();
        if (geometry != null) geometry.close();
        geometry = null;
        geometryKey = null;
        displayed = null;
        phase = Phase.STEADY;
        visibility = 1f;
        animatedWidth = -1f;
        animatedHeight = HEIGHT;
        separation = 0f;
        satelliteVisibility = 0f;
        leftSeparation = 0f;
        leftSatelliteVisibility = 0f;
        renderedWidth = MIN_WIDTH;
        lastFrameNs = 0L;
        satelliteSong = null;
        leftSatelliteSong = null;
        satelliteFps = "0";
        satelliteMusic = false;
        lyricText = "";
        previousLyric = "";
        lyricSongId = -1L;
        lyricTime = -1L;
        lyricProgress = 1f;
        java.util.Arrays.fill(musicLevels, 0f);
        DynamicIslandLyrics.reset();
    }

    private enum Kind { IDLE, MUSIC, TAB, BLOCK, ITEM, ALERT, NOTIFICATION }
    private enum Phase { STEADY, EXIT, RESIZE, ENTER }
    private record IslandContent(String brand, String username, String location, String fps, boolean doubleRow, boolean split) { }
    private record IslandLayout(float width, float height, float radius, boolean isTab) { }
    private record GeometryKey(float x, float y, float width, float height, float radius, float leftProgress, float progress, float scale) { }
    private record DisplayFrame(Kind kind, String key, IslandContent content, List<PlayerInfo> players,
                                BlockCountDisplayRenderer.Snapshot block, ItemUseStatusRenderer.Snapshot item,
                                LowHealthHandler.Snapshot alert, DynamicIslandNotificationCard notification,
                                DynamicIslandLyrics.LyricsCard lyrics, Song song) { }
}
