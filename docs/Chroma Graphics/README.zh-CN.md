# Chroma Graphics

[English](README.md) · 简体中文

## 1. 概述

Chroma Graphics 是 PVPUtils 的客户端渲染框架，负责二维界面、HUD、共享文字绘制、背景模糊、液态玻璃及三维标记的渲染接入。框架统一管理常规二维绘制的 Skia 上下文、帧缓冲包装、绘制时序和 OpenGL 状态恢复，使功能模块能够通过现有接口提交绘制内容。

Chroma Graphics 是系统名称，当前 Java 类型仍使用 `SkijaRenderer`、`SkijaUi` 等名称；不存在名为 `ChromaGraphics` 的统一入口类。

| 项目 | 当前实现 |
| --- | --- |
| Minecraft | 1.21.11，官方 Mojang 映射 |
| Java | 21 |
| Skia Java 绑定 | Skija 0.143.17 |
| 几何类型 | HumbleUI Types 0.2.0 |
| 当前打包的本地后端 | Windows x64 |
| 常规二维渲染 | Skija、OpenGL、GUI 逻辑坐标 |
| 三维渲染 | `WorldRender` 封装 Minecraft Gizmos |

框架提供绘制能力，不自动完成组件注册、布局、输入命中、文本换行、异步任务调度或业务缓存。底层画布适配 GUI 缩放，不等于界面布局自动适配所有分辨率。

### 1.1 模块分工

| 模块 | 职责 | 使用范围 |
| --- | --- | --- |
| `SkijaScreen` | 屏幕的二维绘制入口 | 自定义 `Screen` |
| `SkijaRenderer` | 活动画布、主目标与叠加队列、纹理借用、背景模糊 | 功能代码使用指定入口；帧调度由框架维护 |
| `SkijaUi` | 图形、文字、图标、共享字体及预备文本 | 常规 UI/HUD 首选 |
| `SkijaFrameRenderer` | HUD、屏幕和队列的帧级调度 | 框架内部 |
| `LiquidGlassRenderer` | 液态玻璃面板 | 明确需要该效果的组件 |
| `GlassCaptureRenderer`、`GlassGlBackend` | 玻璃采样与专用 Skia/OpenGL 后端 | 框架内部 |
| `SkijaBackdrop`、`GlassGlState` | 背景采样、GL 状态保存与恢复 | 包内实现 |
| `WorldRender` | 世界空间图形及世界坐标投影 | 三维功能模块 |

常规二维绘制复用一个受管理的 Skia 上下文。液态玻璃有独立的受管理后端，通过显式提交与状态恢复衔接。三维图形走 Minecraft Gizmos，不经过二维 Canvas。

## 2. 绘制时序与接入方式

### 2.1 当前帧的执行顺序

1. `MinecraftMixin` 在 `Minecraft.runTick` 开始时调用 `SkijaRenderer.beginFrame()`，清空两条提交队列。
2. Minecraft 收集 GUI 绘制内容。此期间在活动 Skia 画布之外调用 `draw()`，会进入主目标队列。
3. `GameRendererMixin` 在 `GuiRenderer.render(GpuBufferSlice)` 之前调用 `renderMainSubmitted()`，将主目标队列绘制到 Minecraft 主颜色目标。
4. Minecraft 将主目标 `blitToScreen()` 后，`SkijaFrameRenderer.render()` 开始叠加绘制。窗口最小化或存在 Minecraft 加载叠加层时跳过。
5. 叠加通道依次绘制已接入的 HUD 与插件、当前 `SkijaScreen`、`submit()` 队列、HUD 编辑叠加层。
6. 通道结束时提交 Skia 命令并恢复外部 GL 状态。玻璃效果的中间衔接可能产生额外提交。

主目标通道使用包装 Minecraft 主颜色纹理的 FBO；叠加通道使用默认帧缓冲 FBO 0。通道开始时完成尺寸检查、Surface 复用或重建、GL 状态同步及 GUI 坐标缩放。

