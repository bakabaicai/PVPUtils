# Chroma Graphics

[English](README.md) · 简体中文

## 1. 概述

Chroma Graphics 是基于 Skia 的客户端渲染框架，提供二维图形、文字与图标、背景模糊及世界空间图形接口。框架统一管理常规二维绘制的 Skia 上下文、帧缓冲包装、绘制时序和 OpenGL 状态恢复。本文说明当前实现的接口、坐标空间、调用约束、资源生命周期与性能使用方法。当前后端接入 Minecraft，依赖如下。

通用接口统一位于 `com.pvp_utils.client.render.chroma` 及其子包。`ChromaGraphics` 管理二维绘制通道与提交，`ChromaDraw` 提供图形和文字接口，`ChromaWorld` 提供世界绘制接口。

| 项目 | 当前实现 |
| --- | --- |
| Minecraft | 1.21.11，官方 Mojang 映射 |
| Java | 21 |
| Skia Java 绑定 | Skija 0.143.17 |
| 几何类型 | HumbleUI Types 0.2.0 |
| 当前打包的本地后端 | Windows x64 |
| 常规二维渲染 | Skija、OpenGL、GUI 逻辑坐标 |
| 三维渲染 | `ChromaWorld` 封装 Minecraft Gizmos |

框架提供绘制能力，不自动完成组件注册、布局、输入命中、文本换行、异步任务调度或业务缓存。底层画布适配 GUI 缩放，不等于界面布局自动适配所有分辨率。

### 1.1 模块分工

| 模块 | 职责 | 使用范围 |
| --- | --- | --- |
| `ChromaScreen` | 屏幕的二维绘制入口 | 自定义 `Screen` |
| `ChromaOverlay` | 加载叠加层的二维绘制入口及资源生命周期 | 自定义 `Overlay` |
| `ChromaGraphics` | 活动画布、主目标与叠加队列、纹理借用、背景模糊 | 功能代码使用指定入口；帧调度由框架维护 |
| `ChromaDraw` | 图形、文字、图标、共享字体及预备文本 | 常规 UI/HUD 首选 |
| `ChromaFrameRenderer` | HUD、屏幕、加载叠加层和队列的帧级调度 | 框架内部 |
| `ChromaBackdrop` | 背景采样、GL 状态保存与恢复 | 包内实现 |
| `ChromaWorld` | 世界空间图形及世界坐标投影 | 三维功能模块 |

常规二维绘制复用一个受管理的 Skia 上下文。三维图形走 Minecraft Gizmos，不经过二维 Canvas。

### 1.2 包结构

| 包 | 内容 |
| --- | --- |
| `com.pvp_utils.client.render.chroma` | 二维提交、屏幕与叠加层契约、图形与文字接口、帧调度及背景采样 |
| `com.pvp_utils.client.render.chroma.effects` | 阴影、内容过渡、轮廓分离与状态图标 |
| `com.pvp_utils.client.render.chroma.world` | 世界几何、坐标投影、绘制注册与粒子集合 |

文件位置与职责见第 8 节。方法的调用约束、坐标空间与资源所有权由各接口章节说明。

## 2. 绘制时序与接入方式

### 2.1 当前帧的执行顺序

1. `MinecraftMixin` 在 `Minecraft.runTick` 开始时调用 `ChromaGraphics.beginFrame()`，清空两条提交队列，并使上一帧世界投影失效。
2. Minecraft 收集 GUI 绘制内容。此期间在活动 Skia 画布之外调用 `draw()`，会进入主目标队列。
3. `GameRendererMixin` 在 `GuiRenderer.render(GpuBufferSlice)` 之前调用 `renderMainSubmitted()`，将主目标队列绘制到 Minecraft 主颜色目标。
4. Minecraft 将主目标 `blitToScreen()` 后，`ChromaFrameRenderer.render()` 开始叠加绘制。窗口最小化时跳过；存在加载叠加层时，仅处理已启用的 `ChromaOverlay`。
5. 无加载叠加层时，叠加通道依次执行宿主接入的 HUD 绘制、当前 `ChromaScreen`、`submit()` 队列和宿主的末尾叠加绘制。有已启用的 `ChromaOverlay` 时，按其显示策略绘制后方屏幕与队列，最后绘制该叠加层，不调度常规 HUD。
6. 通道结束时提交 Skia 命令并恢复外部 GL 状态。

主目标通道使用包装 Minecraft 主颜色纹理的 FBO；叠加通道使用默认帧缓冲 FBO 0。主目标纹理按对象身份检查重建，不以可复用的 OpenGL 纹理编号作为生命周期标识。通道开始时完成尺寸检查、Surface 复用或重建、GL 状态同步及 GUI 坐标缩放。

状态边界同时恢复实际 OpenGL 状态和 Minecraft 的缓存状态。纹理保护范围包括 Minecraft 使用的前 12 个单元、Skia 的临时纹理单元及进入通道时的活动单元；另保存栅格模式、多边形偏移、多重采样、点尺寸模式、线宽、抖动和 renderbuffer 绑定。借用未加载的宿主纹理时，框架隔离其冷加载操作，规范化像素上传状态，并使 Skia 重新同步 GL 状态。已加载纹理直接借用，不增加完整状态保存。

常规 HUD 的显示条件由宿主控制，前台菜单默认不调度该层。需要在菜单中显示的叠加内容应明确其显示条件与层级；队列提交和实际绘制时均应检查当前界面，避免切换界面后继续覆盖前景。

### 2.2 入口选择

