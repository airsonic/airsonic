/*
 * This file is part of Libresonic.
 *
 *  Libresonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libresonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import org.libresonic.player.domain.PodcastChannel;
import org.libresonic.player.domain.PodcastEpisode;
import org.libresonic.player.service.PodcastService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;

/**
 * Controller for the "Podcast channels" page.
 *
 * @author Sindre Mehus
 */
public class PodcastChannelsController extends ParameterizableViewController {

    private PodcastService podcastService;
    private SecurityService securityService;
    private SettingsService settingsService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);

        Map<PodcastChannel, List<PodcastEpisode>> channels = new LinkedHashMap<PodcastChannel, List<PodcastEpisode>>();
        Map<Integer, PodcastChannel> channelMap = new HashMap<Integer, PodcastChannel>();
        for (PodcastChannel channel : podcastService.getAllChannels()) {
            channels.put(channel, podcastService.getEpisodes(channel.getId()));
            channelMap.put(channel.getId(), channel);
        }

        map.put("user", securityService.getCurrentUser(request));
        map.put("channels", channels);
        map.put("channelMap", channelMap);
        map.put("newestEpisodes", podcastService.getNewestEpisodes(10));
        return result;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
