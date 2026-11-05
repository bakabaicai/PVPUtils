# Chroma Graphics

English · [简体中文](README.zh-CN.md)

## 1. Introduction

Chroma Graphics is the client rendering framework of PVPUtils. It provides rendering integration for 2D screens, HUD elements, shared typography, backdrop blur, liquid glass, and world markers. The framework manages the regular 2D Skia context, framebuffer wrappers, rendering order, and OpenGL state restoration so that features can submit their content through established interfaces.

Chroma Graphics is the system name. Existing Java types retain names such as `SkijaRenderer` and `SkijaUi`; there is no unified entry class named `ChromaGraphics`.

| Item | Current implementation |
| --- | --- |
| Minecraft | 1.21.11, official Mojang mappings |
| Java | 21 |
| Skia Java binding | Skija 0.143.17 |
| Geometry types | HumbleUI Types 0.2.0 |
| Bundled native backend | Windows x64 |
| Regular 2D rendering | Skija, OpenGL, GUI logical coordinates |
| World rendering | `WorldRender` wrapping Minecraft Gizmos |

The framework supplies drawing facilities. It does not automatically register components, perform layout or hit testing, wrap text, schedule asynchronous tasks, or manage feature caches. Canvas scaling follows the GUI scale; responsive layout remains the responsibility of each interface.

### 1.1 Components

| Component | Responsibility | Intended callers |
| --- | --- | --- |
| `SkijaScreen` | 2D rendering entry for a screen | Custom `Screen` implementations |
| `SkijaRenderer` | Active canvas, main-target and overlay queues, texture borrowing, backdrop blur | Features use designated entry points; the framework owns frame scheduling |
| `SkijaUi` | Shapes, text, icons, shared fonts, prepared text | Preferred interface for regular UI and HUD rendering |
| `SkijaFrameRenderer` | Frame-level scheduling of HUDs, screens, and queues | Framework internals |
| `LiquidGlassRenderer` | Liquid-glass panels | Components that explicitly require this effect |
| `GlassCaptureRenderer`, `GlassGlBackend` | Glass capture and its dedicated Skia/OpenGL backend | Framework internals |
| `SkijaBackdrop`, `GlassGlState` | Backdrop sampling and GL state preservation | Package-private implementation |
| `WorldRender` | World geometry and world-to-screen projection | World feature modules |

Regular 2D rendering shares one managed Skia context. Liquid glass uses a separate managed backend with explicit flush and state-restoration boundaries. World geometry uses Minecraft Gizmos rather than the 2D Canvas.

## 2. Rendering Order and Integration

### 2.1 Current frame sequence

1. `MinecraftMixin` calls `SkijaRenderer.beginFrame()` at the start of `Minecraft.runTick`, clearing both submission queues.
2. Minecraft collects GUI content. Calls to `draw()` outside an active Skia canvas during this stage enter the main-target queue.
3. `GameRendererMixin` calls `renderMainSubmitted()` before `GuiRenderer.render(GpuBufferSlice)`, drawing that queue into the Minecraft main color target.
4. After Minecraft calls `blitToScreen()`, `SkijaFrameRenderer.render()` starts the overlay pass. It skips rendering when the window is minimized or a Minecraft loading overlay is present.
5. The overlay pass draws integrated HUDs and plugins, the current `SkijaScreen`, the `submit()` queue, and the HUD editor overlay, in that order.
6. At pass completion, Skia commands are submitted and external GL state is restored. Glass integration can introduce additional intermediate flushes.

The main-target pass uses an FBO wrapping the Minecraft main color texture. The overlay pass uses the default framebuffer, FBO 0. Pass setup checks dimensions, reuses or recreates the Surface, synchronizes GL state, and applies GUI coordinate scaling.

### 2.2 Choosing an entry point

| Scenario | Standard approach |
| --- | --- |
| New complete Skia screen | Implement `Screen implements SkijaScreen`; draw directly in `renderSkija(Canvas)` |
| A framework Canvas is already available | Call `SkijaUi`, `PreparedText`, or necessary Skija Canvas methods directly |
| Encapsulated local drawing callback | Use `SkijaRenderer.draw(painter)`; it executes immediately and protects Canvas state inside an active pass |
| Main-target content during GUI collection | Call `draw(painter)` before the main-target queue is drained |
| Overlay content from an existing per-frame HUD collection hook | Call `SkijaRenderer.submit(painter)` before the overlay queue is drained |
| World lines or markers | Call `WorldRender` in the existing world Gizmos collection stage |
| Liquid glass | Call `LiquidGlassRenderer.getInstance().renderPanel(...)` |