| 场景 | 标准方式 |
| --- | --- |
| 新建完整 Skia 屏幕 | `Screen implements ChromaScreen`，在 `renderChroma(Canvas)` 中直接绘制 |
| 新建 Skia 加载叠加层 | `Overlay implements ChromaOverlay`，在常规 `render(...)` 中收集帧状态，在 `renderChroma(Canvas)` 中绘制 |
| 已持有框架提供的 Canvas | 直接调用 `ChromaDraw`、`PreparedText` 或必要的 Skija Canvas 方法 |
| 将局部绘制封装为回调 | `ChromaGraphics.draw(painter)`；活动通道内立即执行并保护 Canvas 状态 |
| 在 GUI 收集阶段绘制主目标内容 | 在主目标队列消费前调用 `draw(painter)` |
| 从已有的当前帧 HUD 收集入口提交叠加内容 | `ChromaGraphics.submit(painter)`，在叠加队列消费前提交 |
| 世界空间线框或标记 | 在现有世界渲染 Gizmos 收集阶段调用 `ChromaWorld` |

`draw()` 与 `submit()` 不能互换：前者没有活动 Canvas 时进入主目标队列，后者始终进入叠加队列。叠加队列在屏幕之后绘制；不应覆盖菜单的 HUD 必须自行检查 `client.screen` 等显示条件。

队列仅服务当前帧，不是持久任务队列，也不具备线程安全保证。不得从网络线程提交 Canvas 操作，不得在队列消费过程中继续入队并依赖下一帧执行。异步工作只发布数据；客户端线程在正确阶段读取数据并提交绘制。

同一内容只能接入一个绘制入口。已由 `ChromaFrameRenderer` 调用的屏幕或 HUD，不应再额外提交相同内容。实现 HUD 类本身不会自动注册到帧调度器。

### 2.3 加载叠加层

`ChromaOverlay` 复用现有叠加通道，不创建独立上下文。`isChromaOverlayActive()` 默认返回 `true`；返回 `false` 时，框架保留该叠加层的原版绘制行为。常规 `render(...)` 每帧只更新一次布局和动画状态，`renderChroma(...)` 消费同一份状态。

`rendersScreenBehind()` 默认返回 `false`。返回 `true` 时，叠加层的常规 `render(...)` 必须先调用后方屏幕的原版绘制入口；框架随后依次绘制该屏幕的 Skia 部分、`submit()` 队列和叠加层。仅在这个分支消费叠加队列，常规 HUD 不参与加载叠加层通道。全屏渐隐应由最后绘制的叠加层覆盖后方内容，避免混合原版控件与 Skia 内容时产生时序差异。

调用 `Minecraft.setOverlay(...)` 替换实例时，框架关闭前一个 `ChromaOverlay`；客户端渲染器关闭时也关闭仍在使用的实例。实现方在 `close()` 中释放自己持有的文字、路径、图像及滤镜，保持重复关闭安全。原版 `Overlay` 没有屏幕的 `removed()` 生命周期，不能依赖该方法释放叠加层资源。

## 3. 接口列表

以下表格使用紧凑调用记法，不是可直接复制的 Java 声明。`Canvas` 表示首个画布参数；几何尺寸、字号、线宽和强度一般为 `float`，颜色为 `int`，文字和字体名为 `String`。方括号表示已有可选重载，不表示可选 Java 参数。除另行注明外，绘制方法返回 `void`。完整类型与重载以链接的当前源码为准。

### 3.1 屏幕、提交与纹理

| 接口 | 行为与约束 |
| --- | --- |
| `ChromaScreen.renderChroma(Canvas)` | 当前屏幕的实际 Skia 绘制；Canvas 由框架借出 |
| `ChromaOverlay.renderChroma(Canvas)` | 加载叠加层的实际 Skia 绘制；继承 `ChromaScreen`，使用同一共享 Canvas |
| `ChromaOverlay.isChromaOverlayActive(): boolean` | 是否启用该实例的 Skia 叠加绘制；默认 `true` |
| `ChromaOverlay.rendersScreenBehind(): boolean` | 是否先绘制后方屏幕及提交队列；默认 `false` |
| `ChromaOverlay.close()` | 释放该实例所属资源；由叠加层替换与客户端关闭触发 |
| `ChromaGraphics.draw(Consumer<Canvas>)` | 活动画布内立即绘制；否则加入主目标队列 |
| `ChromaGraphics.draw(String source, Consumer<Canvas>)` | 与 `draw(painter)` 使用相同通道，并为诊断日志提供稳定的组件名称 |
| `ChromaGraphics.submit(Consumer<Canvas>)` | 加入当前帧叠加队列 |
| `ChromaGraphics.submit(String source, Consumer<Canvas>)` | 提交叠加内容并保留诊断名称 |
| `ChromaGraphics.isDrawing(): boolean` | 是否存在活动的共享画布 |
| `ChromaGraphics.hasFailed(): boolean` | 共享渲染器是否已因错误禁用 |
| `ChromaGraphics.isMainTargetAvailable(): boolean` | 在 GUI 内容收集阶段检查主颜色纹理与包装 FBO；必要时重建包装并恢复外部 GL 状态。不可用时由调用方保留宿主绘制 |
| `drawBlurredBackdrop(Canvas, RRect, x, y, width, height, strength)` | 按屏幕 GUI 坐标采样，圆角矩形裁剪 |
| `drawBlurredBackdrop(Canvas, io.github.humbleui.skija.Path, x, y, width, height, strength)` | 按屏幕 GUI 坐标采样，路径裁剪 |
| `borrowTexture(Identifier): BorrowedImage` | 借用 Minecraft GL 纹理，可能返回 `null` |
| `BorrowedImage.image(): Image` | 获取借用包装中的 Skia 图像 |
| `BorrowedImage.close()` | 释放包装，不销毁 Minecraft 拥有的纹理 |