### 2.2 入口选择

| 场景 | 标准方式 |
| --- | --- |
| 新建完整 Skia 屏幕 | `Screen implements SkijaScreen`，在 `renderSkija(Canvas)` 中直接绘制 |
| 已持有框架提供的 Canvas | 直接调用 `SkijaUi`、`PreparedText` 或必要的 Skija Canvas 方法 |
| 将局部绘制封装为回调 | `SkijaRenderer.draw(painter)`；活动通道内立即执行并保护 Canvas 状态 |
| 在 GUI 收集阶段绘制主目标内容 | 在主目标队列消费前调用 `draw(painter)` |
| 从已有的当前帧 HUD 收集入口提交叠加内容 | `SkijaRenderer.submit(painter)`，在叠加队列消费前提交 |
| 世界空间线框或标记 | 在现有世界渲染 Gizmos 收集阶段调用 `WorldRender` |
| 液态玻璃 | `LiquidGlassRenderer.getInstance().renderPanel(...)` |

`draw()` 与 `submit()` 不能互换：前者没有活动 Canvas 时进入主目标队列，后者始终进入叠加队列。叠加队列在屏幕之后绘制；不应覆盖菜单的 HUD 必须自行检查 `client.screen` 等显示条件。

队列仅服务当前帧，不是持久任务队列，也不具备线程安全保证。不得从网络线程提交 Canvas 操作，不得在队列消费过程中继续入队并依赖下一帧执行。异步工作只发布数据；客户端线程在正确阶段读取数据并提交绘制。

同一内容只能接入一个绘制入口。已由 `SkijaFrameRenderer` 调用的屏幕或 HUD，不应再额外提交相同内容。实现 HUD 类本身不会自动注册到帧调度器。

## 3. 接口列表

以下表格使用紧凑调用记法，不是可直接复制的 Java 声明。`Canvas` 表示首个画布参数；几何尺寸、字号、线宽和强度一般为 `float`，颜色为 `int`，文字和字体名为 `String`。方括号表示已有可选重载，不表示可选 Java 参数。除另行注明外，绘制方法返回 `void`。完整类型与重载以链接的当前源码为准。

### 3.1 屏幕、提交与纹理

| 接口 | 行为与约束 |
| --- | --- |
| `SkijaScreen.renderSkija(Canvas)` | 当前屏幕的实际 Skia 绘制；Canvas 由框架借出 |
| `SkijaRenderer.draw(Consumer<Canvas>)` | 活动画布内立即绘制；否则加入主目标队列 |
| `SkijaRenderer.submit(Consumer<Canvas>)` | 加入当前帧叠加队列 |
| `SkijaRenderer.isDrawing(): boolean` | 是否存在活动的共享画布 |
| `SkijaRenderer.hasFailed(): boolean` | 共享渲染器是否已因错误禁用 |
| `drawBlurredBackdrop(Canvas, RRect, x, y, width, height, strength)` | 按屏幕 GUI 坐标采样，圆角矩形裁剪 |
| `drawBlurredBackdrop(Canvas, io.github.humbleui.skija.Path, x, y, width, height, strength)` | 按屏幕 GUI 坐标采样，路径裁剪 |
| `borrowTexture(Identifier): BorrowedImage` | 借用 Minecraft GL 纹理，可能返回 `null` |
| `BorrowedImage.image(): Image` | 获取借用包装中的 Skia 图像 |
| `BorrowedImage.close()` | 释放包装，不销毁 Minecraft 拥有的纹理 |

### 3.2 图形与效果：SkijaUi

