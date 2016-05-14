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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.Player;
import org.libresonic.player.domain.TransferStatus;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.StatusService;

/**
 * Controller for showing what's currently playing.
 *
 * @author Sindre Mehus
 */
public class NowPlayingController extends AbstractController {

    private PlayerService playerService;
    private StatusService statusService;
    private MediaFileService mediaFileService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Player player = playerService.getPlayer(request, response);
        List<TransferStatus> statuses = statusService.getStreamStatusesForPlayer(player);

        MediaFile current = statuses.isEmpty() ? null : mediaFileService.getMediaFile(statuses.get(0).getFile());
        MediaFile dir = current == null ? null : mediaFileService.getParentOf(current);

        String url;
        if (dir != null && !mediaFileService.isRoot(dir)) {
            url = "main.view?id=" + dir.getId();
        } else {
            url = "home.view";
        }

        return new ModelAndView(new RedirectView(url));
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