诊断名称应为稳定的组件名称，不要每帧生成唯一标识。省略名称时，框架在异常日志中使用绘制回调的所属类。`isMainTargetAvailable()` 仅表示检查时的目标可用，不确认后续命令执行或 GPU 完成；应在替换宿主绘制命令之前调用，不在活动 Canvas 内调用。

渲染异常日志包含通道、执行阶段、组件、FBO 及物理与 GUI 尺寸。主目标不可用、纹理包装失败和背景采样失败分别记录；背景采样失败仅停用背景模糊，保留其他二维绘制。相同问题的重复日志间隔为 10 秒，并统计被抑制的次数；短暂纹理或快照缺失持续 2 秒后才记录。主目标与叠加通道分别每秒采样一次 OpenGL 错误，区分进入通道前已有的错误与本次提交后的错误，不进行逐帧像素回读。日志不能自动判断布局偏移、层级选择或颜色配置等语义错误。

### 3.2 图形与效果：ChromaDraw

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

线段与路径描边复用共享 Paint，线宽至少为 0.1，调用结束后恢复填充模式、默认线宽、端点与连接状态。

### 3.3 文字与图标：ChromaDraw

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

### 3.4 字体管理：ChromaDraw

| 接口 | 用途 |
| --- | --- |
| `CLIENT_FONT` | 内置字体名 `"harmony"` |
| `PRODUCT_SANS_REGULAR` | 内置 Product Sans Regular 字体名 `"product_sans_regular"` |
| `PRODUCT_SANS_LIGHT` | 内置 Product Sans Light 字体名 `"product_sans_light"` |
| `MISANS_REGULAR` | 内置 MiSans Regular 字体名 `"misans_regular"` |
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

Product Sans 按字体名区分 Regular 与 Light。局部绘制使用 `withFont()` 选择字重，预备文本和字体度量应在同一字体覆盖范围内计算。生成的 `PreparedText` 已保存所选字体的字形，后续绘制无需再次切换字体。内置 Typeface 与 Font 由框架管理，调用方仅释放自己持有的预备文本。

### 3.5 世界绘制：ChromaWorld

所有位置均为世界坐标。绘制在 Minecraft Gizmos 收集区间内提交，填充与线条由宿主批量处理；`throughWalls=false` 保留深度遮挡，`true` 使用始终置顶绘制。

| 接口 | 用途 |
| --- | --- |
| `line(Vec3 from, Vec3 to, int color, float width[, boolean throughWalls])` | 线段；默认不穿墙 |
| `box(AABB, int outlineColor)` | 盒体线框 |
| `box(AABB, int outlineColor, int fillColor)` | 盒体线框与填充 |
| `box(AABB, int outlineColor, int fillColor, float width, boolean throughWalls)` | 指定线宽与深度行为的盒体 |
| `filledBox(AABB, int fillColor[, boolean throughWalls])` | 盒体填充；默认不穿墙 |
| `face(AABB, Vec3 viewer, int color, float width)` | 根据观察者方向选取盒体表面描边 |
| `face(AABB, Direction, int color, float width)` | 指定表面描边 |
| `filledFace(AABB, Direction, int color, boolean throughWalls)` | 指定表面填充 |
| `point(Vec3, int color, float size, boolean throughWalls)` | 屏幕尺寸的点标记 |
| `arrow(Vec3 from, Vec3 to, int color, float width, boolean throughWalls)` | 箭头 |
| `polyline(List<Vec3>, int color, float width, boolean closed, boolean throughWalls)` | 最多 4096 个顶点的折线或闭合线框 |
| `triangle(Vec3 a, Vec3 b, Vec3 c, int color, boolean throughWalls)` | 三角形填充 |
| `quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, boolean throughWalls)` | 四边形填充 |
| `circle(Vec3 center, Vec3 normal, double radius, int segments, int outlineColor, int fillColor, float width, boolean throughWalls)` | 任意平面的圆环或圆盘；分段数限制为 3–128 |
| `sphere(Vec3 center, double radius, int segments, int rings, int color, float width, boolean throughWalls)` | 球体线框；经线 6–64 段、纬度 3–32 段 |
| `billboard(Vec3 center, float width, float height, float rotation, int color, ParticleShape, boolean throughWalls)` | 面向相机的图形；尺寸为世界单位，旋转为弧度 |
| `ParticleShape` | `QUAD`、`CIRCLE`、`STAR`、`HEART`、`BLOOM`；几何图形，不依赖外部贴图 |
| `project(Vec3): ScreenPoint` | 世界坐标转 GUI 坐标；可能返回 `null`，不执行深度遮挡检测 |
| `ScreenPoint(float x, float y)` | 投影后的 GUI 坐标 |
| `projectDetailed(Vec3): ChromaProjection.Point` | 当前帧完整投影；屏幕外的点保留坐标，使用 `onScreen` 判断窗口范围 |
| `projectBounds(AABB): ChromaProjection.Bounds` | 当前帧包围盒投影；先裁剪近、远平面，再计算窗口相交范围 |
| `projectionFrame(): ProjectionFrame` | 当前世界和 GUI 尺寸匹配的投影帧；没有有效捕获时返回 `null` |
| `ProjectionFrame(ChromaProjection projection, long id, float partialTick, float frozenPartialTick)` | 不可变投影、帧编号与宿主普通/冻结实体的插值进度 |
| `register(Consumer<Frame>): Registration` | 注册世界绘制回调，返回由调用方持有的注册句柄 |
| `Frame(Vec3 cameraPosition, float partialTick)` | 当前帧的相机位置及插值进度 |
| `Registration.close()` | 注销回调；可重复调用 |
| `capture(Camera, Matrix4fc modelView, Matrix4fc projection[, float partialTick, float frozenPartialTick])`、`beginFrame()`、`renderRegistered(float partialTick)` | 框架内部的捕获、帧失效和回调调度；三参数捕获的两个插值值均为 1 |