| 接口 | 用途 |
| --- | --- |
| `fill(Canvas, x, y, width, height, color)` | 矩形填充 |
| `rounded(Canvas, x, y, width, height, radius, color)` | 圆角矩形填充 |
| `gradient(Canvas, x, y, width, height, startColor, endColor, boolean vertical, radius)` | 水平或垂直渐变 |
| `gradientDiagonal(Canvas, x, y, width, height, startColor, endColor, radius)` | 对角渐变 |
| `line(Canvas, x1, y1, x2, y2, thickness, color)` | 线段 |
| `line(Canvas, x1, y1, x2, y2, thickness, color, PaintStrokeCap cap)` | 指定端点形状的线段；原重载使用 BUTT |
| `outline(Canvas, x, y, width, height, radius, thickness, color)` | 矩形或圆角矩形描边 |
| `fillPath(Canvas, io.github.humbleui.skija.Path, color)` | 路径填充 |
| `strokePath(Canvas, io.github.humbleui.skija.Path, thickness, color, PaintStrokeCap cap, PaintStrokeJoin join)` | 指定端点和连接形状的路径描边 |
| `dropShadowPath(Canvas, io.github.humbleui.skija.Path, blur, color)` | 路径阴影 |
| `dropShadowRounded(Canvas, x, y, width, height, radius, offsetY, blur, color)` | 圆角矩形阴影 |
| `glowLayer(Canvas, x, y, width, height, blur, int strength, Runnable drawing)` | 局部滤镜图层；`strength` 被限制为 1–10 |

`glowLayer` 中的回调用于生成发光层；需要清晰前景时另行绘制前景。阴影、发光和渐变接口不意味着最终像素结果已缓存。

线段与路径描边复用共享 Paint，线宽至少为 0.1，调用结束后恢复填充模式、默认线宽、端点与连接状态。音乐 UI 的圆角、线段及图标路径通过这些接口绘制，不单独创建基础图形 Paint。

### 3.3 文字与图标：SkijaUi

| 接口 | 用途 |
| --- | --- |
| `text(Canvas, text, x, top, height, color[, size[, fontName]])` | 普通文字，在行框中垂直居中 |
| `boldText(Canvas, text, x, top, height, color[, size])` | 粗体文字，在行框中垂直居中 |
| `textAtTop(Canvas, text, x, top, color, size, boolean bold)` | 按字体行框顶部定位 |
| `textShadow(Canvas, text, x, top, height, color[, size[, fontName]])` | 普通文字及阴影 |
| `boldTextShadow(Canvas, text, x, top, height, color, size)` | 粗体文字及阴影 |
| `textWithFallback(Canvas, text, x, top, height, color, size[, fontName])` | 显式使用回退文字入口 |
| `textShadowWithFallback(Canvas, text, x, top, height, color, size[, fontName])` | 回退文字及阴影 |
| `textWidth(text[, size[, fontName]]): float` | 普通文字宽度，包含回退字体 |
| `boldTextWidth(text[, size]): float` | 粗体文字宽度 |
| `textWidthWithFallback(text, size[, fontName]): float` | 回退文字宽度 |
| `textMetrics(size): FontMetrics` | 普通主字体的度量，不是混排文字的实际墨迹边界 |
| `icon(Canvas, glyph, x, top, height, color, size, IconSet)` | 图标字形 |
| `iconWidth(glyph, size, IconSet): float` | 图标宽度 |
| `iconMetrics(size, IconSet): FontMetrics` | 图标字体度量 |
| `prepareText(text, size, boolean bold): PreparedText` | 创建可复用的文本字形数据 |
| `PreparedText.width(): float` | 已计算的宽度 |
| `PreparedText.draw(Canvas, x, baseline, color)` | 按基线绘制 |
| `PreparedText.drawTop(Canvas, x, top, color)` | 按行框顶部绘制 |
| `PreparedText.close()` | 释放所属 TextBlob，可重复关闭 |

省略字号的重载使用 9。普通 `text` 与 `boldText` 已接入共享字体回退，不需要为中文另建渲染器。`IconSet` 支持 `PVP_CONTROLS` 和 `MATERIAL_SYMBOLS`。

### 3.4 字体管理：SkijaUi

