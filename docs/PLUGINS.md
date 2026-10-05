# 插件编写规范与 API 参考

本文用于查阅插件格式、运行规则和全部公开接口。第一次开发请先阅读 [从零开发插件](PLUGIN_TUTORIAL.md)。
仅记录当前代码已实现的接口；新增玩家接口仍属于 API 1 的兼容扩展。

## 1. 插件格式与管理规范

插件根目录是当前游戏运行目录中的 `PVPUtils/plugin`，客户端启动时自动创建。
本项目开发运行目录为 `E:\JavaCode\其他项目\PVPUtils-1.21.11\run\PVPUtils\plugin`；
第三方启动器应以其实际游戏目录为准。

```text
PVPUtils/plugin/
  my-plugin/
    plugin.json
    main.js
  .data/
    my-plugin.json
  .enabled.json
```

每个插件使用独立子目录。扫描只检查根目录直接子目录，忽略名称以 `.` 开头的目录。
不要求目录名与 ID 一致，但建议一致。单次最多扫描 64 个非隐藏子目录，包括没有 manifest 的子目录。
插件由用户自行安装；框架不会安装默认示例。

### 1.1 plugin.json

使用 UTF-8、标准 JSON，没有注释或尾逗号。

```json
{
  "id": "my-plugin",
  "name": "我的插件",
  "version": "1.0.0",
  "description": "插件说明",
  "author": "你的名字",
  "apiVersion": 1,
  "main": "main.js",
  "enabled": false
}
```

| 字段 | 类型 | 必填 | 默认值与规则 |
|---|---|---|---|
| `id` | string | 是 | 匹配 `[a-z][a-z0-9_-]{0,63}`，全局唯一 |
| `name` | string | 否 | ID；用于列表显示 |
| `version` | string | 否 | `"1.0.0"`；当前按文本显示 |
| `description` | string | 否 | `""` |
| `author` | string | 否 | `""` |
| `apiVersion` | integer | 否 | `1`；当前仅接受 1，建议显式填写 |
| `main` | string | 否 | `"main.js"`；相对于插件自身目录 |
| `enabled` | boolean | 否 | `false`；只在没有已保存启用状态时作为默认值 |

文本字段最长 2048 字符，manifest 最大 64 KiB，入口脚本最大 1 MiB。
入口文件解析后的真实路径必须位于插件自身目录内，插件目录的真实路径也必须位于插件根目录内。
修改 ID 后使用刷新列表，而不是单插件重载。

### 1.2 管理操作

ClickGUI 的“插件”页包含开关、设置、单插件重载、错误信息、“打开目录”和“刷新列表”。
客户端命令使用配置的命令前缀，下面以默认 `.` 为例：

| 命令 | 行为 |
|---|---|
| `.plugins` | 列出已发现的插件、版本、启用状态和错误 |
| `.plugins check` | 检查所有插件的 manifest 和 JavaScript 语法，不启用、不执行插件 |
| `.plugins folder` | 创建并打开插件根目录 |
| `.plugins refresh` | 卸载全部实例，重新扫描并按保存状态加载 |
| `.plugins enable my-plugin` | 启用并保存启用状态 |
| `.plugins disable my-plugin` | 卸载并保存关闭状态 |
| `.plugins reload my-plugin` | 重读 manifest 与入口；原来启用的插件重新加载，原来关闭的仍关闭 |
| `.plugins screen my-plugin controls` | 打开已启用插件注册的 `controls` 自定义界面 |

框架异常会关闭当前实例并显示错误，但不会自动把其保存的启用状态改成关闭。
因此刷新或下一次启动可能再次尝试加载，需要修改脚本或手动关闭插件。

## 2. 执行与生命周期规范

- Rhino 1.8.1、ES6 模式、解释执行。使用普通 JS 入口脚本，不是浏览器、Node.js 或 ES module。
- 全局入口固定为 `pvputils`，没有 `pvp` 别名；没有 Node.js `require`、npm 加载器、浏览器 DOM、`console` 或定时器桥接。
- 每个插件拥有独立 JS scope；入口、事件、HUD 和玩家操作均在客户端线程执行。
- Java 包、原始 Minecraft 对象和反射不作为公开接口。游戏数据均转换成纯 JS 副本或受控句柄。
- 只安装可信插件。进程内接口边界与执行预算不等于操作系统隔离或内存硬限制。
- 将设置、事件和 HUD 注册放在入口顶层，避免每 Tick 重复注册。
- 使用事件执行小量增量工作；避免忙等待、大量持久化写入、反复扫描背包或逐帧发送日志。
- 离开世界、重生和切换维度后重新获取玩家句柄；容器切换后重新获取容器句柄。
- 绘制只在 render/HUD 回调内进行。玩家修改放在 `tick` 等非渲染回调内。
- 检查玩家是否存在和操作结果，区分参数错误、状态失效与服务器确认。

