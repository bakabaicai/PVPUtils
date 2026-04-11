package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
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

    public SettingsScreen(Screen lastScreen) {
        super(Component.literal(Config.isChinese ? "PVPUtils 设置" : "PVPUtils Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (inAnimPage) {
            initAnimPage();
        } else if (inHitMarkerPage) {
            initHitMarkerPage();
        } else if (inTargetHudPage) {
            initTargetHudPage();
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
                    this.init();
                }).bounds(centerX - 155, centerY - 20, 150, 40).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(cn ? "其他功能" : "Other Features"),
                (button) -> {
                    inOtherPage = true;
                    inAnimPage = false;
                    this.init();
                }).bounds(centerX + 5, centerY - 20, 150, 40).build());

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
                    Config.autoScreenshot = false;
                    Config.hitMarker = false;
                    Config.hitSound = true;
                    Config.damageRecord = true;
                    Config.lowHealthNotify = true;
                    Config.targetHud = false;
                    Config.fallDamagePredict = false;
                    Config.targetHudX = -300f;
                    Config.targetHudY = -100f;
                    Config.targetHudZ = 0f;
                    Config.hitSoundType = Config.HitSoundType.NETHERITE;
                    Config.hitSoundCondition = Config.HitSoundCondition.BOTH;
                    Config.animationMode = Config.AnimMode.MODE_1_7;
                    Config.save();
                    if (this.minecraft != null) this.minecraft.setScreen(new SettingsScreen(this.lastScreen));
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

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inAnimPage = false;
            inOtherPage = false;
            this.init();
        }).bounds(centerX - 75, centerY + 65, 150, 20).build());
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
                Component.literal(getToggleText(cn ? "伤害数值记录" : "Damage Record", Config.damageRecord, cn)),
                (button) -> {
                    Config.damageRecord = !Config.damageRecord;
                    button.setMessage(Component.literal(getToggleText(cn ? "伤害数值记录" : "Damage Record", Config.damageRecord, cn)));
                    Config.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "返回" : "Back"), (button) -> {
            inOtherPage = false;
            inAnimPage = false;
            this.init();
        }).bounds(centerX - 75, centerY + 100, 150, 20).build());
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
        };
    }

    private void cycleMode() {
        Config.animationMode = switch (Config.animationMode) {
            case MODE_1_7 -> Config.AnimMode.MODE_PUSH;
            case MODE_PUSH -> Config.AnimMode.MODE_1_7_PLUS;
            case MODE_1_7_PLUS -> Config.AnimMode.MODE_1_7;
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