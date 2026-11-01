package com.pvp_utils.client.command.impl;

import com.pvp_utils.client.skin.OfflineSkinManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class OfflineSkinCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("offlineskin", "oskin");
    }

    @Override
    public void execute(String args) {
        String value = args == null ? "" : args.trim().toLowerCase();
        OfflineSkinManager.load();
        if (value.equals("next")) {
            OfflineSkinManager.quickSwitchNext();
            ChatUtils.success("Offline skin switched.");
            return;
        }
        if (value.equals("prev") || value.equals("previous")) {
            OfflineSkinManager.quickSwitchPrevious();
            ChatUtils.success("Offline skin switched.");
            return;
        }
        ChatUtils.warning("Usage: .offlineskin <next/prev>");
    }
}
