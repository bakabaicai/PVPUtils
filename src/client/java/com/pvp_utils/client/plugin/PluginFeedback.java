package com.pvp_utils.client.plugin;

import com.pvp_utils.client.gui.clickgui.UiText;

public final class PluginFeedback {
    private PluginFeedback() {}

    public static String message(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        String translated = switch (value) {
            case "Approval queue limit" -> UiText.t("审批队列已满", "Approval queue limit");
            case "At most 8 filtered effects per frame" -> UiText.t("每帧最多使用 8 个滤镜效果", "At most 8 filtered effects per frame");
            case "Backdrop effects require base coordinates" -> UiText.t("背景效果必须使用基础坐标", "Backdrop effects require base coordinates");
            case "Click type must be pickup, quick_move, swap or throw" -> UiText.t("点击类型必须为 pickup、quick_move、swap 或 throw", "Click type must be pickup, quick_move, swap or throw");
            case "Color must be #RRGGBB or #AARRGGBB" -> UiText.t("颜色格式必须为 #RRGGBB 或 #AARRGGBB", "Color must be #RRGGBB or #AARRGGBB");
            case "Cumulative scale exceeds limit" -> UiText.t("累计缩放超出限制", "Cumulative scale exceeds limit");
            case "Cyclic storage value" -> UiText.t("存储值存在循环引用", "Cyclic storage value");
            case "Drawing argument exceeds limit" -> UiText.t("绘制参数超出限制", "Drawing argument exceeds limit");
            case "Drawing is only available during render" -> UiText.t("只能在渲染回调中绘制", "Drawing is only available during render");
            case "Drawing stack limit" -> UiText.t("绘制状态栈超出限制", "Drawing stack limit");
            case "Effect radius limit is 64" -> UiText.t("效果半径不得超过 64", "Effect radius limit is 64");
            case "Event callback must be a function" -> UiText.t("事件回调必须为函数", "Event callback must be a function");
            case "Expected boolean argument" -> UiText.t("参数必须为布尔值", "Expected boolean argument");
            case "Expected numeric argument" -> UiText.t("参数必须为数值", "Expected numeric argument");
            case "Expected zero or one hand argument" -> UiText.t("手部参数最多为一个", "Expected zero or one hand argument");
            case "Failed to save HUD layouts" -> UiText.t("HUD 布局保存失败", "Failed to save HUD layouts");
            case "Failed to save plugin setting" -> UiText.t("插件设置保存失败", "Failed to save plugin setting");
            case "HUD label exceeds 96 characters" -> UiText.t("HUD 名称不得超过 96 个字符", "HUD label exceeds 96 characters");
            case "HUD requires a render function" -> UiText.t("HUD 必须提供渲染函数", "HUD requires a render function");
            case "Hand must be main or off" -> UiText.t("手部参数必须为 main 或 off", "Hand must be main or off");
            case "Interactive buttons require an untransformed custom screen" -> UiText.t("交互按钮必须位于未变换的自定义界面中", "Interactive buttons require an untransformed custom screen");
            case "Invalid HUD layout" -> UiText.t("HUD 布局无效", "Invalid HUD layout");
            case "Invalid HUD position" -> UiText.t("HUD 位置无效", "Invalid HUD position");
            case "Invalid approval report" -> UiText.t("审批报告无效", "Invalid approval report");
            case "Invalid button" -> UiText.t("按钮无效", "Invalid button");
            case "Invalid button bounds" -> UiText.t("按钮边界无效", "Invalid button bounds");
            case "Invalid font size" -> UiText.t("字号无效", "Invalid font size");
            case "Invalid icon size" -> UiText.t("图标尺寸无效", "Invalid icon size");
            case "Invalid layout number" -> UiText.t("布局数值无效", "Invalid layout number");
            case "Invalid menu arguments" -> UiText.t("容器参数无效", "Invalid menu arguments");
            case "Invalid number" -> UiText.t("数值无效", "Invalid number");
            case "Invalid or duplicate HUD id" -> UiText.t("HUD ID 无效或重复", "Invalid or duplicate HUD id");
            case "Invalid or duplicate button id" -> UiText.t("按钮 ID 无效或重复", "Invalid or duplicate button id");
            case "Invalid plugin entry path or size" -> UiText.t("插件入口路径或文件大小无效", "Invalid plugin entry path or size");
            case "Invalid plugin manifest path or size" -> UiText.t("插件清单路径或文件大小无效", "Invalid plugin manifest path or size");
            case "Invalid plugin source" -> UiText.t("插件源码无效", "Invalid plugin source");
            case "Invalid plugin state file" -> UiText.t("插件状态文件无效", "Invalid plugin state file");
            case "Invalid scale" -> UiText.t("缩放值无效", "Invalid scale");
            case "Invalid setting arguments or too many settings" -> UiText.t("设置参数无效或设置项过多", "Invalid setting arguments or too many settings");
            case "Invalid setting color" -> UiText.t("设置颜色无效", "Invalid setting color");
            case "Invalid setting name" -> UiText.t("设置名称无效", "Invalid setting name");
            case "Invalid setting range" -> UiText.t("设置范围无效", "Invalid setting range");
            case "Invalid size" -> UiText.t("尺寸无效", "Invalid size");
            case "Invalid slot or button" -> UiText.t("槽位或按钮编号无效", "Invalid slot or button");
            case "Invalid storage key" -> UiText.t("存储键无效", "Invalid storage key");
            case "Invalid, duplicate or excessive screen id" -> UiText.t("界面 ID 无效、重复或数量超限", "Invalid, duplicate or excessive screen id");
            case "Missing argument" -> UiText.t("缺少参数", "Missing argument");
            case "Missing backdrop bounds" -> UiText.t("缺少背景边界", "Missing backdrop bounds");
            case "Missing drawing argument" -> UiText.t("缺少绘制参数", "Missing drawing argument");
            case "Missing numeric argument" -> UiText.t("缺少数值参数", "Missing numeric argument");
            case "Missing storage value" -> UiText.t("缺少存储值", "Missing storage value");
            case "No active HUD frame" -> UiText.t("当前没有活动的 HUD 帧", "No active HUD frame");
            case "Player actions are not available during render" -> UiText.t("不能在渲染回调中操作玩家", "Player actions are not available during render");
            case "Player bridge called from another thread" -> UiText.t("玩家接口被其他线程调用", "Player bridge called from another thread");
            case "Player bridge is not available" -> UiText.t("玩家接口不可用", "Player bridge is not available");
            case "Player operations are suspended during confirmation" -> UiText.t("确认期间暂停玩家操作", "Player operations are suspended during confirmation");
            case "Plugin already started" -> UiText.t("插件已经启动", "Plugin already started");
            case "Plugin called from another thread" -> UiText.t("插件被其他线程调用", "Plugin called from another thread");
            case "Plugin directory outside root" -> UiText.t("插件目录超出允许的根目录", "Plugin directory outside root");
            case "Plugin entry outside directory" -> UiText.t("插件入口超出插件目录", "Plugin entry outside directory");
            case "Plugin execution budget exceeded" -> UiText.t("插件执行预算超出限制", "Plugin execution budget exceeded");
            case "Plugin file exceeds size limit" -> UiText.t("插件文件大小超出限制", "Plugin file exceeds size limit");
            case "Plugin id changed; refresh the plugin list" -> UiText.t("插件 ID 已改变，请刷新插件列表", "Plugin id changed; refresh the plugin list");
            case "Plugin is closed" -> UiText.t("插件已关闭", "Plugin is closed");
            case "Plugin is no longer active" -> UiText.t("插件已停用", "Plugin is no longer active");
            case "Plugin rate limit exceeded" -> UiText.t("插件调用频率超出限制", "Plugin rate limit exceeded");
            case "Plugin screen switching limit exceeded" -> UiText.t("插件界面切换频率超出限制", "Plugin screen switching limit exceeded");
            case "Plugin stack limit exceeded" -> UiText.t("插件调用栈超出限制", "Plugin stack limit exceeded");
            case "Plugin state is a symbolic link" -> UiText.t("插件状态文件为符号链接", "Plugin state is a symbolic link");
            case "Plugin storage exceeds limit" -> UiText.t("插件存储大小超出限制", "Plugin storage exceeds limit");
            case "Plugin storage is a symbolic link" -> UiText.t("插件存储文件为符号链接", "Plugin storage is a symbolic link");
            case "Plugin storage nesting exceeds limit" -> UiText.t("插件存储嵌套深度超出限制", "Plugin storage nesting exceeds limit");
            case "Plugin storage outside root" -> UiText.t("插件存储超出允许的根目录", "Plugin storage outside root");
            case "Plugin storage structure exceeds limit" -> UiText.t("插件存储结构超出限制", "Plugin storage structure exceeds limit");
            case "Screen changes require a non-render callback" -> UiText.t("界面切换必须在非渲染回调中执行", "Screen changes require a non-render callback");
            case "Screen requires a render callback" -> UiText.t("界面必须提供渲染回调", "Screen requires a render callback");
            case "Setting must be finite" -> UiText.t("设置值必须为有限数值", "Setting must be finite");
            case "Setting text exceeds limit" -> UiText.t("设置文本长度超出限制", "Setting text exceeds limit");
            case "Storage accepts only JSON values" -> UiText.t("存储仅接受 JSON 值", "Storage accepts only JSON values");
            case "Storage array exceeds limit" -> UiText.t("存储数组大小超出限制", "Storage array exceeds limit");
            case "Storage keys exceed limit" -> UiText.t("存储键数量超出限制", "Storage keys exceed limit");
            case "Storage object exceeds limit" -> UiText.t("存储对象大小超出限制", "Storage object exceeds limit");
            case "Storage structure exceeds limit" -> UiText.t("存储结构超出限制", "Storage structure exceeds limit");
            case "Storage text exceeds limit" -> UiText.t("存储文本长度超出限制", "Storage text exceeds limit");
            case "Text exceeds limit" -> UiText.t("文本长度超出限制", "Text exceeds limit");
            case "Too many HUDs" -> UiText.t("HUD 数量超出限制", "Too many HUDs");
            case "Too many buttons" -> UiText.t("按钮数量超出限制", "Too many buttons");
            case "Too many draw calls" -> UiText.t("绘制调用次数超出限制", "Too many draw calls");
            case "Too many event callbacks" -> UiText.t("事件回调数量超出限制", "Too many event callbacks");
            case "Too many storage writes" -> UiText.t("存储写入次数超出限制", "Too many storage writes");
            case "Transformed drawing bounds exceed limit" -> UiText.t("变换后的绘制边界超出限制", "Transformed drawing bounds exceed limit");
            case "Transformed radius exceeds limit" -> UiText.t("变换后的半径超出限制", "Transformed radius exceeds limit");
            case "Unbalanced restore" -> UiText.t("绘制状态恢复次数不匹配", "Unbalanced restore");
            case "Unknown draw operation" -> UiText.t("未知绘制操作", "Unknown draw operation");
            case "Unknown icon set" -> UiText.t("未知图标集", "Unknown icon set");
            case "Unknown plugin screen" -> UiText.t("插件未启用或指定界面不存在", "Unknown plugin screen");
            case "Unknown screen" -> UiText.t("未知界面", "Unknown screen");
            case "Unknown setting type" -> UiText.t("未知设置类型", "Unknown setting type");
            case "Unsupported plugin API version" -> UiText.t("不支持此插件 API 版本", "Unsupported plugin API version");
            case "click requires slot, button and type" -> UiText.t("click 需要槽位、按钮和类型参数", "click requires slot, button and type");
            case "setSprinting requires a boolean" -> UiText.t("setSprinting 需要布尔值参数", "setSprinting requires a boolean");
            default -> null;
        };
        if (translated != null) return translated;
        if (value.startsWith("Duplicate plugin id: ")) {
            return UiText.t("插件 ID 重复：", "Duplicate plugin id: ") + value.substring(21);
        }
        if (value.startsWith("Duplicate setting: ")) {
            return UiText.t("设置项重复：", "Duplicate setting: ") + value.substring(19);
        }
        if (value.startsWith("Invalid plugin id: ")) {
            return UiText.t("插件 ID 无效：", "Invalid plugin id: ") + value.substring(19);
        }
        if (value.startsWith("Manifest value too long: ")) {
            return UiText.t("清单字段过长：", "Manifest value too long: ") + value.substring(25);
        }
        if (value.startsWith("Unknown event: ")) {
            return UiText.t("未知事件：", "Unknown event: ") + value.substring(15);
        }
        if (value.startsWith("Unknown player operation: ")) {
            return UiText.t("未知玩家操作：", "Unknown player operation: ") + value.substring(26);
        }
        if (value.startsWith("Expected ") && value.endsWith(" arguments")) {
            String count = value.substring(9, value.length() - 10);
            if (count.matches("\\d+")) return UiText.t("所需参数数量：", "Expected arguments: ") + count;
        }
        int suffix = value.lastIndexOf(" (");
        if (suffix > 0) {
            String detail = value.substring(0, suffix);
            String localized = message(detail);
            if (!detail.equals(localized)) return localized + value.substring(suffix);
        }
        int separator = value.indexOf(": ");
        if (separator > 0) {
            String prefix = value.substring(0, separator);
            if (prefix.equals("JavaException") || prefix.startsWith("java.") || prefix.startsWith("Wrapped ")) {
                return value.substring(0, separator + 2) + message(value.substring(separator + 2));
            }
        }
        return value;
    }
}
