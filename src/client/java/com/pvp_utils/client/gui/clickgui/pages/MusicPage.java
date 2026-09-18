package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicManager;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.MusicInfoWidget;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;

import java.util.List;

public class MusicPage extends BasePage {

    public MusicPage() {
        modules.add(new SettingModule(UiText.t("网易云音乐", "Netease Music"),
                UiText.t("在设置界面内直接控制播放", "Control playback directly from the settings screen"),
                new MusicInfoWidget())
                .addSub(UiText.t("播放控制", "Playback"), "", new SettingButton(
                        () -> MusicPlaybackService.INSTANCE.isPlaying() ? UiText.t("暂停", "Pause") : UiText.t("播放", "Play"),
                        MusicPlaybackService.INSTANCE::toggle))
                .addSub(UiText.t("上一首", "Previous"), "", new SettingButton(UiText.t("上一首", "Previous"), MusicPlaybackService.INSTANCE::playPrevious))
                .addSub(UiText.t("下一首", "Next"), "", new SettingButton(UiText.t("下一首", "Next"), MusicPlaybackService.INSTANCE::playNext))
                .addSub(UiText.t("播放模式", "Playback Mode"), "", new SettingCycle(
                        List.of(UiText.t("单曲循环", "Loop"), UiText.t("列表循环", "List"), UiText.t("随机播放", "Random")),
                        () -> MusicPlaybackService.INSTANCE.playbackMode().ordinal(),
                        i -> {
                            while (MusicPlaybackService.INSTANCE.playbackMode().ordinal() != i) {
                                MusicPlaybackService.INSTANCE.cyclePlaybackMode();
                            }
                        }))
                .addSub(UiText.t("完整播放器", "Full Player"), UiText.t("打开完整音乐播放器界面", "Open the full music player interface"),
                        new SettingButton(UiText.t("打开", "Open"), NeteaseMusicManager::open)));
    }

    @Override public String getTitle() { return UiText.t("音乐", "Music"); }
    @Override public String getSubtitle() {
        Song song = MusicPlaybackService.INSTANCE.currentSong();
        if (song != null) {
            return song.name() + " · " + song.displayArtist();
        }
        return Config.isChinese ? "未在播放" : "Nothing playing";
    }
}
