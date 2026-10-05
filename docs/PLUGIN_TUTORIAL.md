# 从零开发自己的 PVPUtils 插件

本教程带你亲手创建插件。
接口参数、返回值和编写规范请查阅 [插件规范与 API 参考](PLUGINS.md)。

## 1. 准备

需要能运行 PVPUtils 的 Minecraft 客户端、文本编辑器和一点 JavaScript 基础。
开发 JS 插件不要求安装 Java 编译器、Gradle、Node.js 或 npm。
这里以自建插件 `my-status` 为例；该 ID 由你自己定义。

在 PVPUtils 的 ClickGUI 中进入“插件”，点击“打开目录”。
这会打开当前游戏目录中的 `PVPUtils/plugin`，不存在时自动创建。
```


## 2. 新建自己的目录与两个文件

在刚打开的目录内新建 `my-status` 文件夹，再创建：

```text
PVPUtils/plugin/my-status/
  plugin.json
  main.js
```

开启 Windows 的文件扩展名显示，确认实际名称不是 `plugin.json.txt` 或 `main.js.txt`。
两个文件都保存为 UTF-8。

在 `plugin.json` 写入：

```json
{
  "id": "my-status",
  "name": "我的状态面板",
  "version": "1.0.0",
  "description": "我自己编写的第一个插件",
  "author": "你的名字",
  "apiVersion": 1,
  "main": "main.js",
  "enabled": false
}
```

把作者换成你的名字，其他字段可以先照此填写。
ID 使用小写字母开头，只含小写字母、数字、横线和下划线。
`enabled: false` 表示首次发现时先保持关闭，方便检查后手动启用。

## 3. 第一次运行：只显示一条消息

先在 `main.js` 写入最小脚本：

```js
pvputils.events.on("load", () => {
    pvputils.log("我的第一个插件加载成功");
    pvputils.notify.show("我的状态面板已加载");
});
```

保存，返回插件页，点击“刷新列表”，找到“我的状态面板”并打开开关。
应当出现一条通知；日志中可搜索 `[plugin:my-status]`。

也可以在游戏聊天中输入默认前缀的命令：

```text
.plugins refresh
.plugins enable my-status
```

如果修改过 PVPUtils 的命令前缀，使用自己的前缀。
列表没有出现插件时先检查目录层级、扩展名与 JSON 格式，再查看第 8 节。

## 4. 添加设置与自己的 HUD

把 `main.js` 的内容完整替换为下面脚本。不是追加到上一个示例后面。
这份完整脚本可以直接运行：

```js
const visible = pvputils.settings.bool("显示面板", true);
const x = pvputils.settings.number("横向位置", 20, 0, 1000);
const y = pvputils.settings.number("纵向位置", 20, 0, 1000);
const size = pvputils.settings.number("文字大小", 14, 8, 32);
const title = pvputils.settings.text("标题", "玩家状态");
const color = pvputils.settings.color("文字颜色", "#FFFFFFFF");

pvputils.events.on("load", () => {
    const loads = pvputils.storage.get("loads", 0) + 1;
    pvputils.storage.set("loads", loads);
    pvputils.log(`状态面板加载完成，第 ${loads} 次`);
    pvputils.notify.show("我的状态面板已加载");
});

pvputils.events.on("unload", () => {
    pvputils.log("状态面板已卸载");
});

