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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.libresonic.player.command.PremiumSettingsCommand;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;

/**
 * Controller for the Subsonic Premium page.
 *
 * @author Sindre Mehus
 */
public class PremiumSettingsController extends SimpleFormController {

    private SettingsService settingsService;
    private SecurityService securityService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        PremiumSettingsCommand command = new PremiumSettingsCommand();
        command.setPath(request.getParameter("path"));
        command.setForceChange(request.getParameter("change") != null);
        command.setLicenseInfo(settingsService.getLicenseInfo());
        command.setBrand(settingsService.getBrand());
        command.setUser(securityService.getCurrentUser(request));
        return command;
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object com, BindException errors)
            throws Exception {
        PremiumSettingsCommand command = (PremiumSettingsCommand) com;
        Date now = new Date();

        settingsService.setLicenseCode(command.getLicenseCode());
        settingsService.setLicenseEmail(command.getLicenseInfo().getLicenseEmail());
        settingsService.setLicenseDate(now);
        settingsService.save();
        settingsService.scheduleLicenseValidation();

        // Reflect changes in view. The validator will validate the license asynchronously.
        command.setLicenseInfo(settingsService.getLicenseInfo());
        command.setToast(true);

        return new ModelAndView(getSuccessView(), errors.getModel());
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}