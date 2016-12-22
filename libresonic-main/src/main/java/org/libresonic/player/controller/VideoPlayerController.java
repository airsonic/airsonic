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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.util.StringUtil;

/**
 * Controller for the page used to play videos.
 *
 * @author Sindre Mehus
 */
public class VideoPlayerController extends ParameterizableViewController {

    public static final int DEFAULT_BIT_RATE = 2000;
    public static final int[] BIT_RATES = {200, 300, 400, 500, 700, 1000, 1200, 1500, 2000, 3000, 5000};

    private MediaFileService mediaFileService;
    private SettingsService settingsService;
    private PlayerService playerService;
    private SecurityService securityService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        Map<String, Object> map = new HashMap<String, Object>();
        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        MediaFile file = mediaFileService.getMediaFile(id);
        mediaFileService.populateStarredDate(file, user.getUsername());

        Integer duration = file.getDurationSeconds();
        String playerId = playerService.getPlayer(request, response).getId();
        String url = request.getRequestURL().toString();
        String streamUrl = url.replaceFirst("/videoPlayer.view.*", "/stream?id=" + file.getId() + "&player=" + playerId);
        String coverArtUrl = url.replaceFirst("/videoPlayer.view.*", "/coverArt.view?id=" + file.getId());

        // Rewrite URLs in case we're behind a proxy.
        if (settingsService.isRewriteUrlEnabled()) {
            String referer = request.getHeader("referer");
            streamUrl = StringUtil.rewriteUrl(streamUrl, referer);
            coverArtUrl = StringUtil.rewriteUrl(coverArtUrl, referer);
        }

        String remoteStreamUrl = settingsService.rewriteRemoteUrl(streamUrl);
        String remoteCoverArtUrl = settingsService.rewriteRemoteUrl(coverArtUrl);

        map.put("video", file);
        map.put("streamUrl", streamUrl);
        map.put("remoteStreamUrl", remoteStreamUrl);
        map.put("remoteCoverArtUrl", remoteCoverArtUrl);
        map.put("duration", duration);
        map.put("bitRates", BIT_RATES);
        map.put("defaultBitRate", DEFAULT_BIT_RATE);
        map.put("user", user);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    public static Map<String, Integer> createSkipOffsets(int durationSeconds) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < durationSeconds; i += 60) {
            result.put(StringUtil.formatDuration(i), i);
        }
        return result;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