注册回调由 `ChromaWorldMixin` 在 `collectPerFrameGizmos()` 区间内调用。回调内可以直接提交上述图形；不得从二维叠加或网络线程调用。回调抛出运行时异常时记录错误并注销，避免每帧重复失败。调用方在停用或销毁时关闭注册句柄，避免持有失效对象。点尺寸和线宽遵循 Gizmos 的屏幕尺寸约定；圆、球体与广告牌尺寸使用世界单位。

`ChromaProjection` 为不持有 GPU 资源的不可变数学快照。构造参数为 `modelView, projection, cameraX, cameraY, cameraZ, width, height`，最后两个参数是正整数 GUI 尺寸。构造时复制矩阵；位置先以 double 减去相机坐标，再按 `projection × modelView` 变换。输入矩阵使用 OpenGL 齐次裁剪约定 `−w ≤ z ≤ w`，视图矩阵作用于相机相对坐标，不能重复包含世界相机平移。

| 接口 | 行为 |
| --- | --- |
| `ChromaProjection.project(double x, double y, double z): Point` | 返回 `x/y/depth/onScreen`；无效数值、相机背后或近裁剪面之前返回 `null` |
| `ChromaProjection.projectBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ): Bounds` | 返回 `minX/minY/maxX/maxY/depth/onScreen`；包围盒的边先裁剪至近、远平面，无深度范围交集时返回 `null` |
| `ChromaProjection.width()`、`height()` | 此快照的 GUI 尺寸 |

单点位于屏幕侧方或远裁剪面之外时保留坐标，`onScreen=false`。包围盒与窗口相交时将矩形边界裁至窗口内；完全位于屏幕侧方时保留投影矩形并返回 `onScreen=false`。`depth` 为点或包围盒中心沿相机前向轴的有符号距离，不是欧氏距离；跨越相机的盒体可能具有负中心深度和非空可见部分。`onScreen` 只描述点或投影矩形的窗口范围，不读取深度缓冲或判断方块遮挡。

`ChromaWorld.project()` 保留原返回类型及仅返回屏幕内点的调用方式，计算统一经过上述投影。每帧开始使旧捕获失效；窗口最小化、世界对象变化、GUI 尺寸不匹配时 `projectionFrame()` 返回 `null`。同帧批量投影先读取一次帧快照并复用；位置与包围盒必须使用相同插值，不能将旧快照当作当前帧。快照只占用普通 Java 内存，无需 `close()`。

在已有画布回调中，可直接使用已按当前帧插值的包围盒：

```java
var frame = ChromaWorld.projectionFrame();
if (frame != null) {
    var screen = frame.projection().projectBounds(
            bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    if (screen != null && screen.onScreen()) {
        ChromaDraw.outline(canvas, (float) screen.minX(), (float) screen.minY(),
                (float) (screen.maxX() - screen.minX()), (float) (screen.maxY() - screen.minY()),
                0f, 1f, 0xFFB3D8EF);
    }
}
```

`ChromaParticles` 提供调用方所属的有限粒子集合：

| 接口 | 行为与约束 |
| --- | --- |
| `ChromaParticles(int capacity)` | 容量为 1–4096；超出容量时淘汰最早粒子 |
| `emit(Vec3 position, Vec3 velocity, Spec): boolean` | 添加粒子；速度为世界单位/秒 |
| `Spec(lifetime, size, int color, ParticleShape, gravity, drag, angularVelocity, boolean throughWalls)` | 生命周期为秒，尺寸为世界单位，重力为世界单位/秒²，阻尼为每秒系数，角速度为弧度/秒 |
| `update(float seconds)` | 推进一次模拟，保存前后位置供插值；不在绘制时自动推进 |
| `render(Frame[, double maxDistance])` | 在已注册世界回调内绘制；默认距离上限为 128 世界单位 |
| `size()`、`capacity()` | 当前粒子数量与容量 |
| `clear()`、`close()` | 清空粒子；关闭后停止接受新粒子 |

粒子集合不自动注册回调或模拟时钟。调用方负责更新、注册和注销，并在世界切换时清空旧粒子。每帧绘制只插值位置与旋转，按寿命衰减透明度和尺寸，并剔除距离过远或近乎透明的粒子。

### 3.6 框架内部接口

下列方法虽然部分为 `public`，但不属于新功能的常规接入接口。

| 类 | 接口组 |
| --- | --- |
| `ChromaFrameRenderer` | `render()`、`close()` |
| `ChromaGraphics` | `beginFrame()`、`renderMainSubmitted()`、`renderSubmitted()` |
| `ChromaGraphics` | `renderOverlay(Consumer<Canvas>)`、`renderMainTarget(Consumer<Canvas>)` |
| `ChromaGraphics` | `close()` |

`renderMainTarget()` 在共享画布已活动时复用当前通道，不强制切换目标。常规组件不得创建新的 `DirectContext`。

### 3.7 组件效果与形变

以下工具使用当前共享 Canvas，不创建渲染后端，也不推进动画时钟。所有尺寸均属于调用方的当前坐标空间；用于背景采样的形变路径必须在屏幕 GUI 坐标中生成。

