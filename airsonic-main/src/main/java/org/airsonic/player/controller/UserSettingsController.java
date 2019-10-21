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

import org.airsonic.player.command.UserSettingsCommand;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.Util;
import org.airsonic.player.validator.UserSettingsValidator;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controller for the page used to administrate users.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/userSettings")
public class UserSettingsController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private TranscodingService transcodingService;

    @InitBinder
    protected void initBinder(WebDataBinder binder, HttpServletRequest request) {
        binder.addValidators(new UserSettingsValidator(securityService, settingsService, request));
    }

    @GetMapping
    protected String displayForm(HttpServletRequest request, Model model) throws Exception {
        UserSettingsCommand command;
        if (!model.containsAttribute("command")) {
            command = new UserSettingsCommand();

            User user = getUser(request);
            if (user != null) {
                command.setUser(user);
                command.setEmail(user.getEmail());
                UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
                command.setTranscodeSchemeName(userSettings.getTranscodeScheme().name());
                command.setAllowedMusicFolderIds(Util.toIntArray(getAllowedMusicFolderIds(user)));
                command.setCurrentUser(securityService.getCurrentUser(request).getUsername().equals(user.getUsername()));
            } else {
                command.setNewUser(true);
                command.setStreamRole(true);
                command.setSettingsRole(true);
            }

        } else {
            command = (UserSettingsCommand) model.asMap().get("command");
        }
        command.setUsers(securityService.getAllUsers());
        command.setTranscodingSupported(transcodingService.isDownsamplingSupported(null));
        command.setTranscodeDirectory(transcodingService.getTranscodeDirectory().getPath());
        command.setTranscodeSchemes(TranscodeScheme.values());
        command.setLdapEnabled(settingsService.isLdapEnabled());
        command.setAllMusicFolders(settingsService.getAllMusicFolders());
        model.addAttribute("command", command);
        return "userSettings";
    }

    private User getUser(HttpServletRequest request) throws ServletRequestBindingException {
        Integer userIndex = ServletRequestUtils.getIntParameter(request, "userIndex");
        if (userIndex != null) {
            List<User> allUsers = securityService.getAllUsers();
            if (userIndex >= 0 && userIndex < allUsers.size()) {
                return allUsers.get(userIndex);
            }
        }
        return null;
    }

    private List<Integer> getAllowedMusicFolderIds(User user) {
        List<Integer> result = new ArrayList<Integer>();
        List<MusicFolder> allowedMusicFolders = user == null
                                                ? settingsService.getAllMusicFolders()
                                                : settingsService.getMusicFoldersForUser(user.getUsername());

        for (MusicFolder musicFolder : allowedMusicFolders) {
            result.add(musicFolder.getId());
        }
        return result;
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") @Validated UserSettingsCommand command, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            if (command.isDeleteUser()) {
                deleteUser(command);
            } else if (command.isNewUser()) {
                createUser(command);
            } else {
                updateUser(command);
            }
            redirectAttributes.addFlashAttribute("settings_reload", true);
            redirectAttributes.addFlashAttribute("settings_toast", true);
        } else {
            redirectAttributes.addFlashAttribute("command", command);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.command", bindingResult);
            redirectAttributes.addFlashAttribute("userIndex", getUserIndex(command));
        }

        return "redirect:userSettings.view";
    }

    private Integer getUserIndex(UserSettingsCommand command) {
        List<User> allUsers = securityService.getAllUsers();
        for (int i = 0; i < allUsers.size(); i++) {
            if (StringUtils.equalsIgnoreCase(allUsers.get(i).getUsername(), command.getUsername())) {
                return i;
            }
        }
        return null;
    }

    private void deleteUser(UserSettingsCommand command) {
        securityService.deleteUser(command.getUsername());
    }

    public void createUser(UserSettingsCommand command) {
        User user = new User(command.getUsername(), command.getPassword(), StringUtils.trimToNull(command.getEmail()));
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        securityService.createUser(user);
        updateUser(command);
    }

    public void updateUser(UserSettingsCommand command) {
        User user = securityService.getUserByName(command.getUsername());
        user.setEmail(StringUtils.trimToNull(command.getEmail()));
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        user.setAdminRole(command.isAdminRole());
        user.setDownloadRole(command.isDownloadRole());
        user.setUploadRole(command.isUploadRole());
        user.setCoverArtRole(command.isCoverArtRole());
        user.setCommentRole(command.isCommentRole());
        user.setPodcastRole(command.isPodcastRole());
        user.setStreamRole(command.isStreamRole());
        user.setJukeboxRole(command.isJukeboxRole());
        user.setSettingsRole(command.isSettingsRole());
        user.setShareRole(command.isShareRole());

        if (command.isPasswordChange()) {
            user.setPassword(command.getPassword());
        }

        securityService.updateUser(user);

        UserSettings userSettings = settingsService.getUserSettings(command.getUsername());
        userSettings.setTranscodeScheme(TranscodeScheme.valueOf(command.getTranscodeSchemeName()));
        userSettings.setChanged(new Date());
        settingsService.updateUserSettings(userSettings);

        List<Integer> allowedMusicFolderIds = Util.toIntegerList(command.getAllowedMusicFolderIds());
        settingsService.setMusicFoldersForUser(command.getUsername(), allowedMusicFolderIds);
    }

}