### 2.1 执行限额

| 项目 | 限额 |
|---|---|
| 入口执行与其 `load` 回调合计 | 200 万条计数指令 / 1 秒 |
| 每次事件派发或整轮插件渲染 | 10 万条计数指令 / 20 毫秒 |
| 指令预算检查间隔 | 1000 条计数指令 |
| 日志与通知 | 每次入口/事件/渲染合计 32 次 |
| 玩家修改 | 每次入口/事件合计 32 次，失败的尝试也计数 |
| 每种事件注册数 | 32 |
| 设置数 | 64 |
| HUD 数 | 16 |
| 每轮绘制与测量调用 | 2048 |
| 每轮阴影、背景模糊和液态玻璃合计 | 8 |
| 绘制状态栈深度 | 32 |
| 每插件自定义 Screen / 每 Screen 每帧按钮 | 8 / 128 |
| 每轮事件的界面切换请求 | 8 |
| 每插件持久化文件 | 1 MiB |

这些是解释器计数与耗时预算，不是对所有内建运算、原生调用或内存分配的硬隔离。
普通 API 参数错误会变成 JS `TypeError`；未处理的脚本错误及预算超限会卸载当前插件。

## 3. 全部接口索引

| 对象 | 公开成员 |
|---|---|
| `pvputils` | `apiVersion`、`id`、`log(message)` |
| `pvputils.notify` | `show(message)` |
| `pvputils.events` | `on(event, callback)` |
| `pvputils.player` | `snapshot()`、`get()` |
| 玩家句柄 | `snapshot()`、`target()`、`setRotation(yaw, pitch)`、`setVelocity(x, y, z)`、`setSprinting(enabled)`、`jump()`、`swing(hand?)`、`useItem(hand?)`、`stopUsingItem()`、`closeMenu()`、`attackTarget()`、`interactTarget(hand?)`、`inventory` |
| 玩家 `inventory` | `getItems()`、`getSlot(slot)`、`getSelectedSlot()`、`select(slot)`、`getMenu()` |
| 容器句柄 | `snapshot()`、`click(slot, button, type)` |
| `pvputils.settings` | `bool(name, defaultValue)`、`number(name, defaultValue, min?, max?)`、`text(name, defaultValue)`、`color(name, defaultValue)` |
| 设置句柄 | 可读写 `.value` |
| `pvputils.storage` | `get(key, fallback?)`、`set(key, value)` |
| `pvputils.hud` | `register({id, render})` |
| `pvputils.ui` | `screen(definition)`、`open(id)`、`close()` |
| 绘制上下文 | `width`、`height`、`screenWidth`、`screenHeight`、`mouseX`、`mouseY`、`text`、`textWidth`、`textShadow`、`rect`、`roundedRect`、`line`、`outline`、`gradient`、`gradientDiagonal`、`shadow`、`circle`、`icon`、`texture`、`blur`、`glass`、`save`、`restore`、`translate`、`scale`、`rotate`、`clip`、`clipRounded`、`button` |

## 4. 基础与通知

### pvputils.apiVersion / pvputils.id

只读属性，分别为数字 `1` 与当前 manifest 的插件 ID。

### pvputils.log(message)

参数会转成字符串，最长 8192 字符。返回 `undefined`。
日志带有 `[plugin:<ID>]` 标识，可在游戏目录 `logs/latest.log` 中查找。

### pvputils.notify.show(message)

参数会转成字符串，最长 8192 字符。返回 `undefined`。
通过 PVPUtils 现有通知组件显示消息，与 `log` 共用发送限额。

```js
pvputils.log(`插件 ${pvputils.id} 使用 API ${pvputils.apiVersion}`);
pvputils.notify.show("插件已加载");
```

## 5. 事件

### pvputils.events.on(event, callback)

`event` 为下表中的名称，`callback` 必须是函数。返回 `undefined`。
同一事件按注册顺序执行；一次派发开始后新增的同类回调留到下一次派发。
当前没有 `off` 或返回的取消订阅函数，卸载时统一清理回调。

