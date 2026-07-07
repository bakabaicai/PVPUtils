package com.pvp_utils.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pvp_utils.client.command.impl.ClientNameCommand;
import com.pvp_utils.client.command.impl.AutoGGCommand;
import com.pvp_utils.client.command.impl.HelpCommand;
import com.pvp_utils.client.command.impl.UpdateCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class CommandManager {
    private CommandManager() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("PVPUtils")
                    .then(HelpCommand.build())
                    .then(UpdateCommand.build())
                    .then(AutoGGCommand.build())
                    .then(ClientNameCommand.build());
            dispatcher.register(root);
        });
    }
}
