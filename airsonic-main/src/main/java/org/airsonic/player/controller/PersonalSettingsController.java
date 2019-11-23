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
package org.airsonic.player.controller;

import org.airsonic.player.command.PersonalSettingsCommand;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Locale;

/**
 * Controller for the page used to administrate per-user settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/personalSettings")
public class PersonalSettingsController  {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request,Model model) {
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
        command.setShowArtistInfoEnabled(userSettings.isShowArtistInfoEnabled());
        command.setNowPlayingAllowed(userSettings.isNowPlayingAllowed());
        command.setMainVisibility(userSettings.getMainVisibility());
        command.setPlaylistVisibility(userSettings.getPlaylistVisibility());
        command.setFinalVersionNotificationEnabled(userSettings.isFinalVersionNotificationEnabled());
        command.setBetaVersionNotificationEnabled(userSettings.isBetaVersionNotificationEnabled());
        command.setSongNotificationEnabled(userSettings.isSongNotificationEnabled());
        command.setAutoHidePlayQueue(userSettings.isAutoHidePlayQueue());
        command.setKeyboardShortcutsEnabled(userSettings.isKeyboardShortcutsEnabled());
        command.setLastFmEnabled(userSettings.isLastFmEnabled());
        command.setLastFmUsername(userSettings.getLastFmUsername());
        command.setLastFmPassword(userSettings.getLastFmPassword());
        command.setListenBrainzEnabled(userSettings.isListenBrainzEnabled());
        command.setListenBrainzToken(userSettings.getListenBrainzToken());
        command.setPaginationSize(userSettings.getPaginationSize());

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

        model.addAttribute("command",command);
    }

    @GetMapping
    protected String displayForm() {
        return "personalSettings";
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") PersonalSettingsCommand command, RedirectAttributes redirectAttributes) {

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
        settings.setShowArtistInfoEnabled(command.isShowArtistInfoEnabled());
        settings.setNowPlayingAllowed(command.isNowPlayingAllowed());
        settings.setMainVisibility(command.getMainVisibility());
        settings.setPlaylistVisibility(command.getPlaylistVisibility());
        settings.setFinalVersionNotificationEnabled(command.isFinalVersionNotificationEnabled());
        settings.setBetaVersionNotificationEnabled(command.isBetaVersionNotificationEnabled());
        settings.setSongNotificationEnabled(command.isSongNotificationEnabled());
        settings.setAutoHidePlayQueue(command.isAutoHidePlayQueue());
        settings.setKeyboardShortcutsEnabled(command.isKeyboardShortcutsEnabled());
        settings.setLastFmEnabled(command.isLastFmEnabled());
        settings.setLastFmUsername(command.getLastFmUsername());
        settings.setListenBrainzEnabled(command.isListenBrainzEnabled());
        settings.setListenBrainzToken(command.getListenBrainzToken());
        settings.setSystemAvatarId(getSystemAvatarId(command));
        settings.setAvatarScheme(getAvatarScheme(command));
        settings.setPaginationSize(command.getPaginationSize());

        if (StringUtils.isNotBlank(command.getLastFmPassword())) {
            settings.setLastFmPassword(command.getLastFmPassword());
        }

        settings.setChanged(new Date());
        settingsService.updateUserSettings(settings);

        redirectAttributes.addFlashAttribute("settings_reload", true);
        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:personalSettings.view";
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

}