| 接口 | 行为与约束 |
| --- | --- |
| `ChromaShadow.drawRounded(Canvas, x, y, width, height, radius, strength, color)` | 绘制圆角轮廓外侧的均匀柔和阴影；`strength` 为 0–1，0 跳过绘制 |
| `ChromaShadow.drawPath(Canvas, Path, strength, color)` | 使用同一个填充路径生成外围阴影；不修改或关闭传入路径 |
| `ChromaTransition.draw(Canvas, Rect bounds, visibility, offsetY, Runnable drawing)` | 非稳态时在 `bounds` 内叠加渐显渐隐与等向模糊；`visibility` 为 0–1，`offsetY` 为可选的显式纵向位移 |
| `ChromaSeparation.create(x, y, width, height, radius, satelliteRadius, gap, progress): Geometry` | 构建主圆角面板及右侧圆球的共用填充路径；进度 0 仅含主面板，1 为具有指定间隙的分体 |
| 上述接口末尾增加 `boolean alignTop` | 默认重载使圆球垂直居中；`true` 将球心锚定在 `y + max(cornerRadius, satelliteRadius)`，主面板增高时分体保持靠上排列 |
| `ChromaSeparation.create(x, y, width, height, radius, satelliteRadius, gap, leftProgress, rightProgress, alignTop): Geometry` | 两侧圆球按独立进度分离，合并为同一个填充路径；原单进度重载等价于 `leftProgress=0` |
| `ChromaSeparation.Geometry.path(): Path`、`bounds(): Rect` | 返回当前形变的路径及紧致外框，可用于同一帧的填充、背景裁剪、阴影及输入范围 |
| `Geometry.satelliteCenterX()`、`satelliteCenterY()`、`satelliteRadius()` | 返回右侧圆球的当前几何；右侧进度 0 时可见半径为 0 |
| `Geometry.leftSatelliteCenterX()`、`leftSatelliteCenterY()`、`leftSatelliteRadius()` | 返回左侧圆球的当前几何；左侧进度 0 时可见半径为 0 |
| `Geometry.close()` | 释放所属 Path，可重复关闭；不得单独关闭其借出的 `path()` |
| `ChromaStatusIcon.draw(Canvas, success, x, y, size, progress, color)` | 按路径长度绘制成功对勾，或按错开的进度区间绘制失败叉号的两条线段，使用圆端点及圆转角 |

阴影应在面板内容和面板内部裁剪之前绘制。其强度同时影响颜色透明度与模糊宽度，颜色为 ARGB；阴影不会覆盖轮廓内部，也不产生包边。形变中应对完整路径调用一次阴影和一次填充，避免在黏附区域反复混合半透明颜色。

`ChromaTransition` 只在过渡期建立局部图层，并在退出调用前释放 Paint 和滤镜、恢复 Canvas。透明度与 `visibility` 成正比；模糊在两个方向使用相同强度，随显现减弱，不根据位移添加纵向拖影。`visibility` 接近 1 且位移为 0 时直接调用绘制函数；此时不建立额外裁剪，稳态内容的裁剪仍由调用方负责。传入的 `bounds` 是过渡期的硬裁剪范围，应包含显式位移所需的空间。不要用全屏矩形包装单行文字，不要从绘制函数再次提交队列。过渡期间保持字号固定。

`offsetY=0f` 表示在原位渐显渐隐并叠加模糊；非零值只对本次回调施加纵向位移。显隐、位移及其动画曲线由调用方提供，接口不管理内容切换或计时。

液体分离的主路径、两侧圆球和黏附桥采用相同绕向的非零填充，一次绘制即可得到合并轮廓；双侧模式不得分别调用两次填充、背景采样或阴影。桥在分离后段逐渐收细并断开。实际圆球半径限制在主面板高度的一半以内；右球心从面板内部移动到 `x + width + gap + satelliteRadius`，左球心移动到 `x - gap - satelliteRadius`。公式中的 `gap` 和 `satelliteRadius` 指经参数限制后的值：间隙不小于 0，圆球半径不超过面板高度的一半。上沿对齐使用经过尺寸限制后的圆角与圆球半径，零半径时不添加圆球或黏附桥。`bounds()` 包含两侧的实际扩展，布局和输入范围应读取同一份几何。`Geometry` 由创建它的组件拥有，在本帧用完或几何失效时关闭；相同参数下允许组件自行复用，框架没有全局几何或图像缓存。

动画进度由调用方每帧计算一次，并在同一帧的各次绘制中复用。`ChromaStatusIcon` 仅依据传入的进度绘制，不保存动画状态。

## 4. 坐标、动画与资源约定

### 4.1 坐标空间

常规 Canvas 已由框架缩放到 GUI 逻辑坐标。完整屏幕使用 `Screen.width/height`，HUD 使用 `Window.getGuiScaledWidth()/getGuiScaledHeight()`；不要再次乘以 GUI scale。物理帧缓冲尺寸由后端管理。

矩形参数为 `x, y, width, height`，不是右下角坐标。颜色统一使用 `0xAARRGGBB`。

布局应根据当前可用宽高计算。若使用设计坐标与局部缩放，鼠标必须应用同一变换的逆变换。例如局部变换为 `screenX = originX + localX * scale`，则 `localX = (mouseX - originX) / scale`。绘制、裁剪和命中测试必须使用同一帧的几何结果。

### 4.2 文字定位与复用

`text(..., top, height, ...)` 将文字按字体度量居中到行框，基线为：

`baseline = top + (height - (descent - ascent)) / 2 - ascent`

`height` 是行框高度，`size` 是字号。`textAtTop()` 和 `PreparedText.drawTop()` 的 top 是字体行框顶部，不是可见字形墨迹的最上沿。`PreparedText.draw()` 则直接接收基线。

