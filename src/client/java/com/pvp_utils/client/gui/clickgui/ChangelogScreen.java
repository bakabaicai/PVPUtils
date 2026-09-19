package com.pvp_utils.client.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChangelogScreen extends Screen {
    private static final String[] LINES = {
            "v1.8-beta.12",
            "新增: PVPUtils 玩家频道聊天 (需服务端安装 PVPUtilsServer)",
            "修复: 掉落物雷达支持拖动移位",
            "v1.8-beta.11",
            "修复: 击杀闪电改为目标死亡时触发",
            "新增: 离线皮肤修复",
            "新增: 掉落物雷达 HUD",
            "新增: 配置预设: 默认 / PVP / 低配优化",
            "新增: 粒子优化",
            "新增: 自定义面板颜色",
            "新增: 独立聊天窗口",
            "新增: 更新日志页面",
            "整合: fabric-api 打包进模组",
            "修复: 实体渲染剔除注入点崩溃",
            "优化: 实体渲染距离剔除",
            "优化: 内存自动清理",
    };

    public ChangelogScreen() {
        super(Component.literal("Changelog"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC101418, 0xCC101418);
        int y = 40;
        for (String line : LINES) {
            graphics.drawString(this.font, line, 30, y, 0xFFF2F4F8, false);
            y += 14;
        }
        graphics.drawString(this.font, "按 ESC 返回", 30, y + 10, 0xFF9AA4B2, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
