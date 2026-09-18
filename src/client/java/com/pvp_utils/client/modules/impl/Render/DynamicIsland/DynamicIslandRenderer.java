package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.modules.impl.Render.ItemUseStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.LowHealthHandler;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

public class DynamicIslandRenderer {
    private static final DynamicIslandRenderer INSTANCE = new DynamicIslandRenderer();
    private static final float HEIGHT = 30f;
    private static final float MIN_WIDTH = 250f;
    private static final float MAX_WIDTH_MARGIN = 24f;
    private static final float TOP = 10f;
    private static final float PADDING_X = 18f;
    private static final float TEXT_SIZE = 11.5f;
    private static final float ICON_SIZE = 13.5f;
    private static final float ICON_GAP = 4f;
    private static final float ICON_Y_OFFSET = 3.0f;
    private static final float SEPARATOR_GAP = 8f;
    private static final float BLOCK_MIN_WIDTH = 206f;
    private static final float BLOCK_HEIGHT = 72f;
    private static final float ALERT_HEIGHT = 58f;
    private static final float BLOCK_ICON_X = 12f;
    private static final float BLOCK_ICON_Y = 10f;
    private static final float BLOCK_ICON_BOX = 42f;
    private static final float BLOCK_TEXT_X = 68f;
    private static final float BLOCK_PROGRESS_X = 16f;
    private static final float BLOCK_PROGRESS_Y = 59f;
    private static final float BLOCK_PROGRESS_H = 9f;
    private static final float BLOCK_RIGHT_PADDING = 18f;
    private static final float TAB_MIN_WIDTH = 340f;
    private static final float TAB_MAX_WIDTH_MARGIN = 34f;
    private static final float TAB_TOP_PADDING = 16f;
    private static final float TAB_BOTTOM_PADDING = 16f;
    private static final float TAB_SIDE_PADDING = 22f;
    private static final float TAB_ROW_HEIGHT = 14f;
    private static final float TAB_COLUMN_GAP = 20f;
    private static final float TAB_NAME_SIZE = 11.5f;
    private static final int TAB_MAX_ROWS = 20;
    private static final String ICON_LINK = "\uE157";
    private static final String ICON_COMPUTER = "\uE30C";
    private static final String ICON_PERSON = "\uE7FD";
    private static final String ICON_BLOCK = "\uF720";
    private static final String ICON_BLOCK_ALT = "\uE934";
    private static final String ICON_ITEM_USE = "\uE425";
    private static final long NOTIFICATION_ICON_ANIMATION_MS = 520L;
    private static final int TAB_SELF_NAME_COLOR = 0xFFFF5555;
    private static final int TAB_DEFAULT_NAME_COLOR = 0xFFFFFFFF;
    private static final int TAB_SPECTATOR_NAME_COLOR = 0xFFAAAAAA;
    private static final float SIZE_EASE_SPEED = 13.5f;
    private static final float TAB_FADE_EASE_SPEED = 9.0f;
    private static final long TAB_REQUEST_TTL_MS = 120L;
    private static final long LYRICS_EXPAND_DURATION_MS = 700L;
    private static final float LYRICS_STAGGER_RATIO = 0.45f;

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private boolean nativeLoaded = false;
    private float animatedWidth = -1f;
    private float animatedHeight = -1f;
    private float tabContentFade = 0f;
    private float lyricsFade = 0f;
    private boolean lastLyricsOpen = false;
    private String lastLyricsText = "";
    private long lyricsExpandStartTime = 0L;
    private long lastLyricsDurationMs = 3000L;
    private long lastAnimationTime = 0L;
    private long lastTabRequestTime = 0L;

    public static DynamicIslandRenderer getInstance() {
        return INSTANCE;
    }

    public float getEditWidth() {
        return getBaseEditWidth() * getScale();
    }

    public float getEditHeight() {
        return getBaseEditHeight() * getScale();
    }

    public float getDefaultY() {
        return TOP;
    }

    public float getRenderX(int screenW) {
        float w = getEditWidth();
        return clamp((screenW - w) * 0.5f + Config.dynamicIslandX, 0f, Math.max(0f, screenW - w));
    }

    public float getRenderY(int screenH) {
        float h = getEditHeight();
        return clamp(getDefaultY() + Config.dynamicIslandY, 0f, Math.max(0f, screenH - h));
    }

    public void requestTabListFrame() {
        lastTabRequestTime = System.currentTimeMillis();
    }

