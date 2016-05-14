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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import org.libresonic.player.domain.Avatar;
import org.libresonic.player.domain.AvatarScheme;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.SettingsService;

/**
 * Controller which produces avatar images.
 *
 * @author Sindre Mehus
 */
public class AvatarController implements Controller, LastModified {

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

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Avatar avatar = getAvatar(request);

        if (avatar == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        response.setContentType(avatar.getMimeType());
        response.getOutputStream().write(avatar.getData());
        return null;
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
        return settingsService.getSystemAvatar(userSettings.getSystemAvatarId());
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}