`draw()` and `submit()` have different semantics: without an active Canvas, the former queues main-target work; the latter always queues overlay work. The overlay queue runs after the screen. HUDs that must not cover menus must check visibility conditions such as `client.screen` themselves.

Queues serve the current frame only. They are neither persistent task queues nor thread-safe dispatchers. Do not submit Canvas operations from networking threads or enqueue during queue draining with the expectation of execution in the next frame. Asynchronous work publishes data; the client thread reads it and submits drawing in the appropriate stage.

Integrate each piece of content through one entry point. A screen or HUD already invoked by `SkijaFrameRenderer` must not submit duplicate drawing. Merely implementing a HUD class does not register it with the frame scheduler.

## 3. Interface Reference

The tables use compact call notation, not copyable Java declarations. `Canvas` denotes the first canvas parameter. Geometry, font size, line width, and effect strength generally use `float`; colors use `int`; text and font names use `String`. Square brackets denote existing overloads, not optional Java parameters. Drawing methods return `void` unless stated otherwise. Linked implementation files provide exact types and overloads.

### 3.1 Screens, submission, and textures

| Interface | Behavior and constraints |
| --- | --- |
| `SkijaScreen.renderSkija(Canvas)` | Actual Skia rendering for the current screen; Canvas is borrowed from the framework |
| `SkijaRenderer.draw(Consumer<Canvas>)` | Draw immediately on the active canvas, or enqueue main-target work |
| `SkijaRenderer.submit(Consumer<Canvas>)` | Enqueue current-frame overlay work |
| `SkijaRenderer.isDrawing(): boolean` | Whether the shared canvas is active |
| `SkijaRenderer.hasFailed(): boolean` | Whether an error has disabled the shared renderer |
| `drawBlurredBackdrop(Canvas, RRect, x, y, width, height, strength)` | Sample in screen GUI coordinates with rounded-rectangle clipping |
| `drawBlurredBackdrop(Canvas, io.github.humbleui.skija.Path, x, y, width, height, strength)` | Sample in screen GUI coordinates with path clipping |
| `borrowTexture(Identifier): BorrowedImage` | Borrow a Minecraft GL texture; may return `null` |
| `BorrowedImage.image(): Image` | Access the Skia image in the borrowed wrapper |
| `BorrowedImage.close()` | Release the wrapper without destroying the Minecraft-owned texture |

### 3.2 Shapes and effects: SkijaUi

| Interface | Purpose |
| --- | --- |
| `fill(Canvas, x, y, width, height, color)` | Rectangle fill |
| `rounded(Canvas, x, y, width, height, radius, color)` | Rounded-rectangle fill |
| `gradient(Canvas, x, y, width, height, startColor, endColor, boolean vertical, radius)` | Horizontal or vertical gradient |
| `gradientDiagonal(Canvas, x, y, width, height, startColor, endColor, radius)` | Diagonal gradient |
| `line(Canvas, x1, y1, x2, y2, thickness, color)` | Line segment |
| `line(Canvas, x1, y1, x2, y2, thickness, color, PaintStrokeCap cap)` | Line with an explicit cap; the original overload uses BUTT |
| `outline(Canvas, x, y, width, height, radius, thickness, color)` | Rectangle or rounded-rectangle stroke |
| `fillPath(Canvas, io.github.humbleui.skija.Path, color)` | Path fill |
| `strokePath(Canvas, io.github.humbleui.skija.Path, thickness, color, PaintStrokeCap cap, PaintStrokeJoin join)` | Path stroke with explicit cap and join styles |
| `dropShadowPath(Canvas, io.github.humbleui.skija.Path, blur, color)` | Path shadow |
| `dropShadowRounded(Canvas, x, y, width, height, radius, offsetY, blur, color)` | Rounded-rectangle shadow |
| `glowLayer(Canvas, x, y, width, height, blur, int strength, Runnable drawing)` | Local filtered layer; `strength` is clamped to 1–10 |

The callback in `glowLayer` produces the glow layer. Draw the sharp foreground separately when required. Shadow, glow, and gradient interfaces do not imply that their final pixels are cached.

