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
package org.libresonic.player.validator;

import org.libresonic.player.command.UserSettingsCommand;
import org.libresonic.player.controller.UserSettingsController;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator for {@link UserSettingsController}.
 *
 * @author Sindre Mehus
 */
public class UserSettingsValidator implements Validator {

    private SecurityService securityService;
    private SettingsService settingsService;

    /**
     * {@inheritDoc}
     */
    public boolean supports(Class clazz) {
        return clazz.equals(UserSettingsCommand.class);
    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object obj, Errors errors) {
        UserSettingsCommand command = (UserSettingsCommand) obj;
        String username = command.getUsername();
        String email = StringUtils.trimToNull(command.getEmail());
        String password = StringUtils.trimToNull(command.getPassword());
        String confirmPassword = command.getConfirmPassword();

        if (command.isNewUser()) {
            if (username == null || username.length() == 0) {
                errors.rejectValue("username", "usersettings.nousername");
            } else if (securityService.getUserByName(username) != null) {
                errors.rejectValue("username", "usersettings.useralreadyexists");
            } else if (email == null) {
                errors.rejectValue("email", "usersettings.noemail");
            } else if (command.isLdapAuthenticated() && !settingsService.isLdapEnabled()) {
                errors.rejectValue("password", "usersettings.ldapdisabled");
            } else if (command.isLdapAuthenticated() && password != null) {
                errors.rejectValue("password", "usersettings.passwordnotsupportedforldap");
            }
        }

        if ((command.isNewUser() || command.isPasswordChange()) && !command.isLdapAuthenticated()) {
            if (password == null) {
                errors.rejectValue("password", "usersettings.nopassword");
            } else if (!password.equals(confirmPassword)) {
                errors.rejectValue("password", "usersettings.wrongpassword");
            }
        }

        if (command.isPasswordChange() && command.isLdapAuthenticated()) {
            errors.rejectValue("password", "usersettings.passwordnotsupportedforldap");
        }

    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}