/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.command;

import org.airsonic.player.controller.GeneralSettingsController;
import org.airsonic.player.domain.Theme;

/**
 * Command used in {@link GeneralSettingsController}.
 *
 * @author Sindre Mehus
 */
public class GeneralSettingsCommand {

    private String playlistFolder;
    private String musicFileTypes;
    private String videoFileTypes;
    private String coverArtFileTypes;
    private String index;
    private String ignoredArticles;
    private String shortcuts;
    private boolean sortAlbumsByYear;
    private boolean gettingStartedEnabled;
    private String welcomeTitle;
    private String welcomeSubtitle;
    private String welcomeMessage;
    private String loginMessage;
    private String localeIndex;
    private String[] locales;
    private String themeIndex;
    private Theme[] themes;

    public String getPlaylistFolder() {
        return playlistFolder;
    }

    public void setPlaylistFolder(String playlistFolder) {
        this.playlistFolder = playlistFolder;
    }

    public String getMusicFileTypes() {
        return musicFileTypes;
    }

    public void setMusicFileTypes(String musicFileTypes) {
        this.musicFileTypes = musicFileTypes;
    }

    public String getVideoFileTypes() {
        return videoFileTypes;
    }

    public void setVideoFileTypes(String videoFileTypes) {
        this.videoFileTypes = videoFileTypes;
    }

    public String getCoverArtFileTypes() {
        return coverArtFileTypes;
    }

    public void setCoverArtFileTypes(String coverArtFileTypes) {
        this.coverArtFileTypes = coverArtFileTypes;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public void setIgnoredArticles(String ignoredArticles) {
        this.ignoredArticles = ignoredArticles;
    }

    public String getShortcuts() {
        return shortcuts;
    }

    public void setShortcuts(String shortcuts) {
        this.shortcuts = shortcuts;
    }

    public String getWelcomeTitle() {
        return welcomeTitle;
    }

    public void setWelcomeTitle(String welcomeTitle) {
        this.welcomeTitle = welcomeTitle;
    }

    public String getWelcomeSubtitle() {
        return welcomeSubtitle;
    }

    public void setWelcomeSubtitle(String welcomeSubtitle) {
        this.welcomeSubtitle = welcomeSubtitle;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getLoginMessage() {
        return loginMessage;
    }

    public void setLoginMessage(String loginMessage) {
        this.loginMessage = loginMessage;
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

    public void setReloadNeeded(boolean reloadNeeded) {
    }

    public boolean isSortAlbumsByYear() {
        return sortAlbumsByYear;
    }

    public void setSortAlbumsByYear(boolean sortAlbumsByYear) {
        this.sortAlbumsByYear = sortAlbumsByYear;
    }

    public boolean isGettingStartedEnabled() {
        return gettingStartedEnabled;
    }

    public void setGettingStartedEnabled(boolean gettingStartedEnabled) {
        this.gettingStartedEnabled = gettingStartedEnabled;
    }

}
