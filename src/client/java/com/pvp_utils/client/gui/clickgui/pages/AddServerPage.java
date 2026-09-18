package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingPasswordBox;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingTextBox;
import com.pvp_utils.client.modules.impl.Tool.ServerAutoLoginManager;
import com.pvp_utils.client.util.ChatUtils;
import net.minecraft.client.Minecraft;

public class AddServerPage extends BasePage {
    private String addressDraft = "";
    private String passwordDraft = "";
    private int delayDraft = 1;

    public AddServerPage() {
        modules.add(new SettingModule(UiText.t("服务器地址", "Server Address"), UiText.t("必填，例如 mc.example.com", "Required, e.g. mc.example.com"),
                new SettingTextBox(() -> addressDraft, v -> addressDraft = v, 255)));

        modules.add(new SettingModule(UiText.t("登录密码", "Login Password"), UiText.t("可留空，输入后加密保存，显示为星号", "Optional, stored encrypted and shown as stars"),
                new SettingPasswordBox(() -> passwordDraft.isEmpty() ? "" : "********", v -> passwordDraft = v, 64)));

        modules.add(new SettingModule(UiText.t("登录延迟", "Login Delay"), UiText.t("进入服务器后等待多久发送登录命令(0-30秒)", "How long to wait after joining before sending the login command (0-30s)"),
                new SettingSlider(0.0, 30.0, "%.0fs", () -> (double) delayDraft,
                        v -> delayDraft = Math.max(0, Math.min(30, v.intValue())))));

        modules.add(new SettingModule(UiText.t("确认添加", "Confirm"), UiText.t("添加该服务器到自动登录列表", "Add this server to the auto login list"),
                new SettingButton(UiText.t("添加并返回", "Add & Back"), this::confirm)));

        modules.add(new SettingModule(UiText.t("放弃", "Discard"), UiText.t("不保存返回工具设置页", "Discard and go back to tool settings"),
                new SettingButton(UiText.t("返回", "Back"), this::back)));
    }

    private void confirm() {
        String address = addressDraft == null ? "" : addressDraft.trim();
        if (address.isEmpty()) {
            ChatUtils.warning(Config.isChinese
                    ? "请先填写服务器地址。"
                    : "Fill in the server address first.");
            return;
        }
        ServerAutoLoginManager.setRule(address, passwordDraft == null ? "" : passwordDraft, delayDraft);
        back();
    }

    private void back() {
        if (Minecraft.getInstance().screen instanceof NewSettingsScreen settings) {
            settings.rebuildCurrentPage();
        }
    }

    @Override public String getTitle() { return UiText.t("添加自动登录服务器", "Add Auto Login Server"); }
    @Override public String getSubtitle() { return UiText.t("填写服务器信息后确认添加", "Fill in the server info and confirm"); }
}
