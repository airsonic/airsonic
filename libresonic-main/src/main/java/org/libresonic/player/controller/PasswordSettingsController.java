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

import org.springframework.web.servlet.mvc.*;
import org.libresonic.player.service.*;
import org.libresonic.player.command.*;
import org.libresonic.player.domain.*;

import javax.servlet.http.*;

/**
 * Controller for the page used to change password.
 *
 * @author Sindre Mehus
 */
public class PasswordSettingsController extends SimpleFormController {

    private SecurityService securityService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        PasswordSettingsCommand command = new PasswordSettingsCommand();
        User user = securityService.getCurrentUser(request);
        command.setUsername(user.getUsername());
        command.setLdapAuthenticated(user.isLdapAuthenticated());
        return command;
    }

    protected void doSubmitAction(Object comm) throws Exception {
        PasswordSettingsCommand command = (PasswordSettingsCommand) comm;
        User user = securityService.getUserByName(command.getUsername());
        user.setPassword(command.getPassword());
        securityService.updateUser(user);

        command.setPassword(null);
        command.setConfirmPassword(null);
        command.setToast(true);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
