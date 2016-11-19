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

import org.libresonic.player.domain.Player;
import org.libresonic.player.domain.Playlist;
import org.libresonic.player.domain.User;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.PlaylistService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the playlist page.
 *
 * @author Sindre Mehus
 */
public class PlaylistController extends ParameterizableViewController {

    private SecurityService securityService;
    private PlaylistService playlistService;
    private SettingsService settingsService;
    private PlayerService playerService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        UserSettings userSettings = settingsService.getUserSettings(username);
        Player player = playerService.getPlayer(request, response);
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            return new ModelAndView(new RedirectView("notFound.view"));
        }

        map.put("playlist", playlist);
        map.put("user", user);
        map.put("player", player);
        map.put("editAllowed", username.equals(playlist.getUsername()) || securityService.isAdmin(username));
        map.put("partyMode", userSettings.isPartyModeEnabled());

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }
}