Lines and path strokes reuse the shared Paint, with a minimum width of 0.1. They restore fill mode, default width, cap, and join state after drawing. Music UI rounded rectangles, lines, and icon paths use these interfaces without allocating separate basic-shape Paint objects.

### 3.3 Text and icons: SkijaUi

| Interface | Purpose |
| --- | --- |
| `text(Canvas, text, x, top, height, color[, size[, fontName]])` | Regular text, vertically centered in a line box |
| `boldText(Canvas, text, x, top, height, color[, size])` | Bold text, vertically centered in a line box |
| `textAtTop(Canvas, text, x, top, color, size, boolean bold)` | Position by the top of the font line box |
| `textShadow(Canvas, text, x, top, height, color[, size[, fontName]])` | Regular text with shadow |
| `boldTextShadow(Canvas, text, x, top, height, color, size)` | Bold text with shadow |
| `textWithFallback(Canvas, text, x, top, height, color, size[, fontName])` | Explicit fallback-text entry |
| `textShadowWithFallback(Canvas, text, x, top, height, color, size[, fontName])` | Fallback text with shadow |
| `textWidth(text[, size[, fontName]]): float` | Regular text width, including fallback fonts |
| `boldTextWidth(text[, size]): float` | Bold text width |
| `textWidthWithFallback(text, size[, fontName]): float` | Fallback text width |
| `textMetrics(size): FontMetrics` | Regular primary-font metrics, not the actual ink bounds of mixed-font text |
| `icon(Canvas, glyph, x, top, height, color, size, IconSet)` | Icon glyph |
| `iconWidth(glyph, size, IconSet): float` | Icon width |
| `iconMetrics(size, IconSet): FontMetrics` | Icon-font metrics |
| `prepareText(text, size, boolean bold): PreparedText` | Create reusable text glyph data |
| `PreparedText.width(): float` | Previously calculated width |
| `PreparedText.draw(Canvas, x, baseline, color)` | Draw using a baseline |
| `PreparedText.drawTop(Canvas, x, top, color)` | Draw using the top of a line box |
| `PreparedText.close()` | Release the owned TextBlob; repeated close is supported |

Overloads without a font size use 9. Regular `text` and `boldText` already use shared font fallback; CJK text does not require a separate renderer. `IconSet` supports `PVP_CONTROLS` and `MATERIAL_SYMBOLS`.

### 3.4 Font management: SkijaUi

| Interface | Purpose |
| --- | --- |
| `CLIENT_FONT` | Built-in font name, `"harmony"` |
| `availableFontNames(): List<String>` | List built-in and imported font names |
| `selectFont(String)` | Change the global font selection |
| `withFont(String, Runnable)` | Temporarily override font selection on the current thread; restore it after the callback |
| `textFontRevision(): long` | Revision of global font selection or font reloads |
| `fontDirectory(): java.nio.file.Path` | Obtain the font directory; may throw `IOException` |
| `importFont(java.nio.file.Path): String` | Import a font and return its name; may throw `IOException` |
| `reloadImportedFonts()` | Reload imported fonts and update the font revision |
| `close()` | Close shared font resources; framework lifecycle only |

Font import and reload perform synchronous file operations. Run them as explicit settings operations, never once per frame. Initial font use can also trigger lazy loading; existing interfaces can be used to warm fonts during initialization.

`textFontRevision()` does not change for temporary `withFont()` overrides. Caches using temporary fonts must also include the font name. Measurement and drawing must use the same font selection.

### 3.5 Liquid glass

The entry point is `LiquidGlassRenderer.getInstance()`.

| Interface | Purpose |
| --- | --- |
| `renderPanel(Minecraft, x, y, width, height, radius, int tintColor, boolean shadow, boolean highlight, float highlightTaper, int slot): boolean` | Draw a glass panel |
| The same overload with a trailing `SlotGrid grid` | Include internal panel slot geometry |
| `SlotGrid(int count, float x, float y, float pitch, float size, float radius)` | Panel-local GUI coordinates; `pitch` is horizontal spacing |
| `panelTint(): int` | Obtain the configured panel tint |
| `tick()`, `destroy()` | Framework-managed updates and cleanup |

Only slot indices 0–3 are valid; the implementation does not guard out-of-range indices. Allocate stable slots for concurrently used panels. Do not pass arbitrary module IDs. Slots reuse offscreen allocations, not the final rendered panel image.