| 事件 | 回调参数 | 触发时机 |
|---|---|---|
| `load` | 无 | 入口执行完成后 |
| `unload` | 无 | 禁用、重载、刷新、错误卸载或退出时 |
| `tick` | 无 | 客户端 Tick 末尾；主菜单也可能触发 |
| `join` | 无 | 当前客户端世界对象从空变为非空，或切换到另一世界对象 |
| `leave` | 无 | 当前世界对象离开或被另一世界对象替换 |
| `render` | `ctx` | 现有 Skija HUD 帧中；先于当前插件注册的 HUD 执行 |

`join` / `leave` 是世界对象变化通知，不是精确的网络连接事件。
切换世界时先派发 `leave` 再派发 `join`；`leave` 时游戏可能已经指向新世界。
在世界内新启用插件时没有补发 `join`，可以在 `load` 或 `tick` 查询玩家。
渲染仅发生在世界存在、HUD 可见、窗口未最小化且没有覆盖式 overlay 的帧中。

```js
pvputils.events.on("load", () => pvputils.log("加载完成"));
pvputils.events.on("tick", () => {
    const player = pvputils.player.snapshot();
    if (!player) return;
});
pvputils.events.on("unload", () => pvputils.log("卸载完成"));
```

## 6. 玩家与动作接口

### 6.1 pvputils.player.snapshot()

返回当前玩家状态副本；玩家或世界不存在时返回 `null`。
与句柄的 `snapshot()` 使用同一数据结构，不必先获取句柄即可读取。

| 字段 | 类型 | 含义 |
|---|---|---|
| `id` | number | 当前客户端实体 ID |
| `uuid` / `name` | string | UUID 与玩家名 |
| `health` / `maxHealth` / `absorption` | number | 血量、最大血量、吸收生命值 |
| `armor` | number | 当前护甲值 |
| `food` / `saturation` | number | 饥饿值与饱和度 |
| `x` / `y` / `z` | number | 世界坐标 |
| `yaw` / `pitch` | number | 视角角度，单位为度 |
| `velocity` | `{x, y, z}` | 当前本地运动向量 |
| `onGround` / `inWater` | boolean | 是否着地、处于水中 |
| `sprinting` / `sneaking` | boolean | 疾跑与潜行状态 |
| `alive` / `usingItem` / `spectator` | boolean | 存活、使用物品与旁观者状态 |
| `selectedSlot` | number | 当前快捷栏槽位，0–8 |
| `inScreen` | boolean | 当前是否打开一个 Screen |

修改副本不会修改游戏。字段来自本地客户端状态，不代表插件拥有服务器状态写入权限。

### 6.2 pvputils.player.get()

返回受控玩家句柄，或在没有玩家/世界时返回 `null`。
句柄绑定获取时的玩家与世界；每次调用读取实时状态，但不会自动切换成另一个玩家。
重生、切换维度或离开世界后，旧句柄的读取返回 `null`，修改返回 `STALE_PLAYER`。

```js
pvputils.events.on("tick", () => {
    const player = pvputils.player.get();
    if (!player) return;
    const state = player.snapshot();
    if (!state || state.inScreen) return;
});
```

### 6.3 player.snapshot()

无参数。返回 6.1 中的当前状态副本；句柄失效时返回 `null`。
适合一次读取多项字段，避免对同一 Tick 重复读取。

### 6.4 player.target()

无参数。返回当前准星命中信息副本；未命中或玩家句柄失效时返回 `null`。
共同字段为 `type`、命中位置 `x/y/z`：

| `type` | 额外字段 |
|---|---|
| `"entity"` | `id`、`name` |
| `"block"` | 整数 `blockX/blockY/blockZ`、大写方向 `face`，例如 `"UP"` |

这是当前客户端准星数据，不是一次新的射线计算。
`attackTarget` / `interactTarget` 执行时再次读取当前准星，而不是操作之前保存的副本。

### 6.5 修改方法的返回值与调用规则

所有修改方法，包括 `inventory.select` 和 `menu.click`，均返回
`{ok: true, code: "OK"}` 或 `{ok: false, code: "原因"}`。
`ok: true` 表示本地执行或发起操作成功，不是服务器确认回执。

