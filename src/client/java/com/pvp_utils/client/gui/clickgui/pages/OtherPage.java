package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.widget.*;

public class OtherPage extends BasePage {

    public OtherPage() {
        modules.add(new SettingModule("胜利音效", "击杀时播放胜利音效",
                new SettingToggle(() -> Config.victorySound, v -> { Config.victorySound = v; Config.save(); })));
    }

    @Override public String getTitle() { return "其他设置"; }
    @Override public String getSubtitle() { return "其他杂项功能"; }
}