Panels use screen GUI coordinates and a dedicated Canvas. They do not inherit the caller's local transform or clip. Sampling reads the Minecraft main framebuffer, whose source and timing differ from the regular overlay backdrop snapshot. A `false` return requires a fallback. A `true` return only indicates outer-flow success; unavailable internal capture can still produce no visible pixels. Provide a base background so that the component remains usable without glass output.

### 3.6 World rendering: WorldRender

| Interface | Purpose |
| --- | --- |
| `line(Vec3 from, Vec3 to, int color, float width[, boolean throughWalls])` | World-space line; depth-tested by default |
| `box(AABB, int outlineColor)` | Box outline |
| `box(AABB, int outlineColor, int fillColor)` | Box outline and fill |
| `filledBox(AABB, int fillColor[, boolean throughWalls])` | Box fill; depth-tested by default |
| `face(AABB, Vec3 viewer, int color, float width)` | Select and outline a box face from the viewer direction |
| `face(AABB, Direction, int color, float width)` | Outline a specified face |
| `project(Vec3): ScreenPoint` | Project world coordinates into GUI coordinates; nullable |
| `ScreenPoint(float x, float y)` | Projected GUI coordinates |
| `capture(Camera, Matrix4fc modelView, Matrix4fc projection)` | Capture the current world view; framework only |

Integrate world commands inside the `collectPerFrameGizmos()` collection established by `WorldRenderLevelRendererMixin`. Do not issue world drawing from 2D overlay callbacks. `project()` checks matrix availability, position relative to the camera, and screen X/Y limits; it does not perform depth or occlusion testing.

### 3.7 Framework internals

Some methods below are `public`, but they are not standard entry points for new features.

| Class | Interface group |
| --- | --- |
| `SkijaFrameRenderer` | `render()`, `close()` |
| `SkijaRenderer` | `beginFrame()`, `renderMainSubmitted()`, `renderSubmitted()` |
| `SkijaRenderer` | `renderOverlay(Consumer<Canvas>)`, `renderMainTarget(Consumer<Canvas>)` |
| `SkijaRenderer` | `currentFramebuffer()`, `flush()`, `restoreContext()`, `close()` |
| `GlassCaptureRenderer` | `getInstance()`, `currentDrawFramebufferId()`, `beginFrame(int)`, `endFrame()`, `context()`, `capture(Minecraft, x, y, width, height, margin)`, `destroy()` |
| `GlassCaptureRenderer.Capture` | `EMPTY`, `image`, `width`, `height`, `dstX`, `dstY`, `dstW`, `dstH` |
| `GlassGlBackend` | `begin()`, `begin(int)`, `end()`, `isDrawing()`, `hasSurface()` |
| `GlassGlBackend` | `getContext()`, `ensureContext()`, `resetCanvasState()`, `runWithSavedState(Runnable)`, `destroy()` |

`renderMainTarget()` reuses the current pass if the shared canvas is active; it does not force a target switch. `currentFramebuffer()` returns the framework's stored target state. Outside a pass, it cannot substitute for querying the actual GL binding. Do not call `flush()` per widget or create a new `DirectContext` for ordinary components.

## 4. Coordinates, Animation, and Resource Contracts

### 4.1 Coordinate spaces

The regular Canvas is already scaled into GUI logical coordinates. Complete screens use `Screen.width/height`; HUDs use `Window.getGuiScaledWidth()/getGuiScaledHeight()`. Do not apply the GUI scale again. Physical framebuffer dimensions belong to the backend.

Rectangle arguments are `x, y, width, height`, not bottom-right coordinates. Colors use `0xAARRGGBB`.

Calculate layout from the current available dimensions. If a design coordinate system and local scaling are used, apply the inverse of the same transform to mouse input. For example, `screenX = originX + localX * scale` implies `localX = (mouseX - originX) / scale`. Drawing, clipping, and hit testing must share the same frame geometry.

`MainUiScale` is specific to main-menu page layout. Changes to those pages should pair `applyPage`, `pageX/pageY/pageEvent`, and `pageScreenX/pageScreenY/pageScreenSize` with consistent scale and center values. Do not apply this policy to every HUD.

### 4.2 Text positioning and reuse

`text(..., top, height, ...)` centers text in a line box using font metrics:

`baseline = top + (height - (descent - ascent)) / 2 - ascent`

