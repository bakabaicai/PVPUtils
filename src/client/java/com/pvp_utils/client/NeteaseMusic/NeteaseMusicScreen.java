package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.SkijaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.pvp_utils.client.NeteaseMusic.MusicUi.*;

public final class NeteaseMusicScreen extends Screen implements SkijaScreen {
    private static final ExecutorService IO = Executors.newFixedThreadPool(3, task -> {
        Thread thread = new Thread(task, "PVPUtils-MusicUI");
        thread.setDaemon(true);
        return thread;
    });
    private final Screen parent;
    private final MusicPlaybackService player = MusicPlaybackService.INSTANCE;
    private final List<Playlist> library = new ArrayList<>();
    private final List<Playlist> recommendations = new ArrayList<>();
    private final List<Song> songs = new ArrayList<>();
    private final List<HitTarget> targets = new ArrayList<>();
    private final List<MenuItem> contextItems = new ArrayList<>();
    private final MusicTextInput search = new MusicTextInput();
    private final MusicTextInput filter = new MusicTextInput();
    private final MusicSettingsView settings = new MusicSettingsView();
    private MusicTextInput focused;
    private MusicLyricsView lyrics;
    private Page page = Page.HOME;
    private Playlist playlist;
    private String heading = "";
    private String status = "";
    private String qrStatus = "";
    private String toast = "";
    private String lastPlaybackStatus = "";
    private long toastUntil;
    private long openedAt;
    private long closedAt;
    private long pageStartedAt;
    private long lyricsStartedAt;
    private long lyricsClosedAt;
    private boolean closing;
    private volatile boolean attached;
    private boolean initialized;
    private boolean reloadOnReturn;
    private boolean restoring;
    private boolean loading;
    private boolean loadingMore;
    private boolean hasMore;
    private boolean tiles;
    private boolean accountOpen;
    private boolean loginOpen;
    private boolean qrBusy;
    private boolean pendingFrame;
    private boolean draggingInput;
    private int requestSerial;
    private int accountSerial;
    private volatile int qrSerial;
    private NeteaseMusicApi.QrLogin qr;
    private int qrCode;
    private float scroll;
    private float scrollTarget;
    private float sidebarScroll;
    private float sidebarTarget;
    private float maxScroll;
    private float maxSidebarScroll;
    private float frameMouseX;
    private float frameMouseY;
    private float mouseX;
    private float mouseY;
    private float panelW;
    private float panelH;
    private float sideW;
    private float contentH;
    private float scale;
    private float offsetX;
    private float offsetY;
    private float contextX;
    private float contextY;
    private float pendingSeek = -1f;
    private float previousVolume = 1f;
    private Drag dragging = Drag.NONE;
    private Rect seekBounds = Rect.makeXYWH(0, 0, 1, 1);
    private Rect volumeBounds = Rect.makeXYWH(0, 0, 1, 1);
    private Rect listBounds = Rect.makeXYWH(0, 0, 1, 1);
    private boolean recordingTargets = true;

