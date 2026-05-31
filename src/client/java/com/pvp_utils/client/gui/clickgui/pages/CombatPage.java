package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.widget.*;

import java.util.List;

public class CombatPage extends BasePage {

    public CombatPage() {
        modules.add(new SettingModule("击中标记", "在命中时显示标记",
                new SettingToggle(() -> Config.hitMarker, v -> { Config.hitMarker = v; Config.save(); })));

        modules.add(new SettingModule("命中提示音", "命中目标时播放音效",
                new SettingToggle(() -> Config.hitSound, v -> { Config.hitSound = v; Config.save(); }))
                .addSub("提示音类型", "选择提示音效果",
                        new SettingCycle(List.of("下界合金块", "经验声"),
                                () -> Config.hitSoundType == Config.HitSoundType.NETHERITE ? 0 : 1,
                                i -> { Config.hitSoundType = i == 0 ? Config.HitSoundType.NETHERITE : Config.HitSoundType.EXPERIENCE; Config.save(); }))
                .addSub("播放时机", "选择触发条件",
                        new SettingCycle(List.of("近战&远程", "仅近战", "仅远程"),
                                () -> switch (Config.hitSoundCondition) { case BOTH -> 0; case MELEE -> 1; case RANGED -> 2; },
                                i -> { Config.hitSoundCondition = switch (i) { case 1 -> Config.HitSoundCondition.MELEE; case 2 -> Config.HitSoundCondition.RANGED; default -> Config.HitSoundCondition.BOTH; }; Config.save(); })));
    }

    @Override public String getTitle() { return "战斗设置"; }
    @Override public String getSubtitle() { return "调整战斗相关参数"; }
}