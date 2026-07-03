package com.pvp_utils.client.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pvp_utils.client.Update;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class UpdateCommand {
    private UpdateCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("update")
                .executes(context -> {
                    Update.startManualCheck();
                    return 1;
                })
                .then(ClientCommandManager.literal("qqgroup")
                        .executes(context -> {
                            Update.copyQqGroupNumber();
                            return 1;
                        }));
    }
}