    public void renderFrameEnd() {
        if (!Config.dynamicIsland) {
            resetAnimation();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || client.getWindow() == null) {
            resetAnimation();
            return;
        }
        if (client.screen instanceof AbstractContainerScreen<?>) {
            return;
        }

        IslandContent content = buildContent(client);
        boolean tabOpen = isTabOpen();
        List<PlayerInfo> tabPlayers = tabOpen ? getTabPlayers(client) : List.of();
        BlockCountDisplayRenderer.Snapshot blockSnapshot = Config.dynamicIslandBlockCount
                ? BlockCountDisplayRenderer.getInstance().snapshot(client)
                : BlockCountDisplayRenderer.Snapshot.EMPTY;
        ItemUseStatusRenderer.Snapshot itemUseSnapshot = Config.dynamicIslandItemUseStatus
                ? ItemUseStatusRenderer.getInstance().snapshot(client)
                : ItemUseStatusRenderer.Snapshot.EMPTY;
        DynamicIslandNotificationCard notificationCard = DynamicIslandNotifications.snapshot();
        LowHealthHandler.Snapshot alertSnapshot = LowHealthHandler.snapshot();
        boolean notificationOpen = !tabOpen && notificationCard.visible();
        boolean alertOpen = !tabOpen && !notificationOpen && alertSnapshot.visible();
        boolean itemUseOpen = !tabOpen && !notificationOpen && !alertOpen && itemUseSnapshot.visible();
        boolean blockOpen = !tabOpen && !notificationOpen && !alertOpen && !itemUseOpen && blockSnapshot.visible();
        DynamicIslandLyrics.LyricsCard lyricsCard = Config.dynamicIslandLyrics && !tabOpen && !notificationOpen && !alertOpen
                ? DynamicIslandLyrics.snapshot()
                : DynamicIslandLyrics.HIDDEN;
        boolean lyricsOpen = !blockOpen && lyricsCard.visible();
        lyricsFade += ((lyricsOpen ? 1f : 0f) - lyricsFade) * 0.15f;
        if (lyricsFade < 0.001f) lyricsFade = 0f;
        if (lyricsOpen && !lyricsCard.text().isEmpty()) {
            if (!lyricsCard.text().equals(lastLyricsText) || !lastLyricsOpen) {
                lyricsExpandStartTime = System.currentTimeMillis();
                lastLyricsDurationMs = lyricsCard.durationMs();
            }
            lastLyricsText = lyricsCard.text();
        } else if (lyricsOpen) {
            lastLyricsText = "";
        }
        lastLyricsOpen = lyricsOpen;
        boolean lyricsShowing = lyricsFade > 0.01f && !lastLyricsText.isEmpty();
        IslandLayout targetLayout = tabOpen ? measureTabLayout(client, tabPlayers) : notificationOpen ? measureNotificationLayout(client, notificationCard) : alertOpen ? measureAlertLayout(client, alertSnapshot) : itemUseOpen ? measureItemUseLayout(client, itemUseSnapshot) : blockOpen ? measureBlockLayout(client, blockSnapshot) : lyricsShowing ? measureLyricsLayout(client, lastLyricsText) : measureLayout(client, content);
        IslandLayout layout = updateAnimatedLayout(targetLayout);
        float islandScale = getScale();
        float x = getRenderX(client.getWindow().getGuiScaledWidth());
        float y = getRenderY(client.getWindow().getGuiScaledHeight());

        boolean blurred = SkiaBlurRenderer.getInstance().render(client, x, y, layout.width * islandScale, layout.height * islandScale, layout.radius * islandScale, blurTint(), blurStrength());
        renderContentDirect(client, content, tabPlayers, blockSnapshot, itemUseSnapshot, alertSnapshot, notificationCard, layout, tabOpen, blockOpen, itemUseOpen, alertOpen, notificationOpen, x, y, islandScale, blurred);
    }

    private boolean isTabOpen() {
        Minecraft client = Minecraft.getInstance();
        boolean keyDown = client.options != null && client.options.keyPlayerList.isDown();
        return keyDown || System.currentTimeMillis() - lastTabRequestTime <= TAB_REQUEST_TTL_MS;
    }

    private IslandLayout updateAnimatedLayout(IslandLayout target) {
        long now = System.currentTimeMillis();
        if (animatedWidth < 0f || animatedHeight < 0f || lastAnimationTime == 0L) {
            animatedWidth = target.width;
            animatedHeight = target.height;
            lastAnimationTime = now;
            return target;
        }

        float dt = Math.min((now - lastAnimationTime) / 1000f, 0.05f);
        lastAnimationTime = now;
        animatedWidth = easeTo(animatedWidth, target.width, SIZE_EASE_SPEED, dt);
        animatedHeight = easeTo(animatedHeight, target.height, SIZE_EASE_SPEED, dt);
        boolean reachedTarget = Math.abs(animatedWidth - target.width) < 1.25f && Math.abs(animatedHeight - target.height) < 1.25f;
        if (reachedTarget) {
            animatedWidth = target.width;
            animatedHeight = target.height;
        }
        tabContentFade = easeTo(tabContentFade, target.isTab && reachedTarget ? 1f : 0f, TAB_FADE_EASE_SPEED, dt);
        if (tabContentFade > 0.985f) {
            tabContentFade = 1f;
        } else if (tabContentFade < 0.015f) {
            tabContentFade = 0f;
        }
        return new IslandLayout(animatedWidth, animatedHeight, Math.min(animatedHeight * 0.5f, target.isTab ? 18f : 16f), target.isTab);
    }

    private float easeTo(float current, float target, float speed, float dt) {
        float alpha = 1f - (float) Math.exp(-speed * dt);
        return current + (target - current) * alpha;
    }