`height` is line-box height; `size` is font size. The top used by `textAtTop()` and `PreparedText.drawTop()` is the font line-box top, not the highest visible ink pixel. `PreparedText.draw()` accepts the baseline directly.

`PreparedText` retains TextBlob glyph data and width to avoid repeating fallback segmentation, measurement, and glyph construction on every frame. It is not a bitmap cache and does not automatically provide wrapping, ellipsis, complex text layout, or font-revision updates. CJK and missing glyphs follow the existing fallback policy. CJK prefers the system fallback font; requesting bold does not guarantee bold fallback glyphs.

Cache keys must include text, discrete font size, bold state, font revision, and any temporary font name. Wrapping or truncation caches must also include available width, translation-display mode, and other relevant layout conditions. Position and color changes do not require rebuilding a TextBlob.

### 4.3 Animation and lifecycle

Advance animation once per frame and retain the resulting position, scale, opacity, and clip geometry. Drawing and input methods read that result. Do not advance animation from multiple getters. Asynchronous data completion updates content and must not unconditionally restart the page entrance animation.

When Minecraft returns to an existing Screen instance, `added()` runs, but the protected `init()` method is not guaranteed to run again. Reset return animations in `added()` or explicit activation logic. Dimension-dependent layout must respond to resize/reposition or use current dimensions each frame. Instances shown again after resource cleanup must recreate their resources on demand.

Draw backdrop blur before local `translate/scale`, using screen GUI coordinates. Blur bounds, clip radius, and foreground must derive from the same frame geometry. Rotation or more complex transforms require an explicit screen-space sampling design; passing an untransformed local rectangle is insufficient.

### 4.4 Ownership

| Resource | Responsibility |
| --- | --- |
| Framework-provided `Canvas` | Borrowed; do not close it, retain it across frames, or pass it to workers |
| Internal `SkijaUi` Font, Typeface, and Paint | Shared resources; feature modules do not close them |
| Feature-created `PreparedText`, Paint, Path, Image, Surface, filters, and similar objects | Owned by the feature; release on invalidation, replacement, or shutdown |
| `BorrowedImage` | Borrow inside an active Skia callback and close the wrapper with try-with-resources |
| Minecraft texture | Owned by the Minecraft texture system, not the borrower |
| Framework contexts, Surface caches, and glass resources | Managed by the framework during recreation or client shutdown |

Pair `save()` with `restoreToCount()` to isolate local transforms and clipping, using `try/finally` to ensure restoration. Use `saveLayer()` only when group opacity or filtering requires it. Do not promote a feature cache into a global policy without evaluating every affected caller.

## 5. Usage Examples

The four classes below can be placed separately in the client source set. They demonstrate interface composition and lifecycle management; they do not supply automatic registration, navigation, or input controls.

### 5.1 Responsive screen with reusable text

Collect current dimensions in `render()` and draw in `renderSkija()`. The title is released when removed and recreated when the same instance returns. This screen draws its entire background and therefore overrides the default background entry. Screens combining Skia with vanilla widgets need an explicit composition order to keep those widgets visible.

```java
package com.pvp_utils.client.render.examples;

import com.pvp_utils.client.render.skia.SkijaScreen;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChromaExampleScreen extends Screen implements SkijaScreen {
    private Frame frame;
    private SkijaUi.PreparedText heading;
    private long fontRevision = -1;

    public ChromaExampleScreen() {
        super(Component.literal("Chroma Graphics"));
    }

    @Override
    public void added() {
        frame = null;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float panelWidth = Math.min(420f, Math.max(0f, width - 32f));
        float panelHeight = Math.min(180f, Math.max(0f, height - 32f));
        frame = new Frame(width, height, (width - panelWidth) * 0.5f,
                (height - panelHeight) * 0.5f, panelWidth, panelHeight);
    }

    @Override
    public void renderSkija(Canvas canvas) {
        Frame current = frame;
        if (current == null) return;
        SkijaUi.fill(canvas, 0f, 0f, current.screenWidth(), current.screenHeight(), 0xFF11151D);
        if (current.width() <= 0f || current.height() <= 0f) return;
        long revision = SkijaUi.textFontRevision();
        if (heading == null || fontRevision != revision) {
            if (heading != null) heading.close();
            heading = SkijaUi.prepareText("Chroma Graphics", 18f, true);
            fontRevision = revision;
        }
        SkijaUi.rounded(canvas, current.x(), current.y(),
                current.width(), current.height(), 12f, 0xFF202835);
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(current.x(), current.y(),
                    current.width(), current.height()));
            heading.drawTop(canvas, current.x() + 16f, current.y() + 16f, 0xFFF3F6FC);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    @Override
    public void removed() {
        if (heading != null) heading.close();
        heading = null;
        frame = null;
    }

    private record Frame(int screenWidth, int screenHeight,
                         float x, float y, float width, float height) {
    }
}
```

