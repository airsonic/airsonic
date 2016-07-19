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
package org.libresonic.player.controller;

import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.libresonic.player.command.PersonalSettingsCommand;
import org.libresonic.player.domain.AlbumListType;
import org.libresonic.player.domain.AvatarScheme;
import org.libresonic.player.domain.Theme;
import org.libresonic.player.domain.User;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;

/**
 * Controller for the page used to administrate per-user settings.
 *
 * @author Sindre Mehus
 */
public class PersonalSettingsController extends SimpleFormController {

    private SettingsService settingsService;
    private SecurityService securityService;

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        PersonalSettingsCommand command = new PersonalSettingsCommand();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        command.setUser(user);
        command.setLocaleIndex("-1");
        command.setThemeIndex("-1");
        command.setAlbumLists(AlbumListType.values());
        command.setAlbumListId(userSettings.getDefaultAlbumList().getId());
        command.setAvatars(settingsService.getAllSystemAvatars());
        command.setCustomAvatar(settingsService.getCustomAvatar(user.getUsername()));
        command.setAvatarId(getAvatarId(userSettings));
        command.setPartyModeEnabled(userSettings.isPartyModeEnabled());
        command.setQueueFollowingSongs(userSettings.isQueueFollowingSongs());
        command.setShowNowPlayingEnabled(userSettings.isShowNowPlayingEnabled());
        command.setShowChatEnabled(userSettings.isShowChatEnabled());
        command.setShowArtistInfoEnabled(userSettings.isShowArtistInfoEnabled());
        command.setNowPlayingAllowed(userSettings.isNowPlayingAllowed());
        command.setMainVisibility(userSettings.getMainVisibility());
        command.setPlaylistVisibility(userSettings.getPlaylistVisibility());
        command.setFinalVersionNotificationEnabled(userSettings.isFinalVersionNotificationEnabled());
        command.setBetaVersionNotificationEnabled(userSettings.isBetaVersionNotificationEnabled());
        command.setSongNotificationEnabled(userSettings.isSongNotificationEnabled());
        command.setAutoHidePlayQueue(userSettings.isAutoHidePlayQueue());
        command.setListReloadDelay(userSettings.getListReloadDelay());
        command.setLastFmEnabled(userSettings.isLastFmEnabled());
        command.setLastFmUsername(userSettings.getLastFmUsername());
        command.setLastFmPassword(userSettings.getLastFmPassword());

        Locale currentLocale = userSettings.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);
            if (locales[i].equals(currentLocale)) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        String currentThemeId = userSettings.getThemeId();
        Theme[] themes = settingsService.getAvailableThemes();
        command.setThemes(themes);
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].getId().equals(currentThemeId)) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        return command;
    }

    @Override
    protected void doSubmitAction(Object comm) throws Exception {
        PersonalSettingsCommand command = (PersonalSettingsCommand) comm;

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = null;
        if (localeIndex != -1) {
            locale = settingsService.getAvailableLocales()[localeIndex];
        }

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        String themeId = null;
        if (themeIndex != -1) {
            themeId = settingsService.getAvailableThemes()[themeIndex].getId();
        }

        String username = command.getUser().getUsername();
        UserSettings settings = settingsService.getUserSettings(username);

        settings.setLocale(locale);
        settings.setThemeId(themeId);
        settings.setDefaultAlbumList(AlbumListType.fromId(command.getAlbumListId()));
        settings.setPartyModeEnabled(command.isPartyModeEnabled());
        settings.setQueueFollowingSongs(command.isQueueFollowingSongs());
        settings.setShowNowPlayingEnabled(command.isShowNowPlayingEnabled());
        settings.setShowChatEnabled(command.isShowChatEnabled());
        settings.setShowArtistInfoEnabled(command.isShowArtistInfoEnabled());
        settings.setNowPlayingAllowed(command.isNowPlayingAllowed());
        settings.setMainVisibility(command.getMainVisibility());
        settings.setPlaylistVisibility(command.getPlaylistVisibility());
        settings.setFinalVersionNotificationEnabled(command.isFinalVersionNotificationEnabled());
        settings.setBetaVersionNotificationEnabled(command.isBetaVersionNotificationEnabled());
        settings.setSongNotificationEnabled(command.isSongNotificationEnabled());
        settings.setAutoHidePlayQueue(command.isAutoHidePlayQueue());
        settings.setListReloadDelay(command.getListReloadDelay());
        settings.setLastFmEnabled(command.isLastFmEnabled());
        settings.setLastFmUsername(command.getLastFmUsername());
        settings.setSystemAvatarId(getSystemAvatarId(command));
        settings.setAvatarScheme(getAvatarScheme(command));

        if (StringUtils.isNotBlank(command.getLastFmPassword())) {
            settings.setLastFmPassword(command.getLastFmPassword());
        }

        settings.setChanged(new Date());
        settingsService.updateUserSettings(settings);

        command.setReloadNeeded(true);
    }

    private int getAvatarId(UserSettings userSettings) {
        AvatarScheme avatarScheme = userSettings.getAvatarScheme();
        return avatarScheme == AvatarScheme.SYSTEM ? userSettings.getSystemAvatarId() : avatarScheme.getCode();
    }

    private AvatarScheme getAvatarScheme(PersonalSettingsCommand command) {
        if (command.getAvatarId() == AvatarScheme.NONE.getCode()) {
            return AvatarScheme.NONE;
        }
        if (command.getAvatarId() == AvatarScheme.CUSTOM.getCode()) {
            return AvatarScheme.CUSTOM;
        }
        return AvatarScheme.SYSTEM;
    }

    private Integer getSystemAvatarId(PersonalSettingsCommand command) {
        int avatarId = command.getAvatarId();
        if (avatarId == AvatarScheme.NONE.getCode() ||
            avatarId == AvatarScheme.CUSTOM.getCode()) {
            return null;
        }
        return avatarId;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
