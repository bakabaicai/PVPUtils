package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.gui.clickgui.widget.SettingTextBox;

import java.util.List;

public class CombatPage extends BasePage {

    public CombatPage() {
        if (Config.restrictedFeaturesUnlocked()) {
        modules.add(new SettingModule(UiText.t("主手辅助", "AutoMainHand"), UiText.t("按使用场景切换主手物品", "Switch main-hand items for the current action"),
                new SettingToggle(() -> Config.mainHandAssist, v -> { Config.mainHandAssist = v; Config.save(); }))
                .addSub(UiText.t("近战武器辅助", "Melee Weapon Assist"), UiText.t("持武器长按右键时切换至快捷栏内的恢复道具", "Switch from a held weapon to a hotbar healing item while holding right click"),
                        new SettingToggle(() -> Config.mainHandAssistMeleeWeapon, v -> { Config.mainHandAssistMeleeWeapon = v; Config.save(); }))
                .addSub(UiText.t("盾牌", "Shield"), UiText.t("右键自动使用盾牌", "Automatically switch to a shield when right clicking"),
                        new SettingToggle(() -> Config.mainHandAssistShield, v -> { Config.mainHandAssistShield = v; Config.save(); }))
                .addSub(UiText.t("快速使用", "Quick Use"), UiText.t("单击右键使用快捷栏内的补给物品", "Use a hotbar supply item with one right click"),
                        new SettingToggle(() -> Config.mainHandAssistQuickUse, v -> { Config.mainHandAssistQuickUse = v; Config.save(); }))
                .addSub(UiText.t("执行完毕后切回原先槽位", "Switch Back After Use"), UiText.t("使用完成或松开右键后切回原先槽位", "Switch back to the original slot after use or after releasing right click"),
                        new SettingToggle(() -> Config.mainHandAssistSwitchBack, v -> { Config.mainHandAssistSwitchBack = v; Config.save(); }))
                .addSub(UiText.t("切换延迟(tick)", "Switch Delay (tick)"), UiText.t("控制按下右键后切换到目标物品或切换回去的延迟", "Delay before switching to the target item or switching back"),
                        new SettingSlider(2.0, 20.0, "%.0f", () -> (double) Math.max(2, Config.mainHandAssistSwitchDelayTicks), v -> { Config.mainHandAssistSwitchDelayTicks = Math.max(2, v.intValue()); Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("鞘翅改进", "Elytra Improvements"), UiText.t("自动展开鞘翅并使用烟花推进", "Automatically deploy elytra and use firework boosts"),
                new SettingToggle(() -> Config.elytraAssist, v -> { Config.elytraAssist = v; Config.save(); }))
                .addSub(UiText.t("自动展开鞘翅", "Auto Deploy Elytra"), UiText.t("跳起时自动展开身上的鞘翅", "Automatically deploy equipped elytra when jumping"),
                        new SettingToggle(() -> Config.elytraAutoDeploy, v -> { Config.elytraAutoDeploy = v; Config.save(); }))
                .addSub(UiText.t("自动烟花推进", "Auto Firework Boost"), UiText.t("手持烟花且没有烟花动能时自动使用", "Automatically use held fireworks when no boost is active"),
                        new SettingToggle(() -> Config.elytraAutoFirework, v -> { Config.elytraAutoFirework = v; Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        }

        modules.add(new SettingModule(UiText.t("命中标记", "Hit Marker"), UiText.t("在命中时显示标记", "Show a marker when you hit a target"),
                new SettingToggle(() -> Config.hitMarker, v -> { Config.hitMarker = v; Config.save(); })));

        List<String> sounds = List.of(UiText.t("下界合金块", "Netherite Block"), UiText.t("经验球", "Experience"), UiText.t("自定义", "Custom"));
        modules.add(new SettingModule(UiText.t("命中提示音", "Hit Sound"), UiText.t("命中目标时播放音效", "Play a sound when a hit is confirmed"),
                new SettingToggle(() -> Config.hitSound, value -> { Config.hitSound = value; Config.save(); }))
                .addSub(UiText.t("近战音效", "Melee Sound"), "",
                        new SettingToggle(() -> Config.hitSoundMeleeEnabled, value -> { Config.hitSoundMeleeEnabled = value; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundMeleeEnabled, UiText.t("音效", "Sound"), "",
                        new SettingCycle(sounds, () -> Config.hitSoundMeleeType.ordinal(),
                                value -> { Config.hitSoundMeleeType = Config.HitSoundType.values()[value]; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundMeleeEnabled && Config.hitSoundMeleeType == Config.HitSoundType.CUSTOM,
                        UiText.t("音频文件", "Audio File"), "WAV / AIFF / AU",
                        new SettingTextBox(() -> Config.hitSoundMeleeFile, value -> { Config.hitSoundMeleeFile = value; Config.save(); }, 1024))
                .addSub(UiText.t("远程音效", "Ranged Sound"), "",
                        new SettingToggle(() -> Config.hitSoundRangedEnabled, value -> { Config.hitSoundRangedEnabled = value; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundRangedEnabled, UiText.t("音效", "Sound"), "",
                        new SettingCycle(sounds, () -> Config.hitSoundRangedType.ordinal(),
                                value -> { Config.hitSoundRangedType = Config.HitSoundType.values()[value]; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundRangedEnabled && Config.hitSoundRangedType == Config.HitSoundType.CUSTOM,
                        UiText.t("音频文件", "Audio File"), "WAV / AIFF / AU",
                        new SettingTextBox(() -> Config.hitSoundRangedFile, value -> { Config.hitSoundRangedFile = value; Config.save(); }, 1024)));

    }

    @Override public String getTitle() { return UiText.t("战斗设置", "Combat Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整战斗相关参数", "Adjust combat options"); }
}