| code | 含义 |
|---|---|
| `OK` | 本地操作执行，或交互返回消耗动作的结果 |
| `STALE_PLAYER` | 玩家/世界消失，或句柄绑定的实例已改变 |
| `INACTIVE` | 没有 gameMode，或窗口未激活 |
| `PLAYER_UNAVAILABLE` | 玩家死亡或处于旁观者模式 |
| `SCREEN_OPEN` | 打开 Screen 时调用了常规动作；选快捷栏和容器点击除外 |
| `NOT_ON_GROUND` | 跳跃时没有着地 |
| `NOT_USING_ITEM` | 停止使用物品时没有正在使用的物品 |
| `NO_ENTITY_TARGET` | 攻击时准星没有实体目标 |
| `NO_TARGET` | 交互时准星没有可用实体/方块目标 |
| `INVALID_TARGET` | 实体来自另一世界，或交互目标位于世界边界外 |
| `OUT_OF_REACH` | 目标不在当前玩家对应交互范围内，或目标不适合当前操作 |
| `STALE_MENU` | 容器句柄已经失效 |
| `PASS` | 物品/目标交互未消耗动作 |
| `FAIL` | 物品/目标交互明确失败 |

有效玩家上的参数格式或范围错误抛出 `TypeError`，不是返回上表结果。
修改操作在 render/HUD 回调中抛出错误；单轮最多 32 次修改尝试。
所有数值参数要求有限 number，不接受数字字符串、`NaN` 或 `Infinity`。

### 6.6 全部玩家修改方法

| 方法 | 参数与行为 |
|---|---|
| `player.setRotation(yaw, pitch)` | 两个 number；yaw 绝对值最多 360000，按 360 取余；pitch 在 −90 到 90；设置本地玩家视角 |
| `player.setVelocity(x, y, z)` | 三个 number，各分量 −10 到 10；设置本地运动向量，单位沿用 Minecraft 每 Tick 的运动量 |
| `player.setSprinting(enabled)` | 一个 boolean；设置当前疾跑标志 |
| `player.jump()` | 无参数；仅着地时调用原版跳跃逻辑 |
| `player.swing(hand?)` | `"main"` 或 `"off"`，默认 `"main"`；播放对应手挥动 |
| `player.useItem(hand?)` | 同上；通过原版 gameMode 使用该手物品，不先尝试准星交互 |
| `player.stopUsingItem()` | 无参数；通过原版 gameMode 释放当前使用物品 |
| `player.closeMenu()` | 无参数；关闭当前非默认容器界面并发送原版关闭容器操作；没有箱子等容器界面时返回 `NO_CONTAINER_SCREEN` |
| `player.attackTarget()` | 无参数；攻击当前准星实体，核对世界、存活与原版攻击范围，挥动主手 |
| `player.interactTarget(hand?)` | 手参数同上；交互当前实体或方块；实体先尝试命中点交互再普通交互，方块调用原版 useItemOn |

`useItem` 和 `interactTarget` 在原版要求客户端挥手时执行挥手。
这两个方法只处理指定的一只手，不自动尝试另一只手或完整模拟一次右键流程。
运动向量和疾跑状态是本地状态修改，后续原版输入、现有模块或服务器同步可能覆盖它们。
服务器仍决定移动、背包和交互的最终结果，`setVelocity` 不是传送接口。

```js
const player = pvputils.player.get();
if (player) {
    const state = player.snapshot();
    if (state && !state.inScreen) {
        const result = player.setRotation(state.yaw + 15, state.pitch);
        if (!result.ok) pvputils.log(result.code);
    }
}
```

## 7. 背包与容器接口

### 7.1 物品副本格式

| 字段 | 类型 | 含义 |
|---|---|---|
| `id` | string | 注册 ID，例如 `"minecraft:stone"` |
| `name` | string | 本地显示名 |
| `count` | number | 数量，空槽为 0 |
| `empty` | boolean | 是否为空 |
| `damage` / `maxDamage` | number | 当前损耗与最大耐久损耗；没有耐久的物品通常为 0 |
| `slot` | number | 所属接口的槽位编号，见下文 |

容器槽位还包含 `inventorySlot`，表示该 Slot 在其底层 Container 中的编号。
该 Container 可能是箱子或玩家背包，所以 `inventorySlot` 不是全局玩家背包编号。
容器的 `carried` 是鼠标拿着的物品，不包含 `slot`。
物品都是数据副本，修改 `count` 等字段没有游戏效果。

### 7.2 player.inventory.getItems()

无参数。返回当前玩家 Inventory 全部槽位的物品数组，包含空槽；旧玩家句柄返回 `null`。
槽位 `slot` 是玩家 Inventory 索引：0–8 为快捷栏，9–35 为主背包，其余为当前版本的装备映射槽位。
数组长度以运行时为准，不应写死为旧版本的槽位数量。

```js
const player = pvputils.player.get();
if (player) {
    const items = player.inventory.getItems();
    const occupied = items ? items.filter(item => !item.empty) : [];
    pvputils.log(`非空槽位：${occupied.length}`);
}
```

