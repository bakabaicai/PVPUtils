# Chroma Graphics

English · [简体中文](README.zh-CN.md)

## 1. Introduction

Chroma Graphics is a Skia-based client rendering framework providing interfaces for 2D graphics, text and icons, backdrop blur, liquid glass, and world geometry. It manages the regular 2D Skia context, framebuffer wrappers, rendering order, and OpenGL state restoration. This document describes the current interfaces, coordinate spaces, calling constraints, resource lifecycles, and performance practices. The current backend integrates with Minecraft and uses the dependencies below.

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
| `GlassStyle`, `GlassShape` | Glass parameters and shape descriptions | Caller-owned |
| `SkijaBackdrop`, `GlassGlState` | Backdrop sampling and GL state preservation | Package-private implementation |
| `WorldRender` | World geometry and world-to-screen projection | World feature modules |

Regular 2D rendering shares one managed Skia context. Liquid glass uses a dedicated OpenGL pass with explicit flush and state-restoration boundaries. World geometry uses Minecraft Gizmos rather than the 2D Canvas.

## 2. Rendering Order and Integration

### 2.1 Current frame sequence

1. `MinecraftMixin` calls `SkijaRenderer.beginFrame()` at the start of `Minecraft.runTick`, clearing both submission queues.
2. Minecraft collects GUI content. Calls to `draw()` outside an active Skia canvas during this stage enter the main-target queue.
3. `GameRendererMixin` calls `renderMainSubmitted()` before `GuiRenderer.render(GpuBufferSlice)`, drawing that queue into the Minecraft main color target.
4. After Minecraft calls `blitToScreen()`, `SkijaFrameRenderer.render()` starts the overlay pass. It skips rendering when the window is minimized or a Minecraft loading overlay is present.
5. The overlay pass runs host-integrated HUD drawing, the current `SkijaScreen`, the `submit()` queue, and the host's final overlay drawing, in that order.
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

Lines and path strokes reuse the shared Paint, with a minimum width of 0.1. They restore fill mode, default width, cap, and join state after drawing.

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

The entry point is `LiquidGlassRenderer.getInstance()`. A dedicated OpenGL pass evaluates rounded-rectangle distance fields, smooth unions, refraction, dispersion, and radial blur sampling.

| Interface | Behavior and constraints |
| --- | --- |
| `renderPanel(Minecraft, x, y, width, height, radius, int tintColor, GlassStyle): boolean` | Draws one rounded panel; colors use ARGB |
| `renderShapes(Minecraft, List<GlassShape>, GlassStyle): boolean` | Draws multiple shapes with one background snapshot; each call processes at most the first 16 shapes |
| The preceding signature plus a final `GlassShadow` | Adds distance-field shadow parameters; the default is `GlassShadow.NONE` |
| `GlassShape(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, smoothness, opacity, int tint)` | Immutable shape description; geometry uses GUI units, while `smoothness` is the distance-field union width in framebuffer pixels |
| `GlassShape.rounded(x, y, width, height, radius, int tint)` | Uses equal corner radii, a union width of 0.3, and shape opacity 1 |
| `GlassShadow(expand, strength, offsetX, offsetY, int color)` | Expansion and offsets use framebuffer pixels; ranges are 0–80, 0–1, −60–60, and −60–60 respectively |
| `GlassStyle.sanitize()` | Replaces non-finite parameters with defaults and clamps values to valid ranges |
| `panelTint(): int` | Returns the host's default glass tint |
| `destroy()` | Releases the shader program, vertex array, and background texture; framework lifecycle use only |

The caller owns `GlassStyle`, which exposes these fields:

| Field | Default | Range | Unit or meaning |
| --- | --- | --- | --- |
| `blur` | 6 | 0–30 | Framebuffer pixels; radial sampling radius |
| `refraction` | 1.35 | 1–5 | Refractive index |
| `dispersion` | 1 | 0–3 | Separation strength of RGB sampling positions |
| `rim` | 0.6 | 0–1 | Edge tint strength |
| `alpha` | 0.9 | 0–1 | Blend proportion between the glass result and the background |
| `glow` | 0.4 | 0–1 | Edge glow strength |
| `thickness` | 10 | 1–120 | Framebuffer pixels; refraction-band thickness |

