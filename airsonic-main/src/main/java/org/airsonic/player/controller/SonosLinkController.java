/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.SonosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Controller for the page used to administrate the Sonos music service settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/sonoslink")
public class SonosLinkController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SonosService sonosService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView doGet(HttpServletRequest request) {
        String linkCode = request.getParameter("linkCode");

        String household = securityService.getHousehold(linkCode);

        String view = household != null ? "sonosLinkLogin" : "sonosLinkNotFound";

        Map<String,Object> model = newHashMap();
        model.put("linkCode", linkCode);

        return new ModelAndView(view, "model", model);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView doPost(HttpServletRequest request) {
        String linkCode = request.getParameter("linkCode");
        String j_username = request.getParameter("j_username");
        String j_password = request.getParameter("j_password");

        String household = securityService.getHousehold(linkCode);
        if (household == null) {
            Map<String,Object> model = newHashMap();
            model.put("linkCode", linkCode);
            return new ModelAndView("sonosLinkNotFound", "model", model);
        }

        try {
            Authentication authenticate = securityService.authenticate(j_username, j_password);
            if (authenticate.isAuthenticated()) {
                securityService.authoriseSonos(j_username, household, linkCode);
                return new ModelAndView("sonosSuccess", "model", newHashMap());
            } else {
                return loginFailed(linkCode);
            }
        } catch (BadCredentialsException e) {
            return loginFailed(linkCode);
        }
    }

    private ModelAndView loginFailed(String linkCode) {
        Map<String,Object> model = newHashMap();
        model.put("linkCode", linkCode);
        model.put("error", true);

        return new ModelAndView("sonosLinkLogin", "model", model);
    }
}
