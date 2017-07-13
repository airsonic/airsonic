/*
 * This file is part of Airsonic.
 *
 * Airsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Airsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2013 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.UPnPService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the page used to administrate the UPnP/DLNA server settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/dlnaSettings")
public class DLNASettingsController {

    @Autowired
    private UPnPService upnpService;

    @Autowired
    private SettingsService settingsService;

    @RequestMapping(method = RequestMethod.GET)
    public String handleGet(Model model) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("dlnaEnabled", settingsService.isDlnaEnabled());
        map.put("dlnaServerName", settingsService.getDlnaServerName());
        map.put("dlnaBaseLANURL", settingsService.getDlnaBaseLANURL());

        model.addAttribute("model", map);
        return "dlnaSettings";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String handlePost(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {
        handleParameters(request);
        redirectAttributes.addFlashAttribute("settings_toast", true);
        return "redirect:dlnaSettings.view";
    }

    private void handleParameters(HttpServletRequest request) {
        boolean dlnaEnabled = ServletRequestUtils.getBooleanParameter(request, "dlnaEnabled", false);
        String dlnaServerName = StringUtils.trimToNull(request.getParameter("dlnaServerName"));
        String dlnaBaseLANURL = StringUtils.trimToNull(request.getParameter("dlnaBaseLANURL"));
        if (dlnaServerName == null) {
            dlnaServerName = "Airsonic";
        }

        upnpService.setMediaServerEnabled(false);
        settingsService.setDlnaEnabled(dlnaEnabled);
        settingsService.setDlnaServerName(dlnaServerName);
        settingsService.setDlnaBaseLANURL(dlnaBaseLANURL);
        settingsService.save();
        upnpService.setMediaServerEnabled(dlnaEnabled);
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setUpnpService(UPnPService upnpService) {
        this.upnpService = upnpService;
    }
}