| 接口 | 用途 |
| --- | --- |
| `CLIENT_FONT` | 内置字体名 `"harmony"` |
| `availableFontNames(): List<String>` | 获取内置和导入字体名 |
| `selectFont(String)` | 切换全局字体 |
| `withFont(String, Runnable)` | 临时覆盖当前线程的字体选择，回调结束后恢复 |
| `textFontRevision(): long` | 全局字体选择或重载的版本号 |
| `fontDirectory(): java.nio.file.Path` | 获取字体目录，可能抛出 `IOException` |
| `importFont(java.nio.file.Path): String` | 导入字体并返回名称，可能抛出 `IOException` |
| `reloadImportedFonts()` | 重载导入字体，更新字体版本 |
| `close()` | 关闭共享字体资源，仅限框架生命周期管理 |

字体导入和重载包含同步文件操作，应置于明确的设置操作中，不得每帧执行。当前实现首次使用字体时也可能触发延迟加载；可在初始化阶段通过现有接口完成预热。

`textFontRevision()` 不随 `withFont()` 的临时覆盖变化。使用临时字体的缓存必须额外包含字体名，测量与绘制必须使用相同的字体选择。

### 3.5 液态玻璃

入口为 `LiquidGlassRenderer.getInstance()`。

| 接口 | 用途 |
| --- | --- |
| `renderPanel(Minecraft, x, y, width, height, radius, int tintColor, boolean shadow, boolean highlight, float highlightTaper, int slot): boolean` | 绘制玻璃面板 |
| 上述接口末尾增加 `SlotGrid grid` | 添加面板内部格槽描述 |
| `SlotGrid(int count, float x, float y, float pitch, float size, float radius)` | 面板局部 GUI 坐标下的格槽；`pitch` 为水平间距 |
| `panelTint(): int` | 获取当前配置的面板着色 |
| `tick()`、`destroy()` | 框架统一维护与释放 |

`slot` 仅允许 0–3，当前实现没有越界保护。为并存面板分配稳定的槽位；不要将任意模块编号直接传入。槽位复用离屏资源分配，不缓存整个面板的最终画面。

面板使用屏幕 GUI 坐标及专用 Canvas，不继承调用方的局部变换或裁剪。采样来自 Minecraft 主帧缓冲，与常规叠加背景快照的来源和时序不同。`false` 表示需要降级；`true` 仅代表外层流程成功，内部采样不可用时仍可能没有可见像素。组件应提供基础背景，避免完全依赖玻璃输出。

### 3.6 世界绘制：WorldRender

| 接口 | 用途 |
| --- | --- |
| `line(Vec3 from, Vec3 to, int color, float width[, boolean throughWalls])` | 世界空间线段；默认不穿墙 |
| `box(AABB, int outlineColor)` | 盒体线框 |
| `box(AABB, int outlineColor, int fillColor)` | 盒体线框与填充 |
| `filledBox(AABB, int fillColor[, boolean throughWalls])` | 盒体填充；默认不穿墙 |
| `face(AABB, Vec3 viewer, int color, float width)` | 根据观察者方向选取盒体表面描边 |
| `face(AABB, Direction, int color, float width)` | 指定表面描边 |
| `project(Vec3): ScreenPoint` | 世界坐标转 GUI 坐标；可能返回 `null` |
| `ScreenPoint(float x, float y)` | 投影后的 GUI 坐标 |
| `capture(Camera, Matrix4fc modelView, Matrix4fc projection)` | 捕获当前世界视图，只由框架调用 |

在 `WorldRenderLevelRendererMixin` 已建立的 `collectPerFrameGizmos()` 收集区间内接入三维命令，不要在二维叠加回调中调用世界绘制。`project()` 检查矩阵是否可用、相机前后及屏幕 X/Y 范围，不执行深度遮挡检测。

### 3.7 框架内部接口

下列方法虽然部分为 `public`，但不属于新功能的常规接入接口。

