package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.PseudoCreativeManager;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicManager;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import com.pvp_utils.client.modules.impl.Tool.ServerAutoLoginManager;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

public class ToolPage extends BasePage {

    public ToolPage() {
        modules.add(ModuleBindingIds.preserve(ToolPage.class, 0, new SettingModule(UiText.t("自动截图", "Auto Screenshot"), UiText.t("获胜时截图并保存至桌面", "Save a screenshot to the desktop after winning"),
                new SettingToggle(() -> Config.autoScreenshot, v -> { Config.autoScreenshot = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 1, new SettingModule(UiText.t("自动GG", "Auto GG"), UiText.t("在获胜后自动发送文字", "Automatically send text after winning"),
                new SettingToggle(() -> Config.autoGG, v -> { Config.autoGG = v; Config.save(); }))
                .addSub(UiText.t("发送文本", "Message"), "",
                        new SettingTextBox(() -> Config.autoGGText, value -> { Config.autoGGText = value; Config.save(); }, 256))
                .addSub(UiText.t("发送延迟(tick)", "Send Delay (tick)"), "",
                        new SettingSlider(0, 100, "%.0f", () -> (double) Config.autoGGDelayTicks,
                                v -> { Config.autoGGDelayTicks = Math.max(0, Math.min(100, v.intValue())); Config.save(); }))));

        SettingModule autoLogin = new SettingModule(UiText.t("自动登录", "Auto Login"), UiText.t("进入已配置的服务器时自动执行登录命令", "Automatically send the login command on configured servers."),
                new SettingToggle(() -> Config.serverAutoLogin, v -> { Config.serverAutoLogin = v; Config.save(); }))
                .addSub(UiText.t("触发方式", "Trigger Mode"), UiText.t("进服延迟：进入服务器后定时发送；检测提示：服务器提示登录时才发送", "Join delay: send after joining; Chat detection: send when the server asks for login"),
                        new SettingCycle(List.of(UiText.t("进服延迟", "Join Delay"), UiText.t("检测提示", "Chat Detection")),
                                () -> Config.serverAutoLoginMode,
                                i -> { Config.serverAutoLoginMode = Math.max(0, Math.min(1, i)); Config.save(); }));
        for (Map.Entry<String, ServerAutoLoginManager.Rule> entry : ServerAutoLoginManager.rules().entrySet()) {
            if (!entry.getValue().enabled) {
                continue;
            }
            String address = entry.getKey();
            autoLogin.addSubGroup(address, UiText.t("登录延迟 " + ServerAutoLoginManager.delayOf(address) + " 秒", "Login delay " + ServerAutoLoginManager.delayOf(address) + "s"));
            autoLogin.addSubChild(UiText.t("登录密码", "Login Password"), UiText.t("输入后立即加密保存，已保存的密码不会显示", "Saved with encryption immediately; the stored password is never shown"),
                    new SettingPasswordBox(
                            () -> ServerAutoLoginManager.hasPassword(address) ? "********" : "",
                            v -> ServerAutoLoginManager.setPassword(address, v), 64));
            autoLogin.addSubChild(UiText.t("登录延迟", "Login Delay"), UiText.t("进入服务器后等待多久发送登录命令(0-30秒)，检测提示模式下不可用", "How long to wait after joining before sending the login command (0-30s); unavailable in chat detection mode"),
                    new SettingSlider(0.0, 30.0, "%.0fs", () -> (double) ServerAutoLoginManager.delayOf(address),
                            v -> ServerAutoLoginManager.setDelay(address, v.intValue()))
                            .setEnabledSupplier(() -> Config.serverAutoLoginMode == 0));
            autoLogin.addSubChild(UiText.t("移除此服务器", "Remove Server"), "",
                    new SettingButton(UiText.t("移除", "Remove"), () -> {
                        if (!ServerAutoLoginManager.removeRule(address)) {
                            return;
                        }
                        if (Minecraft.getInstance().screen instanceof NewSettingsScreen settings) {
                            settings.rebuildCurrentPage();
                        }
                    }));
        }
        autoLogin.addSub(UiText.t("添加服务器", "Add Server"), "",
                new SettingButton(UiText.t("添加", "Add"), () -> {
                    if (Minecraft.getInstance().screen instanceof NewSettingsScreen settings) {
                        settings.showAddServerPage();
                    }
                }));
        modules.add(ModuleBindingIds.preserve(ToolPage.class, 2, autoLogin));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 4, new SettingModule(UiText.t("网易云音乐", "Netease Music"), UiText.t("打开网易云播放器", "Open the NetEase Cloud player"),
                new SettingButton(UiText.t("打开", "Open"), NeteaseMusicManager::open))
                .keybindAction(ModuleKeybindManager.ACTION_OPEN_MUSIC)));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 5, new SettingModule(UiText.t("摔落伤害预测", "Fall Damage Prediction"), UiText.t("预测摔落伤害数值", "Predict fall damage value"),
                new SettingToggle(() -> Config.fallDamagePredict, v -> { Config.fallDamagePredict = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 14 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("自由视角", "Freelook"), UiText.t("开启功能后，使用按键进行触发（默认Mouse 5），可以自由观看四周而不影响原本视角朝向。", "Use a keybind to activate freelook (default Mouse 5), allowing you to look around freely without changing your original view direction."),
                new SettingToggle(() -> Config.freelook, v -> { Config.freelook = v; Config.save(); }))
                .keybindAction(ModuleKeybindManager.ACTION_FREELOOK)
                .addSub(UiText.t("触发模式", "Trigger Mode"), "",
                        new SettingCycle(List.of(
                                UiText.t("长按", "Hold"),
                                UiText.t("切换", "Toggle")),
                                () -> Config.freelookTriggerMode == Config.FreelookTriggerMode.HOLD ? 0 : 1,
                                i -> { Config.freelookTriggerMode = i == 0 ? Config.FreelookTriggerMode.HOLD : Config.FreelookTriggerMode.TOGGLE; Config.save(); }))
                .addSub(UiText.t("灵敏度", "Sensitivity"), "",
                        new SettingSlider(1.0, 100.0, "%.0f%%", () -> (double) Config.freelookSensitivity,
                                v -> { Config.freelookSensitivity = Math.max(1, Math.min(100, v.intValue())); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 15 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("缩放", "Zoom"), UiText.t("按快捷键放大视野", "Zoom the view with a key binding"),
                new SettingToggle(() -> Config.zoom, v -> { Config.zoom = v; Config.save(); }))
                .keybindAction(ModuleKeybindManager.ACTION_ZOOM)
                .addSub(UiText.t("缩放倍率", "Zoom Amount"), UiText.t("按下缩放键时的初始缩放倍率", "Initial zoom multiplier while holding the zoom key"),
                        new SettingSlider(2.0, 20.0, "%.0fx", () -> (double) Config.zoomAmount,
                                v -> { Config.zoomAmount = Math.max(2, Math.min(20, v.intValue())); Config.save(); }))
                .addSub(UiText.t("滚轮缩放", "Scroll Zoom"), UiText.t("按住缩放键时使用滚轮调整缩放倍率", "Use the mouse wheel to adjust zoom while holding the zoom key"),
                        new SettingToggle(() -> Config.zoomScroll, v -> { Config.zoomScroll = v; Config.save(); }))
                .addSubWhen(() -> Config.zoomScroll, UiText.t("滚轮档位", "Scroll Steps"), UiText.t("滚轮缩放允许的最大档位", "Maximum number of scroll zoom steps"),
                        new SettingSlider(1.0, 20.0, "%.0f", () -> (double) Config.zoomScrollSteps,
                                v -> { Config.zoomScrollSteps = Math.max(1, Math.min(20, v.intValue())); Config.save(); }))
                .addSubWhen(() -> Config.zoomScroll, UiText.t("每档倍率", "Step Multiplier"), UiText.t("每滚动一档增加的缩放倍率", "Zoom multiplier added per scroll step"),
                        new SettingSlider(110.0, 250.0, "%.0f%%", () -> (double) Config.zoomPerStep,
                                v -> { Config.zoomPerStep = Math.max(110, Math.min(250, v.intValue())); Config.save(); }))
                .addSub(UiText.t("进入时间", "Zoom In Time"), UiText.t("缩放进入动画耗时", "Zoom-in transition duration"),
                        new SettingSlider(0.0, 1.5, "%.2fs", () -> (double) Config.zoomInTime,
                                v -> { Config.zoomInTime = Math.max(0.0f, Math.min(1.5f, v.floatValue())); Config.save(); }))
                .addSub(UiText.t("退出时间", "Zoom Out Time"), UiText.t("缩放退出动画耗时", "Zoom-out transition duration"),
                        new SettingSlider(0.0, 1.5, "%.2fs", () -> (double) Config.zoomOutTime,
                                v -> { Config.zoomOutTime = Math.max(0.0f, Math.min(1.5f, v.floatValue())); Config.save(); }))
                .addSub(UiText.t("缩放相机灵敏度", "Zoom Camera Sensitivity"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.zoomRelativeSensitivity,
                                v -> { Config.zoomRelativeSensitivity = Math.max(0, Math.min(100, v.intValue())); Config.save(); }))));

        if (Version.DEBUG) {
            modules.add(ModuleBindingIds.preserve(ToolPage.class, 17 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("FakePlayer", "FakePlayer"), UiText.t("测试功能请勿开启", "Test feature, do not enable"),
                    new SettingToggle(FakePlayerManager::isEnabled, FakePlayerManager::setEnabled))
                    .addSub(UiText.t("盔甲", "Armor"), UiText.t("控制假玩家是否穿着下界合金甲", "Control whether the fake player wears netherite armor"),
                            new SettingToggle(FakePlayerManager::hasArmor, FakePlayerManager::setArmor))
                    .addSub(UiText.t("不死图腾", "Totem"), UiText.t("控制假玩家手上是否持有不死图腾", "Control whether the fake player holds a Totem of Undying"),
                            new SettingToggle(FakePlayerManager::hasTotem, FakePlayerManager::setTotem))));
        }

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 17 + (Config.restrictedFeaturesUnlocked() ? 3 : 0) + (Version.DEBUG ? 1 : 0), new SettingModule(UiText.t("快捷存入", "Quick Deposit"), UiText.t("手持物品并左键点击容器时快捷存入手中的物品", "Quickly deposit the held item when left-clicking a container while holding an item"),
                new SettingToggle(() -> Config.autoChestDeposit, v -> { Config.autoChestDeposit = v; Config.save(); }))
                .addSub(UiText.t("仅限资源", "Resources Only"), UiText.t("只存入资源", "Only deposit resources"),
                        new SettingToggle(() -> Config.autoChestDepositResourcesOnly,
                                v -> { Config.autoChestDepositResourcesOnly = v; Config.save(); }))
                .addSub(UiText.t("存入延迟", "Deposit Delay"), UiText.t("打开容器后存入物品的时间(tick)", "Ticks to wait after opening the container before depositing the item"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositDepositDelay,
                                v -> { Config.autoChestDepositDepositDelay = v.intValue(); Config.save(); }))
                .addSub(UiText.t("关闭容器延迟", "Close Container Delay"), UiText.t("存入物品后关闭容器所等待的时间(tick)", "Ticks to wait after depositing the item before closing the container"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositCloseDelay,
                                v -> { Config.autoChestDepositCloseDelay = v.intValue(); Config.save(); }))
                 .visibleWhen(() -> Config.fullMode)));

        modules.add(ModuleBindingIds.preserve(MiscPage.class, 4, new SettingModule(UiText.t("伪装创造", "Pseudo Creative"), UiText.t("在本地显示创造模式状态", "Display Creative mode locally"),
                new SettingToggle(PseudoCreativeManager::isActive, v -> { PseudoCreativeManager.setEnabled(v); Config.save(); }))));

        if (Config.restrictedFeaturesUnlocked()) {
            modules.add(ModuleBindingIds.preserve(CombatPage.class, 0 + (Config.restrictedFeaturesUnlocked() ? 1 : 0), new SettingModule(UiText.t("鞘翅改进", "Elytra Improvements"), UiText.t("自动展开鞘翅并使用烟花推进", "Automatically deploy elytra and use firework boosts"),
                    new SettingToggle(() -> Config.elytraAssist, v -> { Config.elytraAssist = v; Config.save(); }))
                    .addSub(UiText.t("自动展开鞘翅", "Auto Deploy Elytra"), UiText.t("跳起时自动展开身上的鞘翅", "Automatically deploy equipped elytra when jumping"),
                            new SettingToggle(() -> Config.elytraAutoDeploy, v -> { Config.elytraAutoDeploy = v; Config.save(); }))
                    .addSub(UiText.t("自动烟花推进", "Auto Firework Boost"), UiText.t("手持烟花且没有烟花动能时自动使用", "Automatically use held fireworks when no boost is active"),
                            new SettingToggle(() -> Config.elytraAutoFirework, v -> { Config.elytraAutoFirework = v; Config.save(); }))
                    .visibleWhen(() -> Config.fullMode)));
        }

    }

    @Override public String getTitle() { return UiText.t("工具设置", "Tool Settings"); }
    @Override public String getSubtitle() { return UiText.t("实用工具与辅助功能", "Utility and helper features"); }
}
