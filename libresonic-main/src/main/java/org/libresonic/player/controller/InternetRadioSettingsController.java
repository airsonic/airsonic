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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import org.libresonic.player.domain.InternetRadio;
import org.libresonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the page used to administrate the set of internet radio/tv stations.
 *
 * @author Sindre Mehus
 */
public class InternetRadioSettingsController extends ParameterizableViewController {

    private SettingsService settingsService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        if (isFormSubmission(request)) {
            String error = handleParameters(request);
            map.put("error", error);
            if (error == null) {
                map.put("reload", true);
            }
        }

        ModelAndView result = super.handleRequestInternal(request, response);
        map.put("internetRadios", settingsService.getAllInternetRadios(true));

        result.addObject("model", map);
        return result;
    }

    /**
     * Determine if the given request represents a form submission.
     *
     * @param request current HTTP request
     * @return if the request represents a form submission
     */
    private boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
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

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

}
