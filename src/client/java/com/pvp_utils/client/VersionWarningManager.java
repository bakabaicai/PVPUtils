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
        return Component.literal("您当前正在使用" + Version.NAME + "的")
                .withStyle(ChatFormatting.GREEN)
                .append(versionTypeText())
                .append(Component.literal("版本，这些版本为抢先体验为主，可能会有各种各样的报错和问题，出现这些问题时，请及时通过")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("GitHub Issues")
                        .withStyle(linkStyle()))
                .append(Component.literal("提交问题交由开发者处理，而不是去视频相关评论区发表负面言论。")
                        .withStyle(ChatFormatting.GREEN));
    }

    private static MutableComponent englishMessage() {
        return Component.literal("You are currently using the ")
                .withStyle(ChatFormatting.GREEN)
                .append(versionTypeText())
                .append(Component.literal(" version of " + Version.NAME + ". These versions are mainly for early access and may contain errors or issues. If you encounter problems, please report them through ")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("GitHub Issues")
                        .withStyle(linkStyle()))
                .append(Component.literal(" instead of posting negative comments under related videos.")
                        .withStyle(ChatFormatting.GREEN));
    }

    private static MutableComponent versionTypeText() {
        if (Version.TYPE == 1) {
            return Component.literal("Alpha").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
        return Component.literal("Beta").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
    }

    private static Style linkStyle() {
        return Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(ISSUES_URL)));
    }
}
