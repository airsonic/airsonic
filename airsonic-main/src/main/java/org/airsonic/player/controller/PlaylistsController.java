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
 *  Copyright 2014 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the playlists page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playlists")
public class PlaylistsController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PlaylistService playlistService;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet(HttpServletRequest request, Model model) throws Exception {
        Map<String, Object> map = new HashMap<>();

        User user = securityService.getCurrentUser(request);
        List<Playlist> playlists = playlistService.getReadablePlaylistsForUser(user.getUsername());

        map.put("playlists", playlists);
        model.addAttribute("model", map);
        return "playlists";
    }

}
