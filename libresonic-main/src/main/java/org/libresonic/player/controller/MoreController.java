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

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.Player;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.util.StringUtil;

/**
 * Controller for the "more" page.
 *
 * @author Sindre Mehus
 */
public class MoreController extends ParameterizableViewController {

    private SettingsService settingsService;
    private SecurityService securityService;
    private PlayerService playerService;
    private MediaFileService mediaFileService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        User user = securityService.getCurrentUser(request);

        String uploadDirectory = null;
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        if (musicFolders.size() > 0) {
            uploadDirectory = new File(musicFolders.get(0).getPath(), "Incoming").getPath();
        }


        StringBuilder jamstashUrl = new StringBuilder("http://jamstash.com/#/settings?u=" + StringUtil.urlEncode(user.getUsername()) + "&url=");
        if (settingsService.isUrlRedirectionEnabled()) {
            jamstashUrl.append(StringUtil.urlEncode(settingsService.getUrlRedirectUrl()));
        } else {
            jamstashUrl.append(StringUtil.urlEncode(request.getRequestURL().toString().replaceAll("/more.view.*", "")));
        }

        Player player = playerService.getPlayer(request, response);
        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        map.put("user", user);
        map.put("uploadDirectory", uploadDirectory);
        map.put("genres", mediaFileService.getGenres(false));
        map.put("currentYear", Calendar.getInstance().get(Calendar.YEAR));
        map.put("musicFolders", musicFolders);
        map.put("clientSidePlaylist", player.isExternalWithPlaylist() || player.isWeb());
        map.put("brand", settingsService.getBrand());
        map.put("jamstashUrl", jamstashUrl);
        return result;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
