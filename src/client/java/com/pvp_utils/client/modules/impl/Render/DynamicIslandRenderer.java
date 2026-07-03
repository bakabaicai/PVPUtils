package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DynamicIslandRenderer {
    private static final DynamicIslandRenderer INSTANCE = new DynamicIslandRenderer();
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "dynamic_island_text");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);
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
    private static final int TEXT_COLOR = 0xF2111827;
    private static final int SEPARATOR_COLOR = 0x8A111827;
    private static final int BLUR_TINT = 0x72FFFFFF;
    private static final float BLUR_STRENGTH = 0.85f;
    private static final float SIZE_EASE_SPEED = 13.5f;
    private static final float TAB_FADE_EASE_SPEED = 9.0f;
    private static final long TAB_REQUEST_TTL_MS = 120L;

    private Surface surface;
    private DynamicTexture texture;
    private boolean nativeLoaded = false;
    private int textureW = -1;
    private int textureH = -1;
    private int textureRegionW = -1;
    private int textureRegionH = -1;
    private String lastContentKey = "";
    private float lastWidth = -1f;
    private float lastScale = -1f;
    private float animatedWidth = -1f;
    private float animatedHeight = -1f;
    private float tabContentFade = 0f;
    private long lastAnimationTime = 0L;
    private long lastTabRequestTime = 0L;

    public static DynamicIslandRenderer getInstance() {
        return INSTANCE;
    }

    public void requestTabListFrame() {
        lastTabRequestTime = System.currentTimeMillis();
    }

    public void render(GuiGraphics graphics) {
        if (!Config.dynamicIsland) {
            destroyTexture(Minecraft.getInstance());
            resetAnimation();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getWindow() == null) {
            return;
        }
        if (client.screen instanceof AbstractContainerScreen<?>) {
            return;
        }

        IslandContent content = buildContent(client);
        boolean tabOpen = isTabOpen();
        List<PlayerInfo> tabPlayers = tabOpen ? getTabPlayers(client) : List.of();
        IslandLayout targetLayout = tabOpen ? measureTabLayout(client, tabPlayers) : measureLayout(client, content);
        IslandLayout layout = updateAnimatedLayout(targetLayout);
        float x = (client.getWindow().getGuiScaledWidth() - layout.width) * 0.5f;
        float y = TOP;

        SkiaBlurRenderer.getInstance().render(client, x, y, layout.width, layout.height, layout.radius, BLUR_TINT, BLUR_STRENGTH);
        renderTextTexture(client, content, tabPlayers, layout, tabOpen);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f,
                Math.round(layout.width), Math.round(layout.height), textureRegionW, textureRegionH, textureW, textureH);
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
        String username = client.getUser() != null ? client.getUser().getName() : "Unknown";
        String location = getLocationText(client);
        String fps = String.valueOf(client.getFps());
        return new IslandContent("PVPUtils", username, location, fps);
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

    private IslandLayout measureTabLayout(Minecraft client, List<PlayerInfo> players) {
        int count = Math.max(1, players.size());
        int columns = Math.max(1, (count + TAB_MAX_ROWS - 1) / TAB_MAX_ROWS);
        int rows = Math.max(1, (count + columns - 1) / columns);
        float nameW = 120f;
        for (PlayerInfo player : players) {
            nameW = Math.max(nameW, measure(getPlayerName(player)));
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

    private void renderTextTexture(Minecraft client, IslandContent content, List<PlayerInfo> tabPlayers, IslandLayout layout, boolean tabOpen) {
        ensureNativeLoaded();
        float scale = Math.max(1f, (float) client.getWindow().getGuiScale());
        int targetW = Math.max(1, Math.round(layout.width * scale));
        int targetH = Math.max(1, Math.round(layout.height * scale));
        String key = content.key() + "|" + tabKey(tabPlayers, tabOpen) + "|" + Math.round(layout.width) + "x" + Math.round(layout.height) + "|" + Math.round(tabContentFade * 255f);
        if (texture != null && targetW == textureW && targetH == textureH && key.equals(lastContentKey) && scale == lastScale) {
            return;
        }

        if (surface == null || texture == null || targetW > textureW || targetH > textureH) {
            destroyTexture(client);
            int capacityW = nextTextureCapacity(targetW);
            int capacityH = nextTextureCapacity(targetH);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), capacityW, capacityH), 0, SURFACE_PROPS);
            texture = new DynamicTexture("pvp_utils:dynamic_island_text", capacityW, capacityH, false);
            client.getTextureManager().register(TEXTURE_ID, texture);
            textureW = capacityW;
            textureH = capacityH;
        }
        textureRegionW = targetW;
        textureRegionH = targetH;

        Canvas canvas = surface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(scale, scale);
        if (tabOpen || tabContentFade > 0f) {
            drawTabContent(canvas, tabPlayers, layout, tabContentFade);
        } else {
            drawCenteredContent(canvas, content, layout);
        }
        canvas.restore();

        uploadSurface(surface, texture, textureW, textureH);
        lastContentKey = key;
        lastWidth = layout.width;
        lastScale = scale;
    }

    private String tabKey(List<PlayerInfo> players, boolean tabOpen) {
        if (!tabOpen) return "compact";
        StringBuilder key = new StringBuilder("tab:");
        for (int i = 0; i < Math.min(players.size(), 80); i++) {
            PlayerInfo player = players.get(i);
            key.append(player.getProfile().id()).append('@').append(player.getLatency()).append(';');
        }
        return key.toString();
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
        FontRenderer.drawText(canvas, text, x, y, TEXT_SIZE, TEXT_COLOR);
        return x + measure(text);
    }

    private float drawIconSegment(Canvas canvas, String icon, String text, float x, float y) {
        FontRenderer.drawText(canvas, icon, x, y + ICON_Y_OFFSET, ICON_SIZE, TEXT_COLOR, FontRenderer.MATERIAL_SYMBOLS);
        x += measureIcon() + ICON_GAP;
        FontRenderer.drawText(canvas, text, x, y, TEXT_SIZE, TEXT_COLOR);
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
        FontRenderer.drawText(canvas, "|", x, y, TEXT_SIZE, SEPARATOR_COLOR);
        return x + measure("|") + SEPARATOR_GAP;
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
            String name = trimToWidth(getPlayerName(player), columnW - 46f, TAB_NAME_SIZE);
            int color = player.getGameMode() == GameType.SPECTATOR ? 0xFFAAAAAA : 0xFFFFFFFF;
            FontRenderer.drawText(canvas, name, x, y, TAB_NAME_SIZE, withAlpha(color, alpha));

            String latency = formatLatency(player.getLatency());
            float latencyW = FontRenderer.measureTextWidth(latency, 9f);
            FontRenderer.drawText(canvas, latency, x + columnW - latencyW, y, 9f, withAlpha(latencyColor(player.getLatency()), alpha));
        }
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

    private int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (clamp(alpha, 0, 255) << 24);
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int width, int height) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) return;
            long addr = pixmap.getAddr();
            int byteSize = height * pixmap.getRowBytes();
            ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
            GpuTexture gpuTexture = targetTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
        } finally {
            pixmap.close();
        }
    }

    private void destroyTexture(Minecraft client) {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (texture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
        textureW = -1;
        textureH = -1;
        textureRegionW = -1;
        textureRegionH = -1;
        lastContentKey = "";
        lastWidth = -1f;
        lastScale = -1f;
    }

    private void resetAnimation() {
        animatedWidth = -1f;
        animatedHeight = -1f;
        tabContentFade = 0f;
        lastAnimationTime = 0L;
        lastTabRequestTime = 0L;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int nextTextureCapacity(int value) {
        int capacity = 64;
        while (capacity < value) {
            capacity *= 2;
        }
        return capacity;
    }

    private record IslandContent(String brand, String username, String location, String fps) {
        String key() {
            return brand + "|" + username + "|" + location + "|" + fps;
        }
    }

    private record IslandLayout(float width, float height, float radius, boolean isTab) {
    }
}
