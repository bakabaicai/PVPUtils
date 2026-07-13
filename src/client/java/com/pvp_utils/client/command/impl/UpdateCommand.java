package com.pvp_utils.client.command.impl;

import com.pvp_utils.client.Update;

import java.util.List;

public final class UpdateCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("update");
    }

    @Override
    public void execute(String args) {
        if ("qqgroup".equalsIgnoreCase(args.trim())) {
            Update.copyQqGroupNumber();
            return;
        }
        Update.startManualCheck();
    }

    @Override
    public List<String> suggestions(String args) {
        String trimmed = args.trim();
        if (args.contains(" ") || trimmed.isBlank()) {
            return List.of();
        }
        String prefix = trimmed.toLowerCase(java.util.Locale.ROOT);
        return "qqgroup".startsWith(prefix) && !"qqgroup".equals(prefix) ? List.of("qqgroup") : List.of();
    }
}