`PreparedText` 保存 TextBlob 字形数据及宽度，避免每帧重复回退分段、宽度测量和字形构造。它不是位图缓存，也不自动执行换行、省略、复杂文字排版或字体版本更新。中文和缺失字形沿用现有回退逻辑；CJK 优先尝试系统回退字体，粗体请求不保证回退字形具有粗体效果。

缓存键至少包含文字、离散字号、粗体状态、字体版本，以及使用时的临时字体名。换行或截断结果还需包含可用宽度、换行与截断策略等布局条件。位置和颜色变化不需要重建 TextBlob。

完整文本是否能容纳，必须先按实际可用宽度判断；不能先将宽度向下取整作为缓存键，再误把刚好容纳的文本省略。只有确实超宽的文本才进入截断计算，缓存量化不得改变完整文本的显示结果。

### 4.3 动画与生命周期

每帧只推进一次动画状态，保存本帧位置、缩放、透明度和裁剪几何。绘制与输入方法只读取结果；禁止在多个 getter 中重复推进动画。异步数据加载更新内容，不应无条件重启页面进入动画。

Minecraft 返回既有 Screen 实例时，`added()` 会执行，但受保护的 `init()` 不保证再次执行。返回页面时的动画重置使用 `added()` 或显式的激活逻辑；尺寸相关布局需响应 resize/reposition 或按当前宽高计算。资源释放后的实例再次显示时，必须能按需重建资源。

背景模糊应在局部 `translate/scale` 之前绘制，传入屏幕 GUI 坐标。模糊区域、裁剪圆角和前景必须来自同一帧几何。旋转或更复杂变换需要单独设计屏幕空间采样，不能仅传入未经转换的局部矩形。

### 4.4 所有权

| 资源 | 责任 |
| --- | --- |
| 框架提供的 `Canvas` | 借用，不关闭，不跨帧保存，不传给工作线程 |
| `ChromaDraw` 内部 Font、Typeface、Paint | 共享资源；功能模块不关闭 |
| 功能创建的 `PreparedText`、Paint、Path、Image、Surface、滤镜等 | 功能持有并在失效、替换、关闭时释放 |
| `BorrowedImage` | 在活动 Skia 回调中借用并用 try-with-resources 关闭包装 |
| Minecraft 纹理 | 由 Minecraft 纹理系统管理，不由借用者销毁 |
| 框架上下文、Surface 缓存与背景纹理 | 由框架在重建或客户端关闭时管理 |

使用 `save()` 与 `restoreToCount()` 成对保护局部变换和裁剪，并使用 `try/finally` 保证恢复。只有确实需要整体透明度或滤镜时才使用 `saveLayer()`。不能把功能缓存提升为全局策略而不评估所有调用方。

## 5. 使用示例

以下示例可分别放入客户端源码集。示例展示接口组合与生命周期；页面注册、导航和输入派发仍由现有宿主负责。

### 5.1 自适应屏幕与可复用文字

在 `render()` 收集当前尺寸，在 `renderChroma()` 绘制。关闭时释放标题，返回同一实例后自动重建。该示例完全自行绘制背景，因此覆盖默认背景入口；混合原版控件的页面需要另行设计叠加顺序，避免覆盖原版控件。

```java
package com.pvp_utils.client.render.chroma.examples;

import com.pvp_utils.client.render.chroma.ChromaScreen;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChromaExampleScreen extends Screen implements ChromaScreen {
    private Frame frame;
    private ChromaDraw.PreparedText heading;
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
    public void renderChroma(Canvas canvas) {
        Frame current = frame;
        if (current == null) return;
        ChromaDraw.fill(canvas, 0f, 0f, current.screenWidth(), current.screenHeight(), 0xFF11151D);
        if (current.width() <= 0f || current.height() <= 0f) return;
        long revision = ChromaDraw.textFontRevision();
        if (heading == null || fontRevision != revision) {
            if (heading != null) heading.close();
            heading = ChromaDraw.prepareText("Chroma Graphics", 18f, true);
            fontRevision = revision;
        }
        ChromaDraw.rounded(canvas, current.x(), current.y(),
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
package com.pvp_utils.client.render.chroma.examples;

import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;

public final class ChromaHudLabel implements AutoCloseable {
    private ChromaDraw.PreparedText label;
    private String cachedValue;
    private long fontRevision = -1;

    public void submit(String value, float x, float y) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui || client.screen != null) return;
        LabelFrame frame = new LabelFrame(value == null ? "" : value, x, y);
        ChromaGraphics.submit(canvas -> draw(canvas, frame));
    }

    private void draw(Canvas canvas, LabelFrame frame) {
        long revision = ChromaDraw.textFontRevision();
        if (label == null || !frame.value().equals(cachedValue) || fontRevision != revision) {
            if (label != null) label.close();
            label = ChromaDraw.prepareText(frame.value(), 12f, false);
            cachedValue = frame.value();
            fontRevision = revision;
        }
        ChromaDraw.rounded(canvas, frame.x(), frame.y(), label.width() + 16f, 28f, 7f, 0xD9202835);
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
package com.pvp_utils.client.render.chroma.examples;

import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.RRect;

public final class ChromaBlurPanel {
    public static void draw(Canvas canvas, float centerX, float centerY,
                            float width, float height, float radius, float scale,
                            ChromaDraw.PreparedText label) {
        if (!Float.isFinite(scale) || scale <= 0f || width <= 0f || height <= 0f) return;
        float screenWidth = width * scale;
        float screenHeight = height * scale;
        float x = centerX - screenWidth * 0.5f;
        float y = centerY - screenHeight * 0.5f;
        RRect screenClip = RRect.makeXYWH(x, y, screenWidth, screenHeight, radius * scale);
        ChromaGraphics.drawBlurredBackdrop(canvas, screenClip, x, y, screenWidth, screenHeight, 12f);
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            canvas.clipRRect(RRect.makeXYWH(0f, 0f, width, height, radius), true);
            ChromaDraw.rounded(canvas, 0f, 0f, width, height, radius, 0xE6202835);
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
package com.pvp_utils.client.render.chroma.examples;

import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.resources.Identifier;

public final class ChromaTexturePainter {
    public static void draw(Canvas canvas, Identifier texture,
                            float x, float y, float width, float height) {
        if (width <= 0f || height <= 0f) return;
        try (ChromaGraphics.BorrowedImage borrowed = ChromaGraphics.borrowTexture(texture)) {
            if (borrowed == null) {
                ChromaDraw.rounded(canvas, x, y, width, height, 6f, 0xFF202835);
                return;
            }
            canvas.drawImageRect(borrowed.image(), Rect.makeXYWH(x, y, width, height));
        }
    }
}
```

