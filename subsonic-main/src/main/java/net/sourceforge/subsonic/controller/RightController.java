/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.VersionService;

/**
 * Controller for the right frame.
 *
 * @author Sindre Mehus
 */
public class RightController extends ParameterizableViewController {

    private SettingsService settingsService;
    private SecurityService securityService;
    private VersionService versionService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        ModelAndView result = super.handleRequestInternal(request, response);

        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        if (userSettings.isFinalVersionNotificationEnabled() && versionService.isNewFinalVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestFinalVersion());

        } else if (userSettings.isBetaVersionNotificationEnabled() && versionService.isNewBetaVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestBetaVersion());
        }

        map.put("brand", settingsService.getBrand());
        map.put("showNowPlaying", userSettings.isShowNowPlayingEnabled());
        map.put("showChat", userSettings.isShowChatEnabled());
        map.put("user", securityService.getCurrentUser(request));
        map.put("licenseInfo", settingsService.getLicenseInfo());

        result.addObject("model", map);
        return result;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }
}