### 7.3 player.inventory.getSlot(slot)

参数为整数玩家 Inventory 索引，范围为 `0` 到 `getItems().length - 1`。
返回单个物品副本，包含 `slot`；旧玩家句柄返回 `null`，有效玩家上的非法索引抛出错误。

### 7.4 player.inventory.getSelectedSlot()

无参数。返回当前快捷栏槽位整数 0–8，旧玩家句柄返回 `null`。

### 7.5 player.inventory.select(slot)

参数为整数 0–8。切换当前快捷栏选择，返回 6.5 中的操作结果。
可以在打开 Screen 时调用；选择通过原版后续同步流程生效，不代表服务器已经确认。

### 7.6 player.inventory.getMenu()

无参数。返回当前 `containerMenu` 的受控句柄，或 `null`。
即使没有打开箱子，也可能返回玩家默认背包菜单。
绑定当前菜单实例；菜单更换后旧句柄读取返回 `null`，点击返回 `STALE_MENU`。

### 7.7 menu.snapshot()

无参数。返回当前容器数据副本，或在玩家/菜单句柄失效时返回 `null`：

```text
{
  token: string,
  containerId: number,
  slots: ItemSnapshot[],
  carried: ItemSnapshot
}
```

`token` 是不透明的菜单实例标识；容器句柄已自动捕获它，插件无需解析或传回。
`slots[].slot` 为当前菜单 Slot 的列表索引，和玩家 Inventory 编号是两套坐标。
`containerId` 本身可能被复用，框架还会核对菜单实例令牌来阻止旧菜单操作。

### 7.8 menu.click(slot, button, type)

通过原版 `handleInventoryMouseClick` 进行容器操作，返回 6.5 中的结果。
`slot` 必须是 `menu.snapshot().slots` 中的整数索引。
外部点击编号 `-999`、拖拽合成协议与创造复制不在此接口中。

| type | button | 行为 |
|---|---|---|
| `"pickup"` | 0 或 1 | 左键/右键拿取或放置 |
| `"quick_move"` | 0 或 1 | Shift 快速移动；通常使用 0 |
| `"swap"` | 0–8 | 与对应快捷栏槽位交换 |
| `"throw"` | 0 或 1 | 丢弃一件/整叠 |

避免在循环里无间隔批量点击；服务器与原版菜单会校验操作并同步最终结果。
读取后可能发生内容同步变化，令牌保证的是实例未替换，不是物品内容保持不变。

```js
const player = pvputils.player.get();
const menu = player ? player.inventory.getMenu() : null;
const state = menu ? menu.snapshot() : null;
if (state) {
    pvputils.log(`菜单 ${state.containerId}，槽位 ${state.slots.length}`);
}
```

## 8. 设置接口

设置注册函数返回一个只包含公开 `.value` 读写接口的句柄。
设置在启用插件的卡片内显示，GUI 修改或脚本赋值均立即持久化。
名称要求非空白、最长 96 字符、插件内唯一；最多 64 个设置。
重新启用时已保存的值优先于默认值；更名相当于新设置。

| 方法 | 参数与默认范围 | `.value` 类型及校验 |
|---|---|---|
| `bool(name, defaultValue)` | 名称、默认值 | boolean；按 JS 真值转换 |
| `number(name, defaultValue, min?, max?)` | 省略 min/max 时分别为 0/100；均有限且 min < max | number；输入转成数字、要求有限，夹到范围内 |
| `text(name, defaultValue)` | 名称、默认值 | string；转成字符串，最长 512 字符 |
| `color(name, defaultValue)` | 名称、默认值 | string；`#RRGGBB` 或 `#AARRGGBB`，当前 GUI 使用文本输入 |

```js
const visible = pvputils.settings.bool("显示", true);
const size = pvputils.settings.number("字号", 14, 8, 32);
const title = pvputils.settings.text("标题", "玩家状态");
const color = pvputils.settings.color("文字颜色", "#FFFFFFFF");
size.value = 16;
pvputils.log(title.value);
```

## 9. 存储接口

### pvputils.storage.get(key, fallback?)

读取持久化 JSON 值，返回纯 JS 副本。缺失 key 时返回 fallback，省略则返回 `null`。
已保存为 `null` 的值仍返回 `null`，不会触发 fallback。

### pvputils.storage.set(key, value)