| 类 | 接口组 |
| --- | --- |
| `SkijaFrameRenderer` | `render()`、`close()` |
| `SkijaRenderer` | `beginFrame()`、`renderMainSubmitted()`、`renderSubmitted()` |
| `SkijaRenderer` | `renderOverlay(Consumer<Canvas>)`、`renderMainTarget(Consumer<Canvas>)` |
| `SkijaRenderer` | `currentFramebuffer()`、`flush()`、`restoreContext()`、`close()` |
| `GlassCaptureRenderer` | `getInstance()`、`currentDrawFramebufferId()`、`beginFrame(int)`、`endFrame()`、`context()`、`capture(Minecraft, x, y, width, height, margin)`、`destroy()` |
| `GlassCaptureRenderer.Capture` | `EMPTY`、`image`、`width`、`height`、`dstX`、`dstY`、`dstW`、`dstH` |
| `GlassGlBackend` | `begin()`、`begin(int)`、`end()`、`isDrawing()`、`hasSurface()` |
| `GlassGlBackend` | `getContext()`、`ensureContext()`、`resetCanvasState()`、`runWithSavedState(Runnable)`、`destroy()` |

`renderMainTarget()` 在共享画布已活动时复用当前通道，不强制切换目标。`currentFramebuffer()` 返回框架保存的目标状态，通道之外不能用它代替真实 GL 绑定查询。不要为每个控件调用 `flush()` 或创建新的 `DirectContext`。

## 4. 坐标、动画与资源约定

### 4.1 坐标空间

常规 Canvas 已由框架缩放到 GUI 逻辑坐标。完整屏幕使用 `Screen.width/height`，HUD 使用 `Window.getGuiScaledWidth()/getGuiScaledHeight()`；不要再次乘以 GUI scale。物理帧缓冲尺寸由后端管理。

矩形参数为 `x, y, width, height`，不是右下角坐标。颜色统一使用 `0xAARRGGBB`。

布局应根据当前可用宽高计算。若使用设计坐标与局部缩放，鼠标必须应用同一变换的逆变换。例如局部变换为 `screenX = originX + localX * scale`，则 `localX = (mouseX - originX) / scale`。绘制、裁剪和命中测试必须使用同一帧的几何结果。

主界面的 `MainUiScale` 是页面专用布局工具。修改这些页面时配套使用 `applyPage`、`pageX/pageY/pageEvent` 和 `pageScreenX/pageScreenY/pageScreenSize`，并传递一致的 scale 与中心点。不要将该工具默认应用于全部 HUD。

### 4.2 文字定位与复用

`text(..., top, height, ...)` 将文字按字体度量居中到行框，基线为：

`baseline = top + (height - (descent - ascent)) / 2 - ascent`

`height` 是行框高度，`size` 是字号。`textAtTop()` 和 `PreparedText.drawTop()` 的 top 是字体行框顶部，不是可见字形墨迹的最上沿。`PreparedText.draw()` 则直接接收基线。

`PreparedText` 保存 TextBlob 字形数据及宽度，避免每帧重复回退分段、宽度测量和字形构造。它不是位图缓存，也不自动执行换行、省略、复杂文字排版或字体版本更新。中文和缺失字形沿用现有回退逻辑；CJK 优先尝试系统回退字体，粗体请求不保证回退字形具有粗体效果。

缓存键至少包含文字、离散字号、粗体状态、字体版本，以及使用时的临时字体名。换行或截断结果还需包含可用宽度、翻译显示模式等布局条件。位置和颜色变化不需要重建 TextBlob。

### 4.3 动画与生命周期

每帧只推进一次动画状态，保存本帧位置、缩放、透明度和裁剪几何。绘制与输入方法只读取结果；禁止在多个 getter 中重复推进动画。异步数据加载更新内容，不应无条件重启页面进入动画。

Minecraft 返回既有 Screen 实例时，`added()` 会执行，但受保护的 `init()` 不保证再次执行。返回页面时的动画重置使用 `added()` 或显式的激活逻辑；尺寸相关布局需响应 resize/reposition 或按当前宽高计算。资源释放后的实例再次显示时，必须能按需重建资源。

