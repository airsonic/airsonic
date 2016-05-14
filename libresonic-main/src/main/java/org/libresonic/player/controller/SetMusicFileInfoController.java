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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.util.StringUtil;

/**
 * Controller for updating music file metadata.
 *
 * @author Sindre Mehus
 */
public class SetMusicFileInfoController extends AbstractController {

    private MediaFileService mediaFileService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        String action = request.getParameter("action");

        MediaFile mediaFile = mediaFileService.getMediaFile(id);

        if ("comment".equals(action)) {
            mediaFile.setComment(StringUtil.toHtml(request.getParameter("comment")));
            mediaFileService.updateMediaFile(mediaFile);
        }

        String url = "main.view?id=" + id;
        return new ModelAndView(new RedirectView(url));
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
