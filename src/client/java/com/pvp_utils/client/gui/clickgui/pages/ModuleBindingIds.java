package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.widget.SettingModule;

final class ModuleBindingIds {
    private ModuleBindingIds() {
    }

    static SettingModule preserve(Class<? extends BasePage> page, int index, SettingModule module) {
        module.setBindingId(page.getName() + "." + index);
        return module;
    }
}