背景模糊应在局部 `translate/scale` 之前绘制，传入屏幕 GUI 坐标。模糊区域、裁剪圆角和前景必须来自同一帧几何。旋转或更复杂变换需要单独设计屏幕空间采样，不能仅传入未经转换的局部矩形。

### 4.4 所有权

| 资源 | 责任 |
| --- | --- |
| 框架提供的 `Canvas` | 借用，不关闭，不跨帧保存，不传给工作线程 |
| `SkijaUi` 内部 Font、Typeface、Paint | 共享资源；功能模块不关闭 |
| 功能创建的 `PreparedText`、Paint、Path、Image、Surface、滤镜等 | 功能持有并在失效、替换、关闭时释放 |
| `BorrowedImage` | 在活动 Skia 回调中借用并用 try-with-resources 关闭包装 |
| Minecraft 纹理 | 由 Minecraft 纹理系统管理，不由借用者销毁 |
| 框架上下文、Surface 缓存、玻璃资源 | 由框架在重建或客户端关闭时管理 |

使用 `save()` 与 `restoreToCount()` 成对保护局部变换和裁剪，并使用 `try/finally` 保证恢复。只有确实需要整体透明度或滤镜时才使用 `saveLayer()`。不能把功能缓存提升为全局策略而不评估所有调用方。

## 5. 使用示例

以下四个类可分别放入客户端源码集。示例展示接口组合与生命周期；不提供自动注册、导航或输入控件。

### 5.1 自适应屏幕与可复用文字

在 `render()` 收集当前尺寸，在 `renderSkija()` 绘制。关闭时释放标题，返回同一实例后自动重建。该示例完全自行绘制背景，因此覆盖默认背景入口；混合原版控件的页面需要另行设计叠加顺序，避免覆盖原版控件。

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

### 5.2 当前帧 HUD 提交

持有一个 `ChromaHudLabel` 实例，从现有客户端 HUD 收集入口每帧调用一次 `submit(...)`，调用时机必须位于帧开始之后、叠加队列消费之前。关闭模块时停止提交，再调用 `close()`。该示例只缓存当前文字，不随位置变化重建，也不在菜单上方显示。

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

### 5.3 缩放面板与背景模糊对齐

从活动 Skia 回调、尚未施加页面局部变换的 Canvas 调用。scale 由调用方每帧计算一次；文字由调用方准备并管理。屏幕裁剪与局部前景使用同一次 scale 计算结果。

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

这个示例展示动画几何的一致性，不表示连续缩放文字没有成本。追求最低文字开销时优先保持字号与缩放稳定，仅移动位置或改变透明度。模糊不可用的首帧仍有基础填充。

### 5.4 借用 Minecraft 纹理

在活动共享 Skia 回调中调用，传入 Minecraft 纹理管理器所管理的 `Identifier`。不要跨资源重载缓存借用包装；长期图像缓存需要独立的所有权及失效设计。

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

## 6. 性能最佳实践

以下规则用于减少可避免的 CPU、GPU 与本地资源开销，不构成特定设备上的帧率承诺。

### 6.1 推荐顺序

1. 先停止不可见、透明度为零、位于视口外或被完整页面遮挡的内容；长列表只遍历可见区及必要缓冲行。Canvas 裁剪不会替代 CPU 侧的遍历、测量和排版剔除。
2. 使用现有二维通道，合并同一组件的绘制，避免重复队列提交、额外 GL 状态查询与逐控件 flush。
3. 将静态排版、截断、文字测量和 TextBlob 构造移到内容或布局失效时，逐帧只处理必要的位置、颜色与裁剪。
4. 优先使用填充、圆角和渐变；仅对必要区域使用模糊、阴影、发光或玻璃。
5. 稳态页面直接绘制。仅在需要整体合成的过渡期创建有明确边界的图层；不以全屏 `saveLayer` 包裹所有界面。
6. 网络、文件读取和图片解码不放入绘制回调；后台处理普通数据，GPU 资源创建、更新和释放仍由正确的客户端渲染阶段完成。

