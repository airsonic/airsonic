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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.SimpleFormController;

import org.libresonic.player.command.GeneralSettingsCommand;
import org.libresonic.player.domain.Theme;
import org.libresonic.player.service.SettingsService;

/**
 * Controller for the page used to administrate general settings.
 *
 * @author Sindre Mehus
 */
public class GeneralSettingsController extends SimpleFormController {

    private SettingsService settingsService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        GeneralSettingsCommand command = new GeneralSettingsCommand();
        command.setCoverArtFileTypes(settingsService.getCoverArtFileTypes());
        command.setIgnoredArticles(settingsService.getIgnoredArticles());
        command.setShortcuts(settingsService.getShortcuts());
        command.setIndex(settingsService.getIndexString());
        command.setPlaylistFolder(settingsService.getPlaylistFolder());
        command.setMusicFileTypes(settingsService.getMusicFileTypes());
        command.setVideoFileTypes(settingsService.getVideoFileTypes());
        command.setSortAlbumsByYear(settingsService.isSortAlbumsByYear());
        command.setGettingStartedEnabled(settingsService.isGettingStartedEnabled());
        command.setWelcomeTitle(settingsService.getWelcomeTitle());
        command.setWelcomeSubtitle(settingsService.getWelcomeSubtitle());
        command.setWelcomeMessage(settingsService.getWelcomeMessage());
        command.setLoginMessage(settingsService.getLoginMessage());

        Theme[] themes = settingsService.getAvailableThemes();
        command.setThemes(themes);
        String currentThemeId = settingsService.getThemeId();
        for (int i = 0; i < themes.length; i++) {
            if (currentThemeId.equals(themes[i].getId())) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        Locale currentLocale = settingsService.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);

            if (currentLocale.equals(locales[i])) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        return command;

    }

    protected void doSubmitAction(Object comm) throws Exception {
        GeneralSettingsCommand command = (GeneralSettingsCommand) comm;

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        Theme theme = settingsService.getAvailableThemes()[themeIndex];

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = settingsService.getAvailableLocales()[localeIndex];

        command.setToast(true);
        command.setReloadNeeded(!settingsService.getIndexString().equals(command.getIndex()) ||
                                !settingsService.getIgnoredArticles().equals(command.getIgnoredArticles()) ||
                                !settingsService.getShortcuts().equals(command.getShortcuts()) ||
                                !settingsService.getThemeId().equals(theme.getId()) ||
                                !settingsService.getLocale().equals(locale));

        settingsService.setIndexString(command.getIndex());
        settingsService.setIgnoredArticles(command.getIgnoredArticles());
        settingsService.setShortcuts(command.getShortcuts());
        settingsService.setPlaylistFolder(command.getPlaylistFolder());
        settingsService.setMusicFileTypes(command.getMusicFileTypes());
        settingsService.setVideoFileTypes(command.getVideoFileTypes());
        settingsService.setCoverArtFileTypes(command.getCoverArtFileTypes());
        settingsService.setSortAlbumsByYear(command.isSortAlbumsByYear());
        settingsService.setGettingStartedEnabled(command.isGettingStartedEnabled());
        settingsService.setWelcomeTitle(command.getWelcomeTitle());
        settingsService.setWelcomeSubtitle(command.getWelcomeSubtitle());
        settingsService.setWelcomeMessage(command.getWelcomeMessage());
        settingsService.setLoginMessage(command.getLoginMessage());
        settingsService.setThemeId(theme.getId());
        settingsService.setLocale(locale);
        settingsService.save();
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
