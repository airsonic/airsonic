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

import org.airsonic.player.domain.Avatar;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller which produces avatar images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/avatar")
public class AvatarController implements LastModified {

    @Autowired
    private SettingsService settingsService;

    public long getLastModified(HttpServletRequest request) {
        Avatar avatar = getAvatar(request);
        long result = avatar == null ? -1L : avatar.getCreatedDate().getTime();

        String username = request.getParameter("username");
        if (username != null) {
            UserSettings userSettings = settingsService.getUserSettings(username);
            result = Math.max(result, userSettings.getChanged().getTime());
        }

        return result;
    }

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Avatar avatar = getAvatar(request);

        if (avatar == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(avatar.getMimeType());
        response.getOutputStream().write(avatar.getData());
    }

    private Avatar getAvatar(HttpServletRequest request) {
        String id = request.getParameter("id");
        boolean forceCustom = ServletRequestUtils.getBooleanParameter(request, "forceCustom", false);

        if (id != null) {
            return settingsService.getSystemAvatar(Integer.parseInt(id));
        }

        String username = request.getParameter("username");
        if (username == null) {
            return null;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);
        if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM || forceCustom) {
            return settingsService.getCustomAvatar(username);
        }
        if(userSettings.getAvatarScheme() == AvatarScheme.NONE) {
            return null;
        }
        return settingsService.getSystemAvatar(userSettings.getSystemAvatarId());
    }

}
