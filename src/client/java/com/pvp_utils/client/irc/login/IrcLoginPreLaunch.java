package com.pvp_utils.client.irc.login;

import com.pvp_utils.Config;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class IrcLoginPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        Config.load();
        IrcLoginWindow.showBeforeGameStart();
    }
}
