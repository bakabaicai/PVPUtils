package com.pvp_utils.client.command.impl;

import com.pvp_utils.client.irc.IrcBridge;

import java.util.List;

public final class IrcChatCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("c", "chat");
    }

    @Override
    public boolean acceptsFreeText() {
        return true;
    }

    @Override
    public void execute(String args) {
        IrcBridge.sendChat(args);
    }
}