返回 `undefined`。key 必须非空白、最长 128 字符；value 必须具有有效 `JSON.stringify` 结果。
循环对象、顶层 `undefined` 或函数等会抛出错误；数组/对象内部值按 JSON 规则转换。
设置内部使用 `setting:<名称>`，自定义数据应避开 `setting:` 前缀。

文件位于 `plugin/.data/<ID>.json`，总大小最大 1 MiB（包含设置）。
写入采用同目录临时文件替换，优先原子移动；不存在通用文件/目录访问 API。

```js
const count = pvputils.storage.get("loads", 0);
pvputils.storage.set("loads", count + 1);
pvputils.storage.set("preferences", {compact: true, labels: ["血量", "护甲"]});
```

## 10. HUD 与全部绘制接口

### pvputils.hud.register(definition)

definition 为 `{id: string, render: function(ctx)}`，返回 `undefined`。
ID 格式与插件 ID 一致，插件内唯一，最多 16 个 HUD。
按注册顺序渲染，回调的 `this` 为传入的 definition；没有动态取消注册接口。
也可以直接使用 `events.on("render", ctx => ...)`，两者共享绘制上下文及限额。

### ctx.width / ctx.height

只读 number：当前 GUI 坐标宽高，窗口缩放后自动更新。
所有坐标采用 Minecraft GUI 单位，而非屏幕物理像素。

### 绘制函数

| 方法 | 参数与返回值 |
|---|---|
| `ctx.text(text, x, y, size, color)` | 文字、位置、字号、颜色；返回 `undefined`；y 表示字体行框顶部 |
| `ctx.textWidth(text, size)` | 文字、字号；返回 number，使用与绘制一致的 fallback 字体测量宽度 |
| `ctx.rect(x, y, width, height, color)` | 填充矩形；返回 `undefined` |
| `ctx.roundedRect(x, y, width, height, radius, color)` | 填充圆角矩形；返回 `undefined` |
| `ctx.line(x1, y1, x2, y2, thickness, color)` | 线段；返回 `undefined` |

颜色为 `#RRGGBB` 或 `#AARRGGBB`；6 位颜色不透明，8 位颜色的前两位是 alpha。
字号范围 1–128；其他数值必须有限，绝对值最多 100000。
矩形宽高、圆角半径和线宽应使用非负值；越界尺寸不是自动布局接口。
文字最多 8192 字符。每轮最多 2048 次绘制或测量调用。

使用 PVPUtils 已有 Skija 帧与字体 fallback，不创建额外 GPU context/framebuffer。
框架保存并恢复每个插件的 Canvas 状态。不要把 ctx 留到 Tick 或卸载回调中使用。
HUD 可通过 `layout` 接入现有拖动编辑器；纹理、液态玻璃与直接绘制接口见第 12 节。

```js
pvputils.hud.register({
    id: "status",
    render(ctx) {
        const player = pvputils.player.snapshot();
        if (!player) return;
        const text = `${player.name}  ${Math.ceil(player.health)} HP`;
        const width = ctx.textWidth(text, 14) + 24;
        ctx.roundedRect(20, 20, width, 38, 8, "#C0202020");
        ctx.text(text, 32, 30, 14, "#FFFFFF");
    }
});
```

## 11. 当前范围与验证

本阶段已开放玩家状态、受控玩家动作、准星交互、背包读取、快捷栏选择和容器点击。
HTTP、异步任务、动态命令注册、全局键鼠事件、持续按键接管、任意世界实体枚举、
现有模块开关和完整 ClickGUI 组件树尚未实现。自定义 Screen 有独立输入回调。
不要把 Java/Minecraft 方法名直接当成 JS 接口。

开发者验证命令：

```powershell
.\gradlew.bat compileJava compileClientJava pluginSmokeTest build --no-daemon
```

先按项目 `AGENTS.md` 设置进程临时目录。
冒烟测试验证 JS 回调、受控句柄与调用转发、玩家/世界/容器令牌失效、
参数范围、渲染期修改限制、操作预算、卸载清理、HUD、设置与存储。
原版移动效果、容器同步与其他模组交互仍应在实际客户端中测试。

## 12. 高级 UI：直接复用项目绘制接口

这一层不是固定模板：JS 直接调用已有 `SkijaUi`、`SkijaRenderer` 与
`LiquidGlassRenderer` 的绘制桥接，在同一个客户端 Skija 帧中组合自己的 UI。
原始 Canvas、GPU 对象和 Minecraft Screen 对象仍留在 Java 端。
所有以下绘制方法仅在 render 回调中有效，返回 `undefined`，除非另行说明。

### 12.1 图形、文字、图标与纹理

