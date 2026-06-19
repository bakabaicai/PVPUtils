package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;

import java.util.List;

public class CombatPage extends BasePage {

    public CombatPage() {
        modules.add(new SettingModule(UiText.t("鞘翅改进", "Elytra Improvements"), UiText.t("让鞘翅的使用更加便捷", "Make elytra usage more convenient"),
                new SettingToggle(() -> Config.elytraAssist, v -> { Config.elytraAssist = v; Config.save(); }))
                .addSub(UiText.t("自动展开鞘翅", "Auto Deploy Elytra"), UiText.t("跳起时自动展开身上的鞘翅", "Automatically deploy equipped elytra when jumping"),
                        new SettingToggle(() -> Config.elytraAutoDeploy, v -> { Config.elytraAutoDeploy = v; Config.save(); }))
                .addSub(UiText.t("自动烟花推进", "Auto Firework Boost"), UiText.t("手持烟花且没有烟花动能时自动使用", "Automatically use held fireworks when no boost is active"),
                        new SettingToggle(() -> Config.elytraAutoFirework, v -> { Config.elytraAutoFirework = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("暴击辅助", "Critical Assist"), UiText.t("提升暴击的成功率（会使”击退攻击“失效）", "Improve critical hit success rate (will disable sprint knockback hits)"),
                new SettingToggle(() -> Config.criticalAssist, v -> { Config.criticalAssist = v; Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("击中标记", "Hit Marker"), UiText.t("在命中时显示标记", "Show a marker when you hit a target"),
                new SettingToggle(() -> Config.hitMarker, v -> { Config.hitMarker = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("命中提示音", "Hit Sound"), UiText.t("命中目标时播放音效", "Play a sound when you hit a target"),
                new SettingToggle(() -> Config.hitSound, v -> { Config.hitSound = v; Config.save(); }))
                .addSub(UiText.t("提示音类型", "Sound Type"), UiText.t("选择提示音效果", "Choose the hit sound effect"),
                        new SettingCycle(List.of(UiText.t("下界合金块", "Netherite Block"), UiText.t("经验声", "Experience")),
                                () -> Config.hitSoundType == Config.HitSoundType.NETHERITE ? 0 : 1,
                                i -> { Config.hitSoundType = i == 0 ? Config.HitSoundType.NETHERITE : Config.HitSoundType.EXPERIENCE; Config.save(); }))
                .addSub(UiText.t("播放时机", "Trigger Timing"), UiText.t("选择触发条件", "Choose when the sound should play"),
                        new SettingCycle(List.of(UiText.t("近战&远程", "Melee & Ranged"), UiText.t("仅近战", "Melee Only"), UiText.t("仅远程", "Ranged Only")),
                                () -> switch (Config.hitSoundCondition) { case BOTH -> 0; case MELEE -> 1; case RANGED -> 2; },
                                i -> { Config.hitSoundCondition = switch (i) { case 1 -> Config.HitSoundCondition.MELEE; case 2 -> Config.HitSoundCondition.RANGED; default -> Config.HitSoundCondition.BOTH; }; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("战斗设置", "Combat Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整战斗相关参数", "Adjust combat options"); }
}