Panel positions, dimensions, and corner radii use screen GUI coordinates and do not inherit local Canvas transforms or clips. Positions and dimensions convert to framebuffer pixels using the current target's width and height independently, matching the 2D Canvas screen-coordinate scale. Corner radii are limited to half the shorter side and use the smaller axis scale to preserve circular corners; shape opacity is clamped to 0–1. Blur and thickness do not automatically increase with GUI scale; callers must distinguish physical pixels from GUI units.

Inside an active Skia pass, the framework flushes recorded commands and captures that pass's current target. Outside a pass, it uses the Minecraft main color target. The capture texture reuses allocation only: pixels refresh on every call, without caching effects across frames. After drawing, GL state is restored and the shared Skia context is resynchronized. No second Skia context or panel-slot allocation is created.

A `false` result means the call did not draw; callers should provide a base-fill fallback. Shader compilation or execution exceptions are logged and disable the effect until framework destruction resets it. Exterior soft shadows may use `SoftShadow` before glass capture. When using `GlassShadow`, avoid adding another shadow to the same outline. Shapes requiring a smooth union must be submitted in the same `renderShapes` call.

### 3.6 World rendering: WorldRender

Positions use world coordinates. Commands are submitted within Minecraft's Gizmos collection scope, and the host batches fills and lines. `throughWalls=false` retains depth occlusion; `true` uses always-on-top drawing.

| Interface | Purpose |
| --- | --- |
| `line(Vec3 from, Vec3 to, int color, float width[, boolean throughWalls])` | Line segment; depth-tested by default |
| `box(AABB, int outlineColor)` | Box outline |
| `box(AABB, int outlineColor, int fillColor)` | Box outline and fill |
| `box(AABB, int outlineColor, int fillColor, float width, boolean throughWalls)` | Box with explicit line width and depth behavior |
| `filledBox(AABB, int fillColor[, boolean throughWalls])` | Box fill; depth-tested by default |
| `face(AABB, Vec3 viewer, int color, float width)` | Outlines the box face selected from the viewer's direction |
| `face(AABB, Direction, int color, float width)` | Outlines the specified face |
| `filledFace(AABB, Direction, int color, boolean throughWalls)` | Fills the specified face |
| `point(Vec3, int color, float size, boolean throughWalls)` | Point marker with screen-space size |
| `arrow(Vec3 from, Vec3 to, int color, float width, boolean throughWalls)` | Arrow |
| `polyline(List<Vec3>, int color, float width, boolean closed, boolean throughWalls)` | Open or closed polyline with at most 4096 vertices |
| `triangle(Vec3 a, Vec3 b, Vec3 c, int color, boolean throughWalls)` | Filled triangle |
| `quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, boolean throughWalls)` | Filled quadrilateral |
| `circle(Vec3 center, Vec3 normal, double radius, int segments, int outlineColor, int fillColor, float width, boolean throughWalls)` | Ring or disc in any plane; segment count is clamped to 3–128 |
| `sphere(Vec3 center, double radius, int segments, int rings, int color, float width, boolean throughWalls)` | Wireframe sphere; 6–64 longitudinal segments and 3–32 latitude bands |
| `billboard(Vec3 center, float width, float height, float rotation, int color, ParticleShape, boolean throughWalls)` | Camera-facing shape; dimensions use world units and rotation uses radians |
| `ParticleShape` | `QUAD`, `CIRCLE`, `STAR`, `HEART`, and `BLOOM`; geometric shapes without external textures |
| `project(Vec3): ScreenPoint` | World-to-GUI projection; may return `null` and does not test depth occlusion |
| `ScreenPoint(float x, float y)` | Projected GUI coordinates |
| `register(Consumer<Frame>): Registration` | Registers a world drawing callback and returns a caller-owned registration handle |
| `Frame(Vec3 cameraPosition, float partialTick)` | Current camera position and interpolation progress |
| `Registration.close()` | Unregisters the callback; repeated calls are allowed |
| `capture(Camera, Matrix4fc modelView, Matrix4fc projection)`, `renderRegistered(float partialTick)` | Framework-internal view capture and callback scheduling |

`WorldRenderLevelRendererMixin` invokes registered callbacks inside `collectPerFrameGizmos()`. Callbacks may submit these shapes directly; do not invoke them from a 2D overlay or networking thread. A callback that throws a runtime exception is logged and unregistered to prevent repeated frame failures. Close registration handles on deactivation or destruction so they do not retain stale objects. Point size and line width follow Gizmos screen-size conventions; circle, sphere, and billboard dimensions use world units.

