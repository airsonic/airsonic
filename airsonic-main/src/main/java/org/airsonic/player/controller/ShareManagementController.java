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
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Share;
import org.airsonic.player.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

/**
 * Controller for sharing music on Twitter, Facebook etc.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/createShare")
public class ShareManagementController  {

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private ShareService shareService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {

        List<MediaFile> files = getMediaFiles(request);
        MediaFile dir = null;
        if (!files.isEmpty()) {
            dir = files.get(0);
            if (!dir.isAlbum()) {
                dir = mediaFileService.getParentOf(dir);
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("dir", dir);
        map.put("user", securityService.getCurrentUser(request));

        Share share = shareService.createShare(request, files);
        String description = getDescription(request);
        if (description != null) {
            share.setDescription(description);
            shareService.updateShare(share);
        }

        map.put("playUrl", shareService.getShareUrl(request, share));

        return new ModelAndView("createShare", "model", map);
    }

    private String getDescription(HttpServletRequest request) throws ServletRequestBindingException {
        Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");
        return playlistId == null ? null : playlistService.getPlaylist(playlistId).getName();
    }

    private List<MediaFile> getMediaFiles(HttpServletRequest request) throws Exception {
        Integer id = ServletRequestUtils.getIntParameter(request, "id");
        String playerId = request.getParameter("player");
        Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");

        List<MediaFile> result = new ArrayList<>();

        if (id != null) {
            MediaFile album = mediaFileService.getMediaFile(id);
            int[] indexes = ServletRequestUtils.getIntParameters(request, "i");
            if (indexes.length == 0) {
                return Arrays.asList(album);
            }
            List<MediaFile> children = mediaFileService.getChildrenOf(album, true, false, true);
            for (int index : indexes) {
                result.add(children.get(index));
            }
        }

        else if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            PlayQueue playQueue = player.getPlayQueue();
            result = playQueue.getFiles();
        }

        else if (playlistId != null) {
            result = playlistService.getFilesInPlaylist(playlistId);
        }

        return result;
    }

}
