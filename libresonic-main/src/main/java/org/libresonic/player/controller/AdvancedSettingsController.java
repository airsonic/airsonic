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

import org.libresonic.player.command.AdvancedSettingsCommand;
import org.libresonic.player.service.SettingsService;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for the page used to administrate advanced settings.
 *
 * @author Sindre Mehus
 */
public class AdvancedSettingsController extends SimpleFormController {

    private SettingsService settingsService;

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        AdvancedSettingsCommand command = new AdvancedSettingsCommand();
        command.setDownloadLimit(String.valueOf(settingsService.getDownloadBitrateLimit()));
        command.setUploadLimit(String.valueOf(settingsService.getUploadBitrateLimit()));
        command.setLdapEnabled(settingsService.isLdapEnabled());
        command.setLdapUrl(settingsService.getLdapUrl());
        command.setLdapSearchFilter(settingsService.getLdapSearchFilter());
        command.setLdapManagerDn(settingsService.getLdapManagerDn());
        command.setLdapAutoShadowing(settingsService.isLdapAutoShadowing());
        command.setBrand(settingsService.getBrand());

        command.setSmtpServer(settingsService.getSmtpServer());
        command.setSmtpEncryption(settingsService.getSmtpEncryption());
        command.setSmtpPort(settingsService.getSmtpPort());
        command.setSmtpUser(settingsService.getSmtpUser());
        command.setSmtpFrom(settingsService.getSmtpFrom());

        return command;
    }

    @Override
    protected void doSubmitAction(Object comm) throws Exception {
        AdvancedSettingsCommand command = (AdvancedSettingsCommand) comm;

        command.setToast(true);
        command.setReloadNeeded(false);

        try {
            settingsService.setDownloadBitrateLimit(Long.parseLong(command.getDownloadLimit()));
        } catch (NumberFormatException x) { /* Intentionally ignored. */ }
        try {
            settingsService.setUploadBitrateLimit(Long.parseLong(command.getUploadLimit()));
        } catch (NumberFormatException x) { /* Intentionally ignored. */ }

        settingsService.setLdapEnabled(command.isLdapEnabled());
        settingsService.setLdapUrl(command.getLdapUrl());
        settingsService.setLdapSearchFilter(command.getLdapSearchFilter());
        settingsService.setLdapManagerDn(command.getLdapManagerDn());
        settingsService.setLdapAutoShadowing(command.isLdapAutoShadowing());

        if (StringUtils.isNotEmpty(command.getLdapManagerPassword())) {
            settingsService.setLdapManagerPassword(command.getLdapManagerPassword());
        }

        settingsService.setSmtpServer(command.getSmtpServer());
        settingsService.setSmtpEncryption(command.getSmtpEncryption());
        settingsService.setSmtpPort(command.getSmtpPort());
        settingsService.setSmtpUser(command.getSmtpUser());
        settingsService.setSmtpFrom(command.getSmtpFrom());

        if (StringUtils.isNotEmpty(command.getSmtpPassword())) {
            settingsService.setSmtpPassword(command.getSmtpPassword());
        }

        settingsService.save();
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
