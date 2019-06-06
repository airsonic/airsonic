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

import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the top frame.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/top")
public class TopController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws Exception {
        Map<String, Object> map = new HashMap<>();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        map.put("user", user);
        map.put("showSideBar", userSettings.isShowSideBar());
        map.put("showAvatar", userSettings.getAvatarScheme() != AvatarScheme.NONE);
        return new ModelAndView("top","model", map);
    }
}