### 6.2 字号与缓存边界

当前普通与回退 Font 缓存以精确 float 字号为键，没有条目数上限。连续把动画值作为字号传入，会不断建立 Font 对象。优先使用固定字号；确需变化时在功能边界限制范围并离散化，例如：

`size = Math.round(Math.clamp(requestedSize, 8f, 48f) * 4f) * 0.25f`

绘制、测量和缓存键必须使用这个相同结果。0.25 是示例精度，不是框架自动规则。仅限制 PreparedText 缓存数量不能阻止任意新字号进入共享 Font 缓存。

| 数据 | 推荐缓存策略 | 失效条件 |
| --- | --- | --- |
| 单行固定标签 | 单个 `PreparedText` | 内容、字号、字重、字体选择或版本改变 |
| 歌词、多行列表 | 按功能限制数量的文本及排版缓存 | 歌曲、文字、布局宽度、显示模式、字体变化；淘汰时 close |
| 静态图片 | 功能所属图像缓存，限制像素总量 | 图片身份、目标尺寸或资源版本改变 |
| 静态图片的模糊结果 | 只在图像或效果参数变化时生成 | 图片、分辨率、模糊参数变化 |
| 实时世界背景 | 使用框架的当前通道快照 | 不能将旧画面长期当作当前世界 |
| 玻璃槽位 | 复用分配的离屏目标 | 尺寸需要增长或框架销毁 |

音乐模块的 `MusicBlurredCover` 是模糊封面背景的功能缓存，256 像素工作尺寸只用于该类低频细节背景。不得将其推广到正文、图标、普通封面或全部 UI。

### 6.3 背景与特效成本

常规叠加通道在开始时读取已记录的模糊请求标记，通常来自上一帧；通道内才发出的首次请求可能没有快照。快照在需要的通道内刷新，不是已模糊画面的跨帧缓存。底层捕获保留整张帧缓冲图像，具体模糊绘制裁取所需区域；局部模糊不能消除整帧快照本身的成本。

不要为每个控件重新截图，不要将本组件已经绘制的内容再次采样为背景。给效果提供基础填充降级；共享渲染失败会记录错误并在当前会话禁用，背景模糊还有独立的失败禁用状态。先定位日志中的首次异常，不要用空 catch 隐藏绘制错误。

玻璃需要背景捕获、上下文衔接、离屏绘制和滤镜，成本通常高于基础图形。为可见面板提供效果，避免对长列表中的每一项重复建立玻璃流程。

### 6.4 验证

检查客户端编译、不同 GUI 缩放与窗口尺寸的几何计算、首次进入和返回、字体切换后的失效、长期动画中的缓存数量、关闭后的本地资源释放。性能证据应区分 CPU 耗时、GPU 耗时、分配量及缓存命中情况；只凭一次 FPS 读数不能定位瓶颈。

按根目录 `AGENTS.md` 设置项目内临时目录，执行包含 `compileClientJava` 的编译任务。未得到用户明确要求时，不启动 Minecraft 做运行测试。

## 7. AI 开发代理使用规范

处理 UI、HUD、文字、模糊、玻璃或底层渲染任务前，必须阅读本节、相关接口章节及对应实现。

