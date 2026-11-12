package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
import com.pvp_utils.client.AntiCheat;
import com.pvp_utils.client.skin.SkinManager;
import com.pvp_utils.client.skin.McbiServerGate;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

public class RenderPage extends BasePage {
    public RenderPage() {
        SettingModule skinModule = new SettingModule("破解SunWorld皮肤", "仅在 mcbi.top 服务器内可用",
                new SettingToggle(SkinManager::isDisplayEnabled, value -> {
                    Config.mcbiSkinDisplay = value && SkinManager.isAvailable();
                    Config.save();
                })).visibleWhen(SkinManager::isAvailable);
        addSkinOption(skinModule, SkinManager.NETHERITE_SWORD, "下界合金剑皮肤");
        addSkinOption(skinModule, SkinManager.NETHERITE_AXE, "下界合金斧皮肤");
        addSkinOption(skinModule, SkinManager.DIAMOND_SWORD, "钻石剑皮肤");
        addSkinOption(skinModule, SkinManager.ELYTRA, "鞘翅皮肤");
        addSkinOption(skinModule, SkinManager.FISHING_ROD, "钓鱼竿皮肤");
        addSkinOption(skinModule, SkinManager.SHIELD, "盾牌皮肤");
        addSkinOption(skinModule, SkinManager.COD, "鳕鱼皮肤");
        addSkinOption(skinModule, SkinManager.ARROW, "箭矢皮肤");
        addSkinOption(skinModule, SkinManager.PAPER, "纸张皮肤");
        addSkinOption(skinModule, SkinManager.SHEARS, "剪刀皮肤");
        addSkinOption(skinModule, SkinManager.HORSE_ARMOR, "马铠皮肤");
        addSkinOption(skinModule, SkinManager.COSMETIC, "饰品显示");
        modules.add(ModuleBindingIds.preserve(RenderPage.class, 0, skinModule));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 1, new SettingModule("SunWorld头衔破解", "仅在 mcbi.top 服务器内可用",
                new SettingToggle(() -> Config.sunWorldRankSpoof && McbiServerGate.isAvailable(), value -> {
                    if (McbiServerGate.isAvailable()) {
                        Config.sunWorldRankSpoof = value;
                        Config.save();
                    }
                }))
                .addSub("VIP 1", "", new SettingToggle(() -> Config.sunWorldRankVip1, v -> { Config.sunWorldRankVip1 = v; Config.save(); }))
                .addSub("VIP 2", "", new SettingToggle(() -> Config.sunWorldRankVip2, v -> { Config.sunWorldRankVip2 = v; Config.save(); }))
                .addSub("VIP 3", "", new SettingToggle(() -> Config.sunWorldRankVip3, v -> { Config.sunWorldRankVip3 = v; Config.save(); }))
                .addSub("VIP 4", "", new SettingToggle(() -> Config.sunWorldRankVip4, v -> { Config.sunWorldRankVip4 = v; Config.save(); }))
                .addSub("SVIP", "", new SettingToggle(() -> Config.sunWorldRankSvip, v -> { Config.sunWorldRankSvip = v; Config.save(); }))
                .addSub("Creator", "", new SettingToggle(() -> Config.sunWorldRankCreator, v -> { Config.sunWorldRankCreator = v; Config.save(); }))
                .addSub("Guide", "", new SettingToggle(() -> Config.sunWorldRankGuide, v -> { Config.sunWorldRankGuide = v; Config.save(); }))
                .addSub("Admin", "", new SettingToggle(() -> Config.sunWorldRankAdmin, v -> { Config.sunWorldRankAdmin = v; Config.save(); }))
                .visibleWhen(McbiServerGate::isAvailable)));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 2, new SettingModule(UiText.t("1.7 动画", "1.7 Animations"), UiText.t("为部分物品使用 1.7 风格动画", "Use 1.7-style animations for selected items"),
                new SettingToggle(() -> Config.legacy17Animations, v -> { Config.legacy17Animations = v; Config.save(); }))
                .addSub(UiText.t("使用物品挥手偏移", "Item Use Swing Offset"), UiText.t("使用物品时应用 1.7 风格的挥手偏移", "Apply the 1.7 swing offset while using an item"),
                        new SettingToggle(() -> Config.legacy17UseSwing, v -> { Config.legacy17UseSwing = v; Config.save(); }))
                .addSub(UiText.t("第一人称钓鱼竿位置", "First-Person Fishing Rod"), UiText.t("将第一人称钓鱼竿调整为 1.7 风格", "Adjust the first-person fishing rod to the 1.7 style"),
                        new SettingToggle(() -> Config.legacy17FishingRod, v -> { Config.legacy17FishingRod = v; Config.save(); }))
                .addSub(UiText.t("头部旋转", "Head Rotation"), UiText.t("使用 1.7 风格的头部旋转", "Use 1.7-style head rotation"),
                        new SettingToggle(() -> Config.legacy17HeadRotation, v -> { Config.legacy17HeadRotation = v; Config.save(); }))
                .addSub(UiText.t("受伤倾斜", "Hurt Tilt"), UiText.t("使用 1.7 风格的受伤倾斜时机", "Use 1.7-style hurt tilt timing"),
                        new SettingToggle(() -> Config.legacy17HurtTilt, v -> { Config.legacy17HurtTilt = v; Config.save(); }))
                .addSub(UiText.t("物品拾取位置", "Item Pickup Position"), UiText.t("使用 1.7 风格的物品拾取终点", "Use the 1.7 item pickup destination"),
                        new SettingToggle(() -> Config.legacy17ItemPickup, v -> { Config.legacy17ItemPickup = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 3, new SettingModule(UiText.t("UI 编辑", "UI Editor"), UiText.t("调整 HUD 位置和大小", "Adjust HUD positions and sizes"),
                new SettingToggle(() -> false, v -> Minecraft.getInstance().setScreen(new ChatScreen("", false))))
                .addSub(UiText.t("在聊天框中快速启用", "Quick Enable in Chat"), UiText.t("打开聊天框时自动启用 HUD 拖动编辑", "Automatically enable HUD drag editing when opening chat"),
                        new SettingToggle(() -> Config.chatHudEditQuickEnable, v -> { Config.chatHudEditQuickEnable = v; Config.save(); }))));

        SettingModule swordBlockModule = new SettingModule(UiText.t("防砍动画", "Sword Blocking Animation"), UiText.t("模拟旧版格挡动画效果", "Simulate the old blocking animation"),
                new SettingToggle(() -> Config.swordBlock, v -> { Config.swordBlock = v; Config.save(); }))
                .addSub(UiText.t("动画模式", "Animation Mode"), "",
                        new SettingCycle(List.of("1.7", "Push", "1.7+", UiText.t("现代", "Modern")),
                                () -> switch (Config.animationMode) { case MODE_1_7 -> 0; case MODE_PUSH -> 1; case MODE_1_7_PLUS -> 2; case MODE_NEW -> 3; },
                                i -> { Config.animationMode = switch (i) { case 1 -> Config.AnimMode.MODE_PUSH; case 2 -> Config.AnimMode.MODE_1_7_PLUS; case 3 -> Config.AnimMode.MODE_NEW; default -> Config.AnimMode.MODE_1_7; }; Config.save(); }))
                .addSub(UiText.t("偏移 X", "Offset X"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetX, v -> { Config.offsetX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("偏移 Y", "Offset Y"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetY, v -> { Config.offsetY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("偏移 Z", "Offset Z"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetZ, v -> { Config.offsetZ = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), "",
                        new SettingSlider(0.0, 4.0, "%.2f", () -> (double) Config.animSpeed, v -> { Config.animSpeed = v.floatValue(); Config.save(); }));
        swordBlockModule.visibleWhen(() -> !SkinManager.hasSwordSkinActive());
        modules.add(ModuleBindingIds.preserve(RenderPage.class, 4, swordBlockModule));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 5, new SettingModule(UiText.t("去除攻击冷却动画", "Remove Attack Cooldown Animation"), UiText.t("去除武器挥动后高版本额外的抬手动画", "Remove the extra hand raise after weapon swings"),
                new SettingToggle(() -> Config.noAttackCooldownAnimation, v -> { Config.noAttackCooldownAnimation = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 6, new SettingModule(UiText.t("运动相机", "Motion camera"), UiText.t("平滑第三人称相机运动", "Smooth third-person camera movement"),
                new SettingToggle(() -> Config.motionCamera, v -> { Config.setMotionCamera(v); Config.save(); }))
                .addSub(UiText.t("跟随速度", "Follow Speed"), "",
                        new SettingSlider(0.0, 1.0, "%.2f", () -> (double) Config.motionCameraFollowSpeed, v -> { Config.motionCameraFollowSpeed = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("相机距离", "Camera Distance"), "",
                        new SettingSlider(1.0, 8.0, "%.1f", () -> (double) Config.motionCameraDistance, v -> { Config.motionCameraDistance = v.floatValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 9, new SettingModule(UiText.t("自定义方块轮廓", "Custom Block Outline"), UiText.t("自定义准星指向方块的轮廓样式", "Customize the outline of the block under the crosshair"),
                new SettingToggle(() -> Config.customBlockOutline, v -> { Config.customBlockOutline = v; Config.save(); }))
                .addSub(UiText.t("边框粗细", "Border Width"), "",
                        new SettingSlider(1.0, 4.0, "%.1f", () -> (double) Config.customBlockOutlineWidth, v -> { Config.customBlockOutlineWidth = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("边框颜色", "Border Color"), "",
                        new SettingColorPreview(RenderPage::customBlockOutlineArgb, RenderPage::setCustomBlockOutlineArgb, true, Config::save))
                .addSub(UiText.t("填充", "Fill"), "",
                        new SettingToggle(() -> Config.customBlockOutlineFill, v -> { Config.customBlockOutlineFill = v; Config.save(); }))
                .addSubWhen(() -> Config.customBlockOutlineFill, UiText.t("填充颜色", "Fill Color"), "",
                        new SettingColorPreview(RenderPage::customBlockOutlineFillArgb, RenderPage::setCustomBlockOutlineFillArgb, true, Config::save))
                .addSub(UiText.t("动画改进", "Animation Improvements"), UiText.t("为轮廓出现、退出和目标移动添加缓动动画", "Add eased animations for appearance, exit, and target movement"),
                        new SettingToggle(() -> Config.customBlockOutlineAnimation, v -> { Config.customBlockOutlineAnimation = v; Config.save(); }))
                .addSubWhen(() -> Config.customBlockOutlineAnimation, UiText.t("进入/退出速度", "Enter/Exit Speed"), "",
                        new SettingSlider(1.0, 20.0, "%.1f", () -> (double) Config.customBlockOutlineAnimationSpeed, v -> { Config.customBlockOutlineAnimationSpeed = v.floatValue(); Config.save(); }))
                .addSubWhen(() -> Config.customBlockOutlineAnimation, UiText.t("移动速度", "Move Speed"), "",
                        new SettingSlider(1.0, 20.0, "%.1f", () -> (double) Config.customBlockOutlineMoveSpeed, v -> { Config.customBlockOutlineMoveSpeed = v.floatValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 10, new SettingModule(UiText.t("彩虹附魔光效", "Rainbow Enchantment Glint"), UiText.t("将附魔光效更改为彩虹色", "Change the enchantment glint to rainbow colors"),
                new SettingToggle(() -> Config.customEnchantmentGlint, v -> { Config.customEnchantmentGlint = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 12, new SettingModule(UiText.t("挖掘状态显示", "Digging Status"), UiText.t("在准星下方显示当前挖掘进度和预计剩余时间", "Show current digging progress and estimated remaining time under the crosshair"),
                new SettingToggle(() -> Config.diggingStatus, v -> { Config.diggingStatus = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 14, new SettingModule(UiText.t("歌词显示", "Lyrics Display"), UiText.t("显示当前播放音乐的歌词", "Show lyrics for the currently playing music"),
                new SettingToggle(() -> Config.lyricsDisplay, v -> { Config.lyricsDisplay = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 15, new SettingModule(UiText.t("音乐信息显示", "Music Info HUD"), UiText.t("显示当前播放的网易云音乐信息", "Show current Netease Music playback information"),
                new SettingToggle(() -> Config.musicInfoHud, v -> { Config.musicInfoHud = v; Config.save(); }))
                .addSub(UiText.t("封面圆角", "Rounded Cover"), "",
                        new SettingToggle(() -> Config.musicInfoHudCoverRounded, v -> { Config.musicInfoHudCoverRounded = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 36, new SettingModule(UiText.t("击杀横幅", "Kill Banner"), UiText.t("以 CS2 风格还原回合胜利横幅动画", "Recreate the CS2 round-win banner animation"),
                new SettingToggle(() -> Config.killBanner, v -> { Config.killBanner = v; Config.save(); }))
                .addSub(UiText.t("显示时长", "Duration"), "",
                        new SettingSlider(2.0, 10.0, "%.1fs", () -> (double) Config.killBannerDuration, v -> { Config.killBannerDuration = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("胜利时触发", "Trigger On Victory"), UiText.t("仅在检测到胜利提示时播放", "Only play when a victory message is detected"),
                        new SettingToggle(() -> Config.killBannerOnVictory, v -> { Config.killBannerOnVictory = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 16, new SettingModule(UiText.t("物品使用状态显示", "Item Use Status"), UiText.t("在屏幕上显示当前物品使用进度或状态", "Show current item use progress or status on the screen"),
                new SettingToggle(() -> Config.itemUseStatus, v -> { Config.setItemUseStatus(v); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 17, new SettingModule(UiText.t("功能列表", "Arraylist"), UiText.t("显示已启用的功能", "Show enabled modules"),
                new SettingToggle(() -> Config.arraylist, v -> { Config.arraylist = v; Config.save(); }))
                .addSub(UiText.t("文字颜色", "Text Color"), "",
                        new SettingColorPreview(RenderPage::arraylistColorArgb, RenderPage::setArraylistColorArgb, false, Config::save))
                .addSub(UiText.t("渐变", "Gradient"), "",
                        new SettingToggle(() -> Config.arraylistGradient, v -> { Config.arraylistGradient = v; Config.save(); }))
                .addSubWhen(() -> Config.arraylistGradient, UiText.t("渐变颜色", "Gradient Color"), "",
                        new SettingColorPreview(RenderPage::arraylistGradientColorArgb, RenderPage::setArraylistGradientColorArgb, false, Config::save))
                .addSubWhen(() -> Config.arraylistGradient, UiText.t("渐变速度", "Gradient Speed"), "",
                        new SettingSlider(0.0, 5.0, "%.1fx", () -> (double) Config.arraylistGradientSpeed, v -> { Config.arraylistGradientSpeed = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("边框", "Border"), "",
                        new SettingToggle(() -> Config.arraylistBorder, v -> { Config.arraylistBorder = v; Config.save(); }))
                .addSubWhen(() -> Config.arraylistBorder, UiText.t("边框粗细", "Border Width"), "",
                        new SettingSlider(1.0, 4.0, "%.1f", () -> (double) Config.arraylistBorderWidth, v -> { Config.arraylistBorderWidth = v.floatValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 18, new SettingModule(UiText.t("灵动岛", "Dynamic Island"), UiText.t("集中显示状态、通知与音乐", "Display status, notifications, and music"),
                new SettingToggle(() -> Config.dynamicIsland, v -> { Config.setDynamicIsland(v); Config.save(); }))
                .addSub(UiText.t("形态", "Shape"), "",
                        new SettingCycle(List.of(UiText.t("极简胶囊", "Minimal Capsule"), UiText.t("分体悬浮岛", "Split Floating Island")),
                                () -> Config.dynamicIslandShape == Config.DynamicIslandShape.SPLIT ? 1 : 0,
                                i -> { Config.dynamicIslandShape = i == 1 ? Config.DynamicIslandShape.SPLIT : Config.DynamicIslandShape.CAPSULE; Config.save(); }))
                .addSubWhen(() -> Config.dynamicIslandShape == Config.DynamicIslandShape.CAPSULE,
                        UiText.t("双层模式", "Two-row Mode"), UiText.t("显示更多信息", "Show more information"),
                        new SettingToggle(() -> Config.dynamicIslandDoubleRow, v -> { Config.dynamicIslandDoubleRow = v; Config.save(); }))
                .addSub(UiText.t("背景模式", "Background"), "",
                        new SettingCycle(List.of("Blur", UiText.t("纯色", "Solid")),
                                () -> Config.dynamicIslandBackground == Config.DynamicIslandBackground.SOLID ? 1 : 0,
                                i -> { Config.dynamicIslandBackground = i == 1 ? Config.DynamicIslandBackground.SOLID : Config.DynamicIslandBackground.BLUR; Config.save(); }))
                .addSub(UiText.t("背景颜色", "Background Color"), "",
                        new SettingColorPreview(() -> Config.dynamicIslandColor, v -> Config.dynamicIslandColor = v, true, Config::save))
                .addSub(UiText.t("阴影", "Shadow"), "",
                        new SettingToggle(() -> Config.dynamicIslandShadow, v -> { Config.dynamicIslandShadow = v; Config.save(); }))
                .addSubWhen(() -> Config.dynamicIslandShadow, UiText.t("阴影强度", "Shadow Strength"), "",
                        new SettingSlider(0, 1, "%.2f", () -> (double) Config.dynamicIslandShadowStrength,
                                v -> { Config.dynamicIslandShadowStrength = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("方块数量显示", "Block Count Display"), "",
                        new SettingToggle(() -> Config.dynamicIslandBlockCount, v -> {
                            Config.setDynamicIslandBlockCount(v);
                            Config.save();
                        }))
                .addSub("Scaffold Disabled People", "",
                        new SettingToggle(() -> AntiCheat.isDetected() && Config.dynamicIslandBlockCountAltIcon, v -> {
                            Config.dynamicIslandBlockCountAltIcon = v && AntiCheat.isDetected();
                            Config.save();
                        }),
                        () -> Config.dynamicIslandBlockCount && AntiCheat.isDetected())
                .addSub(UiText.t("物品使用状态", "Item Use Status"), "",
                        new SettingToggle(() -> Config.dynamicIslandItemUseStatus, v -> {
                            Config.setDynamicIslandItemUseStatus(v);
                            Config.save();
                        }))
                .addSub(UiText.t("音乐上岛", "Music on Island"), "",
                        new SettingToggle(() -> Config.dynamicIslandLyrics, v -> {
                            Config.dynamicIslandLyrics = v;
                            Config.save();
                        }))
                .addSubWhen(() -> Config.dynamicIslandLyrics, UiText.t("音乐显示方式", "Music Display"), "",
                        new SettingCycle(List.of(UiText.t("歌词上岛", "Lyrics on Island"), UiText.t("始终缩小", "Always Minimized")),
                                () -> Config.dynamicIslandMusicMode == Config.DynamicIslandMusicMode.MINIMIZED ? 1 : 0,
                                i -> { Config.dynamicIslandMusicMode = i == 1 ? Config.DynamicIslandMusicMode.MINIMIZED : Config.DynamicIslandMusicMode.LYRICS; Config.save(); }))
                .addSub(UiText.t("低血量提示", "Low Health Warning"), "",
                        new SettingToggle(() -> Config.dynamicIslandLowHealthWarning, v -> {
                            Config.dynamicIslandLowHealthWarning = v;
                            if (v) Config.lowHealthNotify = false;
                            Config.save();
                        }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 19, new SettingModule(UiText.t("物品物理掉落", "Item Physics"), UiText.t("调整掉落物的旋转和落地表现", "Adjust dropped-item rotation and landing"),
                new SettingToggle(() -> Config.itemPhysics, v -> {
                    Config.itemPhysics = v;
                    if (v) Config.item2DRender = false;
                    Config.save();
                }))
                .addSub(UiText.t("旋转速度", "Rotation Speed"), "",
                        new SettingSlider(0.0, 3.0, "%.1fx", () -> (double) Config.itemPhysicsRotationSpeed,
                                v -> { Config.itemPhysicsRotationSpeed = v.floatValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 20, new SettingModule(UiText.t("掉落物 2D 渲染", "Dropped Item 2D Render"), UiText.t("使用旧版二维掉落物样式", "Use the legacy 2D dropped-item style"),
                new SettingToggle(() -> Config.item2DRender, v -> {
                    Config.item2DRender = v;
                    if (v) Config.itemPhysics = false;
                    Config.save();
                }))));

        List<String> armorLayouts = List.of(UiText.t("分离式", "Separated"),
                UiText.t("竖向", "Vertical"), UiText.t("横向", "Horizontal"));
        List<String> liteArmorLayouts = armorLayouts.subList(1, 3);
        modules.add(ModuleBindingIds.preserve(RenderPage.class, 21, new SettingModule(
                UiText.t("护甲信息显示", "Armor Information"),
                UiText.t("显示装备与剩余耐久", "Show equipped armor and remaining durability"),
                new SettingToggle(() -> Config.armorHud, v -> { Config.armorHud = v; Config.save(); }))
                .addSub(UiText.t("布局", "Layout"), "",
                        new SettingCycle(() -> Config.armorHudMode != Config.ArmorHudMode.LITE ? armorLayouts : liteArmorLayouts,
                                () -> Config.armorHudMode != Config.ArmorHudMode.LITE ? switch (Config.armorHudLayout) {
                                    case SEPARATED -> 0;
                                    case VERTICAL -> 1;
                                    case HORIZONTAL -> 2;
                                } : Config.armorHudLayout == Config.ArmorHudLayout.VERTICAL ? 0 : 1,
                                i -> {
                                    if (Config.armorHudMode != Config.ArmorHudMode.LITE) {
                                        Config.armorHudLayout = switch (i) {
                                            case 1 -> Config.ArmorHudLayout.VERTICAL;
                                            case 2 -> Config.ArmorHudLayout.HORIZONTAL;
                                            default -> Config.ArmorHudLayout.SEPARATED;
                                        };
                                    } else {
                                        Config.armorHudLayout = switch (i) {
                                            case 0 -> Config.ArmorHudLayout.VERTICAL;
                                            default -> Config.ArmorHudLayout.HORIZONTAL;
                                        };
                                    }
                                    Config.save();
                                }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 22, new SettingModule(UiText.t("装备透明度调整", "Armor Transparency"), UiText.t("单独调整四个装备槽位的盔甲透明度", "Adjust armor transparency for each equipment slot"),
                new SettingToggle(() -> Config.armorTransparency, v -> { Config.armorTransparency = v; Config.save(); }))
                .addSub(UiText.t("头盔透明度", "Helmet Transparency"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.armorTransparencyHead, v -> { Config.armorTransparencyHead = clampPercent(v); Config.save(); }))
                .addSub(UiText.t("胸甲透明度", "Chestplate Transparency"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.armorTransparencyChest, v -> { Config.armorTransparencyChest = clampPercent(v); Config.save(); }))
                .addSub(UiText.t("护腿透明度", "Leggings Transparency"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.armorTransparencyLegs, v -> { Config.armorTransparencyLegs = clampPercent(v); Config.save(); }))
                .addSub(UiText.t("靴子透明度", "Boots Transparency"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.armorTransparencyFeet, v -> { Config.armorTransparencyFeet = clampPercent(v); Config.save(); }))
                .addSub(UiText.t("战斗中显示", "Show In Combat"), UiText.t("最近参与战斗时临时恢复盔甲显示", "Temporarily restore armor visibility while recently in combat"),
                        new SettingToggle(() -> Config.armorTransparencyShowInCombat, v -> { Config.armorTransparencyShowInCombat = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 23, new SettingModule(UiText.t("药水状态", "Potion Status"), UiText.t("显示药水效果与剩余时间", "Show active effects and remaining duration"),
                new SettingToggle(() -> Config.potionStatus, v -> { Config.potionStatus = v; Config.save(); }))
                .addSub(UiText.t("显示背景", "Show Background"), "",
                        new SettingToggle(() -> Config.potionStatusBackground, v -> { Config.potionStatusBackground = v; Config.save(); }))
                .addSub(UiText.t("倒计时数字", "Countdown Text"), UiText.t("关闭后只显示效果名称", "When disabled, only the effect name is shown"),
                        new SettingToggle(() -> Config.potionStatusCountdown, v -> { Config.potionStatusCountdown = v; Config.save(); }))
                .addSub(UiText.t("屏蔽原版显示", "Hide Vanilla Effects"), UiText.t("启用后会屏蔽右上角原版药水效果显示", "Hide the vanilla potion effect UI in the top-right while this widget is active"),
                        new SettingToggle(() -> Config.potionStatusHideVanilla, v -> { Config.potionStatusHideVanilla = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 24, new SettingModule(UiText.t("潜行动画调整", "Sneak Animation Adjustment"), UiText.t("调整潜行视角下降效果", "Adjust sneak camera drop effect"),
                new SettingToggle(() -> Config.noSneakAnimation, v -> { Config.noSneakAnimation = v; Config.save(); }))
                .addSub(UiText.t("下降幅度", "Drop Amount"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.sneakDropScale * 100.0, v -> { Config.sneakDropScale = (v.floatValue() / 100.0f); Config.save(); }))
                .addSub(UiText.t("过渡速度", "Transition Speed"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.sneakAnimationSpeed * 100.0, v -> { Config.sneakAnimationSpeed = (v.floatValue() / 100.0f); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 25, new SettingModule(UiText.t("伽马覆写", "Gamma Override"), UiText.t("强制使用自定义亮度值", "Force a custom brightness value"),
                new SettingToggle(() -> Config.gammaOverride, v -> { Config.gammaOverride = v; Config.save(); }))
                .addSub(UiText.t("伽马值", "Gamma Value"), "",
                        new SettingSlider(0.0, 15.0, "%.1f", () -> Config.gammaValue, v -> { Config.gammaValue = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 26, new SettingModule(UiText.t("动态模糊", "Dynamic Motion Blur"), UiText.t("根据相机运动生成速度模糊效果", "Apply velocity blur based on camera motion"),
                new SettingToggle(() -> Config.dynamicMotionBlur, v -> { Config.dynamicMotionBlur = v; Config.save(); }))
                .addSub(UiText.t("算法", "Algorithm"), "",
                        new SettingCycle(List.of("Velocity", "Frame", "Hybrid", "Max", "Mix"),
                                () -> Config.motionBlurAlgorithm.ordinal(),
                                i -> { Config.motionBlurAlgorithm = Config.MotionBlurAlgorithm.values()[i % Config.MotionBlurAlgorithm.values().length]; Config.save(); }))
                .addSub(UiText.t("强度", "Strength"), "",
                        new SettingSlider(0.0, 300.0, "%.0f%%", () -> (double) Config.dynamicMotionBlurStrength * 100.0, v -> { Config.dynamicMotionBlurStrength = v.floatValue() / 100.0f; Config.save(); }))
                .addSub(UiText.t("刷新率缩放", "Refresh Rate Scaling"), UiText.t("高 FPS 时按显示器刷新率自动增强采样", "Scale blur samples against display refresh rate at high FPS"),
                        new SettingToggle(() -> Config.dynamicMotionBlurRefreshRateScaling, v -> { Config.dynamicMotionBlurRefreshRateScaling = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 27, new SettingModule(UiText.t("渲染控制", "Render Control"), UiText.t("选择性关闭游戏内渲染效果", "Selectively disable in-game rendering effects"), null)
                .addSub(UiText.t("告示牌文本", "Sign Text"), UiText.t("隐藏告示牌和悬挂告示牌文字", "Hide text on signs and hanging signs"),
                        new SettingToggle(() -> Config.hideSignText, v -> { Config.hideSignText = v; Config.save(); }))
                .addSub(UiText.t("附魔台悬浮书", "Enchanting Table Book"), UiText.t("隐藏附魔台上方悬浮的书", "Hide the floating book above enchanting tables"),
                        new SettingToggle(() -> Config.hideEnchantTableBook, v -> { Config.hideEnchantTableBook = v; Config.save(); }))
                .addSub(UiText.t("火焰效果", "Fire Overlay"), UiText.t("隐藏第一人称着火遮挡效果", "Hide the first-person fire overlay"),
                        new SettingToggle(() -> Config.hideFireOverlay, v -> { Config.hideFireOverlay = v; Config.save(); }))
                .addSub(UiText.t("屏幕暗角", "Vignette"), UiText.t("隐藏屏幕边缘暗角效果", "Hide the screen edge vignette"),
                        new SettingToggle(() -> Config.hideVignette, v -> { Config.hideVignette = v; Config.save(); }))
                .addSub(UiText.t("视角场迷雾", "View Fog"), UiText.t("隐藏视角中的世界迷雾效果", "Hide world fog in the view"),
                        new SettingToggle(() -> Config.hideFog, v -> { Config.hideFog = v; Config.save(); }))
                .addSub(UiText.t("图腾动画", "Totem Animation"), UiText.t("隐藏图腾触发时的全屏动画效果", "Hide the full-screen animation when a totem triggers"),
                        new SettingToggle(() -> Config.hideTotemAnimation, v -> { Config.hideTotemAnimation = v; Config.save(); }))
                .addSub(UiText.t("爆炸粒子", "Explosion Particles"), UiText.t("隐藏爆炸产生的粒子效果", "Hide particles produced by explosions"),
                        new SettingToggle(() -> Config.hideExplosionParticles, v -> { Config.hideExplosionParticles = v; Config.save(); }))
                .addSub(UiText.t("雨滴粒子", "Rain Particles"), UiText.t("隐藏雨天时的雨滴效果", "Hide raindrop effects during rainy weather"),
                        new SettingToggle(() -> Config.hideRainParticles, v -> { Config.hideRainParticles = v; Config.save(); }))
                .addSub(UiText.t("隐藏Boss血条", "Boss Bar"), UiText.t("隐藏屏幕上方的 Boss 血条", "Hide the boss health bar at the top of the screen"),
                        new SettingToggle(() -> Config.hideBossBar, v -> { Config.hideBossBar = v; Config.save(); }))
                .addSub(UiText.t("受伤抖动", "Hurt Shake"), UiText.t("隐藏受到伤害时的视角抖动", "Disable camera shake when hurt"),
                        new SettingToggle(() -> Config.hideHurtShake, v -> { Config.hideHurtShake = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 30, new SettingModule(UiText.t("目标信息显示", "Target Information"), UiText.t("显示目标名称与生命值", "Show the target name and health"),
                new SettingToggle(() -> Config.targetHud, v -> { Config.targetHud = v; Config.save(); }))
                .addSub(UiText.t("攻击距离", "Attack Reach"), UiText.t("命中时显示目标距离", "Show target distance on hit"),
                        new SettingToggle(() -> Config.attackReachDisplay, v -> { Config.attackReachDisplay = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 31, new SettingModule(UiText.t("按键显示", "Keystrokes"), UiText.t("显示 WASD 和鼠标按键状态", "Show WASD and mouse button states"),
                new SettingToggle(() -> Config.keystrokes, v -> { Config.keystrokes = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 32, new SettingModule(UiText.t("Ping 显示", "Ping HUD"), UiText.t("显示服务器延迟", "Show server latency"),
                new SettingToggle(() -> Config.pingHud, v -> { Config.pingHud = v; Config.save(); }))
                .addSub(UiText.t("显示背景", "Show Background"), "",
                        new SettingToggle(() -> Config.pingHudBackground, v -> { Config.pingHudBackground = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 33, new SettingModule(UiText.t("TPS 显示", "TPS HUD"), UiText.t("估算当前服务器的 TPS", "Estimate the server's current TPS"),
                new SettingToggle(() -> Config.tpsHud, v -> { Config.tpsHud = v; Config.save(); }))
                .addSub(UiText.t("显示背景", "Show Background"), "",
                        new SettingToggle(() -> Config.tpsHudBackground, v -> { Config.tpsHudBackground = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 34, new SettingModule(UiText.t("现实时钟显示", "Real-Time Clock"), UiText.t("显示本地时间", "Show local time"),
                new SettingToggle(() -> Config.clockHud, v -> { Config.clockHud = v; Config.save(); }))
                .addSub(UiText.t("显示背景", "Show Background"), "",
                        new SettingToggle(() -> Config.clockHudBackground, v -> { Config.clockHudBackground = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 35, new SettingModule(UiText.t("名称标签", "Name Tags"), UiText.t("调整原版实体名称标签显示效果", "Adjust vanilla entity name tag rendering"),
                new SettingToggle(() -> Config.nameTag, v -> { Config.nameTag = v; Config.save(); }))
                .addSub(UiText.t("缩放", "Scale"), "",
                        new SettingSlider(50.0, 300.0, "%.0f%%", () -> (double) Config.nameTagScale * 100.0, v -> { Config.nameTagScale = v.floatValue() / 100.0f; Config.save(); }))
                .addSub(UiText.t("动态缩放", "Dynamic Scale"), UiText.t("按距离调整名称标签大小", "Scale name tags by distance"),
                        new SettingToggle(() -> Config.nameTagDynamicScale, v -> { Config.nameTagDynamicScale = v; Config.save(); }))
                .addSub(UiText.t("仅玩家", "Only Player"), UiText.t("仅调整玩家标签并过滤可识别的 NPC", "Adjust player labels and filter identifiable NPCs"),
                        new SettingToggle(() -> Config.nameTagOnlyPlayer, v -> { Config.nameTagOnlyPlayer = v; Config.save(); }))
                .addSub(UiText.t("显示自身名称标签", "Show Own Name Tag"), UiText.t("在第三人称视角显示自己头顶的名称标签", "Show your own overhead name tag in third-person view"),
                        new SettingToggle(() -> Config.nameTagShowSelf, v -> { Config.nameTagShowSelf = v; Config.save(); }))));
        SettingModule target = HudAppearanceSettings.module(modules, "目标信息显示", "Target Information");
        HudAppearanceSettings.style(target, List.of(Config.TargetHudMode.LITE, Config.TargetHudMode.BLUR),
                () -> Config.targetHudMode, value -> Config.targetHudMode = value);
        HudAppearanceSettings.add(target, () -> Config.targetAppearance,
                () -> Config.targetHudMode != Config.TargetHudMode.LITE);
        SettingModule armor = HudAppearanceSettings.module(modules, "护甲信息显示", "Armor Information");
        HudAppearanceSettings.style(armor, List.of(Config.ArmorHudMode.LITE, Config.ArmorHudMode.BLUR),
                () -> Config.armorHudMode, value -> {
                    Config.armorHudMode = value;
                    if (value == Config.ArmorHudMode.LITE && Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED) {
                        Config.armorHudLayout = Config.ArmorHudLayout.HORIZONTAL;
                    }
                });
        HudAppearanceSettings.add(armor, () -> Config.armorAppearance,
                () -> Config.armorHudMode != Config.ArmorHudMode.LITE);
        SettingModule music = HudAppearanceSettings.module(modules, "音乐信息显示", "Music Info HUD");
        HudAppearanceSettings.style(music, List.of(Config.MusicInfoHudMode.LITE, Config.MusicInfoHudMode.BLUR),
                () -> Config.musicInfoHudMode, value -> Config.musicInfoHudMode = value);
        HudAppearanceSettings.add(music, () -> Config.musicInfoAppearance,
                () -> true);
        SettingModule ping = HudAppearanceSettings.module(modules, "Ping 显示", "Ping HUD");
        HudAppearanceSettings.style(ping, List.of(Config.HudStyle.LITE, Config.HudStyle.BLUR),
                () -> Config.pingHudStyle, value -> Config.pingHudStyle = value);
        HudAppearanceSettings.add(ping, () -> Config.pingAppearance,
                () -> Config.pingHudStyle != Config.HudStyle.LITE);
        SettingModule tps = HudAppearanceSettings.module(modules, "TPS 显示", "TPS HUD");
        HudAppearanceSettings.style(tps, List.of(Config.HudStyle.LITE, Config.HudStyle.BLUR),
                () -> Config.tpsHudStyle, value -> Config.tpsHudStyle = value);
        HudAppearanceSettings.add(tps, () -> Config.tpsAppearance,
                () -> Config.tpsHudStyle != Config.HudStyle.LITE);
        SettingModule clock = HudAppearanceSettings.module(modules, "现实时钟显示", "Real-Time Clock");
        HudAppearanceSettings.style(clock, List.of(Config.HudStyle.LITE, Config.HudStyle.BLUR, Config.HudStyle.MATERIAL),
                () -> Config.clockHudStyle, value -> Config.clockHudStyle = value);
        clock.addSubWhen(() -> Config.clockHudStyle == Config.HudStyle.MATERIAL && Config.clockHudBackground,
                        UiText.t("不透明背景", "Opaque Background"), "",
                        new SettingToggle(() -> Config.clockHudOpaqueBackground,
                                value -> { Config.clockHudOpaqueBackground = value; Config.save(); }))
                .addSubChildWhen(() -> Config.clockHudStyle == Config.HudStyle.MATERIAL
                                && Config.clockHudBackground && Config.clockHudOpaqueBackground,
                        UiText.t("背景不透明度", "Background Opacity"), "",
                        new SettingSlider(0, 100, "%.0f%%", () -> (double) Config.clockHudBackgroundOpacity * 100,
                                value -> { Config.clockHudBackgroundOpacity = value.floatValue() / 100f; Config.save(); }))
                .addSubWhen(() -> Config.clockHudStyle == Config.HudStyle.MATERIAL && Config.clockHudBackground,
                        "Blur", "",
                        new SettingToggle(() -> Config.clockHudBlurBackground,
                                value -> { Config.clockHudBlurBackground = value; Config.save(); }));
        HudAppearanceSettings.add(clock, () -> Config.clockAppearance, () -> true,
                () -> Config.clockHudStyle != Config.HudStyle.MATERIAL);
        SettingModule potion = HudAppearanceSettings.module(modules, "药水状态", "Potion Status");
        HudAppearanceSettings.style(potion, List.of(Config.HudStyle.LITE, Config.HudStyle.BLUR),
                () -> Config.potionStatusStyle, value -> Config.potionStatusStyle = value);
        HudAppearanceSettings.shadow(potion, () -> Config.potionAppearance,
                () -> Config.potionStatusStyle != Config.HudStyle.LITE);
        SettingModule keys = HudAppearanceSettings.module(modules, "按键显示", "Keystrokes");
        HudAppearanceSettings.style(keys, List.of(Config.KeystrokesMode.LITE, Config.KeystrokesMode.BLUR),
                () -> Config.keystrokesMode, value -> Config.keystrokesMode = value);
        HudAppearanceSettings.add(keys, () -> Config.keystrokesAppearance,
                () -> Config.keystrokesMode != Config.KeystrokesMode.LITE);
        SettingModule itemUse = HudAppearanceSettings.module(modules, "物品使用状态显示", "Item Use Status");
        HudAppearanceSettings.style(itemUse, List.of(Config.ItemUseStatusMode.LITE, Config.ItemUseStatusMode.BLUR),
                () -> Config.itemUseStatusMode, value -> Config.itemUseStatusMode = value);
        HudAppearanceSettings.add(itemUse, () -> Config.itemUseAppearance,
                () -> Config.itemUseStatusMode != Config.ItemUseStatusMode.LITE);
        HudAppearanceSettings.colors(HudAppearanceSettings.module(modules, "灵动岛", "Dynamic Island"), () -> Config.dynamicIslandAppearance);

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 3, new SettingModule(UiText.t("食物信息显示", "Food Info"), UiText.t("显示食物相关信息", "Show food-related information"),
                new SettingToggle(() -> Config.foodInfo, v -> { Config.foodInfo = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 8 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("隐藏昵称", "Hide Nickname"), UiText.t("在本地替换玩家昵称", "Replace the player nickname locally"),
                new SettingToggle(() -> Config.nickHider, v -> { Config.nickHider = v; Config.save(); }))
                .addSub("Nickname", UiText.t("用于替换真实用户名的显示名称", "Display name used to replace your real username"),
                        new SettingTextBox(() -> Config.nickHiderNickname,
                                v -> { Config.nickHiderNickname = NickHiderManager.normalizeNickname(v); Config.save(); }, 32))
                .addSub(UiText.t("聊天栏", "Chat"), UiText.t("替换聊天消息中的自己用户名", "Replace your username in chat messages"),
                        new SettingToggle(() -> Config.nickHiderChat, v -> { Config.nickHiderChat = v; Config.save(); }))
                .addSub(UiText.t("Tab栏", "Tab List"), UiText.t("替换玩家列表中的自己用户名", "Replace your username in the player list"),
                        new SettingToggle(() -> Config.nickHiderTab, v -> { Config.nickHiderTab = v; Config.save(); }))
                .addSub("Nametag", UiText.t("替换自己头顶名称标签", "Replace your overhead name tag"),
                        new SettingToggle(() -> Config.nickHiderNametag, v -> { Config.nickHiderNametag = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 9 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("手持物品位置调整", "Held Item Position"), UiText.t("调整第一人称主手与副手物品的位置", "Adjust first-person main-hand and off-hand item position"),
                new SettingToggle(() -> Config.heldItemPosition, v -> { Config.heldItemPosition = v; Config.save(); }))
                .addSub(UiText.t("主手 X", "Main Hand X"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemMainX,
                                v -> { Config.heldItemMainX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手 Y", "Main Hand Y"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemMainY,
                                v -> { Config.heldItemMainY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手 Z", "Main Hand Z"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemMainZ,
                                v -> { Config.heldItemMainZ = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手旋转 X", "Main Hand Rotation X"), "",
                        new SettingSlider(-180.0, 180.0, "%.0f°", () -> (double) Config.heldItemMainRotX,
                                v -> { Config.heldItemMainRotX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手旋转 Y", "Main Hand Rotation Y"), "",
                        new SettingSlider(-180.0, 180.0, "%.0f°", () -> (double) Config.heldItemMainRotY,
                                v -> { Config.heldItemMainRotY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手挥手速度", "Main Hand Swing Speed"), "",
                        new SettingSlider(0.0, 3.0, "%.2fx", () -> (double) Config.heldItemMainSwingSpeed,
                                v -> { Config.heldItemMainSwingSpeed = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("主手透明度", "Main Hand Alpha"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.heldItemMainAlpha,
                                v -> { Config.heldItemMainAlpha = Math.max(0, Math.min(100, v.intValue())); Config.save(); }))
                .addSub(UiText.t("副手 X", "Off Hand X"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemOffX,
                                v -> { Config.heldItemOffX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手 Y", "Off Hand Y"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemOffY,
                                v -> { Config.heldItemOffY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手 Z", "Off Hand Z"), "",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.heldItemOffZ,
                                v -> { Config.heldItemOffZ = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手旋转 X", "Off Hand Rotation X"), "",
                        new SettingSlider(-180.0, 180.0, "%.0f°", () -> (double) Config.heldItemOffRotX,
                                v -> { Config.heldItemOffRotX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手旋转 Y", "Off Hand Rotation Y"), "",
                        new SettingSlider(-180.0, 180.0, "%.0f°", () -> (double) Config.heldItemOffRotY,
                                v -> { Config.heldItemOffRotY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手挥手速度", "Off Hand Swing Speed"), "",
                        new SettingSlider(0.0, 3.0, "%.2fx", () -> (double) Config.heldItemOffSwingSpeed,
                                v -> { Config.heldItemOffSwingSpeed = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("副手透明度", "Off Hand Alpha"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.heldItemOffAlpha,
                                v -> { Config.heldItemOffAlpha = Math.max(0, Math.min(100, v.intValue())); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 10 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("去除容器半透明背景", "Remove Container Background"), UiText.t("去除背包和容器界面的半透明背景", "Remove the translucent background from inventory and container screens"),
                new SettingToggle(() -> Config.removeContainerBackground, v -> { Config.removeContainerBackground = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 11 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("方块数量显示", "Block Count Display"), UiText.t("右键放置方块时显示方块数量、放置速度和点击速度", "Show block count, placement speed, and click speed while right-clicking blocks"),
                new SettingToggle(() -> Config.blockCountDisplay, v -> {
                    Config.setBlockCountDisplay(v);
                    Config.save();
                }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 12 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("改变客户端时间", "Time Change"), UiText.t("强制修改客户端显示的世界时间", "Force the client-side world time"),
                new SettingToggle(() -> Config.timeChange, v -> { Config.timeChange = v; Config.save(); }))
                .addSub(UiText.t("时间", "Time"), "",
                        new SettingSlider(0, 23999, "%.0f", () -> (double) Config.clientTime,
                                v -> { Config.clientTime = v.intValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 13 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("改变客户端天气", "Weather Change"), UiText.t("强制修改客户端显示的天气", "Force the client-side weather"),
                new SettingToggle(() -> Config.weatherChange, v -> { Config.weatherChange = v; Config.save(); }))
                .addSub(UiText.t("天气模式", "Weather Mode"), "",
                        new SettingCycle(List.of(
                                UiText.t("晴天", "Clear"),
                                UiText.t("雨天", "Rain"),
                                UiText.t("雪天", "Snow"),
                                UiText.t("雷暴", "Thunder")),
                                () -> Config.weatherMode.ordinal(),
                                i -> { Config.weatherMode = Config.WeatherMode.values()[i % Config.WeatherMode.values().length]; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 16 + (Config.restrictedFeaturesUnlocked() ? 3 : 0), new SettingModule(UiText.t("自定义披风", "Custom Cape"), UiText.t("加载本地的自定义皮肤文件", "Load a local custom skin file"),
                new SettingToggle(() -> Config.customCape, v -> { Config.customCape = v; Config.save(); }))
                .addSub(UiText.t("打开目录", "Open Folder"), "",
                        new SettingButton(UiText.t("打开", "Open"), CustomCapeManager::openFolder))
                .addSub(UiText.t("切换披风", "Switch Cape"), "",
                        new SettingButton(() -> Config.customCapeImage, CustomCapeManager::cycleCape))));

        HudAppearanceSettings.add(HudAppearanceSettings.module(modules, "方块数量显示", "Block Count Display"),
                () -> Config.blockCountAppearance, () -> true);

    }

    @Override public String getTitle() { return UiText.t("视觉设置", "Render Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整视觉与动画效果", "Adjust visuals and animations"); }

    private static void addSkinOption(SettingModule module, String type, String title) {
        module.addSub(title, "", new SettingToggle(
                        () -> SkinManager.isActive(type),
                        value -> SkinManager.setActive(type, value)))
                .addSubWhen(
                        () -> SkinManager.isActive(type),
                        "皮肤名称",
                        "",
                        new SettingCycle(
                                SkinManager.names(type),
                                () -> SkinManager.selection(type),
                                index -> SkinManager.setSelection(type, index)
                        ));
    }

    private static int clampPercent(Double value) {
        return Math.max(0, Math.min(100, value.intValue()));
    }

    private static int customBlockOutlineArgb() {
        return ((Config.customBlockOutlineAlpha & 0xFF) << 24)
                | ((Config.customBlockOutlineRed & 0xFF) << 16)
                | ((Config.customBlockOutlineGreen & 0xFF) << 8)
                | (Config.customBlockOutlineBlue & 0xFF);
    }

    private static int customBlockOutlineFillArgb() {
        return ((Config.customBlockOutlineFillAlpha & 0xFF) << 24)
                | ((Config.customBlockOutlineFillRed & 0xFF) << 16)
                | ((Config.customBlockOutlineFillGreen & 0xFF) << 8)
                | (Config.customBlockOutlineFillBlue & 0xFF);
    }

    private static int arraylistColorArgb() {
        return 0xFF000000
                | ((Config.arraylistColorRed & 0xFF) << 16)
                | ((Config.arraylistColorGreen & 0xFF) << 8)
                | (Config.arraylistColorBlue & 0xFF);
    }

    private static int arraylistGradientColorArgb() {
        return 0xFF000000
                | ((Config.arraylistGradientRed & 0xFF) << 16)
                | ((Config.arraylistGradientGreen & 0xFF) << 8)
                | (Config.arraylistGradientBlue & 0xFF);
    }

    private static void setCustomBlockOutlineArgb(int color) {
        Config.customBlockOutlineAlpha = color >>> 24;
        Config.customBlockOutlineRed = (color >>> 16) & 0xFF;
        Config.customBlockOutlineGreen = (color >>> 8) & 0xFF;
        Config.customBlockOutlineBlue = color & 0xFF;
    }

    private static void setCustomBlockOutlineFillArgb(int color) {
        Config.customBlockOutlineFillAlpha = color >>> 24;
        Config.customBlockOutlineFillRed = (color >>> 16) & 0xFF;
        Config.customBlockOutlineFillGreen = (color >>> 8) & 0xFF;
        Config.customBlockOutlineFillBlue = color & 0xFF;
    }

    private static void setArraylistColorArgb(int color) {
        Config.arraylistColorRed = (color >>> 16) & 0xFF;
        Config.arraylistColorGreen = (color >>> 8) & 0xFF;
        Config.arraylistColorBlue = color & 0xFF;
    }

    private static void setArraylistGradientColorArgb(int color) {
        Config.arraylistGradientRed = (color >>> 16) & 0xFF;
        Config.arraylistGradientGreen = (color >>> 8) & 0xFF;
        Config.arraylistGradientBlue = color & 0xFF;
    }
}
