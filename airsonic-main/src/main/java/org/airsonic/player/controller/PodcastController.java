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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller for the page used to generate the Podcast XML file.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcast")
public class PodcastController  {

    private static final DateFormat RSS_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        String url = request.getRequestURL().toString();
        String username = securityService.getCurrentUsername(request);
        List<Playlist> playlists = playlistService.getReadablePlaylistsForUser(username);
        List<Podcast> podcasts = new ArrayList<>();

        for (Playlist playlist : playlists) {

            List<MediaFile> songs = playlistService.getFilesInPlaylist(playlist.getId());
            if (songs.isEmpty()) {
                continue;
            }
            long length = 0L;
            for (MediaFile song : songs) {
                length += song.getFileSize();
            }
            String publishDate = RSS_DATE_FORMAT.format(playlist.getCreated());

            // Resolve content type.
            String suffix = songs.get(0).getFormat();
            String type = StringUtil.getMimeType(suffix);

            String enclosureUrl = url + "/stream?playlist=" + playlist.getId();

            podcasts.add(new Podcast(playlist.getName(), publishDate, enclosureUrl, length, type));
        }

        Map<String, Object> map = new HashMap<>();

        map.put("url", url);
        map.put("podcasts", podcasts);

        return new ModelAndView("podcast","model",map);
    }




    /**
     * Contains information about a single Podcast.
     */
    public static class Podcast {
        private String name;
        private String publishDate;
        private String enclosureUrl;
        private long length;
        private String type;

        public Podcast(String name, String publishDate, String enclosureUrl, long length, String type) {
            this.name = name;
            this.publishDate = publishDate;
            this.enclosureUrl = enclosureUrl;
            this.length = length;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getPublishDate() {
            return publishDate;
        }

        public String getEnclosureUrl() {
            return enclosureUrl;
        }

        public long getLength() {
            return length;
        }

        public String getType() {
            return type;
        }
    }
}