### 5.2 Current-frame HUD submission

Retain one `ChromaHudLabel` instance. Call `submit(...)` once per frame from an existing client HUD collection hook, after frame start and before the overlay queue drains. Stop submitting before calling `close()` when disabling the module. This example retains only the current text, does not rebuild it for position changes, and does not draw over menus.

```java
package com.pvp_utils.client.render.examples;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;

public final class ChromaHudLabel implements AutoCloseable {
    private SkijaUi.PreparedText label;
    private String cachedValue;
    private long fontRevision = -1;

    public void submit(String value, float x, float y) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui || client.screen != null) return;
        LabelFrame frame = new LabelFrame(value == null ? "" : value, x, y);
        SkijaRenderer.submit(canvas -> draw(canvas, frame));
    }

    private void draw(Canvas canvas, LabelFrame frame) {
        long revision = SkijaUi.textFontRevision();
        if (label == null || !frame.value().equals(cachedValue) || fontRevision != revision) {
            if (label != null) label.close();
            label = SkijaUi.prepareText(frame.value(), 12f, false);
            cachedValue = frame.value();
            fontRevision = revision;
        }
        SkijaUi.rounded(canvas, frame.x(), frame.y(), label.width() + 16f, 28f, 7f, 0xD9202835);
        label.drawTop(canvas, frame.x() + 8f, frame.y() + 6f, 0xFFF3F6FC);
    }

    @Override
    public void close() {
        if (label != null) label.close();
        label = null;
        cachedValue = null;
    }

    private record LabelFrame(String value, float x, float y) {
    }
}
```

### 5.3 Aligned blur for a scaled panel

Call from an active Skia callback before applying page-local transforms to the Canvas. The caller calculates scale once per frame and owns the prepared label. The screen clip and local foreground derive from that same scale value.

```java
package com.pvp_utils.client.render.examples;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.RRect;

public final class ChromaBlurPanel {
    public static void draw(Canvas canvas, float centerX, float centerY,
                            float width, float height, float radius, float scale,
                            SkijaUi.PreparedText label) {
        if (!Float.isFinite(scale) || scale <= 0f || width <= 0f || height <= 0f) return;
        float screenWidth = width * scale;
        float screenHeight = height * scale;
        float x = centerX - screenWidth * 0.5f;
        float y = centerY - screenHeight * 0.5f;
        RRect screenClip = RRect.makeXYWH(x, y, screenWidth, screenHeight, radius * scale);
        SkijaRenderer.drawBlurredBackdrop(canvas, screenClip, x, y, screenWidth, screenHeight, 12f);
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            canvas.clipRRect(RRect.makeXYWH(0f, 0f, width, height, radius), true);
            SkijaUi.rounded(canvas, 0f, 0f, width, height, radius, 0xE6202835);
            label.drawTop(canvas, 16f, 16f, 0xFFF3F6FC);
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
```

This example demonstrates consistent animation geometry; continuous text scaling still has a cost. For the lowest text overhead, prefer stable font sizes and scale, animating position or opacity instead. The base fill remains available on the first frame without a blur snapshot.

### 5.4 Borrowing a Minecraft texture

Call inside an active shared Skia callback with an `Identifier` managed by the Minecraft texture manager. Do not retain borrowed wrappers across resource reloads. Long-lived image caches require explicit ownership and invalidation.

```java
package com.pvp_utils.client.render.examples;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.resources.Identifier;

public final class ChromaTexturePainter {
    public static void draw(Canvas canvas, Identifier texture,
                            float x, float y, float width, float height) {
        if (width <= 0f || height <= 0f) return;
        try (SkijaRenderer.BorrowedImage borrowed = SkijaRenderer.borrowTexture(texture)) {
            if (borrowed == null) {
                SkijaUi.rounded(canvas, x, y, width, height, 6f, 0xFF202835);
                return;
            }
            canvas.drawImageRect(borrowed.image(), Rect.makeXYWH(x, y, width, height));
        }
    }
}
```

