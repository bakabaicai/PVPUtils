package com.old_animation.client.gui;

import com.old_animation.AnimationConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import java.util.Locale;

public class AnimationSettingsScreen extends Screen {
    private final Screen lastScreen;
    private boolean inAnimPage = false;
    private boolean inOtherPage = false;
    private boolean inHitMarkerPage = false;
    private boolean inTargetHudPage = false;

    public AnimationSettingsScreen(Screen lastScreen) {
        super(Component.literal(AnimationConfig.isChinese ? "OldAnimation 设置" : "OldAnimation Settings"));
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
        boolean cn = AnimationConfig.isChinese;

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
                    AnimationConfig.offsetX = 0.0f;
                    AnimationConfig.offsetY = 0.0f;
                    AnimationConfig.offsetZ = 0.0f;
                    AnimationConfig.animSpeed = 1.0f;
                    AnimationConfig.range = 3.0;
                    AnimationConfig.swordBlock = false;
                    AnimationConfig.useSwing = false;
                    AnimationConfig.autoMode = false;
                    AnimationConfig.autoScreenshot = false;
                    AnimationConfig.hitMarker = false;
                    AnimationConfig.hitSound = true;
                    AnimationConfig.damageRecord = true;
                    AnimationConfig.lowHealthNotify = true;
                    AnimationConfig.targetHud = false;
                    AnimationConfig.fallDamagePredict = false;
                    AnimationConfig.targetHudX = -300f;
                    AnimationConfig.targetHudY = -100f;
                    AnimationConfig.targetHudZ = 0f;
                    AnimationConfig.hitSoundType = AnimationConfig.HitSoundType.NETHERITE;
                    AnimationConfig.hitSoundCondition = AnimationConfig.HitSoundCondition.BOTH;
                    AnimationConfig.animationMode = AnimationConfig.AnimMode.MODE_1_7;
                    AnimationConfig.save();
                    if (this.minecraft != null) this.minecraft.setScreen(new AnimationSettingsScreen(this.lastScreen));
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
            AnimationConfig.isChinese = !AnimationConfig.isChinese;
            AnimationConfig.save();
            if (this.minecraft != null) this.minecraft.setScreen(new AnimationSettingsScreen(this.lastScreen));
        }).bounds(5, this.height - 25, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cn ? "完成" : "Done"), (button) -> this.onClose())
                .bounds(centerX - 75, this.height - 28, 150, 20).build());
    }

    private void initAnimPage() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cn = AnimationConfig.isChinese;

        String modePrefix = cn ? "动画模式: " : "Animation Mode: ";
        this.addRenderableWidget(Button.builder(
                Component.literal(modePrefix + getModeName()),
                (button) -> {
                    cycleMode();
                    button.setMessage(Component.literal(modePrefix + getModeName()));
                    AnimationConfig.save();
                }).bounds(centerX - 75, centerY - 110, 150, 20).build());

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 85, 150, 20, Component.empty(), (AnimationConfig.offsetX + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("X: " + String.format(Locale.ROOT, "%.2f", AnimationConfig.offsetX))); }
            @Override protected void applyValue() { AnimationConfig.offsetX = (float) (this.value * 2.0 - 1.0); AnimationConfig.save(); }
        });

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 60, 150, 20, Component.empty(), (AnimationConfig.offsetY + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("Y: " + String.format(Locale.ROOT, "%.2f", AnimationConfig.offsetY))); }
            @Override protected void applyValue() { AnimationConfig.offsetY = (float) (this.value * 2.0 - 1.0); AnimationConfig.save(); }
        });

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 35, 150, 20, Component.empty(), (AnimationConfig.offsetZ + 1f) / 2f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("Z: " + String.format(Locale.ROOT, "%.2f", AnimationConfig.offsetZ))); }
            @Override protected void applyValue() { AnimationConfig.offsetZ = (float) (this.value * 2.0 - 1.0); AnimationConfig.save(); }
        });

        String speedName = cn ? "动画速度" : "Animation Speed";
        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY - 10, 150, 20, Component.empty(), AnimationConfig.animSpeed / 4.0f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal(speedName + ": " + String.format(Locale.ROOT, "%.2f", AnimationConfig.animSpeed))); }
            @Override protected void applyValue() { AnimationConfig.animSpeed = (float) (this.value * 4.0); AnimationConfig.save(); }
        });

        String reachName = cn ? "触发距离" : "Reach";
        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, centerY + 15, 150, 20, Component.empty(), (float)((AnimationConfig.range - 2.0) / 4.0)) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal(reachName + ": " + String.format(Locale.ROOT, "%.2f", AnimationConfig.range))); }
            @Override protected void applyValue() { AnimationConfig.range = 2.0 + (this.value * 4.0); AnimationConfig.save(); }
        });

        int btnW = 85;
        int sX = centerX - (btnW * 3 + 4) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "格挡动画" : "Sword Block", AnimationConfig.swordBlock, cn)),
                (button) -> {
                    AnimationConfig.swordBlock = !AnimationConfig.swordBlock;
                    button.setMessage(Component.literal(getToggleText(cn ? "格挡动画" : "Sword Block", AnimationConfig.swordBlock, cn)));
                    AnimationConfig.save();
                }).bounds(sX, centerY + 40, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "使用动画" : "UseSwing", AnimationConfig.useSwing, cn)),
                (button) -> {
                    AnimationConfig.useSwing = !AnimationConfig.useSwing;
                    button.setMessage(Component.literal(getToggleText(cn ? "使用动画" : "UseSwing", AnimationConfig.useSwing, cn)));
                    AnimationConfig.save();
                }).bounds(sX + btnW + 2, centerY + 40, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "自动格挡" : "Auto", AnimationConfig.autoMode, cn)),
                (button) -> {
                    AnimationConfig.autoMode = !AnimationConfig.autoMode;
                    button.setMessage(Component.literal(getToggleText(cn ? "自动格挡" : "Auto", AnimationConfig.autoMode, cn)));
                    AnimationConfig.save();
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
        boolean cn = AnimationConfig.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "自动截图" : "Auto Screenshot", AnimationConfig.autoScreenshot, cn)),
                (button) -> {
                    AnimationConfig.autoScreenshot = !AnimationConfig.autoScreenshot;
                    button.setMessage(Component.literal(getToggleText(cn ? "自动截图" : "Auto Screenshot", AnimationConfig.autoScreenshot, cn)));
                    AnimationConfig.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "低血量提示" : "Low Health Warning", AnimationConfig.lowHealthNotify, cn)),
                (button) -> {
                    AnimationConfig.lowHealthNotify = !AnimationConfig.lowHealthNotify;
                    button.setMessage(Component.literal(getToggleText(cn ? "低血量提示" : "Low Health Warning", AnimationConfig.lowHealthNotify, cn)));
                    AnimationConfig.save();
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
                Component.literal(getToggleText(cn ? "摔落伤害预测" : "Fall Damage Predict", AnimationConfig.fallDamagePredict, cn)),
                (button) -> {
                    AnimationConfig.fallDamagePredict = !AnimationConfig.fallDamagePredict;
                    button.setMessage(Component.literal(getToggleText(cn ? "摔落伤害预测" : "Fall Damage Predict", AnimationConfig.fallDamagePredict, cn)));
                    AnimationConfig.save();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 25;
        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "伤害数值记录" : "Damage Record", AnimationConfig.damageRecord, cn)),
                (button) -> {
                    AnimationConfig.damageRecord = !AnimationConfig.damageRecord;
                    button.setMessage(Component.literal(getToggleText(cn ? "伤害数值记录" : "Damage Record", AnimationConfig.damageRecord, cn)));
                    AnimationConfig.save();
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
        boolean cn = AnimationConfig.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "击中标记" : "Hit Marker", AnimationConfig.hitMarker, cn)),
                (button) -> {
                    AnimationConfig.hitMarker = !AnimationConfig.hitMarker;
                    AnimationConfig.save();
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        if (AnimationConfig.hitMarker) {
            currentY += 25;
            this.addRenderableWidget(Button.builder(
                    Component.literal(getToggleText(cn ? "命中提示音" : "Hit Sound", AnimationConfig.hitSound, cn)),
                    (button) -> {
                        AnimationConfig.hitSound = !AnimationConfig.hitSound;
                        AnimationConfig.save();
                        this.init();
                    }).bounds(centerX - 75, currentY, 150, 20).build());

            if (AnimationConfig.hitSound) {
                currentY += 25;
                String typeName = AnimationConfig.hitSoundType == AnimationConfig.HitSoundType.NETHERITE ?
                        (cn ? "下界合金块" : "Netherite Block") : (cn ? "经验声" : "Experience");
                this.addRenderableWidget(Button.builder(
                        Component.literal((cn ? "提示音: " : "Sound: ") + typeName),
                        (button) -> {
                            AnimationConfig.hitSoundType = AnimationConfig.hitSoundType == AnimationConfig.HitSoundType.NETHERITE ?
                                    AnimationConfig.HitSoundType.EXPERIENCE : AnimationConfig.HitSoundType.NETHERITE;
                            AnimationConfig.save();
                            this.init();
                        }).bounds(centerX - 75, currentY, 150, 20).build());

                currentY += 25;
                String conditionName = switch (AnimationConfig.hitSoundCondition) {
                    case BOTH -> cn ? "近战、远程命中" : "Melee & Ranged";
                    case MELEE -> cn ? "近战命中" : "Melee Only";
                    case RANGED -> cn ? "远程命中" : "Ranged Only";
                };
                this.addRenderableWidget(Button.builder(
                        Component.literal((cn ? "提示音播放时机: " : "Play On: ") + conditionName),
                        (button) -> {
                            AnimationConfig.hitSoundCondition = switch (AnimationConfig.hitSoundCondition) {
                                case BOTH -> AnimationConfig.HitSoundCondition.MELEE;
                                case MELEE -> AnimationConfig.HitSoundCondition.RANGED;
                                case RANGED -> AnimationConfig.HitSoundCondition.BOTH;
                            };
                            AnimationConfig.save();
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
        boolean cn = AnimationConfig.isChinese;

        int currentY = centerY - 85;

        this.addRenderableWidget(Button.builder(
                Component.literal(getToggleText(cn ? "目标HUD" : "Target HUD", AnimationConfig.targetHud, cn)),
                (button) -> {
                    AnimationConfig.targetHud = !AnimationConfig.targetHud;
                    AnimationConfig.save();
                    this.init();
                }).bounds(centerX - 75, currentY, 150, 20).build());

        currentY += 30;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (AnimationConfig.targetHudX + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD X: " + String.format(Locale.ROOT, "%.0f", AnimationConfig.targetHudX))); }
            @Override protected void applyValue() { AnimationConfig.targetHudX = (float) (this.value * 1000.0 - 500.0); AnimationConfig.save(); }
        });

        currentY += 25;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (AnimationConfig.targetHudY + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD Y: " + String.format(Locale.ROOT, "%.0f", AnimationConfig.targetHudY))); }
            @Override protected void applyValue() { AnimationConfig.targetHudY = (float) (this.value * 1000.0 - 500.0); AnimationConfig.save(); }
        });

        currentY += 25;

        this.addRenderableWidget(new AbstractSliderButton(centerX - 75, currentY, 150, 20, Component.empty(), (AnimationConfig.targetHudZ + 500f) / 1000f) {
            { updateMessage(); }
            @Override protected void updateMessage() { this.setMessage(Component.literal("HUD Z: " + String.format(Locale.ROOT, "%.0f", AnimationConfig.targetHudZ))); }
            @Override protected void applyValue() { AnimationConfig.targetHudZ = (float) (this.value * 1000.0 - 500.0); AnimationConfig.save(); }
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
        return switch (AnimationConfig.animationMode) {
            case MODE_1_7 -> "1.7";
            case MODE_PUSH -> "Push";
            case MODE_1_7_PLUS -> "1.7+";
        };
    }

    private void cycleMode() {
        AnimationConfig.animationMode = switch (AnimationConfig.animationMode) {
            case MODE_1_7 -> AnimationConfig.AnimMode.MODE_PUSH;
            case MODE_PUSH -> AnimationConfig.AnimMode.MODE_1_7_PLUS;
            case MODE_1_7_PLUS -> AnimationConfig.AnimMode.MODE_1_7;
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