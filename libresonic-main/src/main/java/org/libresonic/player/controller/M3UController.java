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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.PlayQueue;
import org.libresonic.player.domain.Player;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.service.TranscodingService;
import org.libresonic.player.util.StringUtil;

/**
 * Controller which produces the M3U playlist.
 *
 * @author Sindre Mehus
 */
public class M3UController implements Controller {

    private PlayerService playerService;
    private SettingsService settingsService;
    private TranscodingService transcodingService;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("audio/x-mpegurl");
        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);

        Player player = playerService.getPlayer(request, response);

        String url = request.getRequestURL().toString();
        url = url.replaceFirst("play.m3u.*", "stream?");

        // Rewrite URLs in case we're behind a proxy.
        if (settingsService.isRewriteUrlEnabled()) {
            String referer = request.getHeader("referer");
            url = StringUtil.rewriteUrl(url, referer);
        }

        url = settingsService.rewriteRemoteUrl(url);

        if (player.isExternalWithPlaylist()) {
            createClientSidePlaylist(response.getWriter(), player, url);
        } else {
            createServerSidePlaylist(response.getWriter(), player, url);
        }
        return null;
    }

    private void createClientSidePlaylist(PrintWriter out, Player player, String url) throws Exception {
        if (player.isM3uBomEnabled()) {
            out.print("\ufeff");
        }
        out.println("#EXTM3U");
        List<MediaFile> result;
        synchronized (player.getPlayQueue()) {
            result = player.getPlayQueue().getFiles();
        }
        for (MediaFile mediaFile : result) {
            Integer duration = mediaFile.getDurationSeconds();
            if (duration == null) {
                duration = -1;
            }
            out.println("#EXTINF:" + duration + "," + mediaFile.getArtist() + " - " + mediaFile.getTitle());
            out.println(url + "player=" + player.getId() + "&id=" + mediaFile.getId() + "&suffix=." + transcodingService.getSuffix(player, mediaFile, null));
        }
    }

    private void createServerSidePlaylist(PrintWriter out, Player player, String url) throws IOException {

        url += "player=" + player.getId();

        // Get suffix of current file, e.g., ".mp3".
        String suffix = getSuffix(player);
        if (suffix != null) {
            url += "&suffix=." + suffix;
        }

        if (player.isM3uBomEnabled()) {
            out.print("\ufeff");
        }
        out.println("#EXTM3U");
        out.println("#EXTINF:-1,Libresonic");
        out.println(url);
    }

    private String getSuffix(Player player) {
        PlayQueue playQueue = player.getPlayQueue();
        return playQueue.isEmpty() ? null : transcodingService.getSuffix(player, playQueue.getFile(0), null);
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }
}
