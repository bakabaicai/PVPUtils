package com.pvp_utils.client;

import com.pvp_utils.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;

public final class VersionWarningManager {
    private static final String ISSUES_URL = "https://github.com/bakabaicai/PVPUtils/issues";
    private static boolean shown;

    private VersionWarningManager() {}

    public static void tick(Minecraft client) {
        if (shown || client.player == null || client.level == null) return;
        if (Version.TYPE != 1 && Version.TYPE != 2) return;

        shown = true;
        client.player.displayClientMessage(Config.isChinese ? chineseMessage() : englishMessage(), false);
    }

    private static MutableComponent chineseMessage() {
        if (Version.TYPE == 1) {
            return Component.literal("您当前正在使用 " + Version.NAME + " 的 ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(versionTypeText())
                    .append(Component.literal(" 版本。该版本属于先行测试阶段，稳定性较低，可能存在大量已知或未知 Bug，且无法保证在所有设备上均可正常运行。发布此类版本的主要目的在于收集错误信息，以便开发者后续持续修复和完善模组内容，因此仅建议用于尝鲜体验。若在使用过程中遇到报错或异常情况，请优先通过 ")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("GitHub Issues").withStyle(linkStyle()))
                    .append(Component.literal(" 向开发者反馈问题。").withStyle(ChatFormatting.YELLOW));
        }

        return Component.literal("您当前正在使用 " + Version.NAME + " 的 ")
                .withStyle(ChatFormatting.GREEN)
                .append(versionTypeText())
                .append(Component.literal(" 版本。该版本已进入公开测试阶段，整体完成度与可用性相对更高，但仍可能存在部分 Bug、细节问题或兼容性异常。发布此类版本的主要目的在于进一步收集反馈并完善正式版体验，因此仍不保证在所有设备与环境下都能完全稳定运行。若在使用过程中发现异常，请通过 ")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("GitHub Issues").withStyle(linkStyle()))
                .append(Component.literal(" 提交反馈，帮助开发者在正式版发布前进一步优化体验。").withStyle(ChatFormatting.GREEN));
    }

    private static MutableComponent englishMessage() {
        if (Version.TYPE == 1) {
            return Component.literal("You are currently using an ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(versionTypeText())
                    .append(Component.literal(" version of " + Version.NAME + ". This build is part of the early testing stage and may be highly unstable, with many known or unknown bugs. Normal operation on all devices cannot be guaranteed. The main purpose of releasing this kind of version is to collect error reports so the mod can be improved and maintained more effectively. It is intended only for users who want an early preview. If you encounter crashes or abnormal behavior, please report them through ")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("GitHub Issues").withStyle(linkStyle()))
                    .append(Component.literal(" first.").withStyle(ChatFormatting.YELLOW));
        }

        return Component.literal("You are currently using a ")
                .withStyle(ChatFormatting.GREEN)
                .append(versionTypeText())
                .append(Component.literal(" version of " + Version.NAME + ". This build has entered the public testing stage, with a higher overall level of completion and usability, but some bugs, edge-case issues, or compatibility problems may still remain. The main purpose of releasing this kind of version is to gather more feedback and improve the final release. Stable operation on all devices and environments is still not guaranteed. If you find any issues during use, please submit them through ")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("GitHub Issues").withStyle(linkStyle()))
                .append(Component.literal(" to help improve the release version.").withStyle(ChatFormatting.GREEN));
    }

    private static MutableComponent versionTypeText() {
        if (Version.TYPE == 1) {
            return Component.literal("Alpha").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
        return Component.literal("Beta");
    }

    private static Style linkStyle() {
        return Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(ISSUES_URL)));
    }
}
