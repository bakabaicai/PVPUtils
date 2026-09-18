package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.ChangelogScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModsPage extends BasePage {

    private final String subtitle;

    public ModsPage() {
        modules.add(new SettingModule(UiText.t("更新日志", "Changelog"), UiText.t("查看本版本更新内容", "View changes in this version"),
                new SettingButton(UiText.t("查看", "View"), () -> Minecraft.getInstance().setScreen(new ChangelogScreen()))));

        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        mods.sort(Comparator.comparing(m -> m.getMetadata().getName(), String.CASE_INSENSITIVE_ORDER));
        subtitle = Config.isChinese
                ? "已安装 " + mods.size() + " 个模组，右键展开查看详情"
                : "Showing " + mods.size() + " mods, right-click for details";

        for (ModContainer container : mods) {
            ModMetadata meta = container.getMetadata();
            String name = meta.getName();
            if (name == null || name.isBlank()) {
                name = meta.getId();
            }
            String version = meta.getVersion().getFriendlyString();
            String description = meta.getDescription();
            if (description == null || description.isBlank()) {
                description = "";
            } else if (description.length() > 90) {
                description = description.substring(0, 87) + "...";
            }
            String sub = version + (description.isEmpty() ? "" : " · " + description);

            SettingModule module = new SettingModule(name, sub, null);
            module.addSub(UiText.t("模组 ID", "Mod ID"), meta.getId(), null);

            String authors = String.join(", ", meta.getAuthors().stream().map(a -> a.getName()).toList());
            if (!authors.isEmpty()) {
                module.addSub(UiText.t("作者", "Authors"), authors, null);
            }

            ContactInformation contact = meta.getContact();
            addLink(module, UiText.t("网站", "Website"), contact.get("homepage").orElse(null));
            addLink(module, UiText.t("源码", "Source"), contact.get("sources").orElse(null));
            addLink(module, UiText.t("问题反馈", "Issues"), contact.get("issues").orElse(null));

            String license = String.join(", ", meta.getLicense());
            if (!license.isBlank()) {
                module.addSub(UiText.t("许可证", "License"), license, null);
            }
            modules.add(module);
        }
    }

    private static void addLink(SettingModule module, String label, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        module.addSub(label, url, new SettingButton(UiText.t("打开", "Open"), () -> openUrl(url)));
    }

    private static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
        }
    }

    @Override public String getTitle() { return UiText.t("模组列表", "Mod List"); }
    @Override public String getSubtitle() { return subtitle; }
}