1. **确认环境与 API。** 以当前仓库代码、Minecraft 1.21.11 官方映射源码和实际依赖为依据。先阅读涉及的 Minecraft 类，再编写调用；不得猜测方法签名或虚构 Chroma Graphics API。
2. **确定阶段。** 明确内容属于 Screen、主目标 GUI、HUD 叠加还是世界 Gizmos，确认提交点在对应队列消费之前。复用现有入口，不重复注册相同内容。
3. **复用共享字体。** 常规文字使用 `SkijaUi`；重复文字使用明确所有权的 `PreparedText`。不要另建字体管理器，不要将连续浮点动画字号直接送入共享缓存。
4. **统一几何。** 每帧只推进一次动画。绘制、模糊、裁剪和鼠标命中读取同一份结果；所有布局明确区分物理像素、GUI 坐标和局部设计坐标。
5. **限制工作量。** 先剔除不可见内容，再处理可见布局。缓存注明键、容量、失效条件和释放时机；单个功能的低分辨率缓存不得改变全局清晰度策略。
6. **维护所有权。** 不保存或关闭借出的 Canvas；关闭自己创建的本地资源；GPU 操作留在客户端渲染阶段。功能模块不得调用共享资源的全局关闭入口。
7. **遵守框架边界。** 不为普通控件新建上下文、截图链路、字体路径或全屏离屏通道。确需修改后端时，核实全部受影响调用方的 GL 状态、资源与时序。
8. **覆盖失败与返回路径。** 处理模糊或纹理不可用、窗口变化、字体重载、页面返回及资源重建。异步结果更新数据，不重复触发进入动画。
9. **完成必要验证。** 按 `AGENTS.md` 修正进程临时目录并包含客户端编译。仅在用户明确要求时启动游戏；不得将静态检查或离线结果表述为实机帧率验证。
10. **同步文档。** 修改公开接口或行为时同步更新中英文文档与示例。遵守项目的禁止新增代码注释规则；解释放在文档正文中。

提交说明应列明所用绘制阶段、缓存与资源生命周期、实际执行的验证及尚未验证的运行行为。

## 8. 实现索引

- [SkijaScreen](../../src/client/java/com/pvp_utils/client/render/skia/SkijaScreen.java)、[SkijaFrameRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaFrameRenderer.java)
- [SkijaRenderer](../../src/client/java/com/pvp_utils/client/render/skia/SkijaRenderer.java)、[SkijaUi](../../src/client/java/com/pvp_utils/client/render/skia/SkijaUi.java)、[SkijaBackdrop](../../src/client/java/com/pvp_utils/client/render/skia/SkijaBackdrop.java)
- [LiquidGlassRenderer](../../src/client/java/com/pvp_utils/client/render/skia/LiquidGlassRenderer.java)、[GlassCaptureRenderer](../../src/client/java/com/pvp_utils/client/render/skia/GlassCaptureRenderer.java)、[GlassGlBackend](../../src/client/java/com/pvp_utils/client/render/skia/GlassGlBackend.java)、[GlassGlState](../../src/client/java/com/pvp_utils/client/render/skia/GlassGlState.java)
- [WorldRender](../../src/client/java/com/pvp_utils/client/render/world/WorldRender.java)、[WorldRenderLevelRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/WorldRenderLevelRendererMixin.java)
- [MinecraftMixin](../../src/client/java/com/pvp_utils/mixin/client/MinecraftMixin.java)、[GameRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/GameRendererMixin.java)
- [MainUiScale](../../src/client/java/com/pvp_utils/client/render/MainUI/MainUiScale.java)、[MainUIFlipAnimation](../../src/client/java/com/pvp_utils/client/render/MainUI/MainUIFlipAnimation.java)
- [共享字体的功能用例：LyricsDisplayRenderer](../../src/client/java/com/pvp_utils/client/modules/impl/Render/LyricsDisplayRenderer.java)、[局部图片缓存：MusicBlurredCover](../../src/client/java/com/pvp_utils/client/NeteaseMusic/MusicBlurredCover.java)
- [构建定义](../../build.gradle)、[项目协作规则](../../AGENTS.md)
- [插件接口](../PLUGINS.md)、[插件教程](../PLUGIN_TUTORIAL.md)、[着色器文档](../SHADERS.md)

JavaScript 插件使用插件系统提供的渲染外观接口，其参数与生命周期以插件文档为准。主界面背景、翻转、音乐背景缓存及着色器是建立在相关渲染路径上的专用功能，不是所有组件都必须采用的全局策略。
