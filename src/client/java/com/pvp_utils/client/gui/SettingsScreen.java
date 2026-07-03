package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
import com.pvp_utils.client.ResetManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import java.util.Locale;

public class SettingsScreen extends Screen {
    private final Screen lastScreen;
    private boolean inAnimPage = false;
    private boolean inOtherPage = false;
    private boolean inHitMarkerPage = false;
    private boolean inTargetHudPage = false;
    private boolean inSneakPage = false;
    private boolean inResetConfirmPage = false;
    private boolean resettingAll = true;

    public SettingsScreen(Screen lastScreen) {
        super(Component.literal(Config.isChinese ? "PVPUtils 设置" : "PVPUtils Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (inResetConfirmPage) {
            initResetConfirmPage();
        } else if (inAnimPage) {
            initAnimPage();
        } else if (inHitMarkerPage) {
            initHitMarkerPage();
        } else if (inTargetHudPage) {
            initTargetHudPage();
        } else if (inSneakPage) {
            initSneakPage();
        } else if (inOtherPage) {
            initOtherPage();
        } else {
            initMainPage();
        }
    }

    private void initMainPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "动画设置" : "Animation Settings"),
                (button) -> {
                    inAnimPage = true;
                    inOtherPage = false;
                    inSneakPage = false;
                    this.init();
                }).bounds(centerX - 155, centerY - 44, 150, 40).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "其他功能" : "Other Features"),
                (button) -> {
                    inOtherPage = true;
                    inAnimPage = false;
                    inSneakPage = false;
                    this.init();
                }).bounds(centerX + 5, centerY - 44, 150, 40).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "潜行动画" : "Sneak Animation"),
                (button) -> {
                    inSneakPage = true;
                    inAnimPage = false;
                    inOtherPage = false;
                    inHitMarkerPage = false;
                    inTargetHudPage = false;
                    this.init();
                }).bounds(centerX - 75, centerY + 4, 150, 40).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "§c重置所有设置" : "§cReset All Settings"),
                (button) -> {
                    Config.offsetX = 0.0f;
                    Config.offsetY = 0.0f;
                    Config.offsetZ = 0.0f;
                    Config.animSpeed = 1.0f;
                    Config.range = 3.0;
                    Config.swordBlock = false;
                    Config.useSwing = false;
                    Config.autoMode = false;
                    Config.noSneakAnimation = false;
                    Config.sneakDropScale = 0.5f;
                    Config.sneakAnimationSpeed = 1.0f;
                    Config.autoScreenshot = false;
                    Config.hitMarker = false;
                    Config.hitSound = true;
                    Config.lowHealthNotify = true;
                    Config.targetHud = false;
                    Config.diggingStatus = false;
                    Config.fallDamagePredict = false;
                    Config.victorySound = false;
                    Config.gammaOverride = false;
                    Config.gammaValue = 15.0;
                    Config.autoSprint = false;
                    Config.blockCountDisplay = false;
                    Config.autoChestDeposit = false;
                    Config.autoChestDepositResourcesOnly = true;
                    Config.autoChestDepositDepositDelay = 4;
                    Config.autoChestDepositCloseDelay = 4;
                    Config.targetHudX = -300f;
                    Config.targetHudY = -100f;
                    Config.targetHudZ = 0f;
                    Config.targetHudScale = 1.0f;
                    Config.nameTag = false;
                    Config.nameTagScale = 1.0f;
                    Config.nameTagDynamicScale = false;
                    Config.nameTagOnlyPlayer = false;
                    Config.dynamicMotionBlur = false;
                    Config.dynamicMotionBlurStrength = 1.0f;
                    Config.dynamicMotionBlurRefreshRateScaling = true;
                    Config.armorHud = false;
                    Config.potionStatus = false;
                    Config.blockCountDisplayX = 0f;
                    Config.blockCountDisplayY = 0f;
                    Config.blockCountDisplayScale = 1.0f;
                    Config.notificationX = Float.NaN;
                    Config.notificationY = Float.NaN;
                    Config.notificationScale = 1.0f;
                    Config.potionStatusX = 0f;
                    Config.potionStatusY = 0f;
                    Config.potionStatusScale = 1.0f;
                    Config.hideSignText = false;
                    Config.hideEnchantTableBook = false;
                    Config.hideFireOverlay = false;
                    Config.hideHurtShake = false;
                    Config.hitSoundType = Config.HitSoundType.NETHERITE;
                    Config.hitSoundCondition = Config.HitSoundCondition.BOTH;
                    Config.targetHudMode = Config.TargetHudMode.NEW;
                    Config.keystrokesMode = Config.KeystrokesMode.NEW;
                    Config.animationMode = Config.AnimMode.MODE_1_7;
                    Config.motionBlurAlgorithm = Config.MotionBlurAlgorithm.VELOCITY_BASED;
                    Config.save();
                    if (this.minecraft != null) this.minecraft.setScreen(new SettingsScreen(this.lastScreen));
                    inResetConfirmPage = true;
                    resettingAll = true;
                    this.init();
                }).bounds(5, this.height - 75, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "§a作者主页" : "§aAuthor"),
                (button) -> {
                    if (this.minecraft != null) {
                        String url = "https://space.bilibili.com/3546915648047958";
                        this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirmed) -> {
                            if (confirmed) {
                                Util.getPlatform().openUri(url);
                            }
                            this.minecraft.setScreen(this);
                        }, url, true));
                    }
                }).bounds(5, this.height - 50, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "Language: CN" : "Language: EN"), (button) -> {
            Config.isChinese = !Config.isChinese;
            Config.save();
            if (this.minecraft != null) this.minecraft.setScreen(new SettingsScreen(this.lastScreen));
        }).bounds(5, this.height - 25, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "完成" : "Done"), (button) -> this.onClose())
                .bounds(centerX - 75, this.height - 28, 150, 20).build());
    }

    private void initAnimPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        String modePrefix = cn ? "动画模式: " : "Animation Mode: ";
        this.addRenderableWidget(Button.builder(
                Component.literal(modePrefix + getModeName()),
                (button) -> {
                    cycleMode();
                    button.setMessage(Component.literal(modePrefix + getModeName()));
                    Config.save();
                }).bounds(centerX - 75, centerY - 110, 150, 20).build());

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 85, 150, 20, Component.empty(), (Config.offsetX + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("X: " + String.format(Locale.ROOT, "%.2f", Config.offsetX))); }
            @Override protected void applyValue() { Config.offsetX = (float) (this.value * 2.0 - 1.0); Config.save(); }
        });

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 60, 150, 20, Component.empty(), (Config.offsetY + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("Y: " + String.format(Locale.ROOT, "%.2f", Config.offsetY))); }
            @Override protected void applyValue() { Config.offsetY = (float) (this.value * 2.0 - 1.0); Config.save(); }
        });

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 35, 150, 20, Component.empty(), (Config.offsetZ + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("Z: " + String.format(Locale.ROOT, "%.2f", Config.offsetZ))); }
            @Override protected void applyValue() { Config.offsetZ = (float) (this.value * 2.0 - 1.0); Config.save(); }
        });

        String speedName = cn ? "动画速度" : "Animation Speed";
        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 10, 150, 20, Component.empty(), Config.animSpeed / 4.0f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal(speedName + ": " + String.format(Locale.ROOT, "%.2f", Config.animSpeed))); }
            @Override protected void applyValue() { Config.animSpeed = (float) (this.value * 4.0); Config.save(); }
        });

        String reachName = cn ? "触发距离" : "Reach";
        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY + 15, 150, 20, Component.empty(), (float)((Config.range - 2.0) / 4.0)) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal(reachName + ": " + String.format(Locale.ROOT, "%.2f", Config.range))); }
            @Override protected void applyValue() { Config.range = 2.0 + (this.value * 4.0); Config.save(); }
        });

        int btnW = 85;
        int sX = centerX - (btnW * 3 + 4) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "格挡动画" : "Sword Block", Config.swordBlock, cn)),
                (button) -> {
                    Config.swordBlock = !Config.swordBlock;
                    button.setMessage(Component.literal(getToggleText(cn ? "格挡动画" : "Sword Block", Config.swordBlock, cn)));
                    Config.save();
                }).bounds(sX, centerY + 40, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "使用动画" : "UseSwing", Config.useSwing, cn)),
                (button) -> {
                    Config.useSwing = !Config.useSwing;
                    button.setMessage(Component.literal(getToggleText(cn ? "使用动画" : "UseSwing", Config.useSwing, cn)));
                    Config.save();
                }).bounds(sX + btnW + 2, centerY + 40, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "自动格挡" : "Auto", Config.autoMode, cn)),
                (button) -> {
                    Config.autoMode = !Config.autoMode;
                    button.setMessage(Component.literal(getToggleText(cn ? "自动格挡" : "Auto", Config.autoMode, cn)));
                    Config.save();
                }).bounds(sX + (btnW + 2) * 2, centerY + 40, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "§c重置此页所有设置" : "§cReset This Page"),
                (button) -> {
                    inResetConfirmPage = true;
                    resettingAll = false;
                    this.init();
                }).bounds(centerX - 75, centerY + 65, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inAnimPage = false;
            inOtherPage = false;
            this.init();
        }).bounds(centerX - 75, centerY + 90, 150, 20).build());
    }

    private void initOtherPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "自动截图" : "Auto Screenshot", Config.autoScreenshot, cn)),
                (button) -> {
                    Config.autoScreenshot = !Config.autoScreenshot;
                    button.setMessage(Component.literal(getToggleText(cn ? "自动截图" : "Auto Screenshot", Config.autoScreenshot, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "低血量提示" : "Low Health Warning", Config.lowHealthNotify, cn)),
                (button) -> {
                    Config.lowHealthNotify = !Config.lowHealthNotify;
                    button.setMessage(Component.literal(getToggleText(cn ? "低血量提示" : "Low Health Warning", Config.lowHealthNotify, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "击中标记..." : "Hit Marker..."),
                (button) -> {
                    inHitMarkerPage = true;
                    inOtherPage = false;
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "目标HUD..." : "Target HUD..."),
                (button) -> {
                    inTargetHudPage = true;
                    inOtherPage = false;
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "摔落伤害预测" : "Fall Damage Predict", Config.fallDamagePredict, cn)),
                (button) -> {
                    Config.fallDamagePredict = !Config.fallDamagePredict;
                    button.setMessage(Component.literal(getToggleText(cn ? "摔落伤害预测" : "Fall Damage Predict", Config.fallDamagePredict, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "盔甲 HUD" : "Armor HUD", Config.armorHud, cn)),
                (button) -> {
                    Config.armorHud = !Config.armorHud;
                    button.setMessage(Component.literal(getToggleText(cn ? "盔甲 HUD" : "Armor HUD", Config.armorHud, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "药水状态" : "Potion Status", Config.potionStatus, cn)),
                (button) -> {
                    Config.potionStatus = !Config.potionStatus;
                    button.setMessage(Component.literal(getToggleText(cn ? "药水状态" : "Potion Status", Config.potionStatus, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "胜利音效" : "Victory Sound", Config.victorySound, cn)),
                (button) -> {
                    Config.victorySound = !Config.victorySound;
                    button.setMessage(Component.literal(getToggleText(cn ? "胜利音效" : "Victory Sound", Config.victorySound, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inOtherPage = false;
            inAnimPage = false;
            this.init();
        }).bounds(centerX - 75, centerY + 100, 150, 20).build());
    }

    private void initSneakPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;
        int currentY = centerY - 60;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "潜行动画" : "Sneak Animation", Config.noSneakAnimation, cn)),
                (button) -> {
                    Config.noSneakAnimation = !Config.noSneakAnimation;
                    button.setMessage(Component.literal(getToggleText(cn ? "潜行动画" : "Sneak Animation", Config.noSneakAnimation, cn)));
                    Config.save();
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        String sliderName = cn ? "潜行下降高度" : "Sneak Drop";
        if (Config.noSneakAnimation) {
            currentY += 25;
            this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), Config.sneakDropScale) {
                { updateMessage(); }
                @Override protected void updateMessage() {
                    int percent = Math.round(Config.sneakDropScale * 100.0f);
                    this.setMessage(Component.literal(sliderName + ": " + percent + "%"));
                }
                @Override protected void applyValue() {
                    Config.sneakDropScale = (float) this.value;
                    Config.save();
                }
            });

            currentY += 25;
            String speedName = cn ? "动画速度" : "Animation Speed";
            this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), Config.sneakAnimationSpeed) {
                { updateMessage(); }
                @Override protected void updateMessage() {
                    if (Config.sneakAnimationSpeed >= 1.0f) {
                        this.setMessage(Component.literal(speedName + ": " + (cn ? "无插帧" : "Instant")));
                    } else {
                        int percent = Math.round(Config.sneakAnimationSpeed * 100.0f);
                        this.setMessage(Component.literal(speedName + ": " + percent + "%"));
                    }
                }
                @Override protected void applyValue() {
                    Config.sneakAnimationSpeed = (float) this.value;
                    Config.save();
                }
            });
        }

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inSneakPage = false;
            this.init();
        }).bounds(centerX - 75, centerY + 65, 150, 20).build());
    }

    private void initHitMarkerPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "击中标记" : "Hit Marker", Config.hitMarker, cn)),
                (button) -> {
                    Config.hitMarker = !Config.hitMarker;
                    Config.save();
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        if (Config.hitMarker) {
            currentY += 25;
            this.addRenderableWidget(Button.builder(
                    Component.literal(getToggleText(cn ? "命中提示音" : "Hit Sound", Config.hitSound, cn)),
                    (button) -> {
                        Config.hitSound = !Config.hitSound;
                        Config.save();
                        this.init();
                    }).bounds(centerX - 75, currentY, 150, 20).build());

            if (Config.hitSound) {
                currentY += 25;
                String typeName = Config.hitSoundType == Config.HitSoundType.NETHERITE ?
                        (cn ? "下界合金块" : "Netherite Block") : (cn ? "经验声" : "Experience");
                this.addRenderableWidget(Button.builder(
                        Component.literal((cn ? "提示音: " : "Sound: ") + typeName),
                        (button) -> {
                            Config.hitSoundType = Config.hitSoundType == Config.HitSoundType.NETHERITE ?
                                    Config.HitSoundType.EXPERIENCE : Config.HitSoundType.NETHERITE;
                            Config.save();
                            this.init();
                        }).bounds(centerX - 75, currentY, 150, 20).build());

                currentY += 25;
                String conditionName = switch (Config.hitSoundCondition) {
                    case BOTH -> cn ? "近战、远程命中" : "Melee & Ranged";
                    case MELEE -> cn ? "近战命中" : "Melee Only";
                    case RANGED -> cn ? "远程命中" : "Ranged Only";
                };
                this.addRenderableWidget(Button.builder(
                        Component.literal((cn ? "提示音播放时机: " : "Play On: ") + conditionName),
                        (button) -> {
                            Config.hitSoundCondition = switch (Config.hitSoundCondition) {
                                case BOTH -> Config.HitSoundCondition.MELEE;
                                case MELEE -> Config.HitSoundCondition.RANGED;
                                case RANGED -> Config.HitSoundCondition.BOTH;
                            };
                            Config.save();
                            this.init();
                        }).bounds(centerX - 75, currentY, 150, 20).build());
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inHitMarkerPage = false;
            inOtherPage = true;
            this.init();
        }).bounds(centerX - 75, centerY + 100, 150, 20).build());
    }

    private void initTargetHudPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "目标HUD" : "Target HUD", Config.targetHud, cn)),
                (button) -> {
                    Config.targetHud = !Config.targetHud;
                    Config.save();
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 30;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (Config.targetHudX + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD X: " + String.format(Locale.ROOT, "%.0f", Config.targetHudX))); }
            @Override protected void applyValue() { Config.targetHudX = (float) (this.value * 1000.0 - 500.0); Config.save(); }
        });

        currentY += 25;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (Config.targetHudY + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD Y: " + String.format(Locale.ROOT, "%.0f", Config.targetHudY))); }
            @Override protected void applyValue() { Config.targetHudY = (float) (this.value * 1000.0 - 500.0); Config.save(); }
        });

        currentY += 25;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (Config.targetHudZ + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD Z: " + String.format(Locale.ROOT, "%.0f", Config.targetHudZ))); }
            @Override protected void applyValue() { Config.targetHudZ = (float) (this.value * 1000.0 - 500.0); Config.save(); }
        });

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inTargetHudPage = false;
            inOtherPage = true;
            this.init();
        }).bounds(centerX - 75, centerY + 100, 150, 20).build());
    }

    private void initResetConfirmPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = Config.isChinese;

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "§c是的，现在立刻重置所有设置" : "§cYes, reset all settings now"),
                (button) -> {
                    if (resettingAll) {
                        ResetManager.resetAll();
                        inAnimPage = false;
                        inOtherPage = false;
                    } else {
                        ResetManager.resetAnimPage();
                        inAnimPage = true;
                    }
                    inResetConfirmPage = false;
                    this.init();
                }).bounds(centerX - 100, centerY - 25, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "§a不，我点错了" : "§aNo, I misclicked"),
                (button) -> {
                    inResetConfirmPage = false;
                    this.init();
                }).bounds(centerX - 100, centerY + 5, 200, 20).build());
    }

    private String getToggleText(String prefix, boolean value, boolean isChinese) {
        if (isChinese) {
            return prefix + ": " + (value ? "开" : "关");
        } else {
            return prefix + ": " + (value ? "ON" : "OFF");
        }
    }

    private String getModeName() {
        return switch (Config.animationMode) {
            case MODE_1_7 -> "1.7";
            case MODE_PUSH -> "Push";
            case MODE_1_7_PLUS -> "1.7+";
            case MODE_NEW -> "New";
        };
    }

    private void cycleMode() {
        Config.animationMode = switch (Config.animationMode) {
            case MODE_1_7 -> Config.AnimMode.MODE_PUSH;
            case MODE_PUSH -> Config.AnimMode.MODE_1_7_PLUS;
            case MODE_1_7_PLUS -> Config.AnimMode.MODE_NEW;
            case MODE_NEW -> Config.AnimMode.MODE_1_7;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.lastScreen);
    }
}
