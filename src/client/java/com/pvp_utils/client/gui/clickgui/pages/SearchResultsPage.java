package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;

import java.util.Collection;

public final class SearchResultsPage extends BasePage {
    private final String query;

    public SearchResultsPage(String query, Collection<SettingModule> results) {
        this.query = query;
        modules.addAll(results);
    }

    @Override
    public String getTitle() {
        return UiText.t("搜索结果", "Search Results");
    }

    @Override
    public String getSubtitle() {
        return UiText.t("与“", "Matches for \"") + query + UiText.t("”匹配的功能", "\"");
    }
}
