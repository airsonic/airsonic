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

import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.SonosService;
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
 * Controller for the page used to administrate the Sonos music service settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/sonosSettings")
public class SonosSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SonosService sonosService;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet(Model model) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("sonosEnabled", settingsService.isSonosEnabled());
        map.put("sonosServiceName", settingsService.getSonosServiceName());

        model.addAttribute("model", map);
        return "sonosSettings";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {
        handleParameters(request);

        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:sonosSettings.view";
    }

    private void handleParameters(HttpServletRequest request) {
        boolean sonosEnabled = ServletRequestUtils.getBooleanParameter(request, "sonosEnabled", false);
        String sonosServiceName = StringUtils.trimToNull(request.getParameter("sonosServiceName"));
        if (sonosServiceName == null) {
            sonosServiceName = "Airsonic";
        }

        settingsService.setSonosEnabled(sonosEnabled);
        settingsService.setSonosServiceName(sonosServiceName);
        settingsService.save();

        sonosService.setMusicServiceEnabled(false, NetworkService.getBaseUrl(request));
        sonosService.setMusicServiceEnabled(sonosEnabled, NetworkService.getBaseUrl(request));
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSonosService(SonosService sonosService) {
        this.sonosService = sonosService;
    }
}