    public NeteaseMusicScreen(Screen parent) {
        super(Component.literal("PVPUtils Music"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        attached = true;
        closing = false;
        openedAt = System.nanoTime();
        if (initialized) return;
        initialized = true;
        player.setVolume(Config.neteaseMusicVolume, false);
        restoreLogin();
    }

    private void restoreLogin() {
        restoring = true;
        int serial = ++accountSerial;
        CompletableFuture.supplyAsync(NeteaseMusicApi::restoreSession, IO).whenComplete((loggedIn, error) ->
                minecraft.execute(() -> {
                    if (!attached || closing || serial != accountSerial) return;
                    restoring = false;
                    if (Boolean.TRUE.equals(loggedIn)) refreshLibrary();
                    else openLogin();
                }));
    }

    @Override
    public void added() {
        attached = true;
        closing = false;
        openedAt = System.nanoTime();
        if (initialized) {
            if (restoring) restoreLogin();
            else if (loginOpen && !NeteaseMusicApi.isLoggedIn()) {
                qrBusy = false;
                startQrLogin();
            } else if (reloadOnReturn) {
                switch (page) {
                    case HOME -> refreshLibrary();
                    case SEARCH -> runSearch();
                    case DAILY -> loadDaily();
                    case PLAYLIST -> {
                        if (playlist != null) loadPlaylist(playlist);
                    }
                    default -> { }
                }
            }
        }
        reloadOnReturn = false;
        super.added();
    }

    @Override
    protected void repositionElements() {
        targets.clear();
        contextItems.clear();
        releaseDrag();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (closing && elapsed(closedAt) >= 220f) {
            minecraft.setScreen(parent);
            return;
        }
        frameMouseX = mouseX;
        frameMouseY = mouseY;
        String playbackStatus = player.status();
        if (!playbackStatus.equals(lastPlaybackStatus)) {
            lastPlaybackStatus = playbackStatus;
            if (playbackStatus.startsWith("Load failed") || playbackStatus.startsWith("Replay failed")
                    || playbackStatus.startsWith("Seek failed") || playbackStatus.equals("Song is unavailable")) {
                notify(MusicFeedback.playbackStatus(playbackStatus));
            }
        }
        pendingFrame = true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void renderSkija(Canvas canvas) {
        if (!pendingFrame || !attached || minecraft.screen != this) return;
        pendingFrame = false;
        updateLayout();
        float visibility = closing ? 1f - ease(elapsed(closedAt) / 220f) : ease(elapsed(openedAt) / 280f);
        float lyricsVisibility = lyrics == null ? 0f : lyricsClosedAt == 0
                ? ease(elapsed(lyricsStartedAt) / 360f) : 1f - ease(elapsed(lyricsClosedAt) / 250f);
        scale *= 0.9f + visibility * 0.1f;
        offsetX = width * 0.5f - panelW * scale * 0.5f;
        offsetY = height * 0.5f - panelH * scale * 0.5f;
        mouseX = (frameMouseX - offsetX) / scale;
        mouseY = (frameMouseY - offsetY) / scale;
        targets.clear();
        rounded(canvas, 0, 0, width, height, 0, alpha(0xFF000000, visibility * 0.62f));
        int save = canvas.save();
        try (Paint opacity = visibility < 1f ? new Paint().setAlphaf(visibility) : null) {
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            if (opacity != null) canvas.saveLayer(null, opacity);
            canvas.clipRect(Rect.makeXYWH(0, 0, panelW, panelH));
            recordingTargets = !loginOpen && !accountOpen && contextItems.isEmpty() && lyrics == null && !restoring;
            if (lyricsVisibility < 1f) {
                rounded(canvas, 0, 0, panelW, panelH, 0, BG);
                renderSidebar(canvas);
                rounded(canvas, sideW, 0, panelW - sideW, contentH, 0, SIDEBAR);
                float reveal = ease(elapsed(pageStartedAt) / 300f);
                int contentSave = canvas.save();
                try (Paint contentOpacity = reveal < 1f ? new Paint().setAlphaf(reveal) : null) {
                    canvas.clipRect(Rect.makeXYWH(sideW, 0, panelW - sideW, contentH));
                    if (contentOpacity != null) canvas.saveLayer(null, contentOpacity);
                    float zoom = 1f + (1f - reveal) * 0.045f;
                    canvas.translate(sideW + (panelW - sideW) * 0.5f, contentH * 0.5f);
                    canvas.scale(zoom, zoom);
                    canvas.translate(-sideW - (panelW - sideW) * 0.5f, -contentH * 0.5f);
                    boolean wasRecording = recordingTargets;
                    recordingTargets &= elapsed(pageStartedAt) >= 300f;
                    renderContent(canvas);
                    recordingTargets = wasRecording;
                } finally {
                    canvas.restoreToCount(contentSave);
                }
                renderControls(canvas);
                line(canvas, sideW, 0, sideW, panelH, 0.5f, 0x10FFFFFF);
                line(canvas, sideW, contentH, panelW, contentH, 0.5f, 0x10FFFFFF);
            }
            recordingTargets = true;
            if (lyrics != null) renderLyrics(canvas, lyricsVisibility);
            if (!contextItems.isEmpty()) renderContext(canvas);
            if (accountOpen) renderAccount(canvas);
            if (loginOpen || restoring) renderLogin(canvas);
            if (System.nanoTime() < toastUntil) {
                float w = Math.min(panelW - 32f, width(toast, 8f) + 24f);
                rounded(canvas, (panelW - w) * 0.5f, panelH - 28f, w, 20f, 5f, 0xF024262C);
                text(canvas, fit(toast, w - 20f, 8f), (panelW - w) * 0.5f + 12f, panelH - 22f, 8f, TEXT);
            }
        } finally {
            canvas.restoreToCount(save);
            recordingTargets = true;
        }
    }

    private void updateLayout() {
        float nativeScale = 2f / Math.max(1f, minecraft.getWindow().getGuiScale());
        scale = Math.max(0.05f, Math.min(nativeScale, Math.min(width / 672f, height / 392f)));
        panelW = width / scale - 32f;
        panelH = height / scale - 32f;
        sideW = Math.max(104f, panelW * 0.15f);
        contentH = panelH - Math.max(40f, panelH * 0.07f);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        sidebarTarget = Math.max(0, Math.min(maxSidebarScroll, sidebarTarget));
        scroll += (scrollTarget - scroll) * 0.25f;
        sidebarScroll += (sidebarTarget - sidebarScroll) * 0.25f;
    }

    private void renderSidebar(Canvas canvas) {
        drawInput(canvas, search, 8, 8, sideW - 16f, 18f, tr("搜索音乐", "Search music"));
        bold(canvas, "PVPUtils", 12, 37, 11f, TEXT);
        text(canvas, "MUSIC", 12, 51, 5.5f, MUTED);
        nav(canvas, "home", tr("主页", "Home"), 66, page == Page.HOME, () -> showPage(Page.HOME));
        nav(canvas, "music", tr("每日推荐", "Daily mix"), 88, page == Page.DAILY, this::loadDaily);
        nav(canvas, "settings", tr("设置", "Settings"), 110, page == Page.SETTINGS, () -> showPage(Page.SETTINGS));
        text(canvas, tr("资料库", "LIBRARY"), 12, 145, 6.5f, MUTED);
        float top = 160f;
        float bottom = panelH - 43f;
        maxSidebarScroll = Math.max(0, library.size() * 22f - (bottom - top));
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeLTRB(4, top, sideW - 4f, bottom));
            if (library.isEmpty()) text(canvas, fit(tr("你的歌单会显示在这里", "Your playlists appear here"), sideW - 24, 6.5f), 12, top + 10, 6.5f, MUTED);
            for (int i = 0; i < library.size(); i++) {
                Playlist entry = library.get(i);
                float y = top + i * 22f - sidebarScroll;
                if (y + 22f < top || y > bottom) continue;
                boolean active = page == Page.PLAYLIST && playlist != null && playlist.id() == entry.id();
                if (active || hit(5, y, sideW - 10f, 21)) rounded(canvas, 5, y, sideW - 10f, 21f, 3.5f, active ? 0xFF303137 : PANEL);
                if (i == 0) icon(canvas, "heart", 12, y + 5f, 11, active ? TEXT : ACCENT);
                else cover(canvas, NeteaseMusicCovers.skiaImage(entry.coverUrl()), 10, y + 2.5f, 16, 2, 255);
                text(canvas, fit(entry.name(), sideW - 40, 7.5f), 32, y + 6.5f, 7.5f, active ? TEXT : 0xFFC0C2C8);
                target(5, Math.max(top, y), sideW - 10, Math.min(bottom, y + 21) - Math.max(top, y), () -> loadPlaylist(entry));
            }
        } finally {
            canvas.restoreToCount(save);
        }
        NeteaseMusicApi.LoginSession session = NeteaseMusicApi.currentSession();
        float userY = panelH - 31f;
        if (hit(6, userY - 4, sideW - 12, 29)) rounded(canvas, 6, userY - 4, sideW - 12, 29, 4, PANEL);
        cover(canvas, session == null ? null : NeteaseMusicCovers.skiaImage(session.avatarUrl()), 12, userY, 20, 10, 255);
        String name = session == null ? tr("登录网易云音乐", "Sign in") : session.nickname().isBlank() ? "PVPUtils" : session.nickname();
        text(canvas, fit(name, sideW - 49, 7.5f), 39, userY + 2, 7.5f, TEXT);
        text(canvas, fit(session == null ? "NETEASE CLOUD MUSIC" : "UID " + session.uid(), sideW - 49, 5.5f), 39, userY + 12, 5.5f, MUTED);
        target(6, userY - 4, sideW - 12, 29, () -> {
            if (NeteaseMusicApi.isLoggedIn()) accountOpen = true;
            else openLogin();
        });
    }

    private void nav(Canvas canvas, String glyph, String label, float y, boolean selected, Runnable action) {
        if (selected || hit(5, y, sideW - 10, 21)) rounded(canvas, 5, y, sideW - 10, 21, 3.5f, selected ? PANEL : 0xFF1C1E23);
        icon(canvas, glyph, 12, y + 5f, 11, selected ? ACCENT : 0xFFC0C2C8);
        text(canvas, label, 32, y + 6.5f, 8, selected ? TEXT : 0xFFC0C2C8);
        target(5, y, sideW - 10, 21, action);
    }

    private void renderContent(Canvas canvas) {
        switch (page) {
            case HOME -> renderHome(canvas);
            case SETTINGS -> {
                maxScroll = 0;
                int save = canvas.save();
                try {
                    canvas.translate(sideW, 0);
                    settings.render(canvas, panelW - sideW, contentH, mouseX - sideW, mouseY, 1f);
                } finally {
                    canvas.restoreToCount(save);
                }
            }
            default -> renderSongs(canvas);
        }
        iconButton(canvas, "close", panelW - 25, 8, 18, this::onClose);
    }

    private void renderHome(Canvas canvas) {
        float x = sideW + 12f;
        bold(canvas, tr("欢迎回来", "Welcome back"), x, 13, 12.5f, TEXT);
        text(canvas, tr("为你推荐的歌单", "Recommended playlists"), x, 34, 7.5f, TEXT);
        iconButton(canvas, "refresh", panelW - 50, 8, 18, this::refreshLibrary);
        float top = 54f;
        float usableW = panelW - sideW - 24f;
        int columns = Math.max(1, (int) ((usableW + 12f) / 117f));
        float cellW = (usableW - (columns - 1) * 12f) / columns;
        float coverSize = Math.min(100f, cellW - 5f);
        float rowH = coverSize + 35f;
        int rows = (recommendations.size() + columns - 1) / columns;
        maxScroll = Math.max(0, rows * rowH - (contentH - top - 12f));
        listBounds = Rect.makeXYWH(x, top, usableW, contentH - top - 8);
        int save = canvas.save();
        try {
            canvas.clipRect(listBounds);
            for (int i = 0; i < recommendations.size(); i++) {
                Playlist entry = recommendations.get(i);
                float cx = x + (i % columns) * (cellW + 12f);
                float cy = top + (i / columns) * rowH - scroll;
                if (cy + rowH < top || cy > contentH) continue;
                float reveal = ease((elapsed(pageStartedAt) - Math.min(i, 10) * 35f) / 450f);
                float shift = 14f * (1f - reveal);
                boolean hovered = hit(cx, cy, cellW, rowH - 5);
                float grow = hovered ? 3f : 0f;
                cover(canvas, NeteaseMusicCovers.skiaImage(entry.coverUrl()), cx - grow * 0.5f, cy + shift - grow * 0.5f, coverSize + grow, 4, Math.round(255 * reveal));
                if (hovered) {
                    rounded(canvas, cx + coverSize - 25, cy + coverSize - 25, 20, 20, 10, 0xEED60017);
                    icon(canvas, "play", cx + coverSize - 20, cy + coverSize - 20, 10, TEXT);
                }
                text(canvas, fit(entry.name(), cellW - 5f, 7.5f), cx, cy + coverSize + 5 + shift, 7.5f, alpha(TEXT, reveal));
                text(canvas, formatCount(entry.playCount()) + tr(" 次播放", " plays"), cx, cy + coverSize + 16 + shift, 6.5f, alpha(MUTED, reveal));
                target(cx, Math.max(top, cy), cellW, Math.min(contentH - 8f, cy + rowH - 5f) - Math.max(top, cy), () -> loadPlaylist(entry));
            }
            if (recommendations.isEmpty()) empty(canvas, x, top, usableW, contentH - top, "library",
                    loading ? tr("正在加载推荐歌单", "Loading your recommendations") : tr("音乐，从这里开始", "Your music starts here"),
                    status.isBlank() ? tr("登录后查看为你推荐的音乐", "Sign in to discover your music") : status);
        } finally {
            canvas.restoreToCount(save);
        }
        drawScrollbar(canvas);
    }

    private List<Song> visibleSongs() {
        List<Song> source = page == Page.QUEUE ? player.getQueue() : songs;
        String query = filter.value().toLowerCase(Locale.ROOT).trim();
        if (query.isBlank()) return List.copyOf(source);
        return source.stream().filter(song -> (song.name() + " " + song.displayArtist()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void renderSongs(Canvas canvas) {
        float x = sideW + 24f;
        float top;
        List<Song> visible = visibleSongs();
        if (page == Page.PLAYLIST && playlist != null) {
            float coverSize = Math.min(128f, contentH * 0.31f);
            cover(canvas, NeteaseMusicCovers.skiaImage(playlist.coverUrl()), x, 24, coverSize, 4, 255);
            float textX = x + coverSize + 20f;
            text(canvas, tr("歌单", "PLAYLIST"), textX, 27, 6.5f, MUTED);
            bold(canvas, fit(playlist.name(), panelW - textX - 24f, 16), textX, 42, 16, TEXT);
            text(canvas, fit(playlist.creator(), panelW - textX - 24f, 8), textX, 69, 8, TEXT);
            text(canvas, playlist.trackCount() + tr(" 首歌曲 · ", " songs · ") + formatCount(playlist.playCount()) + tr(" 次播放", " plays"), textX, 84, 7, MUTED);
            button(canvas, textX, 103, 74, 21, "play", tr("播放歌单", "Play playlist"), true, () -> playAll(false));
            button(canvas, textX + 82, 103, 65, 21, "shuffle", tr("随机播放", "Shuffle"), false, () -> playAll(true));
            top = coverSize + 49f;
        } else {
            String title = page == Page.QUEUE ? tr("播放队列", "Play queue") : page == Page.DAILY ? tr("每日推荐", "Daily mix") : tr("搜索结果", "Search results");
            bold(canvas, title, x, 20, 17, TEXT);
            text(canvas, page == Page.SEARCH ? "“" + fit(heading, panelW - x - 35, 8) + "”" : tr("让喜欢的音乐继续播放", "Keep your music playing"), x, 48, 8, MUTED);
            button(canvas, x, 69, 72, 21, "play", tr("播放全部", "Play all"), true, () -> playAll(false));
            if (page == Page.QUEUE) button(canvas, x + 80, 69, 72, 21, "close", tr("清空队列", "Clear queue"), false, player::clearQueue);
            top = 111f;
        }
        text(canvas, visible.size() + tr(" 首歌曲", " songs") + (hasMore && page != Page.QUEUE ? "+" : ""), x, top + 6, 7.5f, MUTED);
        drawInput(canvas, filter, panelW - 176f, top, 118, 19, tr("筛选此列表", "Filter this list"));
        iconButton(canvas, tiles ? "queue" : "library", panelW - 48, top, 19, () -> {
            tiles = !tiles;
            scroll = scrollTarget = 0;
        });
        top += 29f;
        listBounds = Rect.makeXYWH(sideW + 18f, top, panelW - sideW - 36f, Math.max(1, contentH - top - 12f));
        if (tiles) renderSongTiles(canvas, visible);
        else renderSongRows(canvas, visible);
        if (loading || visible.isEmpty()) empty(canvas, listBounds.getLeft(), top, listBounds.getWidth(), listBounds.getHeight(), "music",
                loading ? tr("正在加载音乐", "Loading music") : tr("没有找到歌曲", "No songs found"),
                loading ? tr("请稍候", "Please wait") : status.isBlank() ? tr("试试其他关键词", "Try another search") : status);
        drawScrollbar(canvas);
    }

    private void renderSongRows(Canvas canvas, List<Song> visible) {
        float top = listBounds.getTop();
        float bottom = listBounds.getBottom();
        float x = listBounds.getLeft();
        float w = listBounds.getWidth();
        maxScroll = Math.max(0, visible.size() * 32f + (hasMore ? 30f : 0) - listBounds.getHeight());
        int save = canvas.save();
        try {
            canvas.clipRect(listBounds);
            Song playing = player.currentSong();
            for (int i = 0; i < visible.size(); i++) {
                Song song = visible.get(i);
                float y = top + i * 32f - scroll;
                if (y + 30 < top || y > bottom) continue;
                int queueIndex = page == Page.QUEUE ? queueIndexAt(visible, i) : -1;
                boolean selected = playing != null && playing.id() == song.id()
                        && (page != Page.QUEUE || queueIndex == player.currentIndex());
                boolean hovered = hit(x, y, w, 30);
                rounded(canvas, x, y, w, 30, 3, selected ? 0xE6D60017 : hovered ? PANEL : i % 2 == 0 ? 0xFF191B20 : 0xFF141519);
                if (selected || hovered) icon(canvas, selected && player.isPlaying() ? "pause" : "play", x + 6, y + 10, 10, TEXT);
                else text(canvas, String.valueOf(i + 1), x + 7, y + 11, 7, MUTED);
                cover(canvas, NeteaseMusicCovers.skiaImage(song.image()), x + 26, y + 3, 24, 2, 255);
                text(canvas, fit(song.name(), w - 143f, 8), x + 58, y + 5, 8, TEXT);
                text(canvas, fit(song.displayArtist(), w - 143f, 6.5f), x + 58, y + 17, 6.5f, selected ? 0xDDFFFFFF : MUTED);
                text(canvas, MusicPlaybackService.formatTime(song.durationMs()), x + w - 58, y + 11, 7, selected ? TEXT : MUTED);
                int index = i;
                float rowTop = Math.max(top, y);
                float rowHeight = Math.min(bottom, y + 30) - rowTop;
                target(x, rowTop, w - 26, rowHeight, () -> playVisible(visible, index));
                target(x, rowTop, w, rowHeight, 1, () -> openSongMenu(song, queueIndex));
                if (hovered && y >= top && y + 30 <= bottom) iconButton(canvas, "more", x + w - 24, y + 5, 20, () -> openSongMenu(song, queueIndex));
            }
            loadMoreButton(canvas, x, top + visible.size() * 32f - scroll, w);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void renderSongTiles(Canvas canvas, List<Song> visible) {
        int columns = Math.max(1, (int) ((listBounds.getWidth() + 12f) / 117f));
        float cell = (listBounds.getWidth() - (columns - 1) * 12f) / columns;
        float size = Math.min(100f, cell);
        float rowH = size + 34f;
        int rows = (visible.size() + columns - 1) / columns;
        maxScroll = Math.max(0, rows * rowH + (hasMore ? 30 : 0) - listBounds.getHeight());
        int save = canvas.save();
        try {
            canvas.clipRect(listBounds);
            for (int i = 0; i < visible.size(); i++) {
                Song song = visible.get(i);
                float x = listBounds.getLeft() + i % columns * (cell + 12f);
                float y = listBounds.getTop() + i / columns * rowH - scroll;
                if (y + rowH < listBounds.getTop() || y > listBounds.getBottom()) continue;
                cover(canvas, NeteaseMusicCovers.skiaImage(song.image()), x, y, size, 4, 255);
                Song playing = player.currentSong();
                int queueIndex = page == Page.QUEUE ? queueIndexAt(visible, i) : -1;
                boolean selected = playing != null && playing.id() == song.id()
                        && (page != Page.QUEUE || queueIndex == player.currentIndex());
                text(canvas, fit(song.name(), cell, 7.5f), x, y + size + 5, 7.5f, selected ? ACCENT : TEXT);
                text(canvas, fit(song.displayArtist(), cell, 6.5f), x, y + size + 17, 6.5f, MUTED);
                int index = i;
                float top = Math.max(listBounds.getTop(), y);
                float h = Math.min(listBounds.getBottom(), y + rowH - 3) - top;
                target(x, top, cell, h, () -> playVisible(visible, index));
                target(x, top, cell, h, 1, () -> openSongMenu(song, queueIndex));
            }
            loadMoreButton(canvas, listBounds.getLeft(), listBounds.getTop() + rows * rowH - scroll, listBounds.getWidth());
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void loadMoreButton(Canvas canvas, float x, float y, float w) {
        if (!hasMore || page == Page.QUEUE || !filter.value().isBlank()) return;
        if (y < listBounds.getTop() || y + 22 > listBounds.getBottom()) return;
        button(canvas, x + (w - 110) * 0.5f, y + 2, 110, 21, "refresh", loadingMore ? tr("加载中…", "Loading…") : tr("加载更多歌曲", "Load more songs"), false, this::loadMore);
    }

    private void renderControls(Canvas canvas) {
        float y = contentH;
        float h = panelH - y;
        float w = panelW - sideW;
        Song song = player.currentSong();
        float size = Math.min(42f, h - 10f);
        cover(canvas, song == null ? null : NeteaseMusicCovers.skiaImage(song.image()), sideW + 5, y + 5, size, 2, 255);
        target(sideW + 5, y + 5, size, size, this::openLyrics);
        float infoW = Math.max(50, w * 0.25f - size - 17);
        text(canvas, fit(song == null ? tr("还没有播放音乐", "Nothing playing") : song.name(), infoW, 7.5f), sideW + size + 12, y + h * 0.5f - 10, 7.5f, TEXT);
        String subtitle = song == null ? "PVPUtils Music" : player.status().startsWith("Loading ")
                ? tr("正在加载…", "Loading…") : player.status().startsWith("Downloading ")
                ? tr("正在下载…", "Downloading…") : song.displayArtist();
        text(canvas, fit(subtitle, infoW, 6.5f), sideW + size + 12, y + h * 0.5f + 2, 6.5f, MUTED);
        float center = sideW + w * 0.53f;
        float controlsY = y + h * 0.5f - 19f;
        iconButton(canvas, modeIcon(), center - 60, controlsY + 1, 18, player::cyclePlaybackMode);
        iconButton(canvas, "previous", center - 34, controlsY, 20, player::playPrevious);
        iconButton(canvas, player.isPlaying() ? "pause" : "play", center - 10, controlsY - 1, 22, player::toggle);
        iconButton(canvas, "next", center + 16, controlsY, 20, player::playNext);
        seekBounds = Rect.makeXYWH(center - 67.5f, y + h * 0.5f + 9, 135f, 3f);
        float progress = pendingSeek >= 0 ? pendingSeek : playbackProgress();
        slider(canvas, seekBounds, progress, false);
        long duration = player.totalDurationMs();
        text(canvas, MusicPlaybackService.formatTime((long) (duration * progress)), seekBounds.getLeft() - 30, seekBounds.getTop() - 2, 6, MUTED);
        text(canvas, "-" + MusicPlaybackService.formatTime(Math.max(0, duration - (long) (duration * progress))), seekBounds.getRight() + 5, seekBounds.getTop() - 2, 6, MUTED);
        target(seekBounds.getLeft(), seekBounds.getTop() - 5, seekBounds.getWidth(), 13, () -> {
            dragging = Drag.SEEK;
            updateDrag(mouseX, mouseY);
        });
        iconButton(canvas, "queue", panelW - 25, y + (h - 20) * 0.5f, 20, () -> showPage(Page.QUEUE));
        float volumeW = Math.min(62, w * 0.115f);
        volumeBounds = Rect.makeXYWH(panelW - 39 - volumeW, y + h * 0.5f, volumeW, 3);
        iconButton(canvas, player.volume() <= 0.001f ? "mute" : "volume", volumeBounds.getLeft() - 23, y + (h - 18) * 0.5f, 18, this::toggleMute);
        slider(canvas, volumeBounds, player.volume(), true);
        target(volumeBounds.getLeft(), volumeBounds.getTop() - 6, volumeW, 15, () -> {
            dragging = Drag.VOLUME;
            updateDrag(mouseX, mouseY);
        });
    }

    private void drawScrollbar(Canvas canvas) {
        if (maxScroll <= 0) return;
        float h = listBounds.getHeight();
        float thumbH = Math.max(24f, h * h / (h + maxScroll));
        float y = listBounds.getTop() + (h - thumbH) * scroll / maxScroll;
        rounded(canvas, panelW - 8, listBounds.getTop(), 3, h, 1.5f, 0x0AFFFFFF);
        rounded(canvas, panelW - 8, y, 3, thumbH, 1.5f, hit(panelW - 12, y, 10, thumbH) ? 0xAA8A8D94 : 0x668A8D94);
        target(panelW - 12, listBounds.getTop(), 10, h, () -> {
            dragging = Drag.SCROLL;
            updateDrag(mouseX, mouseY);
        });
    }

    private void renderLyrics(Canvas canvas, float visibility) {
        int save = canvas.save();
        try (Paint opacity = visibility < 1f ? new Paint().setAlphaf(visibility) : null) {
            if (opacity != null) canvas.saveLayer(null, opacity);
            float zoom = 1.1f - visibility * 0.1f;
            canvas.translate(panelW * 0.5f, panelH * 0.5f);
            canvas.scale(zoom, zoom);
            canvas.translate(-panelW * 0.5f, -panelH * 0.5f);
            lyrics.setTranslationVisible(Config.lyricsDisplayTranslation);
            lyrics.render(canvas, panelW, panelH, mouseX, mouseY, 1f);
        } finally {
            canvas.restoreToCount(save);
        }
        if (lyrics.wantsClose() && lyricsClosedAt == 0) closeLyrics();
        if (lyricsClosedAt != 0 && visibility <= 0.001f) {
            lyrics.close();
            lyrics = null;
            lyricsClosedAt = 0;
        }
    }

    private void renderLogin(Canvas canvas) {
        targets.clear();
        rounded(canvas, 0, 0, panelW, panelH, 0, 0xAA000000);
        float w = Math.min(340, panelW - 36);
        float h = Math.min(304, panelH - 24);
        float x = (panelW - w) * 0.5f;
        float y = (panelH - h) * 0.5f;
        rounded(canvas, x, y, w, h, 6, SIDEBAR);
        if (!restoring) iconButton(canvas, "close", x + w - 27, y + 8, 18, this::closeLogin);
        bold(canvas, "PVPUtils Music", x + 22, y + 20, 15, TEXT);
        text(canvas, tr("打开网易云音乐 App，扫码登录", "Scan with the NetEase Cloud Music app"), x + 22, y + 47, 8, MUTED);
        float qrSize = Math.min(156, h - 132);
        float qrX = x + (w - qrSize) * 0.5f;
        float qrY = y + 73;
        rounded(canvas, qrX - 8, qrY - 8, qrSize + 16, qrSize + 16, 6, 0xFFF7F7F8);
        Image image = qr == null ? null : NeteaseMusicCovers.skiaImage(qr.qrImage());
        if (image != null) cover(canvas, image, qrX, qrY, qrSize, 0, 255);
        else icon(canvas, restoring || qrBusy ? "refresh" : "music", qrX + qrSize * 0.35f, qrY + qrSize * 0.35f, qrSize * 0.3f, 0xFF8A8D94);
        String message = fit(restoring ? tr("正在恢复登录", "Restoring your session") : qrStatus, w - 40, 8);
        text(canvas, message, x + (w - width(message, 8)) * 0.5f, qrY + qrSize + 19, 8, qrCode == 802 ? ACCENT : TEXT);
        button(canvas, x + (w - 108) * 0.5f, y + h - 34, 108, 21, "refresh",
                qrBusy ? tr("等待扫码确认", "Waiting for confirmation") : tr("刷新二维码", "Refresh QR code"), false, () -> {
            if (!restoring && !qrBusy) startQrLogin();
        });
    }

    private void renderAccount(Canvas canvas) {
        targets.clear();
        rounded(canvas, 0, 0, panelW, panelH, 0, 0x70000000);
        float w = 240;
        float h = 154;
        float x = (panelW - w) * 0.5f;
        float y = (panelH - h) * 0.5f;
        rounded(canvas, x, y, w, h, 6, SIDEBAR);
        NeteaseMusicApi.LoginSession session = NeteaseMusicApi.currentSession();
        cover(canvas, session == null ? null : NeteaseMusicCovers.skiaImage(session.avatarUrl()), x + 18, y + 20, 40, 20, 255);
        bold(canvas, fit(session == null ? "PVPUtils" : session.nickname(), 151, 11), x + 69, y + 26, 11, TEXT);
        text(canvas, session == null ? "" : "UID " + session.uid(), x + 69, y + 44, 7, MUTED);
        text(canvas, tr("网易云音乐账号", "NetEase Cloud Music account"), x + 18, y + 78, 8, MUTED);
        button(canvas, x + 18, y + 113, w - 36, 23, "logout", tr("退出登录", "Sign out"), true, this::logout);
        iconButton(canvas, "close", x + w - 26, y + 7, 18, () -> accountOpen = false);
    }

    private void renderContext(Canvas canvas) {
        targets.clear();
        float w = 144;
        float h = contextItems.size() * 22f + 8f;
        contextX = Math.max(4, Math.min(panelW - w - 4, contextX));
        contextY = Math.max(4, Math.min(panelH - h - 4, contextY));
        rounded(canvas, contextX, contextY, w, h, 4, 0xFF202126);
        for (int i = 0; i < contextItems.size(); i++) {
            MenuItem item = contextItems.get(i);
            float y = contextY + 4 + i * 22;
            if (hit(contextX + 4, y, w - 8, 22)) rounded(canvas, contextX + 4, y, w - 8, 22, 3, 0xFF373940);
            icon(canvas, item.icon(), contextX + 10, y + 6, 10, TEXT);
            text(canvas, item.label(), contextX + 29, y + 7, 7.5f, TEXT);
            target(contextX + 4, y, w - 8, 22, () -> {
                contextItems.clear();
                item.action().run();
            });
        }
    }

    private void openSongMenu(Song song, int queueIndex) {
        focused = null;
        contextX = mouseX;
        contextY = mouseY;
        contextItems.clear();
        contextItems.add(new MenuItem("play", tr("播放", "Play"), () -> {
            if (queueIndex >= 0) {
                if (queueEntryMatches(queueIndex, song)) player.playQueueIndex(queueIndex);
                return;
            }
            List<Song> visible = visibleSongs();
            int index = visible.indexOf(song);
            if (index >= 0) playVisible(visible, index);
            else player.playSong(song);
        }));
        contextItems.add(new MenuItem("next", tr("下一首播放", "Play next"), () -> {
            player.addToQueue(song, true);
            notify(tr("已添加为下一首", "Added to play next"));
        }));
        contextItems.add(new MenuItem("add", tr("添加到播放队列", "Add to queue"), () -> {
            player.addToQueue(song, false);
            notify(tr("已添加到播放队列", "Added to queue"));
        }));
        contextItems.add(new MenuItem("music", tr("复制歌曲 ID", "Copy song ID"), () -> {
            minecraft.keyboardHandler.setClipboard(Long.toString(song.id()));
            notify(tr("已复制歌曲 ID", "Song ID copied"));
        }));
        if (page == Page.QUEUE) contextItems.add(new MenuItem("close", tr("从队列移除", "Remove from queue"), () -> {
            if (queueEntryMatches(queueIndex, song)) player.removeQueueIndex(queueIndex);
        }));
    }

    private void empty(Canvas canvas, float x, float y, float w, float h, String glyph, String title, String description) {
        float center = x + w * 0.5f;
        float cy = y + Math.max(20, h * 0.36f);
        icon(canvas, glyph, center - 17, cy - 30, 34, 0xFF494D56);
        String label = fit(title, Math.max(10, w - 24), 10);
        text(canvas, label, center - width(label, 10) * 0.5f, cy + 18, 10, TEXT);
        String line = fit(description, Math.max(10, w - 28), 7.5f);
        text(canvas, line, center - width(line, 7.5f) * 0.5f, cy + 37, 7.5f, MUTED);
    }

    private void button(Canvas canvas, float x, float y, float w, float h, String glyph, String label, boolean accent, Runnable action) {
        rounded(canvas, x, y, w, h, 3.5f, accent ? hit(x, y, w, h) ? 0xFFF01A34 : ACCENT : hit(x, y, w, h) ? 0xFF34363E : PANEL);
        float textW = width(label, 7.5f);
        float contentW = textW + (glyph.isBlank() ? 0 : 16);
        float start = x + (w - contentW) * 0.5f;
        if (!glyph.isBlank()) icon(canvas, glyph, start, y + (h - 10) * 0.5f, 10, TEXT);
        text(canvas, label, start + (glyph.isBlank() ? 0 : 16), y + (h - 8f) * 0.5f, 7.5f, TEXT);
        target(x, y, w, h, action);
    }

    private void iconButton(Canvas canvas, String glyph, float x, float y, float size, Runnable action) {
        if (hit(x, y, size, size)) rounded(canvas, x, y, size, size, 4, 0x20FFFFFF);
        icon(canvas, glyph, x + 4, y + 4, size - 8, TEXT);
        target(x, y, size, size, action);
    }

    private void slider(Canvas canvas, Rect bounds, float value, boolean knob) {
        rounded(canvas, bounds.getLeft(), bounds.getTop(), bounds.getWidth(), bounds.getHeight(), 1.5f, 0x40FFFFFF);
        rounded(canvas, bounds.getLeft(), bounds.getTop(), bounds.getWidth() * clamp(value), bounds.getHeight(), 1.5f, TEXT);
        if (knob || hit(bounds.getLeft(), bounds.getTop() - 5, bounds.getWidth(), 13)) rounded(canvas,
                bounds.getLeft() + bounds.getWidth() * clamp(value) - 3, bounds.getTop() - 1.5f, 6, 6, 3, TEXT);
    }

    private void drawInput(Canvas canvas, MusicTextInput input, float x, float y, float w, float h, String placeholder) {
        input.draw(canvas, x, y, w, h, focused == input, placeholder);
        target(x, y, w, h, () -> {
            focused = input;
            input.click(mouseX, false);
            draggingInput = true;
        });
    }

    private void target(float x, float y, float w, float h, Runnable action) {
        target(x, y, w, h, 0, action);
    }

    private void target(float x, float y, float w, float h, int button, Runnable action) {
        if (recordingTargets && w > 0 && h > 0) targets.add(new HitTarget(Rect.makeXYWH(x, y, w, h), button, action));
    }

    private boolean hit(float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing || scale <= 0) return true;
        mouseX = (float) (event.x() - offsetX) / scale;
        mouseY = (float) (event.y() - offsetY) / scale;
        if (lyrics != null && !loginOpen && !accountOpen) {
            if (lyricsClosedAt == 0 && elapsed(lyricsStartedAt) >= 360) lyrics.mouseClicked(mouseX, mouseY, event.button());
            return true;
        }
        if (!loginOpen && !accountOpen && !restoring && contextItems.isEmpty()
                && mouseX >= sideW && mouseY < contentH && elapsed(pageStartedAt) < 300f) return true;
        for (int i = targets.size() - 1; i >= 0; i--) {
            HitTarget target = targets.get(i);
            if (target.button() == event.button() && target.bounds().contains(mouseX, mouseY)) {
                MusicTextInput before = focused;
                draggingInput = false;
                target.action().run();
                if (!draggingInput && focused == before) focused = null;
                targets.clear();
                return true;
            }
        }
        if (!contextItems.isEmpty()) {
            contextItems.clear();
            return true;
        }
        if (loginOpen || accountOpen || restoring) return true;
        focused = null;
        if (page == Page.SETTINGS && mouseX >= sideW && mouseY < contentH) {
            settings.mouseClicked(mouseX - sideW, mouseY, event.button());
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (closing || scale <= 0) return true;
        float x = (float) (event.x() - offsetX) / scale;
        float y = (float) (event.y() - offsetY) / scale;
        if (lyrics != null && !loginOpen && !accountOpen) {
            if (lyricsClosedAt == 0) lyrics.mouseDragged(x, y);
        } else if (draggingInput && focused != null) focused.drag(x);
        else if (dragging != Drag.NONE) updateDrag(x, y);
        else if (page == Page.SETTINGS && !loginOpen && !accountOpen) settings.mouseDragged(x - sideW, y);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        releaseDrag();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (closing || loginOpen || accountOpen || restoring || scale <= 0 || !contextItems.isEmpty()) return true;
        float localX = (float) (x - offsetX) / scale;
        float localY = (float) (y - offsetY) / scale;
        if (lyrics != null) {
            if (lyricsClosedAt == 0) lyrics.mouseScrolled(localX, localY, (float) vertical);
        } else if (localX < sideW) {
            sidebarTarget = Math.max(0, Math.min(maxSidebarScroll, sidebarTarget - (float) vertical * 35f));
        } else if (page == Page.SETTINGS) {
            if (elapsed(pageStartedAt) >= 300f) settings.mouseScrolled(localX - sideW, localY, (float) vertical);
        } else if (listBounds.contains(localX, localY)) {
            scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) vertical * 44f));
            if (hasMore && scrollTarget >= maxScroll - 60f && filter.value().isBlank()) loadMore();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (!contextItems.isEmpty()) contextItems.clear();
            else if (accountOpen) accountOpen = false;
            else if (loginOpen) closeLogin();
            else if (lyrics != null) closeLyrics();
            else if (focused != null) focused = null;
            else onClose();
            return true;
        }
        if (loginOpen || accountOpen || restoring) return true;
        if (event.hasControlDownWithQuirk() && event.key() == GLFW.GLFW_KEY_F && lyrics == null) {
            focused = search;
            return true;
        }
        if (focused != null && lyrics == null) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                if (focused == search) runSearch();
                else focused = null;
                return true;
            }
            String before = filter.value();
            focused.keyPressed(event.key(), event.hasControlDownWithQuirk(), event.hasShiftDown(),
                    minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard);
            if (!filter.value().equals(before)) scroll = scrollTarget = 0;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            player.toggle();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (focused == null || closing || loginOpen || accountOpen || lyrics != null) return false;
        if (event.isAllowedChatCharacter()) {
            focused.insert(event.codepointAsString());
            if (focused == filter) scroll = scrollTarget = 0;
        }
        return true;
    }

    private void updateDrag(float x, float y) {
        if (dragging == Drag.SEEK) pendingSeek = clamp((x - seekBounds.getLeft()) / seekBounds.getWidth());
        if (dragging == Drag.VOLUME) player.setVolume(clamp((x - volumeBounds.getLeft()) / volumeBounds.getWidth()), false);
        if (dragging == Drag.SCROLL) {
            float thumbH = Math.max(24f, listBounds.getHeight() * listBounds.getHeight() / (listBounds.getHeight() + maxScroll));
            scrollTarget = maxScroll * clamp((y - listBounds.getTop() - thumbH * 0.5f) / Math.max(1, listBounds.getHeight() - thumbH));
            scroll = scrollTarget;
        }
    }

    private void releaseDrag() {
        if (dragging == Drag.SEEK && pendingSeek >= 0) player.seekToProgress(pendingSeek);
        if (dragging == Drag.VOLUME) player.setVolume(player.volume(), true);
        settings.mouseReleased();
        if (lyrics != null) lyrics.mouseReleased();
        pendingSeek = -1;
        dragging = Drag.NONE;
        draggingInput = false;
    }

    @Override
    public void onClose() {
        if (closing) return;
        releaseDrag();
        closing = true;
        closedAt = System.nanoTime();
        qrSerial++;
        requestSerial++;
        NeteaseMusicApi.cancelPendingLogin();
    }

    @Override
    public void removed() {
        releaseDrag();
        if (lyrics != null) lyrics.close();
        attached = false;
        reloadOnReturn = loading || loadingMore;
        pendingFrame = false;
        qrSerial++;
        requestSerial++;
        accountSerial++;
        targets.clear();
        contextItems.clear();
        NeteaseMusicApi.cancelPendingLogin();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void showPage(Page next) {
        releaseDrag();
        page = next;
        requestSerial++;
        loading = false;
        loadingMore = false;
        hasMore = false;
        scroll = scrollTarget = maxScroll = 0;
        pageStartedAt = System.nanoTime();
        focused = null;
        filter.clear();
        contextItems.clear();
        targets.clear();
        status = "";
    }

    private void refreshLibrary() {
        if (!NeteaseMusicApi.isLoggedIn()) {
            openLogin();
            return;
        }
        int serial = ++accountSerial;
        if (page == Page.HOME) loading = true;
        async(NeteaseMusicApi::getRecommendedPlaylists).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!attached || closing || serial != accountSerial) return;
            if (page == Page.HOME) loading = false;
            if (error != null) {
                if (page == Page.HOME) status = message(error);
                notify(message(error));
                return;
            }
            recommendations.clear();
            recommendations.addAll(result);
        }));
        async(NeteaseMusicApi::getUserPlaylists).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!attached || closing || serial != accountSerial) return;
            if (error != null) {
                notify(message(error));
                return;
            }
            library.clear();
            library.addAll(result);
        }));
    }

    private void runSearch() {
        String query = search.value().trim();
        if (query.isBlank()) return;
        showPage(Page.SEARCH);
        heading = query;
        songs.clear();
        loading = true;
        int serial = requestSerial;
        async(() -> NeteaseMusicApi.search(query, 30, 0)).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!currentRequest(serial)) return;
            loading = false;
            if (error != null) {
                status = message(error);
                return;
            }
            songs.addAll(result);
            hasMore = result.size() == 30;
        }));
    }

    private void loadDaily() {
        if (!NeteaseMusicApi.isLoggedIn()) {
            openLogin();
            return;
        }
        showPage(Page.DAILY);
        songs.clear();
        loading = true;
        int serial = requestSerial;
        async(NeteaseMusicApi::getDailyRecommendSongs).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!currentRequest(serial)) return;
            loading = false;
            if (error != null) status = message(error);
            else songs.addAll(result);
        }));
    }

    private void loadPlaylist(Playlist entry) {
        showPage(Page.PLAYLIST);
        playlist = entry;
        songs.clear();
        loading = true;
        int serial = requestSerial;
        async(() -> NeteaseMusicApi.getPlaylistDetail(entry.id(), 80, 0)).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!currentRequest(serial)) return;
            loading = false;
            if (error != null) {
                status = message(error);
                return;
            }
            songs.addAll(result);
            hasMore = result.size() == 80 && (entry.trackCount() <= 0 || songs.size() < entry.trackCount());
        }));
    }

    private void loadMore() {
        if (loading || loadingMore || !hasMore || !filter.value().isBlank()) return;
        if (page != Page.SEARCH && page != Page.PLAYLIST) return;
        loadingMore = true;
        int serial = requestSerial;
        int offset = songs.size();
        int limit = page == Page.PLAYLIST ? 80 : 30;
        Playlist source = playlist;
        String query = heading;
        boolean playlistPage = page == Page.PLAYLIST;
        async(() -> playlistPage ? NeteaseMusicApi.getPlaylistDetail(source.id(), limit, offset)
                : NeteaseMusicApi.search(query, limit, offset)).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!currentRequest(serial)) return;
            loadingMore = false;
            if (error != null) {
                notify(message(error));
                return;
            }
            songs.addAll(result);
            hasMore = result.size() == limit && (!playlistPage || source.trackCount() <= 0 || songs.size() < source.trackCount());
        }));
    }

    private boolean currentRequest(int serial) {
        return attached && !closing && serial == requestSerial;
    }

    private void openLogin() {
        focused = null;
        loginOpen = true;
        accountOpen = false;
        contextItems.clear();
        if (!qrBusy && !restoring) startQrLogin();
    }

    private void closeLogin() {
        loginOpen = false;
        qrSerial++;
        qrBusy = false;
        NeteaseMusicApi.cancelPendingLogin();
    }

    private void startQrLogin() {
        if (NeteaseMusicApi.isLoggedIn() || restoring) return;
        NeteaseMusicApi.cancelPendingLogin();
        int serial = ++qrSerial;
        qr = null;
        qrBusy = true;
        qrCode = 0;
        qrStatus = tr("正在生成二维码", "Creating QR code");
        async(NeteaseMusicApi::createQrLogin).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!attached || closing || serial != qrSerial) return;
            if (error != null) {
                qrBusy = false;
                qrStatus = message(error);
                return;
            }
            qr = result;
            qrStatus = tr("等待扫码", "Waiting for scan");
            NeteaseMusicCovers.preload(qr.qrImage());
            pollQr(serial, result.key());
        }));
    }

    private void pollQr(int serial, String key) {
        CompletableFuture.supplyAsync(() -> {
            if (!attached || serial != qrSerial) return null;
            try {
                return NeteaseMusicApi.checkQrLogin(key);
            } catch (Exception error) {
                throw new java.util.concurrent.CompletionException(error);
            }
        }, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS, IO)).whenComplete((result, error) -> minecraft.execute(() -> {
            if (!attached || closing || serial != qrSerial || !loginOpen) return;
            if (error != null || result == null) {
                qrBusy = false;
                qrStatus = error == null ? tr("登录已取消", "Login cancelled") : message(error);
                return;
            }
            qrCode = result.code();
            qrStatus = switch (result.code()) {
                case 800 -> tr("二维码已过期，请刷新", "QR expired. Refresh to continue");
                case 801 -> tr("等待扫码", "Waiting for scan");
                case 802 -> tr("已扫码，请在手机上确认", "Scanned. Confirm on your phone");
                case 803 -> tr("登录成功", "Signed in");
                default -> tr("登录暂不可用，请重试", "Login unavailable. Try again");
            };
            if (result.session() != null) {
                closeLogin();
                showPage(Page.HOME);
                refreshLibrary();
            } else if (result.code() == 800 || result.code() < 0) qrBusy = false;
            else pollQr(serial, key);
        }));
    }

    private void logout() {
        closeLogin();
        accountOpen = false;
        accountSerial++;
        NeteaseMusicApi.logout();
        library.clear();
        recommendations.clear();
        songs.clear();
        playlist = null;
        showPage(Page.HOME);
        openLogin();
    }

    private void playVisible(List<Song> visible, int index) {
        if (index < 0 || index >= visible.size()) return;
        Song selected = visible.get(index);
        Song current = player.currentSong();
        int queueIndex = page == Page.QUEUE ? queueIndexAt(visible, index) : -1;
        if (current != null && current.id() == selected.id()
                && (page != Page.QUEUE || queueIndex == player.currentIndex())) {
            player.toggle();
            return;
        }
        if (page == Page.QUEUE) {
            if (queueIndex >= 0) player.playQueueIndex(queueIndex);
        } else {
            player.setPlaylist(visible, index);
            player.playQueueIndex(index);
        }
    }

    private int queueIndexAt(List<Song> visible, int index) {
        List<Song> queue = player.getQueue();
        int match = 0;
        for (int i = 0; i < queue.size() && match <= index; i++) {
            if (queue.get(i).equals(visible.get(match))) {
                if (match == index) return i;
                match++;
            }
        }
        return -1;
    }

    private boolean queueEntryMatches(int index, Song song) {
        List<Song> queue = player.getQueue();
        return index >= 0 && index < queue.size() && queue.get(index).equals(song);
    }

    private void playAll(boolean shuffle) {
        List<Song> visible = visibleSongs();
        if (visible.isEmpty()) return;
        if (shuffle) {
            for (int i = 0; i < 3 && player.playbackMode() != MusicPlaybackService.PlaybackMode.RANDOM; i++) player.cyclePlaybackMode();
        }
        int index = shuffle ? java.util.concurrent.ThreadLocalRandom.current().nextInt(visible.size()) : 0;
        player.setPlaylist(visible, index);
        player.playQueueIndex(index);
    }

    private void openLyrics() {
        if (player.currentSong() == null) return;
        releaseDrag();
        focused = null;
        contextItems.clear();
        if (lyrics != null) lyrics.close();
        lyrics = new MusicLyricsView();
        lyricsStartedAt = System.nanoTime();
        lyricsClosedAt = 0;
        targets.clear();
    }

    private void closeLyrics() {
        if (lyrics == null || lyricsClosedAt != 0) return;
        lyrics.mouseReleased();
        lyricsClosedAt = System.nanoTime();
    }

    private float playbackProgress() {
        long duration = player.totalDurationMs();
        return duration <= 0 ? 0 : clamp(player.positionMs() / (float) duration);
    }

    private String modeIcon() {
        return switch (player.playbackMode()) {
            case LOOP -> "repeat_one";
            case LIST -> "repeat";
            case RANDOM -> "shuffle";
        };
    }

    private void toggleMute() {
        if (player.volume() > 0.001f) {
            previousVolume = player.volume();
            player.setVolume(0);
        } else player.setVolume(Math.max(0.05f, previousVolume));
    }

    private void notify(String message) {
        toast = message;
        toastUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    }

    private static String tr(String chinese, String english) {
        return Config.isChinese ? chinese : english;
    }

    private static String formatCount(long count) {
        if (Config.isChinese && count >= 10000) return String.format(Locale.ROOT, "%.1f万", count / 10000f);
        if (!Config.isChinese && count >= 1000) return String.format(Locale.ROOT, "%.1fk", count / 1000f);
        return Long.toString(count);
    }

    private static String message(Throwable error) {
        return MusicFeedback.error(error);
    }

    private static float elapsed(long start) {
        return start == 0 ? Float.MAX_VALUE : (System.nanoTime() - start) / 1_000_000f;
    }

    private static float ease(float progress) {
        float remaining = 1f - clamp(progress);
        return 1f - remaining * remaining * remaining;
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    private static <T> CompletableFuture<T> async(CheckedTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception error) {
                throw new java.util.concurrent.CompletionException(error);
            }
        }, IO);
    }

    private interface CheckedTask<T> {
        T get() throws Exception;
    }

    private enum Page { HOME, SEARCH, PLAYLIST, DAILY, QUEUE, SETTINGS }
    private enum Drag { NONE, SEEK, VOLUME, SCROLL }
    private record HitTarget(Rect bounds, int button, Runnable action) { }
    private record MenuItem(String icon, String label, Runnable action) { }
}