    private IslandContent buildContent(Minecraft client) {
        String username = getAccountDisplayName(client);
        String location = getLocationText(client);
        String fps = String.valueOf(client.getFps());
        String brand = Config.clientName == null || Config.clientName.isBlank() ? "PVPUtils" : Config.clientName;
        return new IslandContent(brand, username, location, fps);
    }

    private String getAccountDisplayName(Minecraft client) {
        if (Config.nickHider && Config.nickHiderTab && Config.nickHiderNickname != null && !Config.nickHiderNickname.isBlank()) {
            return Config.nickHiderNickname;
        }
        if (client.getUser() != null && client.getUser().getName() != null && !client.getUser().getName().isBlank()) {
            return client.getUser().getName();
        }
        return client.player != null ? client.player.getScoreboardName() : "Unknown";
    }

    private String getLocationText(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return "Singleplayer";
    }

    private IslandLayout measureLayout(Minecraft client, IslandContent content) {
        float textW = measure(content.brand)
                + measureIconSegment(content.username)
                + measureIconSegment(content.location)
                + measureIconSegment("FPS:" + content.fps)
                + measure("|") * 3f
                + SEPARATOR_GAP * 6f;
        float maxW = Math.max(120f, client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(MIN_WIDTH, textW + PADDING_X * 2f), MIN_WIDTH, maxW);
        return new IslandLayout(width, HEIGHT, Math.min(HEIGHT * 0.5f, 16f), false);
    }

    private IslandLayout measureBlockLayout(Minecraft client, BlockCountDisplayRenderer.Snapshot snapshot) {
        String title = snapshot.itemName();
        String detail = blockDetail(snapshot);
        float contentW = BLOCK_TEXT_X + Math.max(measureBlockText(title, 13.5f), measureBlockText(detail, 11f)) + BLOCK_RIGHT_PADDING;
        float maxW = Math.max(BLOCK_MIN_WIDTH, client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(BLOCK_MIN_WIDTH, contentW), BLOCK_MIN_WIDTH, maxW);
        return new IslandLayout(width, BLOCK_HEIGHT, 14f, false);
    }

    private IslandLayout measureItemUseLayout(Minecraft client, ItemUseStatusRenderer.Snapshot snapshot) {
        String title = itemUseTitle(snapshot);
        String detail = itemUseDetail(snapshot);
        float contentW = BLOCK_TEXT_X + Math.max(measureBlockText(title, 13.5f), measureBlockText(detail, 11f)) + BLOCK_RIGHT_PADDING;
        float maxW = Math.max(BLOCK_MIN_WIDTH, client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(BLOCK_MIN_WIDTH, contentW), BLOCK_MIN_WIDTH, maxW);
        return new IslandLayout(width, BLOCK_HEIGHT, 14f, false);
    }

    private IslandLayout measureAlertLayout(Minecraft client, LowHealthHandler.Snapshot snapshot) {
        float contentW = BLOCK_TEXT_X + Math.max(measureBlockText(snapshot.title(), 13.5f), measureBlockText(snapshot.message(), 11f)) + BLOCK_RIGHT_PADDING;
        float maxW = Math.max(BLOCK_MIN_WIDTH, client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(BLOCK_MIN_WIDTH, contentW), BLOCK_MIN_WIDTH, maxW);
        return new IslandLayout(width, ALERT_HEIGHT, 14f, false);
    }

    private IslandLayout measureNotificationLayout(Minecraft client, DynamicIslandNotificationCard card) {
        float contentW = BLOCK_TEXT_X + Math.max(measureBlockText(card.title(), 13.5f), measureBlockText(card.message(), 11f)) + BLOCK_RIGHT_PADDING;
        float maxW = Math.max(BLOCK_MIN_WIDTH, client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(BLOCK_MIN_WIDTH, contentW), BLOCK_MIN_WIDTH, maxW);
        return new IslandLayout(width, ALERT_HEIGHT, 14f, false);
    }

    private IslandLayout measureLyricsLayout(Minecraft client, String text) {
        Song song = MusicPlaybackService.INSTANCE.currentSong();
        if (song == null) return new IslandLayout(BLOCK_MIN_WIDTH, BLOCK_HEIGHT, 14f, false);

        float textStartX = BLOCK_ICON_X;
        float lyricsW = (text != null && !text.isEmpty()) ? measureBlockText(text, 13f) : 0f;
        float songInfoW = 0f;
        String songName = song.name();
        String artist = song.displayArtist();
        String combined = "";
        if (songName != null && !songName.isBlank()) {
            combined = songName;
        }
        if (artist != null && !artist.isBlank()) {
            combined = combined.isEmpty() ? artist : combined + " - " + artist;
        }
        if (!combined.isEmpty()) {
            songInfoW = measureBlockText(combined, 10f);
        }
        float contentW = textStartX + Math.max(lyricsW, songInfoW) + BLOCK_RIGHT_PADDING;
        float maxW = client.getWindow().getGuiScaledWidth() - MAX_WIDTH_MARGIN * 2f;
        float width = clamp(contentW, 100f, maxW);

        boolean hasSongInfo = (songName != null && !songName.isBlank()) || (artist != null && !artist.isBlank());
        float height = hasSongInfo ? 50f : 36f;
        float maxH = Math.max(HEIGHT, client.getWindow().getGuiScaledHeight() - TOP * 2f);
        return new IslandLayout(width, clamp(height, 50f, maxH), 14f, false);
    }