### 5.5 共用形变轮廓与局部文字过渡

在已有 HUD 绘制入口的活动 Canvas 中调用，`x/y` 为屏幕 GUI 坐标，所有进度已由调用方更新。示例先绘制外围阴影，再采样当前背景并填充面板，最后使文字在原位渐显渐隐并叠加模糊。文字的所有权由调用方保留。

```java
package com.pvp_utils.client.render.chroma.examples;

import com.pvp_utils.client.render.chroma.effects.ChromaTransition;
import com.pvp_utils.client.render.chroma.effects.ChromaSeparation;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.effects.ChromaShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

public final class ChromaAnimatedCapsule {
    public static void draw(Canvas canvas, float x, float y, float leftSeparation, float rightSeparation,
                            float visibility, ChromaDraw.PreparedText label) {
        try (ChromaSeparation.Geometry shape = ChromaSeparation.create(
                x, y, 240f, 32f, 16f, 14f, 7f, leftSeparation, rightSeparation, true)) {
            Rect bounds = shape.bounds();
            ChromaShadow.drawPath(canvas, shape.path(), 0.45f, 0xB0000000);
            ChromaGraphics.drawBlurredBackdrop(canvas, shape.path(),
                    bounds.getLeft(), bounds.getTop(), bounds.getWidth(), bounds.getHeight(), 10f);
            ChromaDraw.fillPath(canvas, shape.path(), 0xD91B1F26);
            int save = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(x + 14f, y + 4f, 212f, 24f));
                ChromaTransition.draw(canvas, Rect.makeXYWH(x + 14f, y + 4f, 212f, 24f),
                        visibility, 0f,
                        () -> label.drawTop(canvas, x + 14f, y + 8f, 0xFFF3F6FC));
            } finally {
                canvas.restoreToCount(save);
            }
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
4. 优先使用填充、圆角和渐变；仅对必要区域使用模糊、阴影或发光。
5. 稳态页面直接绘制。仅在需要整体合成的过渡期创建有明确边界的图层；不以全屏 `saveLayer` 包裹所有界面。
6. 网络、文件读取和图片解码不放入绘制回调；后台处理普通数据，GPU 资源创建、更新和释放仍由正确的客户端渲染阶段完成。

### 6.2 字号与缓存边界

当前普通与回退 Font 缓存以精确 float 字号为键，没有条目数上限。连续把动画值作为字号传入，会不断建立 Font 对象。优先使用固定字号；确需变化时在功能边界限制范围并离散化，例如：

`size = Math.round(Math.clamp(requestedSize, 8f, 48f) * 4f) * 0.25f`

绘制、测量和缓存键必须使用这个相同结果。0.25 是示例精度，不是框架自动规则。仅限制 PreparedText 缓存数量不能阻止任意新字号进入共享 Font 缓存。

| 数据 | 推荐缓存策略 | 失效条件 |
| --- | --- | --- |
| 单行固定标签 | 单个 `PreparedText` | 内容、字号、字重、字体选择或版本改变 |
| 多行文本与列表 | 由调用方持有、限制数量的文本及排版缓存 | 内容版本、文字、布局宽度、排版策略或字体变化；淘汰时关闭资源 |
| 静态图片 | 功能所属图像缓存，限制像素总量 | 图片身份、目标尺寸或资源版本改变 |
| 静态图片的模糊结果 | 只在图像或效果参数变化时生成 | 图片、分辨率、模糊参数变化 |
| 实时世界背景 | 使用框架的当前通道快照 | 不能将旧画面长期当作当前世界 |

静态图像模糊缓存由调用方持有，工作分辨率按实际显示尺寸与所需细节确定。低分辨率效果缓存仅用于相应图像效果，不改变文字、图标或其他图像的绘制质量。

### 6.3 背景与特效成本

常规叠加通道在开始时读取已记录的模糊请求标记，通常来自上一帧；通道内才发出的首次请求可能没有快照。快照在需要的通道内刷新，不是已模糊画面的跨帧缓存。底层捕获保留整张帧缓冲图像，具体模糊绘制裁取所需区域；局部模糊不能消除整帧快照本身的成本。

不要为每个控件重新截图，不要将本组件已经绘制的内容再次采样为背景。给效果提供基础填充降级；共享渲染失败会记录错误并在当前会话禁用，背景模糊还有独立的失败禁用状态。先定位日志中的首次异常，不要用空 catch 隐藏绘制错误。

### 6.4 验证

检查客户端编译、不同 GUI 缩放与窗口尺寸的几何计算、首次进入和返回、字体切换后的失效、长期动画中的缓存数量、关闭后的本地资源释放。性能证据应区分 CPU 耗时、GPU 耗时、分配量及缓存命中情况；只凭一次 FPS 读数不能定位瓶颈。

当前 Minecraft 后端的源码验证应包含 `compileClientJava`。静态检查与编译结果不能替代运行环境中的视觉或性能验证。

## 7. AI 开发代理使用规范

使用 Chroma Graphics 前，必须阅读本节、相关接口章节及对应实现。

1. **核实接口与依赖。** 以当前实现及实际依赖为依据，核实类型、签名与调用行为。使用 Minecraft 后端时核对对应版本和映射的源码，不得猜测方法或虚构框架 API。
2. **确定绘制阶段。** 明确内容属于 Screen、加载叠加层、主目标 GUI、HUD 叠加还是世界 Gizmos，确认提交点在对应队列消费之前。复用现有入口，同一内容只接入一次。
3. **复用字体接口。** 常规文字使用 `ChromaDraw`；重复文字使用由调用方持有的 `PreparedText`。保持固定或离散字号，测量与绘制采用相同字体选择；缓存键包含字体版本及临时字体名。
4. **统一坐标与几何。** 每帧只推进一次动画。绘制、背景采样、裁剪和命中测试读取同一份结果；区分物理像素、GUI 坐标和局部坐标，并对输入应用相应逆变换。
5. **限定缓存与工作量。** 先剔除不可见内容，再处理可见布局。明确缓存键、容量、失效条件和释放时机；局部缓存策略仅适用于其所属内容。
6. **维护资源所有权。** 不跨帧保存或关闭借出的 Canvas。关闭自己创建的本地资源，将 GPU 操作留在正确的客户端渲染阶段；调用方不得关闭框架共享资源。
7. **遵守后端边界。** 常规绘制使用现有上下文、背景采样和字体接口。修改后端时，核实受影响通道的 GL 状态、目标帧缓冲、资源所有权及提交顺序。
8. **处理失效与重建。** 覆盖背景或纹理不可用、窗口尺寸变化、字体重载及资源释放后的重建。将异步数据更新与动画生命周期分别管理。
9. **验证实际行为。** 核实编译、坐标计算、绘制顺序、缓存失效和资源释放。性能测量区分 CPU、GPU 与分配成本，不将静态检查或编译结果表述为运行性能结论。
10. **同步文档。** 公开接口或行为变化时，同步更新中英文接口说明和受影响示例，确保名称、参数、坐标空间及所有权描述与实现一致。

变更说明应列明所用绘制阶段、缓存与资源生命周期、实际执行的验证及尚未验证的运行行为。

## 8. 实现索引

| 文件 | 职责 |
| --- | --- |
| [ChromaGraphics](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaGraphics.java) | 二维绘制提交、画布通道、纹理借用与背景模糊 |
| [ChromaDraw](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaDraw.java) | 图形、文字、图标、字体管理与预备文本 |
| [ChromaScreen](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaScreen.java) | 屏幕的活动 Canvas 绘制契约 |
| [ChromaOverlay](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaOverlay.java) | 加载叠加层的绘制策略与关闭契约 |
| [ChromaFrameRenderer](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaFrameRenderer.java) | Minecraft 后端的帧级通道调度 |
| [ChromaBackdrop](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaBackdrop.java) | 背景采样、帧缓冲包装与 GL 状态恢复；包内实现 |
| [ChromaDiagnostics](../../src/client/java/com/pvp_utils/client/render/chroma/ChromaDiagnostics.java) | 限频诊断、重复次数和恢复记录；包内实现 |
| [TextureManagerAccessor](../../src/client/java/com/pvp_utils/mixin/client/TextureManagerAccessor.java) | 只读查询已注册的宿主纹理，用于区分直接借用与冷加载 |
| [ChromaShadow](../../src/client/java/com/pvp_utils/client/render/chroma/effects/ChromaShadow.java) | 圆角与路径的外侧阴影 |
| [ChromaTransition](../../src/client/java/com/pvp_utils/client/render/chroma/effects/ChromaTransition.java) | 有界渐显渐隐与模糊合成 |
| [ChromaSeparation](../../src/client/java/com/pvp_utils/client/render/chroma/effects/ChromaSeparation.java) | 可分离轮廓的几何构建与资源所有权 |
| [ChromaStatusIcon](../../src/client/java/com/pvp_utils/client/render/chroma/effects/ChromaStatusIcon.java) | 对勾与叉号的路径绘制动画 |
| [ChromaWorld](../../src/client/java/com/pvp_utils/client/render/chroma/world/ChromaWorld.java) | 世界空间几何、深度策略、坐标投影与绘制注册 |
| [ChromaProjection](../../src/client/java/com/pvp_utils/client/render/chroma/world/ChromaProjection.java) | 不可变矩阵快照、单点及裁剪包围盒的投影数学 |
| [ChromaParticles](../../src/client/java/com/pvp_utils/client/render/chroma/world/ChromaParticles.java) | 有限粒子集合、模拟更新与插值绘制 |
| [ChromaWorldMixin](../../src/client/java/com/pvp_utils/mixin/client/ChromaWorldMixin.java) | 世界几何收集与相机矩阵接入 |
| [MinecraftMixin](../../src/client/java/com/pvp_utils/mixin/client/MinecraftMixin.java) | 帧开始、帧末绘制与叠加层生命周期接入 |
| [GameRendererMixin](../../src/client/java/com/pvp_utils/mixin/client/GameRendererMixin.java) | 主目标提交队列的消费接入 |
| [LoadingOverlayMixin](../../src/client/java/com/pvp_utils/mixin/client/LoadingOverlayMixin.java) | 原版加载叠加层的 ChromaOverlay 适配 |
| [构建定义](../../build.gradle) | 后端依赖与源码集配置 |
