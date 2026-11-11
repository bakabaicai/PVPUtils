package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.widget.SettingColorPreview;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.modules.impl.Combat.HitSoundFilePicker;

import java.util.List;

public class CombatPage extends BasePage {

    public CombatPage() {
        if (Config.restrictedFeaturesUnlocked()) {
        modules.add(ModuleBindingIds.preserve(CombatPage.class, 0, new SettingModule(UiText.t("主手辅助", "AutoMainHand"), UiText.t("按使用场景切换主手物品", "Switch main-hand items for the current action"),
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
                .visibleWhen(() -> Config.fullMode)));

        }

        modules.add(ModuleBindingIds.preserve(CombatPage.class, 0 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("命中标记", "Hit Marker"), UiText.t("在命中时显示标记", "Show a marker when you hit a target"),
                new SettingToggle(() -> Config.hitMarker, v -> { Config.hitMarker = v; Config.save(); }))));

        List<String> sounds = List.of(UiText.t("下界合金块", "Netherite Block"), UiText.t("经验球", "Experience"), UiText.t("自定义", "Custom"));
        modules.add(ModuleBindingIds.preserve(CombatPage.class, 1 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("命中提示音", "Hit Sound"), UiText.t("命中目标时播放音效", "Play a sound when a hit is confirmed"),
                new SettingToggle(() -> Config.hitSound, value -> { Config.hitSound = value; Config.save(); }))
                .addSub(UiText.t("近战音效", "Melee Sound"), "",
                        new SettingToggle(() -> Config.hitSoundMeleeEnabled, value -> { Config.hitSoundMeleeEnabled = value; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundMeleeEnabled, UiText.t("音效", "Sound"), "",
                        new SettingCycle(sounds, () -> Config.hitSoundMeleeType.ordinal(),
                                value -> { Config.hitSoundMeleeType = Config.HitSoundType.values()[value]; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundMeleeEnabled && Config.hitSoundMeleeType == Config.HitSoundType.CUSTOM,
                        UiText.t("音频文件", "Audio File"), "WAV / AIFF / AU",
                        new SettingButton(() -> Config.hitSoundMeleeFile.isBlank() ? UiText.t("选择文件", "Choose File")
                                : UiText.t("更换文件", "Change File"), () -> HitSoundFilePicker.select(false)))
                .addSub(UiText.t("远程音效", "Ranged Sound"), "",
                        new SettingToggle(() -> Config.hitSoundRangedEnabled, value -> { Config.hitSoundRangedEnabled = value; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundRangedEnabled, UiText.t("音效", "Sound"), "",
                        new SettingCycle(sounds, () -> Config.hitSoundRangedType.ordinal(),
                                value -> { Config.hitSoundRangedType = Config.HitSoundType.values()[value]; Config.save(); }))
                .addSubWhen(() -> Config.hitSoundRangedEnabled && Config.hitSoundRangedType == Config.HitSoundType.CUSTOM,
                        UiText.t("音频文件", "Audio File"), "WAV / AIFF / AU",
                        new SettingButton(() -> Config.hitSoundRangedFile.isBlank() ? UiText.t("选择文件", "Choose File")
                                : UiText.t("更换文件", "Change File"), () -> HitSoundFilePicker.select(true)))));

        SettingModule comboEffects = new SettingModule(UiText.t("连击效果", "Combo Effects"),
                UiText.t("击杀时显示图标并播放连击音效", "Show a kill icon and play combo sounds"),
                new SettingToggle(() -> Config.comboEffects, value -> { Config.comboEffects = value; Config.save(); }))
                .addSubGroup(UiText.t("生效范围", "Target Types"), "")
                .addSubChild(UiText.t("玩家", "Players"), "",
                        new SettingToggle(() -> Config.comboEffectsPlayers, value -> { Config.comboEffectsPlayers = value; Config.save(); }))
                .addSubChild(UiText.t("怪物", "Monsters"), "",
                        new SettingToggle(() -> Config.comboEffectsMonsters, value -> { Config.comboEffectsMonsters = value; Config.save(); }))
                .addSubChild(UiText.t("动物", "Animals"), "",
                        new SettingToggle(() -> Config.comboEffectsAnimals, value -> { Config.comboEffectsAnimals = value; Config.save(); }))
                .addSubChild(UiText.t("其他生物", "Other Living Entities"), "",
                        new SettingToggle(() -> Config.comboEffectsOther, value -> { Config.comboEffectsOther = value; Config.save(); }))
                .addSub(UiText.t("连击持续时间", "Combo Duration"),
                        UiText.t("超时未再次击杀时重置连击", "Reset the combo if no further kill occurs within this time"),
                        new SettingSlider(0.1, 300, "%.1fs", () -> (double) Config.comboEffectsDuration,
                                value -> { Config.comboEffectsDuration = value.floatValue(); Config.save(); }))
                .addSub(UiText.t("图标", "Icon"), "",
                        new SettingToggle(() -> Config.comboEffectsIcon, value -> { Config.comboEffectsIcon = value; Config.save(); }))
                .addSub(UiText.t("音效", "Sound"), "",
                        new SettingToggle(() -> Config.comboEffectsSound, value -> { Config.comboEffectsSound = value; Config.save(); }))
                .addSubWhen(() -> Config.comboEffectsSound, UiText.t("音量", "Volume"), "",
                        new SettingSlider(0, 100, "%.0f%%", () -> (double) Config.comboEffectsVolume * 100,
                                value -> { Config.comboEffectsVolume = value.floatValue() / 100f; Config.save(); }));
        comboEffects.setBindingId(CombatPage.class.getName() + ".combo_effects");
        modules.add(comboEffects);

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 7, new SettingModule(UiText.t("攻击特效", "Attack Effects"), UiText.t("控制攻击时显示的粒子效果", "Control particles shown when attacking"), null)
                .addSub(UiText.t("暴击粒子", "Crit Particles"), UiText.t("攻击时常驻显示暴击粒子", "Always show crit particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsCritParticles, v -> { Config.attackEffectsCritParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsCritParticles, UiText.t("暴击粒子倍数", "Crit Multiplier"), UiText.t("调整额外暴击粒子的显示倍数", "Adjust the extra crit particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsCritMultiplier, v -> { Config.attackEffectsCritMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("锋利粒子", "Sharpness Particles"), UiText.t("攻击时常驻显示锋利粒子", "Always show sharpness particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsSharpnessParticles, v -> { Config.attackEffectsSharpnessParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsSharpnessParticles, UiText.t("锋利粒子倍数", "Sharpness Multiplier"), UiText.t("调整额外锋利粒子的显示倍数", "Adjust the extra sharpness particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsSharpnessMultiplier, v -> { Config.attackEffectsSharpnessMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("火焰粒子", "Flame Particles"), UiText.t("攻击时常驻显示火焰粒子", "Always show flame particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsFlameParticles, v -> { Config.attackEffectsFlameParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsFlameParticles, UiText.t("火焰粒子倍数", "Flame Multiplier"), UiText.t("调整火焰粒子的显示倍数", "Adjust the flame particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsFlameMultiplier, v -> { Config.attackEffectsFlameMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("血液粒子", "Blood Particles"), UiText.t("攻击时常驻显示血液粒子", "Always show blood particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsBloodParticles, v -> { Config.attackEffectsBloodParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsBloodParticles, UiText.t("血液粒子倍数", "Blood Multiplier"), UiText.t("调整血液粒子的显示倍数", "Adjust the blood particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsBloodMultiplier, v -> { Config.attackEffectsBloodMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("闪电", "Lightning"), UiText.t("攻击时渲染闪电效果", "Render lightning effects when attacking"),
                        new SettingToggle(() -> Config.attackEffectsLightning, v -> { Config.attackEffectsLightning = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsLightning, UiText.t("闪电数量", "Lightning Count"), UiText.t("攻击时渲染的闪电数量", "Number of lightning effects rendered per attack"),
                        new SettingSlider(1.0, 5.0, "%.0f", () -> (double) Config.attackEffectsLightningCount, v -> { Config.attackEffectsLightningCount = v.intValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 8, new SettingModule(UiText.t("更改受击颜色", "Hit Color"), UiText.t("更改实体受击时的颜色", "Change the color shown when entities are hit"),
                new SettingToggle(() -> Config.hitColor, v -> { Config.hitColor = v; Config.save(); }))
                .addSub(UiText.t("受击颜色", "Overlay Color"), UiText.t("点击色块调整颜色与透明度", "Click the swatch to edit color and opacity"),
                        new SettingColorPreview(CombatPage::hitColorArgb, CombatPage::setHitColorArgb, true, Config::save))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 11, new SettingModule(UiText.t("自动格挡", "Auto Block"), UiText.t("自动触发格挡动作", "Automatically trigger blocking"),
                new SettingToggle(() -> Config.autoMode, v -> { Config.autoMode = v; Config.save(); }))
                .addSub(UiText.t("触发距离", "Trigger Range"), "",
                        new SettingSlider(2.0, 6.0, "%.2f", () -> Config.range, v -> { Config.range = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 28, new SettingModule(UiText.t("低血量提示", "Low Health Warning"), UiText.t("血量过低时显示警告", "Show a warning when health is low"),
                new SettingToggle(() -> Config.lowHealthNotify, v -> {
                    Config.lowHealthNotify = v;
                    if (v) Config.dynamicIslandLowHealthWarning = false;
                    Config.save();
                }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 29, new SettingModule(UiText.t("伤害数值显示", "Damage Numbers"), UiText.t("显示目标血量变化", "Show target health changes"),
                new SettingToggle(() -> Config.damageNumbers, v -> { Config.damageNumbers = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(MiscPage.class, 2, new SettingModule(UiText.t("胜利音效", "Victory Sound"), UiText.t("获胜时播放自定义音效", "Play a custom sound after winning"),
                new SettingToggle(() -> Config.victorySound, v -> { Config.victorySound = v; Config.save(); }))
                .addSub(UiText.t("打开自定义音效文件夹", "Open custom sound folder"), "", new SettingButton(UiText.t("打开", "Open"), VictorySound::openSoundsFolder))));

        if (Config.restrictedFeaturesUnlocked()) {
            modules.add(ModuleBindingIds.preserve(ToolPage.class, 6, new SettingModule(UiText.t("火焰弹落点预测", "Fireball Landing Prediction"), UiText.t("显示大型火焰弹的轨迹和预测落点", "Show the trajectory and predicted impact point of large fireballs"),
                    new SettingToggle(() -> Config.fireballLandingPredict, v -> { Config.fireballLandingPredict = v; Config.save(); }))));

            modules.add(ModuleBindingIds.preserve(ToolPage.class, 6 + (Config.restrictedFeaturesUnlocked() ? 1 : 0), new SettingModule(UiText.t("投掷物与弓箭轨迹预测", "Projectile Trajectory Prediction"), UiText.t("预测弓箭、雪球和末影珍珠的飞行轨迹与落点", "Predict the flight trajectory and impact point of arrows, snowballs and ender pearls"),
                    new SettingToggle(() -> Config.projectileTrajectoryPredict, v -> { Config.projectileTrajectoryPredict = v; Config.save(); }))
                    .addSub(UiText.t("弓箭轨迹预测", "Bow Trajectory Prediction"), UiText.t("拉弓时显示箭矢的预测轨迹和落点", "Show the predicted arrow trajectory and impact point while drawing a bow"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictBow, v -> { Config.projectileTrajectoryPredictBow = v; Config.save(); }))
                    .addSub(UiText.t("雪球轨迹预测", "Snowball Trajectory Prediction"), UiText.t("显示雪球轨迹与落点", "Show the snowball trajectory and impact point"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictSnowball, v -> { Config.projectileTrajectoryPredictSnowball = v; Config.save(); }))
                    .addSub(UiText.t("末影珍珠轨迹预测", "Ender Pearl Trajectory Prediction"), UiText.t("显示末影珍珠轨迹与传送位置", "Show the ender-pearl trajectory and teleport destination"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictEnderPearl, v -> { Config.projectileTrajectoryPredictEnderPearl = v; Config.save(); }))
                    .addSub(UiText.t("落点方块预测", "Block Impact Prediction"), UiText.t("预测箭矢命中的方块", "Predict blocks hit by the arrow"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictBlock, v -> { Config.projectileTrajectoryPredictBlock = v; Config.save(); }))
                    .addSub(UiText.t("落点实体预测", "Entity Impact Prediction"), UiText.t("预测箭矢命中的实体", "Predict entities hit by the arrow"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictEntity, v -> { Config.projectileTrajectoryPredictEntity = v; Config.save(); }))
                    .addSubWhen(() -> Config.projectileTrajectoryPredictEntity, UiText.t("实体运动预测", "Entity Movement Prediction"), UiText.t("根据实体当前移动方向预测其落点位置", "Predict entity positions from their current movement direction"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictEntityMovement, v -> { Config.projectileTrajectoryPredictEntityMovement = v; Config.save(); }))
                    .addSub(UiText.t("其他玩家箭矢预警", "Other Player Arrow Warning"), UiText.t("仅显示预测会命中自己的其他玩家箭矢", "Only show other player arrows predicted to hit you"),
                            new SettingToggle(() -> Config.projectileTrajectoryPredictOtherPlayers, v -> { Config.projectileTrajectoryPredictOtherPlayers = v; Config.save(); }))));

            modules.add(ModuleBindingIds.preserve(ToolPage.class, 11 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("钓鱼竿辅助", "Fishing Rod Assist"), UiText.t("切换到钓鱼竿时自动右键使用", "Automatically right-click when switching to a fishing rod"),
                    new SettingToggle(() -> Config.fishingRodAssist, v -> { Config.fishingRodAssist = v; Config.save(); }))
                    .addSub(UiText.t("使用间隔(tick)", "Use Delay (tick)"), UiText.t("切换到钓鱼竿格子后等待多久再使用", "Ticks to wait after switching to a fishing rod slot before using it"),
                            new SettingSlider(0, 20, "%.0f", () -> (double) Config.fishingRodAssistUseDelay,
                                    v -> { Config.fishingRodAssistUseDelay = v.intValue(); Config.save(); }))
                    .visibleWhen(() -> Config.fullMode)));
        }

    }

    @Override public String getTitle() { return UiText.t("战斗设置", "Combat Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整战斗相关参数", "Adjust combat options"); }

    private static int hitColorArgb() {
        return ((Config.hitColorAlpha & 0xFF) << 24)
                | ((Config.hitColorRed & 0xFF) << 16)
                | ((Config.hitColorGreen & 0xFF) << 8)
                | (Config.hitColorBlue & 0xFF);
    }

    private static void setHitColorArgb(int color) {
        Config.hitColorAlpha = color >>> 24;
        Config.hitColorRed = (color >>> 16) & 0xFF;
        Config.hitColorGreen = (color >>> 8) & 0xFF;
        Config.hitColorBlue = color & 0xFF;
    }

}
