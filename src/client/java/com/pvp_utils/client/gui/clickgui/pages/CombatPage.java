package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;

import java.util.List;

public class CombatPage extends BasePage {

    public CombatPage() {
        modules.add(new SettingModule(UiText.t("主手辅助", "AutoMainHand"), UiText.t("在合适的时候自动切换主手手持的物品", "Automatically switch the main-hand item at suitable moments"),
                new SettingToggle(() -> Config.mainHandAssist, v -> { Config.mainHandAssist = v; Config.save(); }))
                .addSub(UiText.t("近战武器辅助", "Melee Weapon Assist"), UiText.t("在手持武器时，长按右键可自动切换至物品栏内的血量回复道具", "When holding a weapon, hold right click to switch to a hotbar healing item"),
                        new SettingToggle(() -> Config.mainHandAssistMeleeWeapon, v -> { Config.mainHandAssistMeleeWeapon = v; Config.save(); }))
                .addSub(UiText.t("盾牌", "Shield"), UiText.t("右键自动使用盾牌", "Automatically switch to a shield when right clicking"),
                        new SettingToggle(() -> Config.mainHandAssistShield, v -> { Config.mainHandAssistShield = v; Config.save(); }))
                .addSub(UiText.t("执行完毕后切回原先槽位", "Switch Back After Use"), UiText.t("使用完成或松开右键后切回原先槽位", "Switch back to the original slot after use or after releasing right click"),
                        new SettingToggle(() -> Config.mainHandAssistSwitchBack, v -> { Config.mainHandAssistSwitchBack = v; Config.save(); }))
                .addSub(UiText.t("切换延迟(tick)", "Switch Delay (tick)"), UiText.t("控制按下右键后切换到目标物品或切换回去的延迟", "Delay before switching to the target item or switching back"),
                        new SettingSlider(0.0, 20.0, "%.0f", () -> (double) Config.mainHandAssistSwitchDelayTicks, v -> { Config.mainHandAssistSwitchDelayTicks = v.intValue(); Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("鞘翅改进", "Elytra Improvements"), UiText.t("让鞘翅的使用更加便捷", "Make elytra usage more convenient"),
                new SettingToggle(() -> Config.elytraAssist, v -> { Config.elytraAssist = v; Config.save(); }))
                .addSub(UiText.t("自动展开鞘翅", "Auto Deploy Elytra"), UiText.t("跳起时自动展开身上的鞘翅", "Automatically deploy equipped elytra when jumping"),
                        new SettingToggle(() -> Config.elytraAutoDeploy, v -> { Config.elytraAutoDeploy = v; Config.save(); }))
                .addSub(UiText.t("自动烟花推进", "Auto Firework Boost"), UiText.t("手持烟花且没有烟花动能时自动使用", "Automatically use held fireworks when no boost is active"),
                        new SettingToggle(() -> Config.elytraAutoFirework, v -> { Config.elytraAutoFirework = v; Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("命中标记", "Hit Marker"), UiText.t("在命中时显示标记", "Show a marker when you hit a target"),
                new SettingToggle(() -> Config.hitMarker, v -> { Config.hitMarker = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("命中提示音", "Hit Sound"), UiText.t("命中目标时播放音效", "Play a sound when you hit a target"),
                new SettingToggle(() -> Config.hitSound, v -> { Config.hitSound = v; Config.save(); }))
                .addSub(UiText.t("提示音类型", "Sound Type"), UiText.t("选择提示音效果", "Choose the hit sound effect"),
                        new SettingCycle(List.of(UiText.t("下界合金块", "Netherite Block"), UiText.t("经验球", "Experience")),
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