## 6. Performance Practices

These practices reduce avoidable CPU, GPU, and native-resource overhead. They do not guarantee a frame rate on a particular device.

### 6.1 Recommended order

1. Skip invisible content, zero-opacity content, offscreen content, and content covered by a complete page. For long lists, visit only visible rows and a small required buffer. Canvas clipping does not eliminate CPU-side iteration, measurement, or layout work.
2. Use the existing 2D pass and group a component's drawing. Avoid duplicate submissions, additional GL state queries, and per-widget flushes.
3. Perform static layout, truncation, text measurement, and TextBlob construction when content or layout becomes invalid. Per-frame work should handle only necessary position, color, and clipping changes.
4. Prefer fills, rounded rectangles, and gradients. Apply blur, shadows, glow, and glass only to necessary regions.
5. Draw stable pages directly. Use bounded layers during transitions only when group composition requires them; do not wrap every interface in a fullscreen `saveLayer`.
6. Keep networking, file reads, and image decoding out of drawing callbacks. Workers process ordinary data; GPU resource creation, updates, and release remain in the appropriate client rendering stage.

### 6.2 Font sizes and cache scope

Current regular and fallback Font caches use exact float sizes as keys and have no entry-count limit. Feeding continuous animation values as font sizes keeps creating Font objects. Prefer fixed sizes. When size changes are required, clamp and quantize them at the feature boundary, for example:

`size = Math.round(Math.clamp(requestedSize, 8f, 48f) * 4f) * 0.25f`

Drawing, measurement, and cache keys must use this same result. The 0.25 increment is an example, not an automatic framework rule. Limiting PreparedText entries alone does not prevent arbitrary new sizes from entering the shared Font cache.

| Data | Recommended cache policy | Invalidation |
| --- | --- | --- |
| Single fixed label | One `PreparedText` | Text, size, weight, font selection, or font revision changes |
| Lyrics and multiline lists | Feature-scoped, bounded text and layout cache | Track, text, layout width, display mode, or font changes; close evicted entries |
| Static images | Feature-owned image cache with a total pixel budget | Image identity, target dimensions, or resource revision changes |
| Blurred static images | Generate only when image or effect parameters change | Image, resolution, or blur parameters change |
| Live world background | Use the framework's current-pass snapshot | An old image cannot remain a substitute for the current world |
| Glass slots | Reuse allocated offscreen targets | Required size growth or framework destruction |

The music module's `MusicBlurredCover` is a feature cache for blurred cover backgrounds. Its 256-pixel working size is specific to that low-detail background. Do not extend it to body text, icons, ordinary covers, or all UI rendering.

### 6.3 Backdrop and effect costs

The regular overlay pass reads the recorded blur-request flag at pass start, usually from the previous frame. A first request made inside the pass may therefore have no snapshot. Requested passes refresh the snapshot; it is not a cache of blurred output across frames. Capture retains a full framebuffer image, while each blur draw crops the required region. Local blur therefore does not eliminate the full-frame snapshot cost.

Do not capture the screen separately for every widget or sample the component's already-rendered content back into its background. Provide a base-fill fallback. A shared renderer failure is logged and disables rendering for the session; backdrop blur has a separate failure-disable state. Diagnose the first logged exception instead of hiding drawing errors with empty catch blocks.

Glass requires capture, context transitions, offscreen drawing, and filtering, and generally costs more than basic shapes. Apply it to visible panels and avoid repeating the glass pipeline for every item in a long list.

### 6.4 Validation

Check client compilation, geometry at different GUI scales and window sizes, first entry and return, font-change invalidation, cache counts during sustained animation, and native-resource cleanup. Performance evidence should distinguish CPU time, GPU time, allocations, and cache hits. A single FPS reading does not identify a bottleneck.

Follow the root `AGENTS.md` instructions for the project-local temporary directory and run compilation that includes `compileClientJava`. Do not launch Minecraft for runtime testing unless the user explicitly requests it.

## 7. Instructions for AI Contributors

Before working on UI, HUDs, typography, blur, glass, or rendering internals, read this section, the relevant interface sections, and the corresponding implementation.

