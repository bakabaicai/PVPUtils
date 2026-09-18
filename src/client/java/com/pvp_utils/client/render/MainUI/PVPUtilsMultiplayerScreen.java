package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.gui.MultiplayerCompatibilityScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PVPUtilsMultiplayerScreen extends Screen {
    private final Screen parent;
    private final String shaderPath;
    private final Runnable embeddedBack;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final List<ServerData> servers = new ArrayList<>();
    private final List<Float> hover = new ArrayList<>();
    private final Map<Integer, Image> serverIcons = new HashMap<>();
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private ExecutorService pingExecutor;
    private Image defaultServerIcon;
    private ServerList serverList;
    private boolean pendingFrame;
    private int mouseX;
    private int mouseY;
    private int selected = -1;
    private float scroll;
    private float targetScroll;
    private long openStartMs;
    private long closeStartMs;
    private boolean closingToMain;
    private boolean backDispatched;
    private boolean returningFromManageServer;

    public PVPUtilsMultiplayerScreen(Screen parent) {
        this(parent, MainUISharedBackground.activeShaderPath());
    }

    public PVPUtilsMultiplayerScreen(Screen parent, String shaderPath) {
        this(parent, shaderPath, null);
    }

    public PVPUtilsMultiplayerScreen(Screen parent, String shaderPath, Runnable embeddedBack) {
        super(Component.literal("Multiplayer"));
        this.parent = parent;
        this.shaderPath = shaderPath;
        this.embeddedBack = embeddedBack;
    }

    public void initEmbedded(Minecraft client, int width, int height) {
        init(width, height);
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        backDispatched = false;
        serverList = new ServerList(minecraft);
        serverList.load();
        reloadServers();
    }

    private void reloadServers() {
        servers.clear();
        hover.clear();
        if (serverList != null) {
            for (int i = 0; i < serverList.size(); i++) {
                servers.add(serverList.get(i));
                hover.add(0f);
            }
        }
        selected = servers.isEmpty() ? -1 : Math.min(selected < 0 ? 0 : selected, servers.size() - 1);
        clampScroll();
        pingServers();
    }

    private void pingServers() {
        pinger.removeAll();
        if (pingExecutor == null || pingExecutor.isShutdown()) {
            pingExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "PVPUtils-ServerPing");
                thread.setDaemon(true);
                return thread;
            });
        }
        for (ServerData server : servers) {
            server.setState(ServerData.State.PINGING);
            server.motd = Component.empty();
            server.status = Component.empty();
            pingExecutor.execute(() -> {
                try {
                    pinger.pingServer(
                            server,
                            () -> minecraft.execute(() -> {
                                if (serverList != null) serverList.save();
                            }),
                            () -> minecraft.execute(() -> server.setState(
                                    server.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                            ? ServerData.State.SUCCESSFUL
                                            : ServerData.State.INCOMPATIBLE
                            )),
                            EventLoopGroupHolder.remote(minecraft.options.useNativeTransport())
                    );
                } catch (UnknownHostException exception) {
                    minecraft.execute(() -> server.setState(ServerData.State.UNREACHABLE));
                } catch (Exception exception) {
                    minecraft.execute(() -> server.setState(ServerData.State.UNREACHABLE));
                }
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        pinger.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (embeddedBack == null && minecraft.screen == this) {
            MainUISharedBackground.render(graphics, mouseX, mouseY);
        }
        float layoutScale = layoutScale();
        this.mouseX = MainUiScale.pageX(mouseX, width, layoutScale, layoutCenterX());
        this.mouseY = MainUiScale.pageY(mouseY, height, layoutScale, layoutCenterY());
        scroll += (targetScroll - scroll) * 0.20f;
        pendingFrame = true;
        if (closingToMain && closeProgress() >= 1f && minecraft != null) {
            if (embeddedBack != null) {
                if (!backDispatched) {
                    backDispatched = true;
                    if (minecraft.screen == this) {
                        minecraft.setScreen(PVPUtilsMainUI.returningFromSingleplayer(shaderPath));
                    } else {
                        embeddedBack.run();
                    }
                }
            } else {
                minecraft.setScreen(PVPUtilsMainUI.returningFromSingleplayer(shaderPath));
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public void renderFrameEnd() {
        if (!pendingFrame || minecraft == null || (embeddedBack == null && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            float layoutScale = layoutScale();
            float x = cardX();
            float y = cardY();
            float w = cardW();
            float h = cardH();
            if (!closingToMain) {
                SkiaBlurRenderer.getInstance().render(canvas, glBackend.getContext(), minecraft, mainFramebufferId(),
                        MainUiScale.pageScreenX(x, width, layoutScale, layoutCenterX()),
                        MainUiScale.pageScreenY(y, height, layoutScale, layoutCenterY()),
                        MainUiScale.pageScreenSize(w, layoutScale),
                        MainUiScale.pageScreenSize(h, layoutScale),
                        MainUiScale.pageScreenSize(20f, layoutScale),
                        0x12000000,
                        0.95f);
            }
            canvas.save();
            MainUiScale.applyPage(canvas, width, height, layoutScale, layoutCenterX(), layoutCenterY());
            draw(canvas);
            canvas.restore();
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    private void draw(Canvas canvas) {
        float contentAlpha = closingToMain ? 1f - ease(closeProgress()) : ease(openProgress());
        float x = cardX();
        float y = cardY();
        float w = cardW();
        float h = cardH();
        try (Paint card = new Paint(); Paint stroke = new Paint()) {
            card.setAntiAlias(true);
            card.setColor(0x32101010);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 20f), card);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, w - 1f, h - 1f, 20f), stroke);
        }
        String title = "Multiplayer";
        int alpha = Math.round(255f * contentAlpha);
        FontRenderer.drawText(canvas, title, layoutWidth() * .5f - FontRenderer.measureTextWidth(title, 30f) * .5f, 50f, 30f, (alpha << 24) | 0xFFFFFF);
        drawServers(canvas, x + 16f, y + 16f, w - 32f, h - 40f, alpha);
        float buttonY = y + h + 12f;
        float buttonW = (w - 32f) / 5f;
        drawButton(canvas, bottomButtonX(x, buttonW, 0), buttonY, buttonW, 32f, "Add", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 1), buttonY, buttonW, 32f, "Edit", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 2), buttonY, buttonW, 32f, "Delete", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 3), buttonY, buttonW, 32f, "Settings", alpha);
        drawButton(canvas, bottomButtonX(x, buttonW, 4), buttonY, buttonW, 32f, "Back", alpha);
    }

    private void drawServers(Canvas canvas, float x, float y, float w, float h, int alpha) {
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(x, y, w, h));
        if (servers.isEmpty()) {
            FontRenderer.drawText(canvas, "No servers", x + w * .5f - 42f, y + h * .5f, 14f, (Math.round(alpha * .8f) << 24) | 0xFFFFFF);
        } else {
            float itemY = y - scroll;
            for (int i = 0; i < servers.size(); i++) {
                drawServer(canvas, servers.get(i), i, x, itemY, w, 64f, alpha);
                itemY += 72f;
            }
        }
        canvas.restore();
    }

    private void drawServer(Canvas canvas, ServerData server, int index, float x, float y, float w, float h, int alpha) {
        if (y + h < cardY() + 16f || y > cardY() + cardH() - 24f) return;
        boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float p = hover.get(index);
        p += ((over ? 1f : 0f) - p) * .16f;
        hover.set(index, p);
        float curve = p * p * (3f - 2f * p);
        boolean active = selected == index;
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            float opacity = active ? .18f + curve * .10f : .07f + curve * .12f;
            bg.setColor((Math.round(alpha * opacity) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 13f), bg);
        }
        Image icon = serverIcon(index, server);
        if (icon != null) {
            try (Paint imagePaint = new Paint()) {
                imagePaint.setAntiAlias(true);
                imagePaint.setColor((alpha << 24) | 0xFFFFFF);
                canvas.save();
                canvas.clipRRect(RRect.makeXYWH(x + 10f, y + 10f, 44f, 44f, 10f), true);
                canvas.drawImageRect(icon, Rect.makeXYWH(0f, 0f, icon.getWidth(), icon.getHeight()),
                        Rect.makeXYWH(x + 10f, y + 10f, 44f, 44f), SamplingMode.LINEAR, imagePaint, true);
                canvas.restore();
            }
        } else {
            FontRenderer.drawText(canvas, "\uE88A", x + 26f, y + 39f, 25f, (Math.round(alpha * .86f) << 24) | 0xFFFFFF, FontRenderer.MATERIAL_SYMBOLS);
        }
        float rightX = x + w - 12f;
        String pingText = pingText(server);
        String playersText = playersText(server);
        float reserved = Math.max(FontRenderer.measureTextWidth(pingText, 11f), FontRenderer.measureTextWidth(playersText, 11f)) + 18f;
        float textWidth = Math.max(40f, w - 66f - reserved - 12f);
        String serverName = fitText(server.name, 15f, textWidth);
        String serverAddress = fitText(server.ip, 11f, textWidth);
        FontRenderer.drawText(canvas, serverName, x + 66f, y + 26f, 15f, (alpha << 24) | 0xFFFFFF);
        FontRenderer.drawText(canvas, serverAddress, x + 66f, y + 46f, 11f, (Math.round(alpha * .72f) << 24) | 0xFFFFFF);
        FontRenderer.drawText(canvas, pingText, rightX - FontRenderer.measureTextWidth(pingText, 11f), y + 26f, 11f,
                (Math.round(alpha * .92f) << 24) | pingColor(server));
        FontRenderer.drawText(canvas, playersText, rightX - FontRenderer.measureTextWidth(playersText, 11f), y + 46f, 11f,
                (Math.round(alpha * .72f) << 24) | 0xFFFFFF);
    }

    private void drawButton(Canvas canvas, float x, float y, float w, float h, String label, int alpha) {
        boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float p = over ? 1f : 0f;
        int color = lerpRgb(0x67B9EA, 0xA7E0FF, p);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * (.20f + .12f * p)) << 24) | color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
        }
        FontRenderer.drawText(canvas, label, x + (w - FontRenderer.measureTextWidth(label, 13f)) * .5f, y + 21f, 13f, (alpha << 24) | 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        event = MainUiScale.pageEvent(event, width, height, layoutScale(), layoutCenterX(), layoutCenterY());
        if (event.button() != 0) return true;
        float x = cardX();
        float y = cardY() + cardH() + 12f;
        float buttonW = (cardW() - 32f) / 5f;
        if (inside(event.x(), event.y(), bottomButtonX(x, buttonW, 0), y, buttonW, 32f)) {
            playClick();
            returningFromManageServer = true;
            prepareOverlayReturn();
            ServerData data = new ServerData("", "", ServerData.Type.OTHER);
            minecraft.setScreen(new ManageServerScreen(this, Component.literal("Add Server"), ok -> {
                if (ok) {
                    serverList.add(data, false);
                    serverList.save();
                    reloadServers();
                }
                returnFromOverlay();
            }, data));
            return true;
        }
        if (inside(event.x(), event.y(), bottomButtonX(x, buttonW, 1), y, buttonW, 32f) && selected >= 0) {
            playClick();
            returningFromManageServer = true;
            prepareOverlayReturn();
            ServerData data = servers.get(selected);
            minecraft.setScreen(new ManageServerScreen(this, Component.literal("Edit Server"), ok -> {
                if (ok) {
                    serverList.replace(selected, data);
                    serverList.save();
                    reloadServers();
                }
                returnFromOverlay();
            }, data));
            return true;
        }
        if (inside(event.x(), event.y(), bottomButtonX(x, buttonW, 2), y, buttonW, 32f) && selected >= 0) {
            playClick();
            serverList.remove(servers.get(selected));
            serverList.save();
            reloadServers();
            return true;
        }
        if (inside(event.x(), event.y(), bottomButtonX(x, buttonW, 3), y, buttonW, 32f)) {
            playClick();
            returningFromManageServer = true;
            prepareOverlayReturn();
            minecraft.setScreen(new MultiplayerCompatibilityScreen(embeddedBack == null ? this : parent));
            return true;
        }
        if (inside(event.x(), event.y(), bottomButtonX(x, buttonW, 4), y, buttonW, 32f)) {
            playClick();
            startClose();
            return true;
        }
        int hit = serverAt((float) event.x(), (float) event.y());
        if (hit >= 0) {
            if (selected == hit) {
                playClick();
                ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(servers.get(hit).ip), servers.get(hit), false, null);
            } else {
                selected = hit;
            }
        }
        return true;
    }

    private int serverAt(float mx, float my) {
        float x = cardX() + 16f;
        float y = cardY() + 16f;
        float w = cardW() - 32f;
        float h = cardH() - 40f;
        if (mx < x || mx > x + w || my < y || my > y + h) return -1;
        int index = (int) ((my - y + scroll) / 72f);
        float inside = (my - y + scroll) - index * 72f;
        return index >= 0 && index < servers.size() && inside <= 64f ? index : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll -= (float) verticalAmount * 48f;
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0f, servers.size() * 72f - (cardH() - 40f));
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        scroll = Math.max(0f, Math.min(max, scroll));
    }

    @Override
    public void onClose() {
        startClose();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        for (Image image : serverIcons.values()) if (image != null) image.close();
        serverIcons.clear();
        if (defaultServerIcon != null) {
            defaultServerIcon.close();
            defaultServerIcon = null;
        }
        if (returningFromManageServer) {
            returningFromManageServer = false;
        } else {
            pinger.removeAll();
            if (pingExecutor != null) {
                pingExecutor.shutdownNow();
                pingExecutor = null;
            }
            glBackend.destroy();
        }
        super.removed();
    }

    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private float openProgress() {
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openStartMs) / 440f));
    }

    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / 440f));
    }

    private float ease(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private Image serverIcon(int index, ServerData server) {
        if (serverIcons.containsKey(index)) return serverIcons.get(index);
        byte[] bytes = server.getIconBytes();
        if (bytes == null || bytes.length == 0) {
            return defaultServerIcon();
        }
        try {
            Image image = Image.makeFromEncoded(bytes);
            serverIcons.put(index, image);
            return image;
        } catch (RuntimeException ignored) {
            serverIcons.put(index, null);
            return defaultServerIcon();
        }
    }

    private Image defaultServerIcon() {
        if (defaultServerIcon != null) return defaultServerIcon;
        try {
            Identifier id = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
            var resource = minecraft.getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (var stream = resource.get().open()) {
                    defaultServerIcon = Image.makeFromEncoded(stream.readAllBytes());
                }
            }
        } catch (Exception ignored) {
            defaultServerIcon = null;
        }
        return defaultServerIcon;
    }

    private boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float bottomButtonX(float x, float buttonW, int index) {
        return x + index * (buttonW + 8f);
    }

    private void prepareOverlayReturn() {
        if (parent instanceof PVPUtilsMainUI mainUI && embeddedBack != null) {
            mainUI.preserveEmbeddedPagesForOverlay();
        }
    }

    private void returnFromOverlay() {
        if (parent instanceof PVPUtilsMainUI && embeddedBack != null) {
            minecraft.setScreen(parent);
        } else {
            minecraft.setScreen(this);
        }
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private int lerpRgb(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private String pingText(ServerData server) {
        return switch (server.state()) {
            case INITIAL, PINGING -> "Pinging...";
            case UNREACHABLE -> "Offline";
            case INCOMPATIBLE, SUCCESSFUL -> server.ping >= 0L ? server.ping + " ms" : "-- ms";
        };
    }

    private String playersText(ServerData server) {
        return server.players == null ? "-/-" : server.players.online() + "/" + server.players.max();
    }

    private int pingColor(ServerData server) {
        if (server.state() == ServerData.State.UNREACHABLE) return 0xFF6B6B;
        if (server.state() == ServerData.State.INITIAL || server.state() == ServerData.State.PINGING || server.ping < 0L) return 0xAEB8C2;
        if (server.ping <= 80L) return 0x62E58B;
        if (server.ping <= 150L) return 0xF2D46B;
        if (server.ping <= 250L) return 0xFFAA5C;
        return 0xFF6B6B;
    }

    private String fitText(String text, float size, float maxWidth) {
        String value = text == null ? "" : text;
        if (FontRenderer.measureTextWidth(value, size) <= maxWidth) return value;
        while (value.length() > 1 && FontRenderer.measureTextWidth(value + "...", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private float cardW() {
        return Math.max(320f, Math.min(500f, layoutWidth() * .52f));
    }

    private float cardH() {
        return Math.max(280f, Math.min(layoutHeight() - 150f, layoutHeight() * .70f));
    }

    private float cardX() {
        return (layoutWidth() - cardW()) * .5f;
    }

    private float cardY() {
        return 72f;
    }

    private int layoutWidth() {
        return MainUiScale.pageWidth();
    }

    private int layoutHeight() {
        return MainUiScale.pageHeight();
    }

    private float layoutScale() {
        float x = cardX();
        return MainUiScale.pageScale(
                x,
                20f,
                x + cardW(),
                cardY() + cardH() + 44f
        );
    }

    private float layoutCenterX() {
        return cardX() + cardW() * 0.5f;
    }

    private float layoutCenterY() {
        return (20f + cardY() + cardH() + 44f) * 0.5f;
    }
}
