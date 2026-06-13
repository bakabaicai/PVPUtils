# PVPUtils

PVPUtils 是一个面向 Minecraft 高版本 PVP 的 Fabric 客户端辅助模组。它专注于手感、HUD 信息展示和一些实用的小工具，不修改服务端逻辑，也不追求“全自动化”。

当前版本：`1.2`  
目标版本：`Minecraft 1.21.11`  
默认设置键：`Right Shift`

> 使用前请确认目标服务器规则。部分服务器可能禁止客户端 HUD、自动疾跑、快捷存入或类似功能。

## 预览

### Skija 自绘 ClickGUI

![ClickGUI 预览](docs/media/clickgui-zh.gif)

### 1.8 风格防砍动画

![防砍动画预览](docs/media/block-animation.gif)

### HUD 拖动编辑

![HUD 编辑预览](docs/media/hud-editor.gif)

### 一些实用的小工具（当前展示的是 Keystrokes 与 Digging Status）

![功能演示](docs/media/features.gif)

## 重点功能

### Skija ClickGUI

PVPUtils 使用 Skija 自绘了一套设置界面，而不是直接堆原版按钮。界面支持分页、子页、滚动、缩放适配、打开/关闭动画和双语显示。功能按 Combat、Render、Tool、Optimize、Misc 分类，便于后续继续扩展。

### 防砍动画

还原高版本中缺失的旧版格挡视觉反馈，支持多种动画模式和偏移、速度等细节调节。它只影响第一人称视觉表现，不改变实际攻击、格挡或服务端判定。

### 可拖动 HUD 编辑器

HUD 编辑器可以直接拖动以下元素：

- Target HUD
- Keystrokes
- Notification

拖动时会显示辅助线和吸附参考，方便对齐常用 HUD 元素。

## 功能列表

下面列出的是当前版本已经提供的功能，后续还会继续加入更多适合高版本 PVP 的实用功能。

### Combat

- Hit Marker：命中目标时在准星附近显示命中反馈。
- Hit Sound：命中目标时播放提示音，可选择音色和触发条件。

### Render

- UI Editor：打开 HUD 位置编辑器。
- Sword Blocking Animation：旧版防砍动画视觉效果。
- Auto Block：在合适距离内自动触发防砍视觉动作。
- Use Animation：启用物品使用动画。
- Digging Status：在准星下方显示当前挖掘进度和预计剩余时间，例如 `99%(1s)`。
- Sneak Animation Adjustment：调整潜行视角下降效果。
- Gamma Override：自定义亮度上限。
- Render Control：选择性隐藏告示牌文本、附魔台悬浮书、火焰遮挡和受伤抖动。
- Low Health Warning：低血量时显示通知提醒。
- Target HUD：显示当前目标信息。
- Keystrokes：显示 WASD、鼠标按键和 CPS。

### Tool

- Auto Screenshot：胜利时自动截图并保存至桌面。
- Fall Damage Prediction：预测摔落伤害，不会造成伤害时不显示。
- Auto Sprint：前进时自动进入疾跑输入状态。
- Quick Deposit：手持物品左键点击容器时快速存入，可限制为起床战争资源。

### Optimize

- IME Fix In Game：修复中文、日文、韩文输入法在游戏中导致无法操作的问题。

### Misc

- Victory Sound：胜利时播放自定义音效，并可直接打开音效文件夹。
- Language Switch：切换中文或英文，关闭并重新打开界面后生效。

## 安装要求

| 组件 | 要求 |
| :--- | :--- |
| Minecraft | `1.21.11` |
| Fabric Loader | `>= 0.18.4` |
| Fabric API | `0.141.3+1.21.11` 或兼容版本 |
| Java | `21+` |

## 使用说明

1. 将构建好的 jar 放入 `.minecraft/mods`。
2. 启动游戏后按 `Right Shift` 打开 PVPUtils 设置界面。
3. 在 `Render -> UI Editor` 中进入 HUD 编辑模式。
4. 右键带子项的功能卡片可以展开更多设置。

## 自定义音效

胜利音效目录：

```text
.minecraft/versions/<当前版本目录>/PVPUtils/sounds
```

也可以在 `Misc -> Victory Sound` 右键展开子项，点击“打开”直接打开该目录。

支持的文件类型：

- `.wav`
- `.mp3`

内置音效会在首次运行时自动复制到该目录。你可以把自己的音效文件放进去，模组会从目录中随机选择播放，并尽量避免连续播放同一个文件。

## 构建

Windows:

```powershell
.\gradlew.bat build
```

构建产物位于：

```text
build/libs/
```

## 说明

- 本项目是客户端模组，不提供任何服务端权限能力。
- 自动疾跑只是设置客户端输入中的疾跑状态，仍受原版移动规则限制。
- Target HUD 依赖服务器下发的可见生命值信息，部分服务器可能不可用。
- Quick Deposit 会在自动存入期间临时屏蔽移动输入并锁定视角，以减少误触和异常操作。
- IME Fix 只在需要规避输入法影响的游戏场景中工作。

## 第三方内容

