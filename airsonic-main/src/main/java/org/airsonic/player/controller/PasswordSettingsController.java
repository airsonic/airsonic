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

import org.airsonic.player.command.PasswordSettingsCommand;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.validator.PasswordSettingsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for the page used to change password.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping("/passwordSettings")
public class PasswordSettingsController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PasswordSettingsValidator passwordSettingsValidator;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(passwordSettingsValidator);
    }


    @GetMapping
    protected ModelAndView displayForm(HttpServletRequest request) throws Exception {
        PasswordSettingsCommand command = new PasswordSettingsCommand();
        User user = securityService.getCurrentUser(request);
        command.setUsername(user.getUsername());
        command.setLdapAuthenticated(user.isLdapAuthenticated());
        return new ModelAndView("passwordSettings","command",command);
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") @Validated PasswordSettingsCommand command, BindingResult bindingResult, RedirectAttributes redirectAttributes) throws Exception {
        if (!bindingResult.hasErrors()) {
            User user = securityService.getUserByName(command.getUsername());
            user.setPassword(command.getPassword());
            securityService.updateUser(user);

            command.setPassword(null);
            command.setConfirmPassword(null);
            redirectAttributes.addFlashAttribute("settings_toast", true);
            return "redirect:passwordSettings.view";
        } else {
            return "passwordSettings";
        }
    }

}