    private IslandLayout measureTabLayout(Minecraft client, List<PlayerInfo> players) {
        int count = Math.max(1, players.size());
        int columns = Math.max(1, (count + TAB_MAX_ROWS - 1) / TAB_MAX_ROWS);
        int rows = Math.max(1, (count + columns - 1) / columns);
        float nameW = 120f;
        for (PlayerInfo player : players) {
            nameW = Math.max(nameW, measure(resolveTabName(player, TAB_DEFAULT_NAME_COLOR).getKey()));
        }
        float columnW = clamp(nameW + 48f, 150f, 230f);
        float rawWidth = TAB_SIDE_PADDING * 2f + columns * columnW + (columns - 1) * TAB_COLUMN_GAP;
        float maxW = Math.max(TAB_MIN_WIDTH, client.getWindow().getGuiScaledWidth() - TAB_MAX_WIDTH_MARGIN * 2f);
        float width = clamp(Math.max(TAB_MIN_WIDTH, rawWidth), TAB_MIN_WIDTH, maxW);
        float height = TAB_TOP_PADDING + TAB_BOTTOM_PADDING + rows * TAB_ROW_HEIGHT;
        float maxH = Math.max(HEIGHT, client.getWindow().getGuiScaledHeight() - TOP * 2f);
        return new IslandLayout(width, clamp(height, 74f, maxH), 18f, true);
    }

    private float measure(String text) {
        return FontRenderer.measureTextWidth(text, TEXT_SIZE);
    }

    private float measureBlockText(String text, float size) {
        return FontRenderer.measureTextWidth(text, size);
    }

    private void renderContentDirect(Minecraft client, IslandContent content, List<PlayerInfo> tabPlayers, BlockCountDisplayRenderer.Snapshot blockSnapshot, ItemUseStatusRenderer.Snapshot itemUseSnapshot, LowHealthHandler.Snapshot alertSnapshot, DynamicIslandNotificationCard notificationCard, IslandLayout layout, boolean tabOpen, boolean blockOpen, boolean itemUseOpen, boolean alertOpen, boolean notificationOpen, float x, float y, float islandScale, boolean blurred) {
        ensureNativeLoaded();
        int framebufferId = mainFramebufferId(client);
        if (framebufferId == 0) {
            return;
        }
        Canvas canvas = glBackend.begin(framebufferId);
        if (canvas == null) {
            return;
        }
        try {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(islandScale, islandScale);
            if (!blurred) {
                Paint fallback = new Paint().setAntiAlias(true);
                fallback.setColor(Config.hudTheme == Config.HudTheme.LIGHT ? 0xE0F2F4F8 : 0xE0101420);
                canvas.drawRRect(RRect.makeXYWH(0f, 0f, layout.width, layout.height, layout.radius), fallback);
            }
            if (tabOpen || tabContentFade > 0f) {
                drawTabContent(canvas, tabPlayers, layout, tabContentFade);
            } else if (notificationOpen) {
                drawNotificationContent(canvas, notificationCard, layout);
            } else if (alertOpen) {
                drawAlertContent(canvas, alertSnapshot, layout);
            } else if (itemUseOpen) {
                drawItemUseContent(canvas, itemUseSnapshot, layout);
            } else if (blockOpen) {
                drawBlockCountContent(canvas, blockSnapshot, layout);
            } else if (lyricsFade > 0.01f && !lastLyricsText.isEmpty()) {
                drawLyricsContent(canvas, lastLyricsText, layout, lyricsFade, lastLyricsDurationMs);
            } else {
                drawCenteredContent(canvas, content, layout);
            }
            canvas.restore();
        } finally {
            glBackend.end();
        }
    }