Sneak Animation Adjustment 参考了 NoSneakAnim 的思路和部分实现细节，详见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。

## License

本项目基于 GPLv3，并包含额外的非商业和署名条款。完整内容以 [LICENSE](./LICENSE) 为准。

---

## English

PVPUtils is a Fabric client-side utility mod for Minecraft 1.21.11 PVP. It focuses on visual feedback, HUD editing, practical tools, and a custom Skija-based settings UI.

Current version: `1.2`  
Target version: `Minecraft 1.21.11`  
Default settings key: `Right Shift`

> Check the rules of the server you play on before using it. Some servers may disallow client-side HUDs, auto sprint, quick deposit, or similar utilities.

## Preview

### Skija ClickGUI

![ClickGUI Preview](docs/media/clickgui-en.gif)

### 1.8-Style Sword Blocking Animation

![Blocking Animation Preview](docs/media/block-animation.gif)

### Drag-Editable HUD

![HUD Editor Preview](docs/media/hud-editor.gif)

### Practical Utilities (currently showing Keystrokes and Digging Status)

![Feature Preview](docs/media/features.gif)

## Highlights

### Skija ClickGUI

PVPUtils uses a custom Skija-rendered settings interface instead of relying on vanilla buttons. It supports pages, sub-options, scrolling, UI scaling, open/close animation, and bilingual text. Features are grouped into Combat, Render, Tool, Optimize, and Misc pages.

### Sword Blocking Animation

Restores the old blocking feel as a first-person visual effect. Multiple animation modes and fine-tuning options are available. It does not change actual attacks, blocking behavior, or server-side combat logic.

### Drag-Editable HUD

The HUD editor lets you drag these elements directly:

- Target HUD
- Keystrokes
- Notification

Alignment guides are shown while editing, making it easier to place HUD elements cleanly.

## Feature List

The features below are available in the current version. More practical high-version PVP utilities will be added over time.

### Combat

- Hit Marker: shows hit feedback near the crosshair when you hit a target.
- Hit Sound: plays a sound when you hit a target, with configurable sound type and trigger condition.

### Render

- UI Editor: opens the HUD position editor.
- Sword Blocking Animation: old-style sword blocking visual effect.
- Auto Block: automatically triggers the blocking visual action at a suitable range.
- Use Animation: enables item use animation.
- Digging Status: shows current block-breaking progress and estimated remaining time under the crosshair, for example `99%(1s)`.
- Sneak Animation Adjustment: adjusts the camera drop effect while sneaking.
- Gamma Override: applies a custom brightness value.
- Render Control: selectively hides sign text, enchanting table book, fire overlay, and hurt shake.
- Low Health Warning: shows a notification when health is low.
- Target HUD: displays target information.
- Keystrokes: displays WASD, mouse buttons, and CPS.

### Tool

- Auto Screenshot: automatically takes a screenshot when you win and saves it to the desktop.
- Fall Damage Prediction: predicts fall damage and hides the text when no damage would be taken.
- Auto Sprint: sets the sprint input state while moving forward.
- Quick Deposit: quickly deposits the held item when left-clicking a container, with an optional BedWars resource-only filter.

### Optimize

- IME Fix In Game: fixes Chinese, Japanese, and Korean input methods causing controls to stop working in game.

### Misc

- Victory Sound: plays a custom sound when you win and provides a button to open the sound folder.
- Language Switch: switches between Chinese and English after closing and reopening the UI.

## Requirements

| Component | Requirement |
| :--- | :--- |
| Minecraft | `1.21.11` |
| Fabric Loader | `>= 0.18.4` |
| Fabric API | `0.141.3+1.21.11` or compatible |
| Java | `21+` |

## Usage

1. Put the built jar into `.minecraft/mods`.
2. Launch the game and press `Right Shift` to open the PVPUtils settings screen.
3. Open `Render -> UI Editor` to edit HUD positions.
4. Right-click feature cards with sub-options to expand more settings.

## Custom Victory Sounds

Victory sound folder:

```text
.minecraft/versions/<current version folder>/PVPUtils/sounds
```

You can also right-click `Misc -> Victory Sound` and click `Open` to open the folder directly.

Supported file types:

- `.wav`
- `.mp3`

Built-in sounds are copied to the folder on first launch. You can put your own files there, and the mod will randomly choose one while trying to avoid playing the same file twice in a row.

## Build

```powershell
.\gradlew.bat build
```

Build output:

```text
build/libs/
```

## Notes

- This is a client-side mod and does not provide server-side permissions or capabilities.
- Auto Sprint only sets the client sprint input state and is still limited by vanilla movement rules.
- Target HUD depends on visible health data sent by the server, so it may not work on every server.
- Quick Deposit temporarily blocks movement input and locks rotation while depositing to reduce accidental or abnormal operations.
- IME Fix only works in situations where input method behavior may affect gameplay controls.

## Third-Party Notices

Sneak Animation Adjustment is based on ideas and implementation details from NoSneakAnim. See [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).

## License

This project is based on GPLv3 with additional non-commercial and attribution terms. See [LICENSE](./LICENSE) for the full text.
