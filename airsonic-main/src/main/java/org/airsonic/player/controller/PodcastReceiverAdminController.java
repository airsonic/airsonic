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

import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for the "Podcast receiver" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastReceiverAdmin")
public class PodcastReceiverAdminController {

    @Autowired
    private PodcastService podcastService;

    @RequestMapping(method = { RequestMethod.POST, RequestMethod.GET })
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer channelId = ServletRequestUtils.getIntParameter(request, "channelId");

        if (request.getParameter("add") != null) {
            String url = StringUtils.trim(request.getParameter("add"));
            podcastService.createChannel(url);
            return new ModelAndView(new RedirectView("podcastChannels.view"));
        }
        if (request.getParameter("downloadEpisode") != null && channelId != null) {
            download(StringUtil.parseInts(request.getParameter("downloadEpisode")));
            return new ModelAndView(new RedirectView("podcastChannel.view?id=" + channelId));
        }
        if (request.getParameter("deleteChannel") != null && channelId != null) {
            podcastService.deleteChannel(channelId);
            return new ModelAndView(new RedirectView("podcastChannels.view"));
        }
        if (request.getParameter("deleteEpisode") != null) {
            for (int episodeId : StringUtil.parseInts(request.getParameter("deleteEpisode"))) {
                podcastService.deleteEpisode(episodeId, true);
            }
            return new ModelAndView(new RedirectView("podcastChannel.view?id=" + channelId));
        }
        if (request.getParameter("refresh") != null) {
            if (channelId != null) {
                podcastService.refreshChannel(channelId, true);
                return new ModelAndView(new RedirectView("podcastChannel.view?id=" + channelId));
            } else {
                podcastService.refreshAllChannels(true);
                return new ModelAndView(new RedirectView("podcastChannels.view"));
            }
        }

        return new ModelAndView(new RedirectView("podcastChannels.view"));
    }

    private void download(int[] episodeIds) {
        for (int episodeId : episodeIds) {
            PodcastEpisode episode = podcastService.getEpisode(episodeId, false);
            if (episode != null && episode.getUrl() != null &&
                (episode.getStatus() == PodcastStatus.NEW ||
                 episode.getStatus() == PodcastStatus.ERROR ||
                 episode.getStatus() == PodcastStatus.SKIPPED)) {

                podcastService.downloadEpisode(episode);
            }
        }
    }

}
