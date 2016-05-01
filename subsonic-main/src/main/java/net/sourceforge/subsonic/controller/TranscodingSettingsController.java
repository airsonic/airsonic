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

import net.sourceforge.subsonic.domain.Transcoding;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the page used to administrate the set of transcoding configurations.
 *
 * @author Sindre Mehus
 */
public class TranscodingSettingsController extends ParameterizableViewController {

    private TranscodingService transcodingService;
    private SettingsService settingsService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        if (isFormSubmission(request)) {
            handleParameters(request, map);
            map.put("toast", true);
        }

        ModelAndView result = super.handleRequestInternal(request, response);
        map.put("transcodings", transcodingService.getAllTranscodings());
        map.put("transcodeDirectory", transcodingService.getTranscodeDirectory());
        map.put("downsampleCommand", settingsService.getDownsamplingCommand());
        map.put("hlsCommand", settingsService.getHlsCommand());
        map.put("brand", settingsService.getBrand());

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

    private void handleParameters(HttpServletRequest request, Map<String, Object> map) {

        for (Transcoding transcoding : transcodingService.getAllTranscodings()) {
            Integer id = transcoding.getId();
            String name = getParameter(request, "name", id);
            String sourceFormats = getParameter(request, "sourceFormats", id);
            String targetFormat = getParameter(request, "targetFormat", id);
            String step1 = getParameter(request, "step1", id);
            String step2 = getParameter(request, "step2", id);
            boolean delete = getParameter(request, "delete", id) != null;

            if (delete) {
                transcodingService.deleteTranscoding(id);
            } else if (name == null) {
                map.put("error", "transcodingsettings.noname");
            } else if (sourceFormats == null) {
                map.put("error", "transcodingsettings.nosourceformat");
            } else if (targetFormat == null) {
                map.put("error", "transcodingsettings.notargetformat");
            } else if (step1 == null) {
                map.put("error", "transcodingsettings.nostep1");
            } else {
                transcoding.setName(name);
                transcoding.setSourceFormats(sourceFormats);
                transcoding.setTargetFormat(targetFormat);
                transcoding.setStep1(step1);
                transcoding.setStep2(step2);
                transcodingService.updateTranscoding(transcoding);
            }
        }

        String name = StringUtils.trimToNull(request.getParameter("name"));
        String sourceFormats = StringUtils.trimToNull(request.getParameter("sourceFormats"));
        String targetFormat = StringUtils.trimToNull(request.getParameter("targetFormat"));
        String step1 = StringUtils.trimToNull(request.getParameter("step1"));
        String step2 = StringUtils.trimToNull(request.getParameter("step2"));
        boolean defaultActive = request.getParameter("defaultActive") != null;

        if (name != null || sourceFormats != null || targetFormat != null || step1 != null || step2 != null) {
            Transcoding transcoding = new Transcoding(null, name, sourceFormats, targetFormat, step1, step2, null, defaultActive);
            if (name == null) {
                map.put("error", "transcodingsettings.noname");
            } else if (sourceFormats == null) {
                map.put("error", "transcodingsettings.nosourceformat");
            } else if (targetFormat == null) {
                map.put("error", "transcodingsettings.notargetformat");
            } else if (step1 == null) {
                map.put("error", "transcodingsettings.nostep1");
            } else {
                transcodingService.createTranscoding(transcoding);
            }
            if (map.containsKey("error")) {
                map.put("newTranscoding", transcoding);
            }
        }
        settingsService.setDownsamplingCommand(StringUtils.trim(request.getParameter("downsampleCommand")));
        settingsService.setHlsCommand(StringUtils.trim(request.getParameter("hlsCommand")));
        settingsService.save();
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