| 方法 | 参数与效果 |
|---|---|
| `ctx.outline(x,y,w,h,radius,thickness,color)` | 项目圆角描边 |
| `ctx.gradient(x,y,w,h,startColor,endColor,vertical?,radius?)` | 项目双色线性渐变；默认水平、半径 0 |
| `ctx.gradientDiagonal(x,y,w,h,startColor,endColor,radius?)` | 项目对角渐变 |
| `ctx.shadow(x,y,w,h,radius,offsetY,blur,color)` | 项目圆角投影；模糊参数范围 0–64 |
| `ctx.circle(x,y,radius,color)` | 抗锯齿填充圆 |
| `ctx.text(text,x,y,size,color,font?)` | 与原有文字接口兼容，可选项目字体名 |
| `ctx.textShadow(text,x,y,size,color,font?)` | 项目带阴影 fallback 文字 |
| `ctx.textWidth(text,size,font?)` | 返回 number；可选择与绘制一致的字体 |
| `ctx.icon(glyph,x,y,size,color,set?)` | 项目图标字体；set 为 `"material"`（默认）或 `"pvp"` |
| `ctx.texture(resource,x,y,w,h)` | 项目已加载 Minecraft 纹理；resource 是资源 ID 字符串 |

字体名使用项目已有字体名称，例如 `"harmony"` 或用户在客户端导入的字体名。
未指定字体时沿用当前客户端字体。图标使用实际字符，例如 `"\ue87b"`。
纹理使用资源 ID，例如 `"minecraft:textures/gui/title/minecraft.png"`，不是磁盘路径。
纹理由 Minecraft 纹理管理器拥有；借用的 Skija 包装在绘制后释放，插件无需关闭资源。
图形尺寸范围 0–4096，文字/图标字号 1–128，坐标须有限且绝对值不超过 100000。

### 12.2 状态栈、裁剪和坐标变换

| 方法 | 行为 |
|---|---|
| `ctx.save()` / `ctx.restore()` | 保存/恢复 Canvas 状态；最多 32 层，额外 restore 抛出错误 |
| `ctx.translate(x,y)` | 平移 |
| `ctx.scale(x,y)` | 缩放 |
| `ctx.rotate(degrees)` | 旋转，单位度 |
| `ctx.clip(x,y,w,h)` | 矩形裁剪 |
| `ctx.clipRounded(x,y,w,h,radius)` | 圆角裁剪 |

每个 render 事件、每个 HUD 和每个 Screen 的绘制状态分别隔离；
未配对的 save 在回调结束时清理，但仍建议显式配对。
裁剪通常包在 save/restore 内，不影响其他插件。

```js
pvputils.events.on("render", ctx => {
    ctx.save();
    ctx.translate(20, 20);
    ctx.clipRounded(0, 0, 180, 48, 10);
    ctx.gradient(0, 0, 180, 48, "#304080", "#182038", false, 10);
    ctx.textShadow("直接使用项目绘制接口", 12, 14, 12, "#FFFFFF");
    ctx.restore();
});
```

### 12.3 背景模糊与液态玻璃

| 方法 | 行为 |
|---|---|
| `ctx.blur(x,y,w,h,radius,strength)` | 复用项目 framebuffer 背景模糊；strength 0–64 |
| `ctx.glass(x,y,w,h,radius,tint?,shadow?,highlight?)` | 复用原有液态玻璃；默认项目 tint、开启投影和高光 |

液态玻璃的折射、色散与模糊参数沿用客户端现有配置。
若液态玻璃渲染失败，则绘制同色圆角背景；模糊快照首帧可能还未准备好。
这些效果采样屏幕背景，仅支持回调的基础坐标；
在调用 save、手动变换或裁剪后使用它们会抛出错误。
对于带 `layout` 的 HUD，框架会自动换算其位置与缩放。
同一插件每帧阴影、背景模糊、液态玻璃合计最多 8 次，避免无界滤镜成本。
图形效果的真实视觉效果仍需在游戏内检验。

### 12.4 HUD 布局与现有拖动编辑器

`hud.register` 增加可选 `label` 和 `layout`：

```js
pvputils.hud.register({
    id: "glass-status",
    label: "我的玻璃 HUD",
    layout: { x: 20, y: 20, width: 190, height: 48, draggable: true },
    render(ctx) {
        ctx.glass(0, 0, ctx.width, ctx.height, 10, "#40182030");
        const state = pvputils.player.snapshot();
        ctx.text(state ? state.name : "等待玩家", 12, 16, 14, "#FFFFFF");
    }
});
```