`WorldParticles` supplies a caller-owned, bounded particle collection:

| Interface | Behavior and constraints |
| --- | --- |
| `WorldParticles(int capacity)` | Capacity is 1–4096; new particles evict the oldest when full |
| `emit(Vec3 position, Vec3 velocity, Spec): boolean` | Adds a particle; velocity uses world units per second |
| `Spec(lifetime, size, int color, ParticleShape, gravity, drag, angularVelocity, boolean throughWalls)` | Lifetime is in seconds, size in world units, gravity in world units/s², drag is a per-second coefficient, and angular velocity is in radians/s |
| `update(float seconds)` | Advances simulation once and retains previous/current positions for interpolation; drawing does not advance it automatically |
| `render(Frame[, double maxDistance])` | Draws inside a registered world callback; the default distance limit is 128 world units |
| `size()`, `capacity()` | Current particle count and capacity |
| `clear()`, `close()` | Clears particles; a closed collection stops accepting new particles |

The collection does not register a callback or simulation clock automatically. Callers own updates, registration, and cleanup, and must clear old particles when the world changes. Per-frame drawing interpolates position and rotation, fades opacity and size by lifetime, and culls distant or nearly transparent particles.

### 3.7 Framework internals

Some methods below are `public`, but they are not standard entry points for new features.

| Class | Interface group |
| --- | --- |
| `SkijaFrameRenderer` | `render()`, `close()` |
| `SkijaRenderer` | `beginFrame()`, `renderMainSubmitted()`, `renderSubmitted()` |
| `SkijaRenderer` | `renderOverlay(Consumer<Canvas>)`, `renderMainTarget(Consumer<Canvas>)` |
| `SkijaRenderer` | `currentFramebuffer()`, `flush()`, `restoreContext()`, `close()` |

`renderMainTarget()` reuses the current pass if the shared canvas is active; it does not force a target switch. `currentFramebuffer()` returns the framework's stored target state. Outside a pass, it cannot substitute for querying the actual GL binding. Do not call `flush()` per widget or create a new `DirectContext` for ordinary components.

### 3.8 Component effects and shape transitions

These utilities use the current shared Canvas. They do not create a rendering backend or advance animation clocks. Dimensions use the caller's current coordinate space; a deformation path used for backdrop sampling must be created in screen GUI coordinates.

| Interface | Behavior and constraints |
| --- | --- |
| `SoftShadow.drawRounded(Canvas, x, y, width, height, radius, strength, color)` | Draws an even soft shadow outside a rounded shape; `strength` is 0–1, with 0 skipping drawing |
| `SoftShadow.drawPath(Canvas, Path, strength, color)` | Generates an exterior shadow from the same fill path; neither modifies nor closes the supplied path |
| `ContentTransition.draw(Canvas, Rect bounds, visibility, offsetY, Runnable drawing)` | Composites a fade and isotropic blur within `bounds`; `visibility` is 0–1 and `offsetY` is an optional explicit vertical displacement |
| `LiquidSeparation.create(x, y, width, height, radius, satelliteRadius, gap, progress): Geometry` | Builds a shared fill path for a rounded main panel and a right-hand circle; progress 0 contains only the main panel, while 1 produces separate shapes with the specified gap |
| The preceding signature plus a final `boolean alignTop` | The default overload centers the circle vertically; `true` anchors its center at `y + max(cornerRadius, satelliteRadius)` so it stays near the top as the main panel grows taller |
| `LiquidSeparation.create(x, y, width, height, radius, satelliteRadius, gap, leftProgress, rightProgress, alignTop): Geometry` | Separates both circles with independent progress values and combines them into one fill path; single-progress overloads are equivalent to `leftProgress=0` |
| `LiquidSeparation.Geometry.path(): Path`, `bounds(): Rect` | Returns the current path and tight bounds for same-frame fills, backdrop clipping, shadows, and input geometry |
| `Geometry.satelliteCenterX()`, `satelliteCenterY()`, `satelliteRadius()` | Returns the right circle's current geometry; its visible radius is 0 when right progress is 0 |
| `Geometry.leftSatelliteCenterX()`, `leftSatelliteCenterY()`, `leftSatelliteRadius()` | Returns the left circle's current geometry; its visible radius is 0 when left progress is 0 |
| `Geometry.close()` | Releases the owned Path; repeated closure is allowed. Do not separately close the borrowed `path()` |
| `AnimatedStatusIcon.draw(Canvas, success, x, y, size, progress, color)` | Reveals a success check by path length or draws the two failure-cross strokes over staggered progress intervals, using round caps and joins |