    private void drawLyricsContent(Canvas canvas, String text, IslandLayout layout, float fade, long durationMs) {
        int alpha = Math.round(clamp(fade, 0f, 1f) * 255f);

        Song song = MusicPlaybackService.INSTANCE.currentSong();
        if (song == null) return;

        float centerX = layout.width * 0.5f;

        float infoY = 18f;
        String songName = song.name();
        String artist = song.displayArtist();
        String combined = "";
        if (songName != null && !songName.isBlank()) {
            combined = songName;
        }
        if (artist != null && !artist.isBlank()) {
            combined = combined.isEmpty() ? artist : combined + " - " + artist;
        }
        if (!combined.isEmpty()) {
            float infoW = FontRenderer.measureTextWidth(combined, 10f);
            FontRenderer.drawText(canvas, combined, centerX - infoW * 0.5f, infoY, 10f, withAlpha(0xFFA0A4B0, alpha));
            infoY += 14f;
        }

        if (text == null || text.isEmpty()) return;

        float textSize = 13f;
        float textY = infoY + 4f;

        int charCount = text.length();
        float totalTextW = FontRenderer.measureTextWidth(text, textSize);
        float textCenterX = centerX;

        long now = System.currentTimeMillis();
        float expandElapsed = now - lyricsExpandStartTime;
        float expandDuration = Math.max(300f, (float) durationMs);
        float expandProgress = clamp(expandElapsed / expandDuration, 0f, 1f);
        expandProgress = easeOutCubic(expandProgress);

        float staggerWindow = Math.max(1f, charCount * LYRICS_STAGGER_RATIO);

        float cursorX = centerX - totalTextW * 0.5f;
        for (int i = 0; i < charCount; i++) {
            String ch = text.substring(i, i + 1);
            float charW = FontRenderer.measureTextWidth(ch, textSize);

            float charProgress = clamp((expandProgress * (charCount + staggerWindow) - i) / staggerWindow, 0f, 1f);
            charProgress = easeOutCubic(charProgress);

            float finalX = cursorX;
            float drawX = textCenterX + (finalX - textCenterX) * charProgress;
            int charAlpha = Math.round(charProgress * alpha);
            FontRenderer.drawText(canvas, ch, drawX, textY, textSize, withAlpha(0xFFF2F4F8, charAlpha));
            cursorX += charW;
        }
    }

    private String tabKey(List<PlayerInfo> players, boolean tabOpen) {
        if (!tabOpen) return "compact";
        StringBuilder key = new StringBuilder("tab:")
                .append(Config.nickHider)
                .append('|')
                .append(Config.nickHiderTab)
                .append('|')
                .append(Config.nickHiderNickname)
                .append(':');
        for (int i = 0; i < Math.min(players.size(), 80); i++) {
            PlayerInfo player = players.get(i);
            key.append(player.getProfile().id())
                    .append(':').append(getPlayerName(player))
                    .append('@').append(player.getLatency())
                    .append(';');
        }
        return key.toString();
    }

    private String blockKey(BlockCountDisplayRenderer.Snapshot snapshot, boolean blockOpen) {
        if (!blockOpen || !snapshot.visible()) return "block:none";
        return "block:" + snapshot.itemName() + "|" + snapshot.blocksLeft() + "|" + snapshot.blocksPerSecond() + "|" + Math.round(snapshot.progress() * 160f) + "|" + Math.round(snapshot.alpha() * 255f);
    }

    private String itemUseKey(ItemUseStatusRenderer.Snapshot snapshot, boolean itemUseOpen) {
        if (!itemUseOpen || !snapshot.visible()) return "itemuse:none";
        return "itemuse:" + snapshot.itemName() + "|" + Math.round(snapshot.progress() * 220f) + "|" + Math.round(snapshot.alpha() * 255f);
    }

    private String alertKey(LowHealthHandler.Snapshot snapshot, boolean alertOpen) {
        if (!alertOpen || !snapshot.visible()) return "alert:none";
        return "alert:" + snapshot.stage() + "|" + snapshot.icon() + "|" + snapshot.title() + "|" + snapshot.message() + "|" + timedIconAnimationFrame(snapshot.createdAtMs());
    }

    private String notificationKey(DynamicIslandNotificationCard card, boolean notificationOpen) {
        if (!notificationOpen || !card.visible()) return "notification:none";
        return "notification:" + card.icon() + "|" + card.title() + "|" + card.message() + "|" + card.accentColor() + "|" + timedIconAnimationFrame(card.createdAtMs());
    }

    private String blockDetail(BlockCountDisplayRenderer.Snapshot snapshot) {
        return snapshot.blocksLeft() + " blocks left - " + String.format(Locale.ROOT, "%.2f block/s", snapshot.blocksPerSecond());
    }

    private String itemUseTitle(ItemUseStatusRenderer.Snapshot snapshot) {
        return "Using " + snapshot.itemName();
    }

    private String itemUseDetail(ItemUseStatusRenderer.Snapshot snapshot) {
        return Math.round(clamp(snapshot.progress(), 0f, 1f) * 100f) + "%";
    }

    private void drawCenteredContent(Canvas canvas, IslandContent content, IslandLayout layout) {
        float fullW = measure(content.brand)
                + measureIconSegment(content.username)
                + measureIconSegment(content.location)
                + measureIconSegment("FPS:" + content.fps)
                + measure("|") * 3f
                + SEPARATOR_GAP * 6f;
        float x = (layout.width - fullW) * 0.5f;
        float y = 19.5f;
        x = drawSegment(canvas, content.brand, x, y);
        x = drawSeparator(canvas, x, y);
        x = drawIconSegment(canvas, ICON_PERSON, content.username, x, y);
        x = drawSeparator(canvas, x, y);
        x = drawIconSegment(canvas, ICON_LINK, content.location, x, y);
        x = drawSeparator(canvas, x, y);
        drawIconSegment(canvas, ICON_COMPUTER, "FPS:" + content.fps, x, y);
    }

    private float drawSegment(Canvas canvas, String text, float x, float y) {
        FontRenderer.drawText(canvas, text, x, y, TEXT_SIZE, compactTextColor());
        return x + measure(text);
    }

