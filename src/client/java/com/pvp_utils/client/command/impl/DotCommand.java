package com.pvp_utils.client.command.impl;

import java.util.List;

public interface DotCommand {
    List<String> names();

    void execute(String args);

    default List<String> suggestions(String args) {
        return List.of();
    }

    default boolean acceptsFreeText() {
        return false;
    }
}
