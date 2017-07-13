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

import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the page used to administrate the set of internet radio/tv stations.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/internetRadioSettings")
public class InternetRadioSettingsController {

    @Autowired
    private SettingsService settingsService;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet(Model model) throws Exception {

        Map<String, Object> map = new HashMap<>();

        map.put("internetRadios", settingsService.getAllInternetRadios(true));

        model.addAttribute("model", map);
        return "internetRadioSettings";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {

        String error = handleParameters(request);
        Map<String, Object> map = new HashMap<>();
        if(error == null) {
            redirectAttributes.addFlashAttribute("settings_toast", true);
            redirectAttributes.addFlashAttribute("settings_reload", true);
        }
        redirectAttributes.addFlashAttribute("error", error);
        return "redirect:internetRadioSettings.view";
    }

    private String handleParameters(HttpServletRequest request) {
        List<InternetRadio> radios = settingsService.getAllInternetRadios(true);
        for (InternetRadio radio : radios) {
            Integer id = radio.getId();
            String streamUrl = getParameter(request, "streamUrl", id);
            String homepageUrl = getParameter(request, "homepageUrl", id);
            String name = getParameter(request, "name", id);
            boolean enabled = getParameter(request, "enabled", id) != null;
            boolean delete = getParameter(request, "delete", id) != null;

            if (delete) {
                settingsService.deleteInternetRadio(id);
            } else {
                if (name == null) {
                    return "internetradiosettings.noname";
                }
                if (streamUrl == null) {
                    return "internetradiosettings.nourl";
                }
                settingsService.updateInternetRadio(new InternetRadio(id, name, streamUrl, homepageUrl, enabled, new Date()));
            }
        }

        String name = StringUtils.trimToNull(request.getParameter("name"));
        String streamUrl = StringUtils.trimToNull(request.getParameter("streamUrl"));
        String homepageUrl = StringUtils.trimToNull(request.getParameter("homepageUrl"));
        boolean enabled = StringUtils.trimToNull(request.getParameter("enabled")) != null;

        if (name != null || streamUrl != null || homepageUrl != null) {
            if (name == null) {
                return "internetradiosettings.noname";
            }
            if (streamUrl == null) {
                return "internetradiosettings.nourl";
            }
            settingsService.createInternetRadio(new InternetRadio(name, streamUrl, homepageUrl, enabled, new Date()));
        }

        return null;
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

}
