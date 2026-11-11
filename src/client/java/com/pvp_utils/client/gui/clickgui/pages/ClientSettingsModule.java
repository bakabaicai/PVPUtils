package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.Update;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingLabel;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingTextBox;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;

final class ClientSettingsModule extends SettingModule {
    private String nameDraft;
    private String prefixDraft;
    private String saveStatus = "";
    private String updateStatus = "";
    private boolean checking;
    private URI downloadUri;

    ClientSettingsModule() {
        super(UiText.t("客户端设置", "Client Settings"), "", null);
        setBindingId(MiscPage.class.getName() + ".client_settings");
        refreshDrafts();
        addSub(UiText.t("客户端名称", "Client Name"), "",
                new SettingTextBox(() -> nameDraft, value -> { nameDraft = value; saveStatus = ""; }, 256));
        addSub(UiText.t("命令前缀", "Command Prefix"), UiText.t("使用单个符号", "Use a single symbol"),
                new SettingTextBox(() -> prefixDraft, value -> { prefixDraft = value; saveStatus = ""; }, 1));
        addSub(UiText.t("保存名称与前缀", "Save Name and Prefix"), "",
                new SettingButton(() -> UiText.t("保存", "Save"), this::saveDrafts));
        addSubVisible(UiText.t("保存结果", "Save Result"), "", new SettingLabel(() -> saveStatus),
                () -> !saveStatus.isEmpty());
        addSub(UiText.t("版本提示", "Version Warning"), "",
                new SettingToggle(() -> !Config.versionWarningDisabled,
                        value -> { Config.versionWarningDisabled = !value; Config.save(); }));
        addSub(UiText.t("检查更新", "Check for Updates"), "",
                new SettingButton(() -> checking ? UiText.t("检查中", "Checking") : UiText.t("检查", "Check"),
                        this::checkForUpdates));
        addSubVisible(UiText.t("更新状态", "Update Status"), "", new SettingLabel(() -> updateStatus),
                () -> !updateStatus.isEmpty());
        addSubVisible(UiText.t("更新下载", "Download Update"), "",
                new SettingButton("GitHub", this::openDownload), () -> downloadUri != null);
    }

    @Override
    public void setFlyoutOpen(boolean open) {
        if (open && !isFlyoutOpen()) refreshDrafts();
        super.setFlyoutOpen(open);
    }

    @Override
    public void openFlyoutInstant(float x, float y, float width, float height) {
        if (!isFlyoutOpen()) refreshDrafts();
        super.openFlyoutInstant(x, y, width, height);
    }

    private void refreshDrafts() {
        nameDraft = Config.clientName == null ? "PVPUtils" : Config.clientName;
        prefixDraft = CommandManager.getPrefix();
        saveStatus = "";
    }

    private void saveDrafts() {
        String name = nameDraft.strip();
        if (name.isEmpty()) {
            saveStatus = UiText.t("客户端名称不能为空", "Client name cannot be empty");
            return;
        }
        if (!CommandManager.isValidPrefix(prefixDraft)) {
            saveStatus = UiText.t("命令前缀必须为单个符号", "The command prefix must be a single symbol");
            return;
        }
        Config.clientName = name;
        CommandManager.setPrefix(prefixDraft);
        nameDraft = name;
        SettingTextBox.clearFocus();
        saveStatus = UiText.t("已保存", "Saved");
    }

    private void checkForUpdates() {
        if (checking) return;
        checking = true;
        downloadUri = null;
        Update.startManualCheck(message -> updateStatus = message.getString(), this::receiveUpdateStatus);
    }

    private void receiveUpdateStatus(MutableComponent message) {
        checking = false;
        updateStatus = message.plainCopy().getString().strip();
        for (Component sibling : message.getSiblings()) {
            if (sibling.getStyle().getClickEvent() instanceof ClickEvent.OpenUrl link
                    && "github.com".equalsIgnoreCase(link.uri().getHost())) {
                downloadUri = link.uri();
            }
        }
    }

    private void openDownload() {
        if (downloadUri == null) return;
        Minecraft client = Minecraft.getInstance();
        ConfirmLinkScreen.confirmLinkNow(client.screen, downloadUri);
    }
}