    private float drawIconSegment(Canvas canvas, String icon, String text, float x, float y) {
        FontRenderer.drawText(canvas, icon, x, y + ICON_Y_OFFSET, ICON_SIZE, compactTextColor(), FontRenderer.MATERIAL_SYMBOLS);
        x += measureIcon() + ICON_GAP;
        FontRenderer.drawText(canvas, text, x, y, TEXT_SIZE, compactTextColor());
        return x + measure(text);
    }

    private float measureIconSegment(String text) {
        return measureIcon() + ICON_GAP + measure(text);
    }

    private float measureIcon() {
        return FontRenderer.measureTextWidth(ICON_PERSON, ICON_SIZE, FontRenderer.MATERIAL_SYMBOLS);
    }

    private float drawSeparator(Canvas canvas, float x, float y) {
        x += SEPARATOR_GAP;
        FontRenderer.drawText(canvas, "|", x, y, TEXT_SIZE, separatorColor());
        return x + measure("|") + SEPARATOR_GAP;
    }

    private void drawBlockCountContent(Canvas canvas, BlockCountDisplayRenderer.Snapshot snapshot, IslandLayout layout) {
        int alpha = Math.round(clamp(snapshot.alpha(), 0f, 1f) * 255f);
        if (alpha <= 0) return;

        Paint iconBg = new Paint().setAntiAlias(true);
        iconBg.setColor(iconChipColor(alpha));
        canvas.drawRRect(RRect.makeXYWH(BLOCK_ICON_X, BLOCK_ICON_Y, BLOCK_ICON_BOX, BLOCK_ICON_BOX, 10f), iconBg);
        String icon = Config.dynamicIslandBlockCountAltIcon ? ICON_BLOCK_ALT : ICON_BLOCK;
        float iconSize = 23f;
        float iconW = FontRenderer.measureTextWidth(icon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float iconX = BLOCK_ICON_X + (BLOCK_ICON_BOX - iconW) * 0.5f;
        FontRenderer.drawText(canvas, icon, iconX + 0.2f, 42.8f, iconSize, withAlpha(detailPrimaryTextColor(), alpha), FontRenderer.MATERIAL_SYMBOLS);

        String title = trimToWidth(snapshot.itemName(), layout.width - BLOCK_TEXT_X - BLOCK_RIGHT_PADDING, 13.5f);
        String detail = blockDetail(snapshot);
        FontRenderer.drawText(canvas, title, BLOCK_TEXT_X, 24f, 13.5f, withAlpha(detailPrimaryTextColor(), alpha));
        FontRenderer.drawText(canvas, detail, BLOCK_TEXT_X, 40f, 11f, withAlpha(detailSecondaryTextColor(), alpha));

        float progressX = BLOCK_PROGRESS_X;
        float progressW = Math.max(80f, layout.width - progressX * 2f);

        Paint track = new Paint().setAntiAlias(true);
        track.setColor(progressTrackColor(alpha));
        canvas.drawRRect(RRect.makeXYWH(progressX, BLOCK_PROGRESS_Y, progressW, BLOCK_PROGRESS_H, BLOCK_PROGRESS_H * 0.5f), track);

        float fillW = Math.max(BLOCK_PROGRESS_H, progressW * clamp(snapshot.progress(), 0f, 1f));
        Paint fill = new Paint().setAntiAlias(true);
        fill.setColor(progressColor(snapshot.progress(), alpha));
        canvas.drawRRect(RRect.makeXYWH(progressX, BLOCK_PROGRESS_Y, fillW, BLOCK_PROGRESS_H, BLOCK_PROGRESS_H * 0.5f), fill);
    }

    private void drawItemUseContent(Canvas canvas, ItemUseStatusRenderer.Snapshot snapshot, IslandLayout layout) {
        int alpha = Math.round(clamp(snapshot.alpha(), 0f, 1f) * 255f);
        if (alpha <= 0) return;

        Paint iconBg = new Paint().setAntiAlias(true);
        iconBg.setColor(iconChipColor(alpha));
        canvas.drawRRect(RRect.makeXYWH(BLOCK_ICON_X, BLOCK_ICON_Y, BLOCK_ICON_BOX, BLOCK_ICON_BOX, 10f), iconBg);
        float iconSize = 23f;
        float iconW = FontRenderer.measureTextWidth(ICON_ITEM_USE, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float iconX = BLOCK_ICON_X + (BLOCK_ICON_BOX - iconW) * 0.5f;
        FontRenderer.drawText(canvas, ICON_ITEM_USE, iconX + 0.2f, 42.8f, iconSize, withAlpha(detailPrimaryTextColor(), alpha), FontRenderer.MATERIAL_SYMBOLS);

        String title = trimToWidth(itemUseTitle(snapshot), layout.width - BLOCK_TEXT_X - BLOCK_RIGHT_PADDING, 13.5f);
        String detail = itemUseDetail(snapshot);
        FontRenderer.drawText(canvas, title, BLOCK_TEXT_X, 24f, 13.5f, withAlpha(detailPrimaryTextColor(), alpha));
        FontRenderer.drawText(canvas, detail, BLOCK_TEXT_X, 40f, 11f, withAlpha(detailSecondaryTextColor(), alpha));

        float progressX = BLOCK_PROGRESS_X;
        float progressW = Math.max(80f, layout.width - progressX * 2f);

        Paint track = new Paint().setAntiAlias(true);
        track.setColor(progressTrackColor(alpha));
        canvas.drawRRect(RRect.makeXYWH(progressX, BLOCK_PROGRESS_Y, progressW, BLOCK_PROGRESS_H, BLOCK_PROGRESS_H * 0.5f), track);

        float progress = clamp(snapshot.progress(), 0f, 1f);
        float fillW = Math.max(BLOCK_PROGRESS_H, progressW * progress);
        Paint fill = new Paint().setAntiAlias(true);
        fill.setColor(progressColor(progress, alpha));
        canvas.drawRRect(RRect.makeXYWH(progressX, BLOCK_PROGRESS_Y, fillW, BLOCK_PROGRESS_H, BLOCK_PROGRESS_H * 0.5f), fill);
    }

    private void drawAlertContent(Canvas canvas, LowHealthHandler.Snapshot snapshot, IslandLayout layout) {
        drawNotificationLikeContent(canvas, snapshot.icon(), snapshot.title(), snapshot.message(), 0xFFFFFFFF, layout, timedIconAnimationProgress(snapshot.createdAtMs()), true);
    }

    private void drawNotificationContent(Canvas canvas, DynamicIslandNotificationCard card, IslandLayout layout) {
        drawNotificationLikeContent(canvas, card.icon(), card.title(), card.message(), card.accentColor(), layout, timedIconAnimationProgress(card.createdAtMs()), true);
    }

    private void drawNotificationLikeContent(Canvas canvas, String icon, String title, String message, int accentColor, IslandLayout layout, float iconProgress, boolean animateIcon) {
        int alpha = 255;
        Paint iconBg = new Paint().setAntiAlias(true);
        iconBg.setColor(iconChipColor(alpha));
        canvas.drawRRect(RRect.makeXYWH(BLOCK_ICON_X, 8f, BLOCK_ICON_BOX, BLOCK_ICON_BOX, 10f), iconBg);

        float iconSize = 23f;
        float iconW = FontRenderer.measureTextWidth(icon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float iconX = BLOCK_ICON_X + (BLOCK_ICON_BOX - iconW) * 0.5f;
        if (animateIcon) {
            drawAnimatedNotificationIcon(canvas, icon, iconX + 0.2f, 40.8f, iconSize, accentColor, iconProgress);
        } else {
            FontRenderer.drawText(canvas, icon, iconX + 0.2f, 40.8f, iconSize, withAlpha(accentColor, alpha), FontRenderer.MATERIAL_SYMBOLS);
        }

        String trimmedTitle = trimToWidth(title, layout.width - BLOCK_TEXT_X - BLOCK_RIGHT_PADDING, 13.5f);
        String trimmedMessage = trimToWidth(message, layout.width - BLOCK_TEXT_X - BLOCK_RIGHT_PADDING, 11f);
        FontRenderer.drawText(canvas, trimmedTitle, BLOCK_TEXT_X, 24f, 13.5f, withAlpha(accentColor, alpha));
        FontRenderer.drawText(canvas, trimmedMessage, BLOCK_TEXT_X, 41f, 11f, withAlpha(detailSecondaryTextColor(), alpha));
    }

    private int blurTint() {
        return Config.skiaBlurTintColor();
    }

    private float blurStrength() {
        return Config.skiaBlurStrength;
    }

    private int compactTextColor() {
        return Config.hudPrimaryTextColor();
    }

    private int separatorColor() {
        return Config.hudSecondaryTextColor();
    }

    private int detailPrimaryTextColor() {
        return Config.hudPrimaryTextColor();
    }

    private int detailSecondaryTextColor() {
        return Config.hudSecondaryTextColor();
    }

    private int iconChipColor(int alpha) {
        int base = Config.hudTheme == Config.HudTheme.LIGHT ? 0x40FFFFFF : 0x332A3345;
        return multiplyAlpha(base, alpha);
    }

    private int progressTrackColor(int alpha) {
        int base = Config.hudTheme == Config.HudTheme.LIGHT ? 0x55FFFFFF : 0x55A8B3CF;
        return multiplyAlpha(base, alpha);
    }

    private int timedIconAnimationFrame(long createdAtMs) {
        if (createdAtMs <= 0L) {
            return 24;
        }
        long elapsed = System.currentTimeMillis() - createdAtMs;
        if (elapsed >= NOTIFICATION_ICON_ANIMATION_MS) {
            return 24;
        }
        return Math.round(clamp(elapsed / (float) NOTIFICATION_ICON_ANIMATION_MS, 0f, 1f) * 24f);
    }

    private float timedIconAnimationProgress(long createdAtMs) {
        if (createdAtMs <= 0L) {
            return 1f;
        }
        float raw = clamp((System.currentTimeMillis() - createdAtMs) / (float) NOTIFICATION_ICON_ANIMATION_MS, 0f, 1f);
        return easeOutCubic(raw);
    }

    private float easeOutCubic(float value) {
        float t = 1f - clamp(value, 0f, 1f);
        return 1f - t * t * t;
    }

    private void drawAnimatedNotificationIcon(Canvas canvas, String icon, float iconX, float iconY, float iconSize, int accentColor, float progress) {
        float p = clamp(progress, 0f, 1f);
        int animatedAlpha = Math.round(255f * p);
        canvas.save();
        float cx = BLOCK_ICON_X + BLOCK_ICON_BOX * 0.5f;
        float cy = 8f + BLOCK_ICON_BOX * 0.5f;
        float scale = 0.82f + 0.18f * p;
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);
        FontRenderer.drawText(canvas, icon, iconX, iconY, iconSize, withAlpha(accentColor, animatedAlpha), FontRenderer.MATERIAL_SYMBOLS);
        canvas.restore();
    }

    private List<PlayerInfo> getTabPlayers(Minecraft client) {
        if (client.getConnection() == null) return List.of();
        List<PlayerInfo> players = new ArrayList<>(client.getConnection().getListedOnlinePlayers());
        players.sort(Comparator
                .comparingInt(PlayerInfo::getTabListOrder)
                .thenComparing(info -> getPlayerName(info).toLowerCase()));
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
        float startY = TAB_TOP_PADDING + (layout.height - TAB_TOP_PADDING - TAB_BOTTOM_PADDING - totalContentH) * 0.5f + 10.5f;

        for (int i = 0; i < count; i++) {
            int column = i / rows;
            int row = i % rows;
            float x = TAB_SIDE_PADDING + column * (columnW + TAB_COLUMN_GAP);
            float y = startY + row * TAB_ROW_HEIGHT;
            PlayerInfo player = players.get(i);
            int baseColor = player.getGameMode() == GameType.SPECTATOR ? TAB_SPECTATOR_NAME_COLOR : TAB_DEFAULT_NAME_COLOR;
            Map.Entry<String, Integer> formattedName = resolveTabName(player, baseColor);
            String name = trimToWidth(formattedName.getKey(), columnW - 46f, TAB_NAME_SIZE);
            int color = isLocalPlayer(player) ? TAB_SELF_NAME_COLOR : formattedName.getValue();
            FontRenderer.drawText(canvas, name, x, y, TAB_NAME_SIZE, withAlpha(color, alpha));

            String latency = formatLatency(player.getLatency());
            float latencyW = FontRenderer.measureTextWidth(latency, 9f);
            FontRenderer.drawText(canvas, latency, x + columnW - latencyW, y, 9f, withAlpha(latencyColor(player.getLatency()), alpha));
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

    private String trimToWidth(String text, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        String suffix = "...";
        float suffixW = FontRenderer.measureTextWidth(suffix, size);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = out + text.substring(i, i + 1);
            if (FontRenderer.measureTextWidth(next, size) + suffixW > maxWidth) break;
            out.append(text.charAt(i));
        }
        return out + suffix;
    }

    private String formatLatency(int latency) {
        return latency < 0 ? "?" : latency + "ms";
    }

    private int latencyColor(int latency) {
        if (latency < 0) return 0xFFAAAAAA;
        if (latency < 150) return 0xFF53D86A;
        if (latency < 300) return 0xFFFFD166;
        return 0xFFFF6B6B;
    }

    private int progressColor(float progress, int alpha) {
        progress = clamp(progress, 0f, 1f);
        int r;
        int g;
        if (progress < 0.5f) {
            float t = progress * 2.0f;
            r = 255;
            g = Math.round(255.0f * t);
        } else {
            float t = (progress - 0.5f) * 2.0f;
            r = Math.round(255.0f * (1.0f - t));
            g = 255;
        }
        return ((alpha & 0xFF) << 24) | (r << 16) | (g << 8);
    }

    private int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (clamp(alpha, 0, 255) << 24);
    }

    private int multiplyAlpha(int argb, int alpha) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int finalAlpha = Math.round(baseAlpha * (clamp(alpha, 0, 255) / 255f));
        return (argb & 0x00FFFFFF) | (finalAlpha << 24);
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private void resetAnimation() {
        animatedWidth = -1f;
        animatedHeight = -1f;
        tabContentFade = 0f;
        lyricsFade = 0f;
        lastLyricsOpen = false;
        lastLyricsText = "";
        lyricsExpandStartTime = 0L;
        lastAnimationTime = 0L;
        lastTabRequestTime = 0L;
    }

    private float getBaseEditWidth() {
        return animatedWidth > 0f ? animatedWidth : MIN_WIDTH;
    }

    private float getBaseEditHeight() {
        return animatedHeight > 0f ? animatedHeight : HEIGHT;
    }

    private float getScale() {
        return Math.max(0.5f, Config.dynamicIslandScale);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record IslandContent(String brand, String username, String location, String fps) {
        String key() {
            return brand + "|" + username + "|" + location + "|" + fps;
        }
    }

    private record IslandLayout(float width, float height, float radius, boolean isTab) {
    }
}