1. **Verify the environment and API.** Use the current repository, Minecraft 1.21.11 sources with official mappings, and actual dependencies. Read affected Minecraft classes before writing calls. Do not guess signatures or invent Chroma Graphics APIs.
2. **Identify the stage.** Classify the content as a Screen, main-target GUI, HUD overlay, or world Gizmo. Confirm submission precedes the relevant queue drain. Use an established entry point without registering the same content twice.
3. **Reuse shared typography.** Use `SkijaUi` for ordinary text and explicitly owned `PreparedText` for repeated text. Do not introduce another font manager or pass continuous animated float sizes directly into shared caches.
4. **Unify geometry.** Advance animation once per frame. Drawing, blur, clipping, and hit testing must read the same result. Distinguish physical pixels, GUI coordinates, and local design coordinates.
5. **Bound the work.** Cull invisible content before processing visible layout. Define cache keys, capacity, invalidation, and release. A feature's reduced-resolution cache must not change global image-quality policy.
6. **Preserve ownership.** Do not retain or close borrowed Canvas objects. Close owned native resources and keep GPU operations in the client rendering stage. Feature modules must not invoke global shared-resource shutdown.
7. **Respect backend boundaries.** Ordinary widgets do not need new contexts, capture pipelines, font paths, or fullscreen offscreen passes. If backend changes are necessary, verify GL state, resource ownership, and ordering for every affected caller.
8. **Handle failure and return paths.** Account for missing blur or textures, window changes, font reloads, screen re-entry, and resource recreation. Asynchronous results update data without retriggering entrance animations.
9. **Perform required validation.** Apply the process-local temporary-directory setup in `AGENTS.md` and include client compilation. Launch the game only when explicitly requested. Do not report static or offline checks as runtime frame-rate measurements.
10. **Keep documentation aligned.** Update both language versions and examples when public interfaces or behavior change. Follow the project's prohibition on adding code comments; place explanations in document prose.

Change reports should identify the rendering stage used, cache and resource lifecycles, checks actually performed, and any runtime behavior left unverified.

## 8. Implementation Index

- [SkijaScreen](../../src/client/java/com/pvp_utils/client/render/skia/SkijaScreen.java), [SkijaFrameRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaFrameRenderer.java)
- [SkijaRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaRenderer.java), [SkijaUi](../../src/client/java/com/pvp_utils/client/render/skia/SkijaUi.java), [SkijaBackdrop](../../src/client/java/com/pvp_utils/client/render/skia/SkijaBackdrop.java)
- [LiquidGlassRenderer](../../src/client/java/com/pvp_utils/client/render/skia/LiquidGlassRenderer.java), [GlassCaptureRenderer](../../src/client/java/com/pvp_utils/client/render/skia/GlassCaptureRenderer.java), [GlassGlBackend](../../src/client/java/com/pvp_utils/client/render/skia/GlassGlBackend.java), [GlassGlState](../../src/client/java/com/pvp_utils/client/render/skia/GlassGlState.java)
- [WorldRender](../../src/client/java/com/pvp_utils/client/render/world/WorldRender.java), [WorldRenderLevelRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/WorldRenderLevelRendererMixin.java)
- [MinecraftMixin](../../src/client/java/com/pvp_utils/mixin/client/MinecraftMixin.java), [GameRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/GameRendererMixin.java)
- [MainUiScale](../../src/client/java/com/pvp_utils/client/render/MainUI/MainUiScale.java), [MainUIFlipAnimation](../../src/client/java/com/pvp_utils/client/render/MainUI/MainUIFlipAnimation.java)
- [Shared typography in use: LyricsDisplayRenderer](../../src/client/java/com/pvp_utils/client/modules/impl/Render/LyricsDisplayRenderer.java), [Local image cache: MusicBlurredCover](../../src/client/java/com/pvp_utils/client/NeteaseMusic/MusicBlurredCover.java)
- [Build definition](../../build.gradle), [Project collaboration rules](../../AGENTS.md)
- [Plugin interfaces](../PLUGINS.md), [Plugin tutorial](../PLUGIN_TUTORIAL.md), [Shader documentation](../SHADERS.md)

JavaScript plugins use the rendering facade exposed by the plugin system. Its parameters and lifecycle are defined in the plugin documentation. Main-menu backgrounds, page flips, music background caches, and shaders are specialized features built on their respective rendering paths, not mandatory policies for every component.
