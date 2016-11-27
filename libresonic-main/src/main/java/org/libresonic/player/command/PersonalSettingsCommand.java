/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.command;

import java.util.List;

import org.libresonic.player.controller.PersonalSettingsController;
import org.libresonic.player.domain.AlbumListType;
import org.libresonic.player.domain.Avatar;
import org.libresonic.player.domain.Theme;
import org.libresonic.player.domain.User;
import org.libresonic.player.domain.UserSettings;

/**
 * Command used in {@link PersonalSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PersonalSettingsCommand {
    private User user;
    private String localeIndex;
    private String[] locales;
    private String themeIndex;
    private Theme[] themes;
    private String albumListId;
    private AlbumListType[] albumLists;
    private int avatarId;
    private List<Avatar> avatars;
    private Avatar customAvatar;
    private UserSettings.Visibility mainVisibility;
    private UserSettings.Visibility playlistVisibility;
    private boolean partyModeEnabled;
    private boolean showNowPlayingEnabled;
    private boolean showChatEnabled;
    private boolean showArtistInfoEnabled;
    private boolean nowPlayingAllowed;
    private boolean autoHidePlayQueue;
    private boolean keyboardShortcutsEnabled;
    private boolean finalVersionNotificationEnabled;
    private boolean betaVersionNotificationEnabled;
    private boolean songNotificationEnabled;
    private boolean queueFollowingSongs;
    private boolean lastFmEnabled;
    private int listReloadDelay;
    private String lastFmUsername;
    private String lastFmPassword;
    private boolean isReloadNeeded;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLocaleIndex() {
        return localeIndex;
    }

    public void setLocaleIndex(String localeIndex) {
        this.localeIndex = localeIndex;
    }

    public String[] getLocales() {
        return locales;
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }

    public String getThemeIndex() {
        return themeIndex;
    }

    public void setThemeIndex(String themeIndex) {
        this.themeIndex = themeIndex;
    }

    public Theme[] getThemes() {
        return themes;
    }

    public void setThemes(Theme[] themes) {
        this.themes = themes;
    }

    public String getAlbumListId() {
        return albumListId;
    }

    public void setAlbumListId(String albumListId) {
        this.albumListId = albumListId;
    }

    public AlbumListType[] getAlbumLists() {
        return albumLists;
    }

    public void setAlbumLists(AlbumListType[] albumLists) {
        this.albumLists = albumLists;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public List<Avatar> getAvatars() {
        return avatars;
    }

    public void setAvatars(List<Avatar> avatars) {
        this.avatars = avatars;
    }

    public Avatar getCustomAvatar() {
        return customAvatar;
    }

    public void setCustomAvatar(Avatar customAvatar) {
        this.customAvatar = customAvatar;
    }

    public UserSettings.Visibility getMainVisibility() {
        return mainVisibility;
    }

    public void setMainVisibility(UserSettings.Visibility mainVisibility) {
        this.mainVisibility = mainVisibility;
    }

    public UserSettings.Visibility getPlaylistVisibility() {
        return playlistVisibility;
    }

    public void setPlaylistVisibility(UserSettings.Visibility playlistVisibility) {
        this.playlistVisibility = playlistVisibility;
    }

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public boolean isShowNowPlayingEnabled() {
        return showNowPlayingEnabled;
    }

    public void setShowNowPlayingEnabled(boolean showNowPlayingEnabled) {
        this.showNowPlayingEnabled = showNowPlayingEnabled;
    }

    public boolean isShowChatEnabled() {
        return showChatEnabled;
    }

    public void setShowChatEnabled(boolean showChatEnabled) {
        this.showChatEnabled = showChatEnabled;
    }

    public boolean isShowArtistInfoEnabled() {
        return showArtistInfoEnabled;
    }

    public void setShowArtistInfoEnabled(boolean showArtistInfoEnabled) {
        this.showArtistInfoEnabled = showArtistInfoEnabled;
    }

    public boolean isNowPlayingAllowed() {
        return nowPlayingAllowed;
    }

    public void setNowPlayingAllowed(boolean nowPlayingAllowed) {
        this.nowPlayingAllowed = nowPlayingAllowed;
    }

    public boolean isFinalVersionNotificationEnabled() {
        return finalVersionNotificationEnabled;
    }

    public void setFinalVersionNotificationEnabled(boolean finalVersionNotificationEnabled) {
        this.finalVersionNotificationEnabled = finalVersionNotificationEnabled;
    }

    public boolean isBetaVersionNotificationEnabled() {
        return betaVersionNotificationEnabled;
    }

    public void setBetaVersionNotificationEnabled(boolean betaVersionNotificationEnabled) {
        this.betaVersionNotificationEnabled = betaVersionNotificationEnabled;
    }

    public void setSongNotificationEnabled(boolean songNotificationEnabled) {
        this.songNotificationEnabled = songNotificationEnabled;
    }

    public boolean isSongNotificationEnabled() {
        return songNotificationEnabled;
    }

    public boolean isAutoHidePlayQueue() {
        return autoHidePlayQueue;
    }

    public void setAutoHidePlayQueue(boolean autoHidePlayQueue) {
        this.autoHidePlayQueue = autoHidePlayQueue;
    }

    public boolean isKeyboardShortcutsEnabled() {
        return keyboardShortcutsEnabled;
    }

    public void setKeyboardShortcutsEnabled(boolean keyboardShortcutsEnabled) {
        this.keyboardShortcutsEnabled = keyboardShortcutsEnabled;
    }

    public boolean isLastFmEnabled() {
        return lastFmEnabled;
    }

    public void setLastFmEnabled(boolean lastFmEnabled) {
        this.lastFmEnabled = lastFmEnabled;
    }

    public int getListReloadDelay() {
        return listReloadDelay;
    }

    public void setListReloadDelay(int listReloadDelay) {
        this.listReloadDelay = listReloadDelay;
    }

    public String getLastFmUsername() {
        return lastFmUsername;
    }

    public void setLastFmUsername(String lastFmUsername) {
        this.lastFmUsername = lastFmUsername;
    }

    public String getLastFmPassword() {
        return lastFmPassword;
    }

    public void setLastFmPassword(String lastFmPassword) {
        this.lastFmPassword = lastFmPassword;
    }

    public boolean isReloadNeeded() {
        return isReloadNeeded;
    }

    public void setReloadNeeded(boolean reloadNeeded) {
        isReloadNeeded = reloadNeeded;
    }

    public boolean isQueueFollowingSongs() {
        return queueFollowingSongs;
    }

    public void setQueueFollowingSongs(boolean queueFollowingSongs) {
        this.queueFollowingSongs = queueFollowingSongs;
    }
}
