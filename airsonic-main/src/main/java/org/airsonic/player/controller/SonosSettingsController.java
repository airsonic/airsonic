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
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
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
    public ModelAndView doGet(HttpServletRequest request, Model model) throws Exception {
        return new ModelAndView("sonosSettings", "model", getModel(request));
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView doPost(HttpServletRequest request) throws Exception {

        boolean sonosEnabled = ServletRequestUtils.getBooleanParameter(request, "sonosEnabled", false);
        String sonosServiceName = StringUtils.trimToNull(request.getParameter("sonosServiceName"));
        if (sonosServiceName == null) {
            sonosServiceName = "Airsonic";
        }

        settingsService.setSonosLinkMethod(request.getParameter("sonosLinkMethod"));
        settingsService.setSonosEnabled(sonosEnabled);
        settingsService.setSonosServiceName(sonosServiceName);
        settingsService.setSonosCallbackHostAddress(request.getParameter("callbackHostAddress"));
        settingsService.save();

        List<String> returnCodes = sonosService.setMusicServiceEnabled(sonosEnabled, settingsService.getSonosCallbackHostAddress());


        Map<String, Object> map = getModel(request);

        map.put("returnCodes", returnCodes);

        return new ModelAndView("sonosSettings", "model", map);
    }

    private  Map<String, Object> getModel(HttpServletRequest request){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("sonosEnabled", settingsService.isSonosEnabled());
        map.put("sonosServiceName", settingsService.getSonosServiceName());
        map.put("sonosLinkMethod", settingsService.getSonosLinkMethod());
        map.put("callbackHostAddress", settingsService.getSonosCallbackHostAddress(NetworkService.getBaseUrl(request)));

        return map;
    }
}
