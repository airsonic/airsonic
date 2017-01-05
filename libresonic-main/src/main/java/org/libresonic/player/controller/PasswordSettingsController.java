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

import org.libresonic.player.validator.PasswordSettingsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.*;
import org.libresonic.player.service.*;
import org.libresonic.player.command.*;
import org.libresonic.player.domain.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.*;

/**
 * Controller for the page used to change password.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping("/passwordSettings")
@SessionAttributes( value="command", types={PasswordSettingsCommand.class} )
public class PasswordSettingsController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PasswordSettingsValidator passwordSettingsValidator;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(passwordSettingsValidator);
    }

  /*  @ModelAttribute
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        PasswordSettingsCommand command = new PasswordSettingsCommand();
        User user = securityService.getCurrentUser(request);
        command.setUsername(user.getUsername());
        command.setLdapAuthenticated(user.isLdapAuthenticated());
        return command;
    }*/

    @RequestMapping(method = RequestMethod.GET)
    protected ModelAndView displayForm(HttpServletRequest request) throws Exception {
        PasswordSettingsCommand command = new PasswordSettingsCommand();
        User user = securityService.getCurrentUser(request);
        command.setUsername(user.getUsername());
        command.setLdapAuthenticated(user.isLdapAuthenticated());
        return new ModelAndView("passwordSettings","command",command);
    }

    @RequestMapping(method = RequestMethod.POST)
    protected String doSubmitAction(@ModelAttribute("command") @Validated PasswordSettingsCommand command) throws Exception {
        User user = securityService.getUserByName(command.getUsername());
        user.setPassword(command.getPassword());
        securityService.updateUser(user);

        command.setPassword(null);
        command.setConfirmPassword(null);
        command.setToast(true);
        return "passwordSettings";
    }

}