pvputils.hud.register({
    id: "status",
    render(ctx) {
        if (!visible.value) return;
        const player = pvputils.player.snapshot();
        if (!player) return;
        const text = `${title.value}：${Math.ceil(player.health)} / ${Math.ceil(player.maxHealth)} HP`;
        const width = ctx.textWidth(text, size.value) + 24;
        const height = size.value + 24;
        const drawX = Math.max(0, Math.min(x.value, ctx.width - width));
        const drawY = Math.max(0, Math.min(y.value, ctx.height - height));
        ctx.roundedRect(drawX, drawY, width, height, 8, "#C0202020");
        ctx.text(text, drawX + 12, drawY + 10, size.value, color.value);
    }
});
```

保存后点击该插件的“重载”，不必重启 Minecraft。
进入世界，确保没有隐藏 HUD，应看到血量面板。
插件卡片现在会显示位置、字号、标题和颜色设置；修改后即时保存。
再次重载，设置应保留，日志中的加载计数会增加。

这份脚本做了五件事：

1. 顶层声明设置，每个设置只注册一次，通过 `.value` 读取。
2. `load` 中读取并更新自己的持久化计数，不在每帧写文件。
3. 注册名为 `status` 的 HUD。
4. 每帧读取最新玩家状态；主菜单中返回 `null` 时直接结束。
5. 测量文字后画背景和文字，使用 GUI 单位与现有 Skija 渲染。

`#C0202020` 的前两位 `C0` 是透明度；`#FFFFFF` 是不透明白色。
GUI 位置不是屏幕物理像素，所以窗口缩放会改变可用坐标范围。

## 5. 学会使用玩家接口：一次切换快捷栏

继续在刚才脚本末尾添加：

```js
const switchOnce = pvputils.settings.bool("切换到第一个快捷栏", false);

pvputils.events.on("tick", () => {
    if (!switchOnce.value) return;
    const player = pvputils.player.get();
    if (!player) return;
    const state = player.snapshot();
    if (!state || state.inScreen) return;
    switchOnce.value = false;
    const result = player.inventory.select(0);
    pvputils.notify.show(result.ok ? "已选择第一个快捷栏" : `操作未完成：${result.code}`);
});
```

重载后进入世界，在插件卡片打开“切换到第一个快捷栏”，然后关闭设置界面。
脚本会在一个 Tick 中把开关恢复为关闭，再尝试选择快捷栏 0（界面中的第一个）。
窗口未激活等情况下会显示结果原因；需要时重新打开开关再试。

这里使用一次性设置开关，而不是每 Tick 不断切换槽位。
`player.get()` 返回绑定当前玩家的句柄；`snapshot()` 是实时状态读取。
重生或切换世界后重新获取句柄，不长期缓存旧玩家。
玩家修改不要放在 `render` 中；渲染只负责显示。

## 6. 学会查看背包与容器

需要统计材料时，可在某个一次性动作或低频 Tick 工作中使用：

```js
const player = pvputils.player.get();
if (player) {
    const items = player.inventory.getItems();
    const stoneCount = items ? items
        .filter(item => item.id === "minecraft:stone")
        .reduce((count, item) => count + item.count, 0) : 0;
    pvputils.log(`石头数量：${stoneCount}`);
}
```

读取当前菜单结构：

```js
const player = pvputils.player.get();
const menu = player ? player.inventory.getMenu() : null;
const state = menu ? menu.snapshot() : null;
if (state) {
    const occupied = state.slots.filter(item => !item.empty);
    pvputils.log(`当前菜单 ${state.containerId} 有 ${occupied.length} 个非空槽位`);
}
```

这些片段是按需参考，不必全部追加到自己的脚本里。
顶层运行只执行一次，游戏未进入世界时玩家为 `null`；
想在进入世界后读取，就放入自己的事件回调，并控制频率。

背包 `getSlot` 使用玩家 Inventory 编号，容器 `click` 使用当前菜单编号，两者不同。
真正操作容器之前，先用 `menu.snapshot()` 确认目标槽位，然后查阅 API 参考中
`menu.click(slot, button, type)` 的四种点击类型和返回码。
容器关闭或切换后获取新句柄；不要修改物品副本的 `count` 来尝试更改游戏数量。

## 7. 日常修改、保存与发布

| 改动 | 后续操作 |
|---|---|
| 修改 `main.js` 或显示说明 | 保存后点击该插件“重载” |
| 修改插件 ID、新建/删除插件目录 | “刷新列表” |
| 暂停运行 | 关闭插件开关 |
| 调整已注册设置 | 在插件卡片编辑，自动保存 |
| 查看错误 | 插件卡片错误信息和游戏目录 `logs/latest.log` |

