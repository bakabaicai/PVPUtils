package com.pvp_utils.client.NeteaseMusic;

import com.mojang.blaze3d.platform.InputConstants;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_END;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_HOME;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;

public class NeteaseMusicScreen extends Screen {
    private static final long ENTER_DURATION_MS = 320L;
    private static final long EXIT_DURATION_MS = 260L;
    private static final long PAGE_TRANSITION_MS = 420L;
    private static final int SIDEBAR_WIDTH = 170;
    private static final int PLAYER_HEIGHT = 78;
    private static final int GRID_GAP = 34;
    private static final int GRID_TEXT_HEIGHT = 54;
    private static final ExecutorService IO = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-NeteaseMusicUi");
        thread.setDaemon(true);
        return thread;
    });

    private final Screen parent;
    private final List<Song> songs = new ArrayList<>();
    private final List<Song> fullPlaylistSongs = new ArrayList<>();
    private final List<Playlist> playlists = new ArrayList<>();
    private final List<Playlist> recommendedPlaylists = new ArrayList<>();
    private final List<Song> transitionSongs = new ArrayList<>();
    private final List<Playlist> transitionRecommendedPlaylists = new ArrayList<>();
    private final Map<String, Float> sidebarSelectAnimations = new HashMap<>();
    private final Map<String, Float> coverHoverAnimations = new HashMap<>();
    private long openStartedAt;
    private long closeStartedAt;
    private boolean closing;
    private boolean loading;
    private boolean draggingProgress;
    private boolean draggingListSlider;
    private SliderTarget draggingSliderTarget = SliderTarget.NONE;
    private LoginMode loginMode = LoginMode.QR;
    private Focus focus = Focus.NONE;
    private Focus draggingTextSelection = Focus.NONE;
    private String query = "";
    private String playlistSearchQuery = "";
    private String phone = "";
    private String password = "";
    private int queryCursor;
    private int querySelection = -1;
    private int playlistSearchCursor;
    private int playlistSearchSelection = -1;
    private int phoneCursor;
    private int phoneSelection = -1;
    private int passwordCursor;
    private int passwordSelection = -1;
    private String statusText = "Waiting for login";
    private int firstSongIndex;
    private int firstPlaylistIndex;
    private float visualFirstSongIndex;
    private float visualFirstPlaylistIndex;
    private int selectedPlaylistIndex = -1;
    private Playlist currentPlaylist;
    private ViewMode viewMode = ViewMode.HOME;
    private long pageTransitionStartedAt;
    private ViewMode transitionViewMode = ViewMode.HOME;
    private Playlist transitionCurrentPlaylist;
    private int transitionFirstSongIndex;
    private int transitionFirstPlaylistIndex;
    private float transitionVisualFirstSongIndex;
    private float transitionVisualFirstPlaylistIndex;
    private boolean renderingTransitionSnapshot;
    private float pendingProgress = -1.0F;
    private NeteaseMusicApi.QrLogin qrLogin;
    private boolean qrPolling;
    private int qrSerial;
    private int searchInputX;
    private int searchInputY = 14;
    private int searchInputW = SIDEBAR_WIDTH - 24;
    private int searchInputH = 22;
    private int playlistSearchInputX;
    private int playlistSearchInputY;
    private int playlistSearchInputW;
    private int playlistSearchInputH;
    private int phoneInputX;
    private int passwordInputX;
    private int lastGridSliderX;
    private int lastGridSliderY;
    private int lastGridSliderH;
    private int lastGridSliderColumns;
    private int lastGridSliderVisibleCards;
    private int playlistLoadSerial;

    public NeteaseMusicScreen(Screen parent) {
        super(Component.literal("Netease Music"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (openStartedAt == 0L) {
            openStartedAt = System.currentTimeMillis();
            NeteaseMusicApi.restoreSession();
            if (NeteaseMusicApi.isLoggedIn()) {
                loadRecommendedPlaylists();
                loadUserPlaylists();
            } else {
                statusText = Config.isChinese ? "请先登录网易云音乐" : "Please login to Netease Music";
                startQrLogin();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        AnimationState animation = animationState();
        if (closing && animation.done()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }

        int alpha = Math.round(255.0F * animation.alpha());
        updateScrollAnimation();
        renderBackground(graphics, alpha);
        renderSidebar(graphics, mouseX, mouseY, alpha);
        ContentTransition transition = contentTransition();
        renderContentFrame(graphics, mouseX, mouseY, Math.round(alpha * transition.alpha()), transition.scale(), transition.oldPage());
        renderPlayer(graphics, mouseX, mouseY, alpha);
        renderCloseButton(graphics, mouseX, mouseY, alpha);
        renderSkiaTextLayer(graphics, alpha, transition);

        if (!NeteaseMusicApi.isLoggedIn()) {
            renderLoginGate(graphics, mouseX, mouseY, alpha);
        }
    }

    private void renderBackground(GuiGraphics graphics, int alpha) {
        graphics.fill(0, 0, width, height, withAlpha(0x111315, Math.round(alpha * 0.68F)));
        graphics.fill(0, 0, width, height, withAlpha(0x07120E, Math.round(alpha * 0.25F)));
        graphics.fill(0, 0, SIDEBAR_WIDTH, height, withAlpha(0x1E1D17, Math.round(alpha * 0.64F)));
        graphics.fill(0, height - PLAYER_HEIGHT, width, height, withAlpha(0x111315, Math.round(alpha * 0.78F)));
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        searchInputX = 12;
        searchInputY = 14;
        searchInputW = SIDEBAR_WIDTH - 24;
        searchInputH = 22;
        drawInput(graphics, searchInputX, searchInputY, searchInputW, searchInputH, mouseX, mouseY, query,
                Config.isChinese ? "搜索..." : "Search...", Focus.SEARCH, alpha);

        drawSidebarItem(graphics, 14, 74, SIDEBAR_WIDTH - 28, "Home", viewMode == ViewMode.HOME, mouseX, mouseY, alpha, "home");

        int y = 136;
        if (!NeteaseMusicApi.isLoggedIn()) {
        } else if (playlists.isEmpty()) {
        } else {
            for (int i = 0; i < Math.min(12, playlists.size()); i++) {
                Playlist playlist = playlists.get(i);
                drawSidebarItem(graphics, 24, y, SIDEBAR_WIDTH - 36, trim(playlist.name(), 17), selectedPlaylistIndex == i, mouseX, mouseY, alpha, "playlist:" + playlist.id());
                y += 28;
            }
        }

        NeteaseMusicApi.LoginSession session = NeteaseMusicApi.currentSession();
        String name = session == null ? Minecraft.getInstance().getUser().getName() : session.nickname();
        int avatarY = height - PLAYER_HEIGHT - 34;
        graphics.fill(0, height - PLAYER_HEIGHT - 50, SIDEBAR_WIDTH, height - PLAYER_HEIGHT, withAlpha(0x0D1412, Math.round(alpha * 0.55F)));
        if (session != null && !session.avatarUrl().isBlank()) {
            renderCover(graphics, session.avatarUrl(), 14, avatarY, 18, alpha);
        } else {
            graphics.fill(14, avatarY, 32, avatarY + 18, withAlpha(0xDDEBFF, alpha));
        }
    }

    private void drawSidebarItem(GuiGraphics graphics, int x, int y, int w, String text, boolean selected, int mouseX, int mouseY, int alpha, String key) {
        boolean hovered = hit(x - 6, y - 6, w, 22, mouseX, mouseY);
        float target = selected ? 1.0F : hovered ? 0.45F : 0.0F;
        float current = sidebarSelectAnimations.getOrDefault(key, 0.0F);
        current = approach(current, target, 0.18F);
        if (current < 0.01F) {
            sidebarSelectAnimations.remove(key);
        } else {
            sidebarSelectAnimations.put(key, current);
        }
        int bgAlpha = Math.round(alpha * 0.58F * current);
        graphics.fill(x - 6, y - 6, x + w, y + 16, withAlpha(selected ? 0x30342D : 0x242830, bgAlpha));
        if (selected) {
            graphics.fill(x - 6, y - 2, x - 3, y + 12, withAlpha(0xE60012, Math.round(alpha * current)));
        }
    }

    private void renderMain(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        if (viewMode == ViewMode.HOME) {
            renderHomePlaylists(graphics, mouseX, mouseY, alpha);
            return;
        }
        if (viewMode == ViewMode.PLAYLIST) {
            renderPlaylistDetail(graphics, mouseX, mouseY, alpha);
            return;
        }
        renderSongGrid(graphics, mouseX, mouseY, alpha);
    }

    private void renderMainTransformed(GuiGraphics graphics, int mouseX, int mouseY, int alpha, float scale) {
        graphics.pose().pushMatrix();
        applyContentScale(graphics, scale);
        try {
            renderMain(graphics, mouseX, mouseY, alpha);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void renderContentFrame(GuiGraphics graphics, int mouseX, int mouseY, int alpha, float scale, boolean oldPage) {
        if (oldPage) {
            withTransitionSnapshot(() -> renderMainTransformed(graphics, mouseX, mouseY, alpha, scale));
            return;
        }
        renderMainTransformed(graphics, mouseX, mouseY, alpha, scale);
    }

    private void applyContentScale(GuiGraphics graphics, float scale) {
        if (Math.abs(scale - 1.0F) < 0.001F) {
            return;
        }
        float centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2.0F;
        float centerY = (height - PLAYER_HEIGHT) / 2.0F;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);
    }

    private void renderHomePlaylists(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        int contentX = SIDEBAR_WIDTH + 22;
        int contentY = 24;
        int gridX = contentX;
        int gridY = contentY + 58;
        int availableW = Math.max(1, width - gridX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int cover = cardW;
        int rowH = cover + GRID_TEXT_HEIGHT;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH);

        if (recommendedPlaylists.isEmpty()) {
            String text = loading ? (Config.isChinese ? "加载中..." : "Loading...") : (NeteaseMusicApi.isLoggedIn() ? "No playlists" : "Login required");
            graphics.drawCenteredString(font, text, gridX + availableW / 2, gridY + 70, withAlpha(0xB8C0D4, alpha));
            return;
        }

        GridScroll scroll = gridScroll(visualFirstPlaylistIndex, columns, rowH, maxPlaylistGridStart(columns, visibleRows * columns));
        int visualBase = scroll.base();
        float rowOffset = scroll.offset();
        int visibleCards = (visibleRows + 1) * columns;
        preloadPlaylistCovers(visualBase, visibleCards + columns * 2);
        int clipBottom = height - PLAYER_HEIGHT - 16;
        graphics.enableScissor(gridX - 8, gridY, gridX + availableW + 8, clipBottom);
        try {
            for (int slot = 0; slot < visibleCards; slot++) {
                int index = visualBase + slot;
                if (index >= recommendedPlaylists.size()) {
                    break;
                }
                Playlist playlist = recommendedPlaylists.get(index);
                int col = slot % columns;
                int row = slot / columns;
                int x = gridX + col * (cardW + GRID_GAP);
                int y = Math.round(gridY + row * rowH - rowOffset);
                if (y > clipBottom || y + cover + 30 < gridY) {
                    continue;
                }
                boolean hovered = hit(x, y, cardW, cover + 30, mouseX, mouseY);
                graphics.fill(x - 5, y - 5, x + cardW + 5, y + cover + 33, withAlpha(hovered ? 0x252A31 : 0x000000, hovered ? Math.round(alpha * 0.70F) : 0));
                renderRoundedCover(graphics, playlist.coverUrl(), x, y, cover, alpha, 15, hoverProgress("playlist:" + playlist.id(), hovered), 0x0D1512);
            }
        } finally {
            graphics.disableScissor();
        }
        renderPlaylistGridSlider(graphics, gridX + availableW + 8, gridY, visibleRows * rowH - 12, columns, visibleRows * columns, mouseX, mouseY, alpha);
    }

    private void renderPlaylistDetail(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        float appear = contentAppearProgress();
        int localAlpha = Math.round(alpha * appear);
        int slide = Math.round((1.0F - appear) * 18.0F);
        int contentX = SIDEBAR_WIDTH + 28;
        int contentY = 24 + slide;
        Playlist playlist = currentPlaylist;
        int cover = playlistCoverSize();
        if (playlist != null) {
            graphics.fill(contentX - 10, contentY - 10, contentX + cover + 10, contentY + cover + 10, withAlpha(0x000000, Math.round(localAlpha * 0.22F)));
            renderCover(graphics, playlist.coverUrl(), contentX, contentY, cover, localAlpha);
            int infoX = contentX + cover + 24;
            int buttonY = contentY + cover - 46;
            drawRedButton(graphics, infoX, buttonY, 86, 26, mouseX, mouseY, localAlpha);
            drawRedButton(graphics, infoX + 102, buttonY, 112, 26, mouseX, mouseY, localAlpha);
            searchInputX = infoX + 232;
            searchInputY = buttonY + 1;
            searchInputW = Math.max(128, Math.min(220, width - searchInputX - 28));
            searchInputH = 24;
            playlistSearchInputX = searchInputX;
            playlistSearchInputY = searchInputY;
            playlistSearchInputW = searchInputW;
            playlistSearchInputH = searchInputH;
            drawInput(graphics, playlistSearchInputX, playlistSearchInputY, playlistSearchInputW, playlistSearchInputH, mouseX, mouseY, playlistSearchQuery,
                    Config.isChinese ? "搜索歌曲..." : "Filter songs...", Focus.PLAYLIST_SEARCH, localAlpha);
        }
        int listX = contentX;
        int listY = contentY + cover + 48;
        int listW = width - listX - 26;
        int rowH = 42;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - listY - 10) / rowH);
        if (songs.isEmpty()) {
            graphics.drawCenteredString(font, loading ? "Loading..." : "No songs", listX + listW / 2, listY + 50, withAlpha(0xB8C0D4, localAlpha));
            return;
        }
        float listVisual = Math.max(0.0F, Math.min(Math.max(0, songs.size() - visibleRows), visualFirstSongIndex));
        int visualBase = (int) Math.floor(listVisual);
        float rowOffset = (listVisual - visualBase) * rowH;
        preloadSongCovers(visualBase, visibleRows + 8);
        int clipBottom = height - PLAYER_HEIGHT - 8;
        graphics.enableScissor(listX, listY, listX + listW, clipBottom);
        try {
            for (int row = 0; row < visibleRows + 2; row++) {
                int index = visualBase + row;
                if (index >= songs.size()) {
                    break;
                }
                Song song = songs.get(index);
                int y = Math.round(listY + row * rowH - rowOffset);
                if (y > clipBottom || y + rowH < listY) {
                    continue;
                }
                float rowProgress = clamp((appear * 1.18F) - row * 0.025F);
                int rowAlpha = Math.round(localAlpha * rowProgress);
                int rowSlide = Math.round((1.0F - rowProgress) * 10.0F);
                boolean current = song.equals(MusicPlaybackService.INSTANCE.currentSong());
                boolean hovered = hit(listX, y + rowSlide, listW, rowH - 5, mouseX, mouseY);
                int bgAlpha = current ? Math.round(rowAlpha * 0.46F) : hovered ? Math.round(rowAlpha * 0.36F) : Math.round(rowAlpha * 0.22F);
                graphics.fill(listX, y + rowSlide, listX + listW, y + rowSlide + rowH - 5, withAlpha(current ? 0x263754 : 0x2A2A2A, bgAlpha));
                renderCover(graphics, song.image(), listX + 58, y + rowSlide + 6, 30, rowAlpha);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderSongGrid(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        int contentX = SIDEBAR_WIDTH + 22;
        int contentY = 24;
        int gridX = contentX;
        int gridY = contentY + 48;
        int availableW = Math.max(1, width - gridX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int cover = cardW;
        int rowH = cover + GRID_TEXT_HEIGHT;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH);
        if (songs.isEmpty()) {
            graphics.drawCenteredString(font, loading ? "Loading..." : "No songs", gridX + availableW / 2, gridY + 70, withAlpha(0xB8C0D4, alpha));
            return;
        }
        GridScroll scroll = gridScroll(visualFirstSongIndex, columns, rowH, maxGridStart(columns, visibleRows * columns));
        int visualBase = scroll.base();
        float rowOffset = scroll.offset();
        int visibleCards = (visibleRows + 1) * columns;
        preloadSongCovers(visualBase, visibleCards + columns * 2);
        int clipBottom = height - PLAYER_HEIGHT - 16;
        graphics.enableScissor(gridX - 8, gridY, gridX + availableW + 8, clipBottom);
        try {
            for (int slot = 0; slot < visibleCards; slot++) {
                int index = visualBase + slot;
                if (index >= songs.size()) break;
                Song song = songs.get(index);
                int col = slot % columns;
                int row = slot / columns;
                int x = gridX + col * (cardW + GRID_GAP);
                int y = Math.round(gridY + row * rowH - rowOffset);
                if (y > clipBottom || y + cover + 30 < gridY) {
                    continue;
                }
                boolean current = song.equals(MusicPlaybackService.INSTANCE.currentSong());
                boolean hovered = hit(x, y, cardW, cover + 30, mouseX, mouseY);
                graphics.fill(x - 5, y - 5, x + cardW + 5, y + cover + 33, withAlpha(current ? 0x1D355E : hovered ? 0x252A31 : 0x000000, current || hovered ? Math.round(alpha * 0.70F) : 0));
                renderRoundedCover(graphics, song.image(), x, y, cover, alpha, 15, hoverProgress("song:" + song.id(), hovered), 0x0D1512);
            }
        } finally {
            graphics.disableScissor();
        }
        renderGridSlider(graphics, gridX + availableW + 8, gridY, visibleRows * rowH - 12, columns, visibleRows * columns, mouseX, mouseY, alpha);
    }

    private void renderPlayer(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song current = player.currentSong();
        int y = height - PLAYER_HEIGHT;
        if (current != null) {
            renderCover(graphics, current.image(), SIDEBAR_WIDTH + 44, y + 14, 48, alpha);
        } else {
        }

        int centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2;
        drawIconButton(graphics, centerX - 72, y + 18, 42, 22, mouseX, mouseY, "\uE045", alpha);
        drawIconButton(graphics, centerX - 22, y + 16, 44, 26, mouseX, mouseY, player.isPlaying() ? "\uE034" : "\uE037", alpha);
        drawIconButton(graphics, centerX + 30, y + 18, 42, 22, mouseX, mouseY, "\uE044", alpha);
        drawIconButton(graphics, centerX + 88, y + 18, 42, 22, mouseX, mouseY, playbackModeIcon(player.playbackMode()), alpha);

        int progressX = centerX - 160;
        int progressY = y + 58;
        int progressW = 320;
        renderProgress(graphics, progressX, progressY, progressW, alpha);
    }

    private void renderProgress(GuiGraphics graphics, int x, int y, int w, int alpha) {
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        long total = player.totalDurationMs();
        float progress = draggingProgress && pendingProgress >= 0.0F ? pendingProgress : total <= 0L ? 0.0F : clamp(player.positionMs() / (float) total);
        graphics.fill(x, y, x + w, y + 3, withAlpha(0xFFFFFF, Math.round(alpha * 0.35F)));
        graphics.fill(x, y, x + Math.round(w * progress), y + 3, withAlpha(0xFFFFFF, alpha));
    }

    private void renderLoginGate(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        graphics.fill(0, 0, width, height, withAlpha(0x000000, Math.round(alpha * 0.62F)));
        int w = 320;
        int h = loginMode == LoginMode.QR ? 210 : 190;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x - 8, y - 8, x + w + 8, y + h + 8, withAlpha(0x000000, Math.round(alpha * 0.35F)));
        graphics.fill(x, y, x + w, y + h, withAlpha(0x14181F, alpha));
        graphics.fill(x, y, x + w, y + 2, withAlpha(0xD63B35, alpha));
        graphics.drawCenteredString(font, "Netease Music Login", x + w / 2, y + 14, withAlpha(0xFFFFFF, alpha));
        graphics.drawCenteredString(font, trim(statusText, 42), x + w / 2, y + 32, withAlpha(loading ? 0xE6C45B : 0xB8C0D4, alpha));
        drawButton(graphics, x + 62, y + 52, 88, 22, mouseX, mouseY, "QR", alpha);
        drawButton(graphics, x + 170, y + 52, 88, 22, mouseX, mouseY, "Password", alpha);

        if (loginMode == LoginMode.QR) {
            renderQrLogin(graphics, x, y, w, mouseX, mouseY, alpha);
        } else {
            phoneInputX = x + 38;
            passwordInputX = x + 38;
            drawInput(graphics, phoneInputX, y + 86, w - 76, 22, mouseX, mouseY, phone, Config.isChinese ? "手机号" : "Phone", Focus.PHONE, alpha);
            drawInput(graphics, passwordInputX, y + 116, w - 76, 22, mouseX, mouseY, "*".repeat(password.length()), Config.isChinese ? "密码" : "Password", Focus.PASSWORD, alpha);
            drawButton(graphics, x + 106, y + 150, 108, 24, mouseX, mouseY, loading ? "..." : "Login", alpha);
        }
    }

    private void renderQrLogin(GuiGraphics graphics, int x, int y, int w, int mouseX, int mouseY, int alpha) {
        int qrSize = 96;
        int qrX = x + 42;
        int qrY = y + 88;
        graphics.fill(qrX, qrY, qrX + qrSize, qrY + qrSize, withAlpha(0xFFFFFF, alpha));
        if (qrLogin != null && !qrLogin.qrImage().isBlank()) {
            Identifier texture = NeteaseMusicCovers.texture(qrLogin.qrImage());
            if (texture != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, qrX + 4, qrY + 4, 0f, 0f, qrSize - 8, qrSize - 8, 96, 96, 96, 96);
            }
        } else {
            graphics.drawCenteredString(font, "QR", qrX + qrSize / 2, qrY + 42, withAlpha(0x111111, alpha));
        }
        graphics.drawString(font, Config.isChinese ? "使用网易云音乐扫码" : "Scan with Netease app", qrX + qrSize + 20, qrY + 12, withAlpha(0xFFFFFF, alpha), false);
        drawButton(graphics, qrX + qrSize + 20, qrY + 42, 110, 24, mouseX, mouseY, qrPolling ? "Waiting" : "Refresh QR", alpha);
    }

    private void renderGridSlider(GuiGraphics graphics, int x, int y, int h, int columns, int visibleCards, int mouseX, int mouseY, int alpha) {
        int maxStart = maxGridStart(columns, visibleCards);
        if (maxStart <= 0) {
            return;
        }
        lastGridSliderX = x;
        lastGridSliderY = y;
        lastGridSliderH = h;
        lastGridSliderColumns = columns;
        lastGridSliderVisibleCards = visibleCards;
        int knobH = Math.max(28, Math.round(h * (visibleCards / (float) songs.size())));
        int knobY = y + Math.round((h - knobH) * (visualFirstSongIndex / (float) maxStart));
        boolean hovered = hit(x - 4, y, 10, h, mouseX, mouseY);
        graphics.fill(x, y, x + 3, y + h, withAlpha(0xFFFFFF, Math.round(alpha * 0.16F)));
        graphics.fill(x - 1, knobY, x + 4, knobY + knobH, withAlpha(hovered || draggingSliderTarget == SliderTarget.SONG_GRID ? 0xFFFFFF : 0x8F98AA, alpha));
    }

    private void renderPlaylistGridSlider(GuiGraphics graphics, int x, int y, int h, int columns, int visibleCards, int mouseX, int mouseY, int alpha) {
        int maxStart = maxPlaylistGridStart(columns, visibleCards);
        if (maxStart <= 0) {
            return;
        }
        lastGridSliderX = x;
        lastGridSliderY = y;
        lastGridSliderH = h;
        lastGridSliderColumns = columns;
        lastGridSliderVisibleCards = visibleCards;
        int knobH = Math.max(28, Math.round(h * (visibleCards / (float) recommendedPlaylists.size())));
        int knobY = y + Math.round((h - knobH) * (visualFirstPlaylistIndex / (float) maxStart));
        boolean hovered = hit(x - 4, y, 10, h, mouseX, mouseY);
        graphics.fill(x, y, x + 3, y + h, withAlpha(0xFFFFFF, Math.round(alpha * 0.16F)));
        graphics.fill(x - 1, knobY, x + 4, knobY + knobH, withAlpha(hovered || draggingSliderTarget == SliderTarget.PLAYLIST_GRID ? 0xFFFFFF : 0x8F98AA, alpha));
    }

    private void renderCloseButton(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        int x = width - 30;
        int y = 12;
        boolean hovered = hit(x, y, 18, 18, mouseX, mouseY);
        graphics.fill(x, y, x + 18, y + 18, withAlpha(hovered ? 0xE5484D : 0x252B35, alpha));
        graphics.drawCenteredString(font, "x", x + 9, y + 5, withAlpha(0xFFFFFF, alpha));
    }

    private void drawInput(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, String value, String placeholder, Focus field, int alpha) {
        boolean focused = focus == field;
        boolean hovered = hit(x, y, w, h, mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, withAlpha(focused || hovered ? 0x2B313C : 0x20242A, alpha));
        if (value.isBlank() && !focused) {
            graphics.drawString(font, trim(placeholder, 32), x + 8, y + Math.max(6, h / 2 - 4), withAlpha(0x7E8799, alpha), false);
            return;
        }
        int selA = selectionAnchor(field);
        int cursor = cursor(field);
        int start = Math.min(selA < 0 ? cursor : selA, cursor);
        int end = Math.max(selA < 0 ? cursor : selA, cursor);
        if (focused && start != end) {
            int sx = x + 8 + font.width(value.substring(0, Math.min(start, value.length())));
            int ex = x + 8 + font.width(value.substring(0, Math.min(end, value.length())));
            graphics.fill(sx, y + 3, ex, y + h - 3, withAlpha(0x57C7FF, Math.round(alpha * 0.42F)));
        }
        graphics.drawString(font, trim(value, Math.max(10, w / 6)), x + 8, y + Math.max(6, h / 2 - 4), withAlpha(0xFFFFFF, alpha), false);
        if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
            int caretX = x + 8 + font.width(value.substring(0, Math.min(cursor, value.length())));
            graphics.fill(caretX, y + 4, caretX + 1, y + h - 4, withAlpha(0xFFFFFF, alpha));
        }
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, String text, int alpha) {
        boolean hovered = hit(x, y, w, h, mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, withAlpha(hovered ? 0x3A4558 : 0x262D3A, alpha));
        graphics.drawCenteredString(font, text, x + w / 2, y + Math.max(5, h / 2 - 4), withAlpha(0xFFFFFF, alpha));
    }

    private void drawIconButton(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, String icon, int alpha) {
    }

    private void drawRedButton(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, int alpha) {
        boolean hovered = hit(x, y, w, h, mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, withAlpha(hovered ? 0xF0192D : 0xD80E22, alpha));
    }

    private void drawCircleButton(GuiGraphics graphics, int x, int y, int size, int mouseX, int mouseY, String text, int alpha) {
        boolean hovered = hit(x, y, size, size, mouseX, mouseY);
        int color = hovered ? 0x4A4A4A : 0x303030;
        graphics.fill(x, y + 6, x + size, y + size - 6, withAlpha(color, Math.round(alpha * 0.72F)));
        graphics.fill(x + 6, y, x + size - 6, y + size, withAlpha(color, Math.round(alpha * 0.72F)));
        graphics.drawCenteredString(font, text, x + size / 2, y + 8, withAlpha(0xBFC2C7, alpha));
    }

    private void renderCover(GuiGraphics graphics, String url, int x, int y, int size, int alpha) {
        graphics.fill(x, y, x + size, y + size, withAlpha(0x273244, alpha));
        Identifier texture = NeteaseMusicCovers.texture(url);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size,
                    NeteaseMusicCovers.TEXTURE_SIZE, NeteaseMusicCovers.TEXTURE_SIZE,
                    NeteaseMusicCovers.TEXTURE_SIZE, NeteaseMusicCovers.TEXTURE_SIZE,
                    withAlpha(0xFFFFFF, alpha));
            return;
        }
        graphics.drawCenteredString(font, "Music", x + size / 2, y + size / 2 - 4, withAlpha(0x57C7FF, alpha));
    }

    private void renderRoundedCover(GuiGraphics graphics, String url, int x, int y, int size, int alpha, int radius, float hoverProgress, int maskColor) {
        float scale = lerp(1.0F, 1.055F, hoverProgress);
        float centerX = x + size / 2.0F;
        float centerY = y + size / 2.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);
        try {
            renderCover(graphics, url, x, y, size, alpha);
            maskRoundedCorners(graphics, x, y, size, radius, maskColor, alpha);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void maskRoundedCorners(GuiGraphics graphics, int x, int y, int size, int radius, int color, int alpha) {
        int r = Math.max(1, radius);
        int mask = withAlpha(color, alpha);
        for (int dy = 0; dy < r; dy++) {
            for (int dx = 0; dx < r; dx++) {
                float ox = r - dx - 0.5F;
                float oy = r - dy - 0.5F;
                if (ox * ox + oy * oy <= r * r) {
                    continue;
                }
                graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, mask);
                graphics.fill(x + size - dx - 1, y + dy, x + size - dx, y + dy + 1, mask);
                graphics.fill(x + dx, y + size - dy - 1, x + dx + 1, y + size - dy, mask);
                graphics.fill(x + size - dx - 1, y + size - dy - 1, x + size - dx, y + size - dy, mask);
            }
        }
    }

    private void renderSkiaTextLayer(GuiGraphics graphics, int alpha, ContentTransition transition) {
        Canvas canvas = SkiaRenderer.beginRegion(0, 0, width, height);
        if (canvas == null) {
            return;
        }
        try {
            canvas.save();
            try {
                applyContentScale(canvas, transition.scale());
                int contentAlpha = Math.round(alpha * transition.alpha());
                if (transition.oldPage()) {
                    withTransitionSnapshot(() -> renderCurrentSkiaText(canvas, contentAlpha));
                } else {
                    renderCurrentSkiaText(canvas, contentAlpha);
                }
            } finally {
                canvas.restore();
            }
            renderPlayerSkiaText(canvas, alpha);
            renderSidebarSkiaText(canvas, alpha, mouseSafeViewMode());
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    private ViewMode mouseSafeViewMode() {
        return viewMode;
    }

    private void renderSidebarSkiaText(Canvas canvas, int alpha, ViewMode mode) {
        FontRenderer.drawText(canvas, "PVPUtils Music", 14f, 66f, 14f, withAlpha(0xBFC2C7, alpha));
        float homeX = 14f;
        FontRenderer.drawText(canvas, "\uE88A", homeX, 84f, 13f, withAlpha(mode == ViewMode.HOME ? 0xFFFFFF : 0xAEB3BD, alpha), FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, "Home", homeX + 18f, 84f, 12f, withAlpha(mode == ViewMode.HOME ? 0xFFFFFF : 0xAEB3BD, alpha));
        FontRenderer.drawText(canvas, Config.isChinese ? "我的歌单" : "My Playlists", 14f, 126f, 11f, withAlpha(0x7C8088, alpha));
        if (!NeteaseMusicApi.isLoggedIn()) {
            FontRenderer.drawText(canvas, Config.isChinese ? "登录后显示歌单" : "Login required", 24f, 146f, 11f, withAlpha(0x8F98AA, alpha));
        } else if (playlists.isEmpty()) {
            FontRenderer.drawText(canvas, loading ? "Loading..." : "Empty", 24f, 146f, 11f, withAlpha(0x8F98AA, alpha));
        } else {
            int y = 146;
            for (int i = 0; i < Math.min(12, playlists.size()); i++) {
                Playlist playlist = playlists.get(i);
                String key = "playlist:" + playlist.id();
                boolean selected = selectedPlaylistIndex == i;
                float itemX = 24f;
                String icon = isLikedPlaylist(playlist) ? "\uE87D" : "\uE9B9";
                FontRenderer.drawText(canvas, icon, itemX, y, 13f, withAlpha(selected ? 0xFFFFFF : 0xAEB3BD, alpha), FontRenderer.MATERIAL_SYMBOLS);
                FontRenderer.drawText(canvas, trimToWidth(playlist.name(), 118f), itemX + 18f, y, 12f, withAlpha(selected ? 0xFFFFFF : 0xAEB3BD, alpha));
                y += 28;
            }
        }
        NeteaseMusicApi.LoginSession session = NeteaseMusicApi.currentSession();
        String name = session == null ? Minecraft.getInstance().getUser().getName() : session.nickname();
        FontRenderer.drawText(canvas, trimToWidth(name, 112f), 40f, height - PLAYER_HEIGHT - 20f, 12f, withAlpha(0xFFFFFF, alpha));
    }

    private void renderCurrentSkiaText(Canvas canvas, int alpha) {
        if (viewMode == ViewMode.HOME) {
            renderHomeSkiaText(canvas, alpha);
        } else if (viewMode == ViewMode.SEARCH) {
            renderSearchSkiaText(canvas, alpha);
        } else if (viewMode == ViewMode.PLAYLIST) {
            renderPlaylistSkiaText(canvas, alpha);
        }
    }

    private void applyContentScale(Canvas canvas, float scale) {
        if (Math.abs(scale - 1.0F) < 0.001F) {
            return;
        }
        float centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2.0F;
        float centerY = (height - PLAYER_HEIGHT) / 2.0F;
        canvas.translate(centerX, centerY);
        canvas.scale(scale, scale);
        canvas.translate(-centerX, -centerY);
    }

    private void renderHomeSkiaText(Canvas canvas, int alpha) {
        int contentX = SIDEBAR_WIDTH + 22;
        int contentY = 24;
        int gridX = contentX;
        int gridY = contentY + 58;
        int availableW = Math.max(1, width - gridX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int cover = cardW;
        int rowH = cover + GRID_TEXT_HEIGHT;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH);

        FontRenderer.drawText(canvas, Config.isChinese ? "欢迎来到 PVPUtils Music!" : "Welcome to PVPUtils Music!",
                contentX, contentY + 18f, 22f, withAlpha(0xF3F5F8, alpha));
        FontRenderer.drawText(canvas, Config.isChinese ? "网易云推荐歌单" : "Netease Recommended Playlists",
                contentX, contentY + 43f, 13f, withAlpha(0x858B96, alpha));
        if (!statusText.isBlank()) {
            FontRenderer.drawText(canvas, trim(statusText, 48), contentX + 260f, contentY + 43f, 12f, withAlpha(0x9AA2AF, Math.round(alpha * 0.9F)));
        }

        if (recommendedPlaylists.isEmpty()) {
            String text = loading ? (Config.isChinese ? "加载中..." : "Loading...") : (NeteaseMusicApi.isLoggedIn() ? "No playlists" : "Login required");
            drawSkiaCentered(canvas, text, gridX + availableW / 2f, gridY + 84f, 15f, withAlpha(0xB8C0D4, alpha));
            return;
        }

        GridScroll scroll = gridScroll(visualFirstPlaylistIndex, columns, rowH, maxPlaylistGridStart(columns, visibleRows * columns));
        int visualBase = scroll.base();
        float rowOffset = scroll.offset();
        int visibleCards = (visibleRows + 1) * columns;
        canvas.save();
        canvas.clipRect(Rect.makeLTRB(gridX - 8, gridY, gridX + availableW + 8, height - PLAYER_HEIGHT - 16));
        try {
            for (int slot = 0; slot < visibleCards; slot++) {
                int index = visualBase + slot;
                if (index >= recommendedPlaylists.size()) {
                    break;
                }
                Playlist playlist = recommendedPlaylists.get(index);
                int col = slot % columns;
                int row = slot / columns;
                int x = gridX + col * (cardW + GRID_GAP);
                int y = Math.round(gridY + row * rowH - rowOffset);
                if (y > height - PLAYER_HEIGHT || y + cover + 30 < gridY) {
                    continue;
                }
                FontRenderer.drawText(canvas, trim(playlist.name(), Math.max(9, cardW / 8)), x, y + cover + 18f, 12f, withAlpha(0xF1F3F6, alpha));
                FontRenderer.drawText(canvas, formatPlayCount(playlist.playCount()), x, y + cover + 34f, 10f, withAlpha(0x8C929D, alpha));
            }
        } finally {
            canvas.restore();
        }
    }

    private void renderSearchSkiaText(Canvas canvas, int alpha) {
        int contentX = SIDEBAR_WIDTH + 22;
        int contentY = 24;
        int gridX = contentX;
        int gridY = contentY + 48;
        int availableW = Math.max(1, width - gridX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int cover = cardW;
        int rowH = cover + GRID_TEXT_HEIGHT;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH);

        FontRenderer.drawText(canvas, Config.isChinese ? "搜索结果" : "Search Results", contentX, contentY + 22f, 22f, withAlpha(0xF3F5F8, alpha));
        FontRenderer.drawText(canvas, query.isBlank() ? (Config.isChinese ? "输入关键词搜索音乐" : "Type keywords to search music") : query,
                contentX, contentY + 43f, 13f, withAlpha(0x858B96, alpha));

        if (songs.isEmpty()) {
            drawSkiaCentered(canvas, loading ? "Loading..." : "No songs", gridX + availableW / 2f, gridY + 84f, 15f, withAlpha(0xB8C0D4, alpha));
            return;
        }

        GridScroll scroll = gridScroll(visualFirstSongIndex, columns, rowH, maxGridStart(columns, visibleRows * columns));
        int visualBase = scroll.base();
        float rowOffset = scroll.offset();
        int visibleCards = (visibleRows + 1) * columns;
        canvas.save();
        canvas.clipRect(Rect.makeLTRB(gridX - 8, gridY, gridX + availableW + 8, height - PLAYER_HEIGHT - 16));
        try {
            for (int slot = 0; slot < visibleCards; slot++) {
                int index = visualBase + slot;
                if (index >= songs.size()) {
                    break;
                }
                Song song = songs.get(index);
                int col = slot % columns;
                int row = slot / columns;
                int x = gridX + col * (cardW + GRID_GAP);
                int y = Math.round(gridY + row * rowH - rowOffset);
                if (y > height - PLAYER_HEIGHT || y + cover + 30 < gridY) {
                    continue;
                }
                FontRenderer.drawText(canvas, trim(song.name(), Math.max(9, cardW / 8)), x, y + cover + 18f, 12f, withAlpha(0xF1F3F6, alpha));
                FontRenderer.drawText(canvas, trim(song.displayArtist(), Math.max(10, cardW / 7)), x, y + cover + 34f, 10f, withAlpha(0x8C929D, alpha));
            }
        } finally {
            canvas.restore();
        }
    }

    private void renderPlaylistSkiaText(Canvas canvas, int alpha) {
        float appear = contentAppearProgress();
        int localAlpha = Math.round(alpha * appear);
        int slide = Math.round((1.0F - appear) * 18.0F);
        int contentX = SIDEBAR_WIDTH + 28;
        int contentY = 24 + slide;
        int cover = playlistCoverSize();
        int listY = contentY + cover + 48;
        int rowH = 42;
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - listY - 10) / rowH);
        float listVisual = Math.max(0.0F, Math.min(Math.max(0, songs.size() - visibleRows), visualFirstSongIndex));
        int visualBase = (int) Math.floor(listVisual);
        float rowOffset = (listVisual - visualBase) * rowH;
        Playlist playlist = currentPlaylist;
        int infoX = contentX + cover + 24;
        if (playlist != null) {
            String count = (playlist.trackCount() > 0 ? playlist.trackCount() : songs.size()) + (Config.isChinese ? "首歌曲" : " songs");
            FontRenderer.drawText(canvas, trim(playlist.name(), 38), infoX, contentY + 40f, 26f, withAlpha(0xF0F0F0, localAlpha));
            FontRenderer.drawText(canvas, count + " · " + estimatePlaylistDuration(), infoX, contentY + 64f, 12f, withAlpha(0x777A80, localAlpha));
            FontRenderer.drawText(canvas, "\uE853", infoX, contentY + 93f, 15f, withAlpha(0xE8E8E8, localAlpha), FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, playlist.creator().isBlank() ? "Netease Music" : playlist.creator(), infoX + 24f, contentY + 92f, 13f, withAlpha(0xE8E8E8, localAlpha));
            int buttonY = contentY + cover - 46;
            drawSkiaButtonLabel(canvas, "\uE037", Config.isChinese ? "播放歌单" : "Play", infoX, buttonY, 86, localAlpha);
            drawSkiaButtonLabel(canvas, "\uE043", Config.isChinese ? "乱序播放歌单" : "Shuffle", infoX + 102, buttonY, 112, localAlpha);
        }
        int clipBottom = height - PLAYER_HEIGHT - 8;
        canvas.save();
        canvas.clipRect(Rect.makeLTRB(contentX, listY, width - 26, clipBottom));
        try {
            for (int row = 0; row < visibleRows + 2; row++) {
                int index = visualBase + row;
                if (index >= songs.size()) {
                    break;
                }
                float rowProgress = clamp((appear * 1.18F) - row * 0.025F);
                int y = Math.round(listY + row * rowH - rowOffset + Math.round((1.0F - rowProgress) * 10.0F));
                if (y > clipBottom || y + rowH < listY) {
                    continue;
                }
                int rowAlpha = Math.round(localAlpha * rowProgress);
                Song song = songs.get(index);
                FontRenderer.drawText(canvas, String.valueOf(index + 1), contentX + 38f, y + 28f, 13f, withAlpha(0x878A90, rowAlpha));
                FontRenderer.drawText(canvas, trim(song.name(), 56), contentX + 98f, y + 19f, 14f, withAlpha(0xF1F1F1, rowAlpha));
                FontRenderer.drawText(canvas, trim(song.displayArtist() + " - " + song.name(), 72), contentX + 98f, y + 36f, 12f, withAlpha(0x777A80, rowAlpha));
                FontRenderer.drawText(canvas, MusicPlaybackService.formatTime(song.durationMs()), width - 74f, y + 27f, 12f, withAlpha(0x858992, rowAlpha));
            }
        } finally {
            canvas.restore();
        }
    }

    private void renderPlayerSkiaText(Canvas canvas, int alpha) {
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song current = player.currentSong();
        int y = height - PLAYER_HEIGHT;
        if (current != null) {
            FontRenderer.drawText(canvas, trim(current.name(), 34), SIDEBAR_WIDTH + 104f, y + 31f, 14f, withAlpha(0xF4F6FA, alpha));
            FontRenderer.drawText(canvas, trim(current.displayArtist(), 38), SIDEBAR_WIDTH + 104f, y + 49f, 11f, withAlpha(0x8B929F, alpha));
        } else {
            FontRenderer.drawText(canvas, Config.isChinese ? "未在播放" : "Not Playing", SIDEBAR_WIDTH + 44f, y + 34f, 14f, withAlpha(0xF4F6FA, alpha));
            FontRenderer.drawText(canvas, Config.isChinese ? "无" : "None", SIDEBAR_WIDTH + 44f, y + 52f, 11f, withAlpha(0x8B929F, alpha));
        }
        int centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2;
        int progressX = centerX - 160;
        int progressY = y + 58;
        long total = player.totalDurationMs();
        String left = MusicPlaybackService.formatTime(draggingProgress && total > 0L ? Math.round(total * pendingProgress) : player.positionMs());
        String right = MusicPlaybackService.formatTime(total);
        FontRenderer.drawText(canvas, left, progressX - 42f, progressY + 5f, 10f, withAlpha(0x8F98AA, alpha));
        FontRenderer.drawText(canvas, right, progressX + 328f, progressY + 5f, 10f, withAlpha(0x8F98AA, alpha));
        drawSkiaCentered(canvas, "\uE045", centerX - 51f, y + 36f, 18f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
        drawSkiaCentered(canvas, player.isPlaying() ? "\uE034" : "\uE037", centerX, y + 36f, 20f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
        drawSkiaCentered(canvas, "\uE044", centerX + 51f, y + 36f, 18f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
        drawSkiaCentered(canvas, playbackModeIcon(player.playbackMode()), centerX + 109f, y + 36f, 18f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
    }

    private void drawSkiaButtonLabel(Canvas canvas, String icon, String text, float x, float y, float w, int alpha) {
        float textW = FontRenderer.measureTextWidth(text, 13f);
        float iconW = FontRenderer.measureTextWidth(icon, 15f, FontRenderer.MATERIAL_SYMBOLS);
        float start = x + (w - iconW - 5f - textW) / 2f;
        FontRenderer.drawText(canvas, icon, start, y + 21f, 15f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, text, start + iconW + 5f, y + 18f, 13f, withAlpha(0xFFFFFF, alpha));
    }

    private static boolean isLikedPlaylist(Playlist playlist) {
        String name = playlist == null ? "" : playlist.name().toLowerCase();
        return name.contains("喜欢") || name.contains("liked") || name.contains("favorite");
    }

    private static String playbackModeIcon(MusicPlaybackService.PlaybackMode mode) {
        return switch (mode) {
            case LOOP -> "\uE040";
            case RANDOM -> "\uE043";
            case LIST -> "\uE8EF";
        };
    }

    private void drawSkiaCentered(Canvas canvas, String text, float centerX, float baselineY, float size, int argb) {
        FontRenderer.drawText(canvas, text, centerX - FontRenderer.measureTextWidth(text, size) / 2.0F, baselineY, size, argb);
    }

    private void drawSkiaCentered(Canvas canvas, String text, float centerX, float baselineY, float size, int argb, String fontName) {
        FontRenderer.drawText(canvas, text, centerX - FontRenderer.measureTextWidth(text, size, fontName) / 2.0F, baselineY, size, argb, fontName);
    }

    private void renderPlaylistTextOverlay(GuiGraphics graphics, int contentX, int contentY, int cover, int listY, int rowH, int visibleRows, int visualBase, float rowOffset, int alpha, float appear) {
        Canvas canvas = SkiaRenderer.beginRegion(0, 0, width, height);
        if (canvas == null) {
            renderPlaylistTextFallback(graphics, contentX, contentY, cover, listY, rowH, visibleRows, visualBase, rowOffset, alpha);
            return;
        }
        try {
            Playlist playlist = currentPlaylist;
            int infoX = contentX + cover + 24;
            if (playlist != null) {
                int titleColor = withAlpha(0xF0F0F0, alpha);
                int mutedColor = withAlpha(0x777A80, alpha);
                int creatorColor = withAlpha(0xE8E8E8, alpha);
                String count = (playlist.trackCount() > 0 ? playlist.trackCount() : songs.size()) + (Config.isChinese ? "首歌曲" : " songs");
                String duration = estimatePlaylistDuration();
                FontRenderer.drawText(canvas, trim(playlist.name(), 38), infoX, contentY + 82f, 26f, titleColor);
                FontRenderer.drawText(canvas, count + " · " + duration, infoX, contentY + 105f, 12f, mutedColor);
                FontRenderer.drawText(canvas, "\uE853", infoX, contentY + 137f, 15f, withAlpha(0xE8E8E8, alpha), FontRenderer.MATERIAL_SYMBOLS);
                FontRenderer.drawText(canvas, playlist.creator().isBlank() ? "Netease Music" : playlist.creator(), infoX + 24f, contentY + 136f, 13f, creatorColor);
            }
            for (int row = 0; row < visibleRows + 1; row++) {
                int index = visualBase + row;
                if (index >= songs.size()) {
                    break;
                }
                float rowProgress = clamp((appear * 1.18F) - row * 0.025F);
                int y = Math.round(listY + row * rowH - rowOffset + Math.round((1.0F - rowProgress) * 10.0F));
                if (y > height - PLAYER_HEIGHT || y + rowH < listY) {
                    continue;
                }
                int rowAlpha = Math.round(scrollFadeAlpha(alpha, y, listY, height - PLAYER_HEIGHT - 8) * rowProgress);
                Song song = songs.get(index);
                FontRenderer.drawText(canvas, String.valueOf(index + 1), contentX + 38f, y + 28f, 13f, withAlpha(0x878A90, rowAlpha));
                FontRenderer.drawText(canvas, trim(song.name(), 56), contentX + 98f, y + 19f, 14f, withAlpha(0xF1F1F1, rowAlpha));
                FontRenderer.drawText(canvas, trim(song.displayArtist() + " - " + song.name(), 72), contentX + 98f, y + 36f, 12f, withAlpha(0x777A80, rowAlpha));
                FontRenderer.drawText(canvas, MusicPlaybackService.formatTime(song.durationMs()), width - 74f, y + 27f, 12f, withAlpha(0x858992, rowAlpha));
            }
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    private void renderPlaylistTextFallback(GuiGraphics graphics, int contentX, int contentY, int cover, int listY, int rowH, int visibleRows, int visualBase, float rowOffset, int alpha) {
        Playlist playlist = currentPlaylist;
        if (playlist != null) {
            int infoX = contentX + cover + 24;
            graphics.drawString(font, trim(playlist.name(), 44), infoX, contentY + 62, withAlpha(0xFFFFFF, alpha), false);
            graphics.drawString(font, estimatePlaylistDuration(), infoX, contentY + 86, withAlpha(0x858992, alpha), false);
            graphics.drawString(font, playlist.creator().isBlank() ? "Netease Music" : playlist.creator(), infoX, contentY + 116, withAlpha(0xE4E6EB, alpha), false);
        }
        for (int row = 0; row < visibleRows + 1; row++) {
            int index = visualBase + row;
            if (index >= songs.size()) {
                break;
            }
            Song song = songs.get(index);
            int y = Math.round(listY + row * rowH - rowOffset);
            if (y > height - PLAYER_HEIGHT || y + rowH < listY) {
                continue;
            }
            int rowAlpha = scrollFadeAlpha(alpha, y, listY, height - PLAYER_HEIGHT - 8);
            graphics.drawString(font, String.valueOf(index + 1), contentX + 38, y + 15, withAlpha(0x838891, rowAlpha), false);
            graphics.drawString(font, trim(song.name(), 42), contentX + 98, y + 8, withAlpha(0xFFFFFF, rowAlpha), false);
            graphics.drawString(font, trim(song.displayArtist(), 48), contentX + 98, y + 22, withAlpha(0x777B84, rowAlpha), false);
            graphics.drawString(font, MusicPlaybackService.formatTime(song.durationMs()), width - 74, y + 15, withAlpha(0x858992, rowAlpha), false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) {
            return true;
        }
        if (event.button() != 0) {
            return super.mouseClicked(event, consumed);
        }
        double mouseX = event.x();
        double mouseY = event.y();

        if (hit(width - 30, 12, 18, 18, mouseX, mouseY)) {
            onClose();
            return true;
        }

        if (!NeteaseMusicApi.isLoggedIn()) {
            return handleLoginClick(mouseX, mouseY);
        }

        if (hit(12, 14, SIDEBAR_WIDTH - 24, 22, mouseX, mouseY)) {
            focusAt(Focus.SEARCH, 12, mouseX);
            return true;
        }
        if (viewMode == ViewMode.PLAYLIST && hit(playlistSearchInputX, playlistSearchInputY, playlistSearchInputW, playlistSearchInputH, mouseX, mouseY)) {
            focusAt(Focus.PLAYLIST_SEARCH, playlistSearchInputX, mouseX);
            return true;
        }
        int progressX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2 - 160;
        if (hit(progressX, height - PLAYER_HEIGHT + 50, 320, 16, mouseX, mouseY)) {
            draggingProgress = true;
            previewSeekFromMouse(mouseX);
            return true;
        }
        if (NeteaseMusicApi.isLoggedIn() && hit(lastGridSliderX - 6, lastGridSliderY, 14, lastGridSliderH, mouseX, mouseY)) {
            if (viewMode == ViewMode.HOME && !recommendedPlaylists.isEmpty()) {
                draggingListSlider = true;
                draggingSliderTarget = SliderTarget.PLAYLIST_GRID;
                updateDraggedGridSlider(mouseY);
                return true;
            }
            if (viewMode == ViewMode.SEARCH && !songs.isEmpty()) {
                draggingListSlider = true;
                draggingSliderTarget = SliderTarget.SONG_GRID;
                updateDraggedGridSlider(mouseY);
                return true;
            }
        }

        if (hit(8, 68, SIDEBAR_WIDTH - 28, 28, mouseX, mouseY)) {
            switchView(ViewMode.HOME);
            selectedPlaylistIndex = -1;
            currentPlaylist = null;
            if (recommendedPlaylists.isEmpty()) {
                loadRecommendedPlaylists();
            }
            return true;
        }
        int playlistY = 130;
        for (int i = 0; i < Math.min(12, playlists.size()); i++) {
            if (hit(18, playlistY + i * 28, SIDEBAR_WIDTH - 36, 24, mouseX, mouseY)) {
                selectedPlaylistIndex = i;
                loadPlaylist(playlists.get(i));
                return true;
            }
        }

        int centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2;
        int playerY = height - PLAYER_HEIGHT;
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        if (hit(centerX - 72, playerY + 18, 42, 22, mouseX, mouseY)) {
            player.playPrevious();
            ensureIndexVisible(player.currentIndex());
            return true;
        }
        if (hit(centerX - 22, playerY + 16, 44, 26, mouseX, mouseY)) {
            player.toggle();
            return true;
        }
        if (hit(centerX + 30, playerY + 18, 42, 22, mouseX, mouseY)) {
            player.playNext();
            ensureIndexVisible(player.currentIndex());
            return true;
        }
        if (hit(centerX + 88, playerY + 18, 42, 22, mouseX, mouseY)) {
            player.cyclePlaybackMode();
            return true;
        }

        if (viewMode == ViewMode.HOME) {
            int playlistIndex = playlistIndexAt(mouseX, mouseY);
            if (playlistIndex >= 0 && playlistIndex < recommendedPlaylists.size()) {
                selectedPlaylistIndex = -1;
                loadPlaylist(recommendedPlaylists.get(playlistIndex));
                return true;
            }
        }
        int playlistButtonY = 24 + playlistCoverSize() - 46;
        int playlistInfoX = SIDEBAR_WIDTH + 28 + playlistCoverSize() + 24;
        if (viewMode == ViewMode.PLAYLIST && hit(playlistInfoX, playlistButtonY, 86, 26, mouseX, mouseY) && !songs.isEmpty()) {
            player.setPlaylist(songs, 0);
            player.playSong(songs.getFirst());
            return true;
        }
        if (viewMode == ViewMode.PLAYLIST && hit(playlistInfoX + 102, playlistButtonY, 112, 26, mouseX, mouseY) && !songs.isEmpty()) {
            int randomIndex = (int) (Math.random() * songs.size());
            player.setPlaylist(songs, randomIndex);
            player.playSong(songs.get(randomIndex));
            return true;
        }
        int clickedIndex = songIndexAt(mouseX, mouseY);
        if (clickedIndex >= 0 && clickedIndex < songs.size()) {
            player.setPlaylist(songs, clickedIndex);
            player.playSong(songs.get(clickedIndex));
            return true;
        }
        focus = Focus.NONE;
        return super.mouseClicked(event, consumed);
    }

    private boolean handleLoginClick(double mouseX, double mouseY) {
        int w = 320;
        int h = loginMode == LoginMode.QR ? 210 : 190;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        if (hit(x + 62, y + 52, 88, 22, mouseX, mouseY)) {
            loginMode = LoginMode.QR;
            focus = Focus.NONE;
            if (qrLogin == null && !qrPolling) {
                startQrLogin();
            }
            return true;
        }
        if (hit(x + 170, y + 52, 88, 22, mouseX, mouseY)) {
            loginMode = LoginMode.PASSWORD;
            cancelQrLogin();
            focus = Focus.PHONE;
            return true;
        }
        if (loginMode == LoginMode.QR) {
            int qrX = x + 42;
            int qrY = y + 88;
            if (hit(qrX + 116, qrY + 42, 110, 24, mouseX, mouseY)) {
                startQrLogin();
                return true;
            }
            return true;
        }
        if (hit(phoneInputX, y + 86, w - 76, 22, mouseX, mouseY)) {
            focusAt(Focus.PHONE, phoneInputX, mouseX);
            return true;
        }
        if (hit(passwordInputX, y + 116, w - 76, 22, mouseX, mouseY)) {
            focusAt(Focus.PASSWORD, passwordInputX, mouseX);
            return true;
        }
        if (hit(x + 106, y + 150, 108, 24, mouseX, mouseY)) {
            login();
            return true;
        }
        focus = Focus.NONE;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (NeteaseMusicApi.isLoggedIn() && viewMode == ViewMode.HOME && hit(SIDEBAR_WIDTH, 0, width - SIDEBAR_WIDTH, height - PLAYER_HEIGHT, mouseX, mouseY) && !recommendedPlaylists.isEmpty()) {
            firstPlaylistIndex = Math.max(0, Math.min(maxPlaylistGridStart(), firstPlaylistIndex - (int) Math.signum(scrollY) * playlistGridColumns()));
            return true;
        }
        if (NeteaseMusicApi.isLoggedIn() && hit(SIDEBAR_WIDTH, 0, width - SIDEBAR_WIDTH, height - PLAYER_HEIGHT, mouseX, mouseY) && !songs.isEmpty()) {
            if (viewMode == ViewMode.PLAYLIST) {
                firstSongIndex = Math.max(0, Math.min(Math.max(0, songs.size() - playlistVisibleRows()), firstSongIndex - (int) Math.signum(scrollY) * 3));
            } else if (viewMode == ViewMode.SEARCH) {
                firstSongIndex = Math.max(0, Math.min(maxGridStart(), firstSongIndex - (int) Math.signum(scrollY) * gridColumns()));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingTextSelection != Focus.NONE) {
            draggingTextSelection = Focus.NONE;
            return true;
        }
        if (event.button() == 0 && draggingProgress) {
            commitSeek();
            draggingProgress = false;
            pendingProgress = -1.0F;
            return true;
        }
        if (event.button() == 0 && draggingListSlider) {
            draggingListSlider = false;
            draggingSliderTarget = SliderTarget.NONE;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggingTextSelection != Focus.NONE) {
            setCursor(draggingTextSelection, cursorFromMouse(draggingTextSelection, inputX(draggingTextSelection), event.x()));
            return true;
        }
        if (event.button() == 0 && draggingProgress) {
            previewSeekFromMouse(event.x());
            return true;
        }
        if (event.button() == 0 && draggingListSlider) {
            updateDraggedGridSlider(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focus == Focus.NONE) {
            if (event.key() == GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            return super.keyPressed(event);
        }
        boolean ctrlDown = isKeyDown(GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW_KEY_RIGHT_CONTROL);
        boolean shiftDown = isKeyDown(GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW_KEY_RIGHT_SHIFT);
        if (ctrlDown && event.key() == GLFW_KEY_A) {
            selectAll();
            return true;
        }
        switch (event.key()) {
            case GLFW_KEY_BACKSPACE -> {
                deletePrevious();
                return true;
            }
            case GLFW_KEY_DELETE -> {
                deleteNext();
                return true;
            }
            case GLFW_KEY_LEFT -> {
                moveCursor(-1, shiftDown);
                return true;
            }
            case GLFW_KEY_RIGHT -> {
                moveCursor(1, shiftDown);
                return true;
            }
            case GLFW_KEY_HOME -> {
                moveCursorTo(0, shiftDown);
                return true;
            }
            case GLFW_KEY_END -> {
                moveCursorTo(focusedText().length(), shiftDown);
                return true;
            }
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (focus == Focus.SEARCH && NeteaseMusicApi.isLoggedIn()) {
                    runSearchOrRefresh();
                } else if (focus == Focus.PLAYLIST_SEARCH) {
                    focus = Focus.NONE;
                } else if (focus == Focus.PASSWORD) {
                    login();
                }
                return true;
            }
            case GLFW_KEY_ESCAPE -> {
                focus = Focus.NONE;
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (focus == Focus.NONE) {
            return super.charTyped(event);
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isEmpty()) {
            return true;
        }
        insertText(typed);
        return true;
    }

    @Override
    public void onClose() {
        if (closing) {
            return;
        }
        closing = true;
        closeStartedAt = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadHome() {
        loadRecommendedPlaylists();
    }

    private void loadRecommendedPlaylists() {
        if (loading || !NeteaseMusicApi.isLoggedIn()) {
            return;
        }
        loading = true;
        switchView(ViewMode.HOME);
        currentPlaylist = null;
        selectedPlaylistIndex = -1;
        statusText = "Loading recommended playlists...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.getRecommendedPlaylists();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((loadedPlaylists, throwable) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (throwable != null) {
                statusText = cleanMessage(throwable);
                return;
            }
            recommendedPlaylists.clear();
            recommendedPlaylists.addAll(loadedPlaylists);
            firstPlaylistIndex = 0;
            visualFirstPlaylistIndex = 0.0F;
            statusText = loadedPlaylists.size() + " playlists loaded";
        }));
    }

    private void runSearchOrRefresh() {
        if (loading || !NeteaseMusicApi.isLoggedIn()) {
            return;
        }
        if (query.isBlank()) {
            loadHome();
            return;
        }
        loading = true;
        statusText = "Searching " + query;
        String search = query;
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.search(search);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((loadedSongs, throwable) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (throwable != null) {
                statusText = cleanMessage(throwable);
                return;
            }
            switchView(ViewMode.SEARCH);
            songs.clear();
            songs.addAll(loadedSongs);
            firstSongIndex = 0;
            visualFirstSongIndex = 0.0F;
            selectedPlaylistIndex = -1;
            currentPlaylist = null;
            statusText = loadedSongs.size() + " results";
        }));
    }

    private void loadUserPlaylists() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.getUserPlaylists();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((loadedPlaylists, throwable) -> Minecraft.getInstance().execute(() -> {
            if (throwable != null) {
                return;
            }
            playlists.clear();
            playlists.addAll(loadedPlaylists);
        }));
    }

    private void loadPlaylist(Playlist playlist) {
        if (playlist == null) return;
        loading = true;
        int serial = ++playlistLoadSerial;
        if (viewMode == ViewMode.PLAYLIST) {
            beginContentTransition();
        } else {
            switchView(ViewMode.PLAYLIST);
        }
        currentPlaylist = playlist;
        playlistSearchQuery = "";
        playlistSearchCursor = 0;
        playlistSearchSelection = -1;
        fullPlaylistSongs.clear();
        songs.clear();
        firstSongIndex = 0;
        visualFirstSongIndex = 0.0F;
        statusText = "Loading " + playlist.name();
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.getPlaylistDetail(playlist.id());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((loadedSongs, throwable) -> Minecraft.getInstance().execute(() -> {
            if (serial != playlistLoadSerial) {
                return;
            }
            loading = false;
            if (throwable != null) {
                statusText = cleanMessage(throwable);
                return;
            }
            fullPlaylistSongs.clear();
            fullPlaylistSongs.addAll(loadedSongs);
            applyPlaylistFilter();
            firstSongIndex = 0;
            visualFirstSongIndex = 0.0F;
            statusText = loadedSongs.size() + " songs";
        }));
    }

    private void applyPlaylistFilter() {
        if (viewMode != ViewMode.PLAYLIST && currentPlaylist == null) {
            return;
        }
        String filter = playlistSearchQuery == null ? "" : playlistSearchQuery.trim().toLowerCase();
        songs.clear();
        if (filter.isBlank()) {
            songs.addAll(fullPlaylistSongs);
        } else {
            for (Song song : fullPlaylistSongs) {
                if (song.name().toLowerCase().contains(filter)) {
                    songs.add(song);
                }
            }
        }
        firstSongIndex = 0;
        visualFirstSongIndex = 0.0F;
    }

    private void login() {
        if (loading) return;
        loading = true;
        statusText = "Logging in...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.loginCellphone(phone, password);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((session, throwable) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (throwable != null) {
                statusText = cleanMessage(throwable);
                return;
            }
            password = "";
            focus = Focus.NONE;
            cancelQrLogin();
            statusText = "Logged in: " + session.nickname();
            loadRecommendedPlaylists();
            loadUserPlaylists();
        }));
    }

    private void startQrLogin() {
        if (loading || qrPolling || NeteaseMusicApi.isLoggedIn()) return;
        int serial = ++qrSerial;
        qrLogin = null;
        qrPolling = true;
        statusText = "Creating QR...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return NeteaseMusicApi.createQrLogin();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((login, throwable) -> Minecraft.getInstance().execute(() -> {
            if (serial != qrSerial || loginMode != LoginMode.QR) return;
            if (throwable != null) {
                qrPolling = false;
                statusText = cleanMessage(throwable);
                return;
            }
            qrLogin = login;
            statusText = "Scan in Netease app";
            pollQrLogin(serial, login.key());
        }));
    }

    private void pollQrLogin(int serial, String key) {
        CompletableFuture.supplyAsync(() -> {
            try {
                while (serial == qrSerial && loginMode == LoginMode.QR && !NeteaseMusicApi.isLoggedIn()) {
                    NeteaseMusicApi.QrLoginStatus status = NeteaseMusicApi.checkQrLogin(key);
                    Minecraft.getInstance().execute(() -> {
                        if (serial == qrSerial && loginMode == LoginMode.QR) {
                            statusText = status.message();
                        }
                    });
                    if (status.session() != null) {
                        return status.session();
                    }
                    if (status.code() == 800) {
                        throw new IllegalStateException(status.message());
                    }
                    Thread.sleep(2000L);
                }
                return null;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, IO).whenComplete((session, throwable) -> Minecraft.getInstance().execute(() -> {
            if (serial != qrSerial || loginMode != LoginMode.QR) return;
            qrPolling = false;
            if (throwable != null) {
                statusText = cleanMessage(throwable);
                return;
            }
            if (session != null) {
                focus = Focus.NONE;
                statusText = "Logged in: " + session.nickname();
                loadRecommendedPlaylists();
                loadUserPlaylists();
            }
        }));
    }

    private void cancelQrLogin() {
        qrSerial++;
        qrLogin = null;
        qrPolling = false;
    }

    private int songIndexAt(double mouseX, double mouseY) {
        if (viewMode == ViewMode.PLAYLIST) {
            int listX = SIDEBAR_WIDTH + 28;
            int cover = playlistCoverSize();
            int listY = 24 + cover + 48;
            int listW = width - listX - 26;
            int rowH = 42;
            if (!hit(listX, listY, listW, playlistVisibleRows() * rowH, mouseX, mouseY)) {
                return -1;
            }
            int visibleRows = playlistVisibleRows();
            float listVisual = Math.max(0.0F, Math.min(Math.max(0, songs.size() - visibleRows), visualFirstSongIndex));
            int visualBase = (int) Math.floor(listVisual);
            float rowOffset = (listVisual - visualBase) * rowH;
            float appear = contentAppearProgress();
            for (int row = 0; row < visibleRows + 2; row++) {
                int index = visualBase + row;
                if (index >= songs.size()) {
                    break;
                }
                float rowProgress = clamp((appear * 1.18F) - row * 0.025F);
                int rowSlide = Math.round((1.0F - rowProgress) * 10.0F);
                int y = Math.round(listY + row * rowH - rowOffset + rowSlide);
                if (hit(listX, y, listW, rowH - 5, mouseX, mouseY)) {
                    return index;
                }
            }
            return -1;
        }
        int contentX = SIDEBAR_WIDTH + 22;
        int gridY = viewMode == ViewMode.SEARCH ? 72 : 82;
        int availableW = Math.max(1, width - contentX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int rowH = cardW + GRID_TEXT_HEIGHT;
        if (mouseX < contentX || mouseY < gridY || mouseY > height - PLAYER_HEIGHT) {
            return -1;
        }
        int col = (int) ((mouseX - contentX) / (cardW + GRID_GAP));
        GridScroll scroll = gridScroll(visualFirstSongIndex, columns, rowH, maxGridStart(columns, Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH) * columns));
        int row = (int) ((mouseY - gridY + scroll.offset()) / rowH);
        int localX = (int) ((mouseX - contentX) - col * (cardW + GRID_GAP));
        int localY = (int) ((mouseY - gridY + scroll.offset()) - row * rowH);
        if (col < 0 || col >= columns || localX > cardW || localY > cardW + 30) {
            return -1;
        }
        return scroll.base() + row * columns + col;
    }

    private int playlistIndexAt(double mouseX, double mouseY) {
        int contentX = SIDEBAR_WIDTH + 22;
        int gridY = 82;
        int availableW = Math.max(1, width - contentX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int columns = Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
        int rowH = cardW + GRID_TEXT_HEIGHT;
        if (mouseX < contentX || mouseY < gridY || mouseY > height - PLAYER_HEIGHT) {
            return -1;
        }
        int col = (int) ((mouseX - contentX) / (cardW + GRID_GAP));
        GridScroll scroll = gridScroll(visualFirstPlaylistIndex, columns, rowH, maxPlaylistGridStart(columns, Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / rowH) * columns));
        int row = (int) ((mouseY - gridY + scroll.offset()) / rowH);
        int localX = (int) ((mouseX - contentX) - col * (cardW + GRID_GAP));
        int localY = (int) ((mouseY - gridY + scroll.offset()) - row * rowH);
        if (col < 0 || col >= columns || localX > cardW || localY > cardW + 30) {
            return -1;
        }
        return scroll.base() + row * columns + col;
    }

    private int playlistVisibleRows() {
        int listY = 24 + playlistCoverSize() + 48;
        return Math.max(1, (height - PLAYER_HEIGHT - listY - 10) / 42);
    }

    private int gridColumns() {
        int contentX = SIDEBAR_WIDTH + 22;
        int availableW = Math.max(1, width - contentX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        return Math.max(1, (availableW + GRID_GAP) / (cardW + GRID_GAP));
    }

    private int playlistGridColumns() {
        return gridColumns();
    }

    private int maxGridStart() {
        int columns = gridColumns();
        int contentX = SIDEBAR_WIDTH + 22;
        int gridY = viewMode == ViewMode.SEARCH ? 72 : 82;
        int availableW = Math.max(1, width - contentX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / (cardW + GRID_TEXT_HEIGHT));
        return maxGridStart(columns, visibleRows * columns);
    }

    private int maxGridStart(int columns, int visibleCards) {
        int maxStart = Math.max(0, songs.size() - visibleCards);
        return maxStart <= 0 ? 0 : (maxStart / Math.max(1, columns)) * Math.max(1, columns);
    }

    private int maxPlaylistGridStart() {
        int columns = playlistGridColumns();
        int contentX = SIDEBAR_WIDTH + 22;
        int gridY = 82;
        int availableW = Math.max(1, width - contentX - 24);
        int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
        int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / (cardW + GRID_TEXT_HEIGHT));
        return maxPlaylistGridStart(columns, visibleRows * columns);
    }

    private int maxPlaylistGridStart(int columns, int visibleCards) {
        int maxStart = Math.max(0, recommendedPlaylists.size() - visibleCards);
        return maxStart <= 0 ? 0 : (maxStart / Math.max(1, columns)) * Math.max(1, columns);
    }

    private void updateScrollAnimation() {
        visualFirstPlaylistIndex = approach(visualFirstPlaylistIndex, firstPlaylistIndex, 0.12F);
        visualFirstSongIndex = approach(visualFirstSongIndex, firstSongIndex, 0.12F);
        if (Math.abs(visualFirstPlaylistIndex - firstPlaylistIndex) < 0.02F) {
            visualFirstPlaylistIndex = firstPlaylistIndex;
        }
        if (Math.abs(visualFirstSongIndex - firstSongIndex) < 0.02F) {
            visualFirstSongIndex = firstSongIndex;
        }
    }

    private int scrollFadeAlpha(int alpha, int y, int top, int bottom) {
        int fade = 28;
        float topFade = clamp((y + fade - top) / (float) fade);
        float bottomFade = clamp((bottom - y) / (float) fade);
        return Math.round(alpha * Math.min(topFade, bottomFade));
    }

    private GridScroll gridScroll(float visualIndex, int columns, int rowH, int maxStart) {
        int safeColumns = Math.max(1, columns);
        float clampedIndex = Math.max(0.0F, Math.min(Math.max(0, maxStart), visualIndex));
        int base = Math.min(Math.max(0, maxStart), (int) Math.floor(clampedIndex / safeColumns) * safeColumns);
        float offset = ((clampedIndex - base) / safeColumns) * rowH;
        return new GridScroll(base, offset);
    }

    private float hoverProgress(String key, boolean hovered) {
        float current = coverHoverAnimations.getOrDefault(key, 0.0F);
        current = approach(current, hovered ? 1.0F : 0.0F, 0.16F);
        if (current < 0.01F && !hovered) {
            coverHoverAnimations.remove(key);
            return 0.0F;
        }
        coverHoverAnimations.put(key, current);
        return current;
    }

    private void updateDraggedGridSlider(double mouseY) {
        int visibleCards = Math.max(1, lastGridSliderVisibleCards);
        int columns = Math.max(1, lastGridSliderColumns);
        if (draggingSliderTarget == SliderTarget.PLAYLIST_GRID) {
            int maxStart = maxPlaylistGridStart(columns, visibleCards);
            if (maxStart <= 0) return;
            int knobH = Math.max(28, Math.round(lastGridSliderH * (visibleCards / (float) Math.max(1, recommendedPlaylists.size()))));
            float ratio = clamp((float) ((mouseY - lastGridSliderY - knobH / 2.0F) / Math.max(1, lastGridSliderH - knobH)));
            firstPlaylistIndex = snapGridStart(Math.round(maxStart * ratio), columns, maxStart);
            return;
        }
        if (draggingSliderTarget == SliderTarget.SONG_GRID) {
            int maxStart = maxGridStart(columns, visibleCards);
            if (maxStart <= 0) return;
            int knobH = Math.max(28, Math.round(lastGridSliderH * (visibleCards / (float) Math.max(1, songs.size()))));
            float ratio = clamp((float) ((mouseY - lastGridSliderY - knobH / 2.0F) / Math.max(1, lastGridSliderH - knobH)));
            firstSongIndex = snapGridStart(Math.round(maxStart * ratio), columns, maxStart);
        }
    }

    private int snapGridStart(int value, int columns, int maxStart) {
        int safeColumns = Math.max(1, columns);
        int snapped = (Math.max(0, value) / safeColumns) * safeColumns;
        return Math.max(0, Math.min(maxStart, snapped));
    }

    private void preloadPlaylistCovers(int start, int count) {
        int from = Math.max(0, start - Math.max(0, count / 3));
        int to = Math.min(recommendedPlaylists.size(), start + Math.max(0, count));
        for (int i = from; i < to; i++) {
            NeteaseMusicCovers.preload(recommendedPlaylists.get(i).coverUrl());
        }
    }

    private void preloadSongCovers(int start, int count) {
        int from = Math.max(0, start - Math.max(0, count / 3));
        int to = Math.min(songs.size(), start + Math.max(0, count));
        for (int i = from; i < to; i++) {
            NeteaseMusicCovers.preload(songs.get(i).image());
        }
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * clamp(speed);
    }

    private float contentAppearProgress() {
        return easeOutCubic((System.currentTimeMillis() - openStartedAt) / 420.0F);
    }

    private void switchView(ViewMode next) {
        if (viewMode != next) {
            beginContentTransition();
        }
        viewMode = next;
    }

    private void beginContentTransition() {
        if (renderingTransitionSnapshot) {
            return;
        }
        pageTransitionStartedAt = System.currentTimeMillis();
        transitionViewMode = viewMode;
        transitionCurrentPlaylist = currentPlaylist;
        transitionFirstSongIndex = firstSongIndex;
        transitionFirstPlaylistIndex = firstPlaylistIndex;
        transitionVisualFirstSongIndex = visualFirstSongIndex;
        transitionVisualFirstPlaylistIndex = visualFirstPlaylistIndex;
        transitionSongs.clear();
        transitionSongs.addAll(songs);
        transitionRecommendedPlaylists.clear();
        transitionRecommendedPlaylists.addAll(recommendedPlaylists);
    }

    private void withTransitionSnapshot(Runnable renderAction) {
        if (renderingTransitionSnapshot) {
            renderAction.run();
            return;
        }
        ViewMode realViewMode = viewMode;
        Playlist realCurrentPlaylist = currentPlaylist;
        int realFirstSongIndex = firstSongIndex;
        int realFirstPlaylistIndex = firstPlaylistIndex;
        float realVisualFirstSongIndex = visualFirstSongIndex;
        float realVisualFirstPlaylistIndex = visualFirstPlaylistIndex;
        List<Song> realSongs = new ArrayList<>(songs);
        List<Playlist> realRecommended = new ArrayList<>(recommendedPlaylists);
        renderingTransitionSnapshot = true;
        try {
            viewMode = transitionViewMode;
            currentPlaylist = transitionCurrentPlaylist;
            firstSongIndex = transitionFirstSongIndex;
            firstPlaylistIndex = transitionFirstPlaylistIndex;
            visualFirstSongIndex = transitionVisualFirstSongIndex;
            visualFirstPlaylistIndex = transitionVisualFirstPlaylistIndex;
            songs.clear();
            songs.addAll(transitionSongs);
            recommendedPlaylists.clear();
            recommendedPlaylists.addAll(transitionRecommendedPlaylists);
            renderAction.run();
        } finally {
            songs.clear();
            songs.addAll(realSongs);
            recommendedPlaylists.clear();
            recommendedPlaylists.addAll(realRecommended);
            viewMode = realViewMode;
            currentPlaylist = realCurrentPlaylist;
            firstSongIndex = realFirstSongIndex;
            firstPlaylistIndex = realFirstPlaylistIndex;
            visualFirstSongIndex = realVisualFirstSongIndex;
            visualFirstPlaylistIndex = realVisualFirstPlaylistIndex;
            renderingTransitionSnapshot = false;
        }
    }

    private ContentTransition contentTransition() {
        if (pageTransitionStartedAt <= 0L) {
            return new ContentTransition(1.0F, 1.0F, false);
        }
        float progress = (System.currentTimeMillis() - pageTransitionStartedAt) / (float) PAGE_TRANSITION_MS;
        if (progress >= 1.0F) {
            pageTransitionStartedAt = 0L;
            return new ContentTransition(1.0F, 1.0F, false);
        }
        if (progress < 0.5F) {
            float phase = easeInOutCubic(progress / 0.5F);
            return new ContentTransition(lerp(1.0F, 0.5F, phase), lerp(1.0F, 0.94F, phase), true);
        }
        float phase = easeOutCubic((progress - 0.5F) / 0.5F);
        return new ContentTransition(phase, lerp(1.08F, 1.0F, phase), false);
    }

    private int playlistCoverSize() {
        return Math.min(205, Math.max(150, (height - PLAYER_HEIGHT) / 3));
    }

    private String estimatePlaylistDuration() {
        long totalMs = 0L;
        for (Song song : songs) {
            totalMs += Math.max(0L, song.durationMs());
        }
        long totalSeconds = totalMs / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (Config.isChinese) {
            return hours > 0L ? hours + "时" + minutes + "分" + seconds + "秒" : minutes + "分" + seconds + "秒";
        }
        return hours > 0L ? hours + "h " + minutes + "m " + seconds + "s" : minutes + "m " + seconds + "s";
    }

    private void previewSeekFromMouse(double mouseX) {
        int centerX = SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2;
        int x = centerX - 160;
        int w = 320;
        pendingProgress = clamp((float) ((mouseX - x) / w));
    }

    private void commitSeek() {
        if (pendingProgress >= 0.0F) {
            MusicPlaybackService.INSTANCE.seekToProgress(pendingProgress);
        }
    }

    private void ensureIndexVisible(int index) {
        if (index < 0) {
            return;
        }
        int columns = gridColumns();
        int maxStart = maxGridStart();
        if (index < firstSongIndex) {
            firstSongIndex = Math.max(0, (index / columns) * columns);
        } else {
            int contentX = SIDEBAR_WIDTH + 22;
            int gridY = 82;
            int availableW = Math.max(1, width - contentX - 24);
            int cardW = Math.max(92, Math.min(132, (availableW - 28) / Math.max(2, availableW / 170)));
            int visibleRows = Math.max(1, (height - PLAYER_HEIGHT - gridY - 16) / (cardW + GRID_TEXT_HEIGHT));
            int visibleCards = visibleRows * columns;
            if (index >= firstSongIndex + visibleCards) {
                firstSongIndex = Math.min(maxStart, Math.max(0, (index / columns - visibleRows + 1) * columns));
            }
        }
    }

    private String focusedText() {
        return text(focus);
    }

    private String text(Focus field) {
        return switch (field) {
            case SEARCH -> query;
            case PLAYLIST_SEARCH -> playlistSearchQuery;
            case PHONE -> phone;
            case PASSWORD -> password;
            case NONE -> "";
        };
    }

    private void setText(Focus field, String value) {
        switch (field) {
            case SEARCH -> query = value;
            case PLAYLIST_SEARCH -> {
                playlistSearchQuery = value;
                applyPlaylistFilter();
            }
            case PHONE -> phone = value;
            case PASSWORD -> password = value;
            case NONE -> {
            }
        }
    }

    private int cursor(Focus field) {
        return switch (field) {
            case SEARCH -> queryCursor;
            case PLAYLIST_SEARCH -> playlistSearchCursor;
            case PHONE -> phoneCursor;
            case PASSWORD -> passwordCursor;
            case NONE -> 0;
        };
    }

    private void setCursor(Focus field, int value) {
        int cursor = Math.max(0, Math.min(value, text(field).length()));
        switch (field) {
            case SEARCH -> queryCursor = cursor;
            case PLAYLIST_SEARCH -> playlistSearchCursor = cursor;
            case PHONE -> phoneCursor = cursor;
            case PASSWORD -> passwordCursor = cursor;
            case NONE -> {
            }
        }
    }

    private int selectionAnchor(Focus field) {
        return switch (field) {
            case SEARCH -> querySelection;
            case PLAYLIST_SEARCH -> playlistSearchSelection;
            case PHONE -> phoneSelection;
            case PASSWORD -> passwordSelection;
            case NONE -> -1;
        };
    }

    private void setSelectionAnchor(Focus field, int value) {
        int anchor = value < 0 ? -1 : Math.max(0, Math.min(value, text(field).length()));
        switch (field) {
            case SEARCH -> querySelection = anchor;
            case PLAYLIST_SEARCH -> playlistSearchSelection = anchor;
            case PHONE -> phoneSelection = anchor;
            case PASSWORD -> passwordSelection = anchor;
            case NONE -> {
            }
        }
    }

    private void focusAt(Focus field, int inputX, double mouseX) {
        focus = field;
        setCursor(field, cursorFromMouse(field, inputX, mouseX));
        setSelectionAnchor(field, -1);
        draggingTextSelection = field;
    }

    private int inputX(Focus field) {
        return switch (field) {
            case SEARCH -> 12;
            case PLAYLIST_SEARCH -> playlistSearchInputX;
            case PHONE -> phoneInputX;
            case PASSWORD -> passwordInputX;
            case NONE -> 0;
        };
    }

    private int cursorFromMouse(Focus field, int inputX, double mouseX) {
        String value = text(field);
        String display = field == Focus.PASSWORD ? "*".repeat(value.length()) : value;
        int relativeX = (int) Math.round(mouseX - inputX - 8);
        if (relativeX <= 0 || value.isEmpty()) {
            return 0;
        }
        int index = 0;
        while (index < value.length()) {
            int next = value.offsetByCodePoints(index, 1);
            int currentWidth = font.width(display.substring(0, Math.min(index, display.length())));
            int nextWidth = font.width(display.substring(0, Math.min(next, display.length())));
            if (relativeX < (currentWidth + nextWidth) / 2) {
                return index;
            }
            index = next;
        }
        return value.length();
    }

    private void insertText(String inserted) {
        Focus field = focus;
        int start = selectionStart(field);
        int end = selectionEnd(field);
        String value = text(field);
        String next = value.substring(0, start) + inserted + value.substring(end);
        setText(field, next);
        setCursor(field, start + inserted.length());
        setSelectionAnchor(field, -1);
    }

    private void deletePrevious() {
        Focus field = focus;
        if (deleteSelection(field)) {
            return;
        }
        String value = text(field);
        int cursor = cursor(field);
        if (cursor <= 0) {
            return;
        }
        int start = value.offsetByCodePoints(cursor, -1);
        setText(field, value.substring(0, start) + value.substring(cursor));
        setCursor(field, start);
    }

    private void deleteNext() {
        Focus field = focus;
        if (deleteSelection(field)) {
            return;
        }
        String value = text(field);
        int cursor = cursor(field);
        if (cursor >= value.length()) {
            return;
        }
        int end = value.offsetByCodePoints(cursor, 1);
        setText(field, value.substring(0, cursor) + value.substring(end));
        setCursor(field, cursor);
    }

    private boolean deleteSelection(Focus field) {
        int start = selectionStart(field);
        int end = selectionEnd(field);
        if (start == end) {
            setSelectionAnchor(field, -1);
            return false;
        }
        String value = text(field);
        setText(field, value.substring(0, start) + value.substring(end));
        setCursor(field, start);
        setSelectionAnchor(field, -1);
        return true;
    }

    private int selectionStart(Focus field) {
        int anchor = selectionAnchor(field);
        int cursor = cursor(field);
        return Math.min(anchor < 0 ? cursor : anchor, cursor);
    }

    private int selectionEnd(Focus field) {
        int anchor = selectionAnchor(field);
        int cursor = cursor(field);
        return Math.max(anchor < 0 ? cursor : anchor, cursor);
    }

    private void moveCursor(int codePointDelta, boolean selecting) {
        Focus field = focus;
        String value = text(field);
        int oldCursor = cursor(field);
        int next = oldCursor;
        if (codePointDelta < 0 && oldCursor > 0) {
            next = value.offsetByCodePoints(oldCursor, -1);
        } else if (codePointDelta > 0 && oldCursor < value.length()) {
            next = value.offsetByCodePoints(oldCursor, 1);
        }
        moveCursorTo(next, selecting);
    }

    private void moveCursorTo(int next, boolean selecting) {
        Focus field = focus;
        if (selecting && selectionAnchor(field) < 0) {
            setSelectionAnchor(field, cursor(field));
        } else if (!selecting) {
            setSelectionAnchor(field, -1);
        }
        setCursor(field, next);
    }

    private void selectAll() {
        Focus field = focus;
        setSelectionAnchor(field, 0);
        setCursor(field, text(field).length());
    }

    private boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    private static boolean hit(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String cleanMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message != null && message.contains("Local Netease service is unavailable")) {
            return Config.isChinese ? "网易云本地服务未启动" : "Netease local service is offline";
        }
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private static String trim(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String trimToWidth(String text, float maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (FontRenderer.measureTextWidth(text, 12f) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + suffix;
            if (FontRenderer.measureTextWidth(candidate, 12f) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return suffix;
    }

    private static String formatPlayCount(long count) {
        if (count >= 100_000_000L) {
            return String.format("%.1f亿", count / 100_000_000.0D);
        }
        if (count >= 10_000L) {
            return String.format("%.1f万", count / 10_000.0D);
        }
        return String.valueOf(Math.max(0L, count));
    }

    private AnimationState animationState() {
        long now = System.currentTimeMillis();
        if (closing) {
            float progress = clamp((now - closeStartedAt) / (float) EXIT_DURATION_MS);
            float eased = easeInOutCubic(progress);
            float scale = progress < 0.32F
                    ? lerp(1.0F, 1.025F, easeOutCubic(progress / 0.32F))
                    : lerp(1.025F, 0.98F, easeInCubic((progress - 0.32F) / 0.68F));
            return new AnimationState(scale, 1.0F - eased, progress >= 1.0F);
        }

        float progress = clamp((now - openStartedAt) / (float) ENTER_DURATION_MS);
        float scale = progress < 0.72F
                ? lerp(0.98F, 1.015F, easeOutCubic(progress / 0.72F))
                : lerp(1.015F, 1.0F, easeInOutCubic((progress - 0.72F) / 0.28F));
        return new AnimationState(scale, easeOutCubic(progress), false);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp(progress);
    }

    private static float easeOutCubic(float value) {
        value = clamp(value);
        float inv = 1.0F - value;
        return 1.0F - inv * inv * inv;
    }

    private static float easeInCubic(float value) {
        value = clamp(value);
        return value * value * value;
    }

    private static float easeInOutCubic(float value) {
        value = clamp(value);
        return value < 0.5F ? 4.0F * value * value * value : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0F) * 0.5F;
    }

    private enum Focus {
        NONE,
        SEARCH,
        PLAYLIST_SEARCH,
        PHONE,
        PASSWORD
    }

    private enum LoginMode {
        PASSWORD,
        QR
    }

    private enum ViewMode {
        HOME,
        PLAYLIST,
        SEARCH
    }

    private enum SliderTarget {
        NONE,
        PLAYLIST_GRID,
        SONG_GRID
    }

    private record AnimationState(float scale, float alpha, boolean done) {
    }

    private record GridScroll(int base, float offset) {
    }

    private record ContentTransition(float alpha, float scale, boolean oldPage) {
    }
}