Draw shadows before panel content and interior clipping. Strength affects both color opacity and blur width, and colors use ARGB. Shadows exclude the shape interior and add no outline. During deformation, shadow and fill the entire path once to avoid repeated translucent blending at the bridge.

`ContentTransition` creates a bounded layer only during transitions. Paints and filters are released and Canvas state is restored before returning. Opacity is proportional to `visibility`; blur has equal strength in both directions and decreases as content appears, without adding directional trails based on displacement. When visibility is near 1 and displacement is 0, it invokes the drawing callback directly without adding a clip; the caller remains responsible for steady-state clipping. The supplied bounds form the transition's hard clip and should include any explicit movement. Do not wrap a single text line in full-screen bounds or enqueue further rendering from the callback. Keep font size fixed during transitions.

`offsetY=0f` fades content in place with blur; a nonzero value applies vertical displacement only to this callback. The caller supplies visibility, displacement, and animation curves. The interface does not manage content changes or timing.

Liquid separation uses a nonzero fill with matching contour directions for the panel, both circles, and their bridges, producing a merged silhouette in one draw. Do not fill, sample the backdrop, or shadow the two sides separately. Each bridge narrows and detaches late in its transition. The final circle radius is limited to half the main panel height. The right center moves from inside the panel to `x + width + gap + satelliteRadius`; the left center moves to `x - gap - satelliteRadius`. In these formulas, `gap` and `satelliteRadius` are the clamped values: the gap is at least 0, and the circle radius is no greater than half the panel height. Top alignment uses the size-constrained corner and circle radii; a zero circle radius adds neither a circle nor a bridge. `bounds()` includes both sides' actual extension, and layout and input bounds should use this same geometry. The creating component owns Geometry and closes it after the frame or when its geometry becomes invalid. Components may reuse identical geometry; the framework provides no global geometry or image cache.

The caller calculates progress once per frame and reuses it across that frame's drawing calls. `AnimatedStatusIcon` draws from the supplied progress without retaining animation state.

## 4. Coordinates, Animation, and Resource Contracts

### 4.1 Coordinate spaces

The regular Canvas is already scaled into GUI logical coordinates. Complete screens use `Screen.width/height`; HUDs use `Window.getGuiScaledWidth()/getGuiScaledHeight()`. Do not apply the GUI scale again. Physical framebuffer dimensions belong to the backend.

Rectangle arguments are `x, y, width, height`, not bottom-right coordinates. Colors use `0xAARRGGBB`.

Calculate layout from the current available dimensions. If a design coordinate system and local scaling are used, apply the inverse of the same transform to mouse input. For example, `screenX = originX + localX * scale` implies `localX = (mouseX - originX) / scale`. Drawing, clipping, and hit testing must share the same frame geometry.

### 4.2 Text positioning and reuse

`text(..., top, height, ...)` centers text in a line box using font metrics:

`baseline = top + (height - (descent - ascent)) / 2 - ascent`

`height` is line-box height; `size` is font size. The top used by `textAtTop()` and `PreparedText.drawTop()` is the font line-box top, not the highest visible ink pixel. `PreparedText.draw()` accepts the baseline directly.

`PreparedText` retains TextBlob glyph data and width to avoid repeating fallback segmentation, measurement, and glyph construction on every frame. It is not a bitmap cache and does not automatically provide wrapping, ellipsis, complex text layout, or font-revision updates. CJK and missing glyphs follow the existing fallback policy. CJK prefers the system fallback font; requesting bold does not guarantee bold fallback glyphs.

Cache keys must include text, discrete font size, bold state, font revision, and any temporary font name. Wrapping or truncation caches must also include available width, wrapping and truncation policies, and other relevant layout conditions. Position and color changes do not require rebuilding a TextBlob.

Test whether the complete text fits using the actual available width before quantizing a cache key. Rounding the width down first must not add an ellipsis to text that already fits. Only text that exceeds the available width enters truncation; cache quantization must not change the display of complete text.

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
| Framework contexts, Surface caches, glass shaders, and capture textures | Managed by the framework during recreation or client shutdown |