设置与数据位于根目录 `.data/<ID>.json`，启用状态在 `.enabled.json`。
日常编写无需编辑这两个框架管理文件。
改变设置名会使用一个新的持久化 key；重载不会自动清空旧数据。

发布时只打包自己的插件目录，例如 `my-status/plugin.json` 与 `my-status/main.js`。
不要附带整个游戏目录、`.data`、`.enabled.json`、用户账号信息或私人配置。
说明适用 PVPUtils 版本、API 版本和插件用途。接收者将目录放进自己的插件根目录后刷新并启用。
一个插件压缩包应解压后直接得到含 `plugin.json` 的目录，避免多套一层父文件夹。

## 8. 常见排错

| 现象 | 检查方式 |
|---|---|
| 刷新后没有插件 | 路径是否来自“打开目录”；目录是否直接含 `plugin.json`；是否额外套了一层目录 |
| 出现 `invalid:<目录名>` | 检查 JSON、ID、重复 ID、API 版本和入口路径 |
| `pvputils is not defined` | 脚本是否由 PVPUtils 加载；普通浏览器/Node.js 中没有这个入口 |
| `pvp is not defined` | 全部调用改用 `pvputils` 前缀 |
| 插件启用后马上关闭 | 查卡片与日志的第一条错误，检查语法、参数和执行预算 |
| HUD 没出现 | 是否进入世界、插件与显示开关是否开启、是否隐藏 HUD、坐标是否在屏幕内 |
| 玩家为 `null` | 主菜单、连接中或玩家/世界切换时是正常情况，先判空 |
| `STALE_PLAYER` / `STALE_MENU` | 重新获取当前玩家/菜单句柄 |
| `SCREEN_OPEN` | 关闭界面后再进行常规玩家动作 |
| `INACTIVE` | 激活游戏窗口再试 |
| 动作出现 `PASS` / `FAIL` | 查询物品/目标是否适合当前交互；这不是脚本语法错误 |
| 本地成功但服务器纠正 | 操作结果不是服务器确认；检查游戏状态和服务器同步 |
| 参数 `TypeError` | 对照 API 参考核对类型、数量、数值范围与槽位编号 |
| 执行预算错误 | 移除无限循环、忙等待及高频重复工作；按 Tick 分批执行 |

建议调试顺序：先让 `load` 通知工作，再添加一个设置，再绘制固定文字，最后读取玩家并接入动作。
每次只改一小部分，重载后观察日志，避免同时排查多个错误。

## 9. 把自己的面板加入现有 HUD 拖动编辑

在第 4 节的 `hud.register` 中加入
`label: "我的状态面板"` 和
`layout: {x: 20, y: 20, width: 190, height: 48, draggable: true}`。
然后把 render 中的绘制位置改为从 `(0,0)` 开始，尺寸可使用 `ctx.width/height`。
不再从 x/y 设置读取位置；位置和缩放由编辑器保存。

重载插件，再打开原来的 HUD 编辑器。
你的插件 HUD 会和 Java 模组 HUD 一起出现，使用同样的拖动、吸附和滚轮缩放。
未声明 layout 的原脚本仍然按绝对坐标绘制，不会加入拖动列表。

## 10. 开发自己的 UI Screen

不必直接访问 Java Screen。使用 `pvputils.ui.screen({id, title, render})` 注册界面，
在 render 中调用 `ctx.gradient`、`ctx.outline`、`ctx.shadow`、`ctx.glass`、
`ctx.icon`、`ctx.texture` 等接口组成 UI。
它们复用项目原来的绘制实现，而不是另外一套渲染引擎。

按钮用 `ctx.button(id, label, x, y, width, height, onClick)`；
回调中处理设置或自己的状态，需要返回时调用 `pvputils.ui.close()`。
在游戏里输入 `.plugins screen my-status controls` 打开自己注册的 `controls` 界面。
完整可运行示例、输入回调和每个高级绘制方法的参数见 API 参考第 12 节。

新增界面后先运行 `.plugins check` 查语法，再重载。
按钮回调、帧绘制与资源效果还要进入实际游戏操作验证，语法检查 OK 不代表逻辑已经正确。