- `layout.x/y` 为默认 GUI 位置；`width/height` 为逻辑尺寸，范围 1–4096。
- 声明 layout 后，绘制自动平移到 HUD 位置，并应用编辑器缩放。脚本从 `(0,0)` 开始绘制。
- 此时 `ctx.width/height` 为 HUD 逻辑尺寸，`ctx.screenWidth/screenHeight` 为窗口 GUI 尺寸。
- `draggable` 默认 true。false 时仍使用布局绘制，但不出现在 HUD 拖动编辑器中。
- 直接进入已有 HUD 编辑器，复用原有拖动、吸附、边界限制与滚轮缩放（0.5–2），没有另建拖动系统。
- 位置/缩放保存在该插件 `.data/<ID>.json` 的 `hud:<HUD-ID>:` 前缀下，鼠标松开、滚轮调整或卸载时保存。
- 多插件可用相同 HUD ID，编辑器以“插件 ID + HUD ID”区分。
- 未声明 layout 的旧 HUD 保留绝对坐标行为，不加入拖动编辑器。

### 12.5 自定义 Screen 与交互控件

`pvputils.ui.screen(definition)` 注册 Screen（返回 undefined），每插件最多 8 个。
definition 必须包含唯一 `id` 与 `render(ctx)`，可选 `title`、`onOpen()`、`onClose()`：

```js
let count = 0;
pvputils.ui.screen({
    id: "controls",
    title: "我的控制界面",
    render(ctx) {
        const panelX = (ctx.width - 240) / 2;
        const panelY = (ctx.height - 160) / 2;
        ctx.shadow(panelX, panelY, 240, 160, 14, 4, 16, "#80000000");
        ctx.gradient(panelX, panelY, 240, 160, "#25304D", "#111827", true, 14);
        ctx.text("计数：" + count, panelX + 20, panelY + 20, 16, "#FFFFFF");
        ctx.button("increment", "点击加一", panelX + 20, panelY + 60, 200, 30, () => count++);
        ctx.button("back", "返回", panelX + 20, panelY + 104, 200, 30, () => pvputils.ui.close());
    }
});
```

打开方式：

- 在非渲染事件/按钮回调中调用 `pvputils.ui.open("controls")`。
- 游戏命令 `.plugins screen <插件ID> controls`。
- `pvputils.ui.close()` 仅关闭当前插件自己的 Screen，返回之前的界面。

界面切换在下一次客户端 Tick 开头应用；render 中禁止切换界面。
Esc 返回上一个界面；界面不暂停单人世界；卸载、重载或运行出错时自动清理本插件界面。
打开插件 Screen 时会先正常关闭原版容器，避免保留一个失去 UI 的箱子菜单。

`ctx.button(id,label,x,y,width,height,onClick,enabled?)`：

- 复用项目圆角与 fallback 文字绘制按钮、悬停和禁用状态。
- 每 Screen 每帧最多 128 个，id 格式与插件 ID 一致且当帧唯一。
- 默认 enabled=true，左键点击执行 onClick；禁用按钮不触发回调。
- 只在自定义 Screen 中具有交互能力，HUD 普通渲染不注册按钮。
- 使用 GUI 绝对坐标；应在手动变换和裁剪前声明按钮，以保证绘制与命中区域一致。
- 每帧重新定义按钮，旧命中区域在重绘及打开/关闭界面时清理。
- 点击回调不在渲染阶段，可以修改设置、保存数据、通知或请求界面切换。
- 原有玩家操作的 `SCREEN_OPEN` 规则不变；需要游戏动作时先关闭 Screen，再在 tick 执行。

`ctx.mouseX/mouseY` 为当前鼠标 GUI 坐标，可自己绘制任意悬停、布局和控件。
自定义 Screen 还可声明以下输入回调，返回 true 表示消费事件：

| 回调 | 参数 |
|---|---|
| `onMouseDown(x,y,button)` | 未被内置按钮消费的鼠标按下 |
| `onMouseUp(x,y,button)` | 鼠标释放 |
| `onDrag(x,y,button,dx,dy)` | Screen 收到的鼠标拖动 |
| `onScroll(x,y,horizontal,vertical)` | 滚轮 |
| `onKey(key,scancode,modifiers)` | 非 Esc 按键，键码沿用 GLFW |

这些输入仅针对当前打开的自定义 Screen，不是全局输入接管。
需要复杂控件时可以用直接绘制与这些输入回调自行组合，不必修改 Java 模组。