Pair `save()` with `restoreToCount()` to isolate local transforms and clipping, using `try/finally` to ensure restoration. Use `saveLayer()` only when group opacity or filtering requires it. Do not promote a feature cache into a global policy without evaluating every affected caller.

## 5. Usage Examples

The examples below can be placed separately in the client source set. They demonstrate interface composition and lifecycle management; registration, navigation, and input dispatch remain the responsibility of the existing host.

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

### 5.5 Shared deformation geometry and bounded text transitions

Call this from an existing HUD entry point with an active Canvas. The `x/y` coordinates are screen GUI coordinates, and the caller has already updated all progress values. The example draws the exterior shadow, samples the current backdrop, fills the panel, and finally fades text in place with blur. The caller retains ownership of the prepared text.

```java
package com.pvp_utils.client.render.examples;

import com.pvp_utils.client.render.skia.ContentTransition;
import com.pvp_utils.client.render.skia.LiquidSeparation;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import com.pvp_utils.client.render.skia.SoftShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

public final class ChromaAnimatedCapsule {
    public static void draw(Canvas canvas, float x, float y, float leftSeparation, float rightSeparation,
                            float visibility, SkijaUi.PreparedText label) {
        try (LiquidSeparation.Geometry shape = LiquidSeparation.create(
                x, y, 240f, 32f, 16f, 14f, 7f, leftSeparation, rightSeparation, true)) {
            Rect bounds = shape.bounds();
            SoftShadow.drawPath(canvas, shape.path(), 0.45f, 0xB0000000);
            SkijaRenderer.drawBlurredBackdrop(canvas, shape.path(),
                    bounds.getLeft(), bounds.getTop(), bounds.getWidth(), bounds.getHeight(), 10f);
            SkijaUi.fillPath(canvas, shape.path(), 0xD91B1F26);
            int save = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(x + 14f, y + 4f, 212f, 24f));
                ContentTransition.draw(canvas, Rect.makeXYWH(x + 14f, y + 4f, 212f, 24f),
                        visibility, 0f,
                        () -> label.drawTop(canvas, x + 14f, y + 8f, 0xFFF3F6FC));
            } finally {
                canvas.restoreToCount(save);
            }
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
| Multiline text and lists | Caller-owned, bounded text and layout cache | Content revision, text, layout width, layout policy, or font changes; close evicted resources |
| Static images | Feature-owned image cache with a total pixel budget | Image identity, target dimensions, or resource revision changes |
| Blurred static images | Generate only when image or effect parameters change | Image, resolution, or blur parameters change |
| Live world background | Use the framework's current-pass snapshot | An old image cannot remain a substitute for the current world |
| Glass background texture | Reuse allocation at the same dimensions and refresh pixels on every call | Target-size changes or framework destruction |

The caller owns static-image blur caches and chooses their working resolution according to display dimensions and required detail. Reduced-resolution effect caches apply only to the corresponding image effect and do not change the drawing quality of text, icons, or other images.

### 6.3 Backdrop and effect costs

The regular overlay pass reads the recorded blur-request flag at pass start, usually from the previous frame. A first request made inside the pass may therefore have no snapshot. Requested passes refresh the snapshot; it is not a cache of blurred output across frames. Capture retains a full framebuffer image, while each blur draw crops the required region. Local blur therefore does not eliminate the full-frame snapshot cost.

Do not capture the screen separately for every widget or sample the component's already-rendered content back into its background. Provide a base-fill fallback. A shared renderer failure is logged and disables rendering for the session; backdrop blur has a separate failure-disable state. Diagnose the first logged exception instead of hiding drawing errors with empty catch blocks.

Glass requires a full-target capture, GL state transitions, and multiple background samples, and generally costs more than basic shapes. A single `renderShapes` call shares one snapshot; group shapes using the same effect to reduce captures and state transitions. Apply it to visible panels and avoid capturing the background separately for every item in a long list.

### 6.4 Validation

Check client compilation, geometry at different GUI scales and window sizes, first entry and return, font-change invalidation, cache counts during sustained animation, and native-resource cleanup. Performance evidence should distinguish CPU time, GPU time, allocations, and cache hits. A single FPS reading does not identify a bottleneck.

Source validation for the current Minecraft backend should include `compileClientJava`. Static checks and compilation do not replace visual or performance validation in the running environment.

## 7. Instructions for AI Contributors

Before using Chroma Graphics, read this section, the relevant interface sections, and the corresponding implementation.

1. **Verify interfaces and dependencies.** Confirm types, signatures, and calling behavior against the current implementation and actual dependencies. For the Minecraft backend, check sources matching its version and mappings. Do not guess methods or invent framework APIs.
2. **Identify the rendering stage.** Classify the content as a Screen, main-target GUI, HUD overlay, or world Gizmo. Confirm submission precedes the relevant queue drain. Reuse an established entry point and connect each piece of content only once.
3. **Reuse typography interfaces.** Use `SkijaUi` for ordinary text and caller-owned `PreparedText` for repeated text. Keep font sizes fixed or discrete, use the same font selection for measurement and drawing, and include font revision and any temporary font name in cache keys.
4. **Unify coordinates and geometry.** Advance animation once per frame. Drawing, backdrop sampling, clipping, and hit testing must read the same result. Distinguish physical pixels, GUI coordinates, and local coordinates, and apply the corresponding inverse transform to input.
5. **Bound caches and work.** Cull invisible content before processing visible layout. Define cache keys, capacity, invalidation, and release. Local cache policies apply only to the content they own.
6. **Preserve resource ownership.** Do not retain borrowed Canvas objects across frames or close them. Close owned native resources and keep GPU operations in the appropriate client rendering stage. Callers must not close framework-shared resources.
7. **Respect backend boundaries.** Use the existing context, backdrop sampling, and typography interfaces for regular drawing. When changing a backend, verify GL state, target framebuffers, resource ownership, and submission order for affected passes.
8. **Handle invalidation and recreation.** Account for unavailable backdrops or textures, window-size changes, font reloads, and recreation after resource release. Manage asynchronous data updates separately from animation lifecycles.
9. **Validate actual behavior.** Check compilation, coordinate calculations, drawing order, cache invalidation, and resource cleanup. Separate CPU, GPU, and allocation costs in performance measurements. Static checks and compilation are not runtime performance evidence.
10. **Keep documentation aligned.** When public interfaces or behavior change, update both language versions and affected examples. Keep names, parameters, coordinate spaces, and ownership descriptions consistent with the implementation.

Change reports should identify the rendering stage used, cache and resource lifecycles, checks actually performed, and any runtime behavior left unverified.

## 8. Implementation Index

- [SkijaScreen](../../src/client/java/com/pvp_utils/client/render/skia/SkijaScreen.java), [SkijaFrameRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaFrameRenderer.java)
- [SkijaRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaRenderer.java), [SkijaUi](../../src/client/java/com/pvp_utils/client/render/skia/SkijaUi.java), [SkijaBackdrop](../../src/client/java/com/pvp_utils/client/render/skia/SkijaBackdrop.java)
- [SoftShadow](../../src/client/java/com/pvp_utils/client/render/skia/SoftShadow.java), [ContentTransition](../../src/client/java/com/pvp_utils/client/render/skia/ContentTransition.java), [LiquidSeparation](../../src/client/java/com/pvp_utils/client/render/skia/LiquidSeparation.java), [AnimatedStatusIcon](../../src/client/java/com/pvp_utils/client/render/skia/AnimatedStatusIcon.java)
- [LiquidGlassRenderer](../../src/client/java/com/pvp_utils/client/render/skia/LiquidGlassRenderer.java), [GlassStyle](../../src/client/java/com/pvp_utils/client/render/skia/GlassStyle.java), [GlassGlState](../../src/client/java/com/pvp_utils/client/render/skia/GlassGlState.java), [liquid_glass.fsh](../../src/client/resources/assets/pvp_utils/shaders/liquid_glass.fsh)
- [WorldRender](../../src/client/java/com/pvp_utils/client/render/world/WorldRender.java), [WorldRenderLevelRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/WorldRenderLevelRendererMixin.java), [WorldParticles](../../src/client/java/com/pvp_utils/client/render/world/WorldParticles.java)
- [MinecraftMixin](../../src/client/java/com/pvp_utils/mixin/client/MinecraftMixin.java), [GameRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/GameRendererMixin.java)
- [Build definition](../../build.gradle)
