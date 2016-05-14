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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import org.libresonic.player.domain.Playlist;
import org.libresonic.player.service.PlaylistService;
import org.libresonic.player.service.SecurityService;

/**
 * @author Sindre Mehus
 */
public class ImportPlaylistController extends ParameterizableViewController {

    private static final long MAX_PLAYLIST_SIZE_MB = 5L;

    private SecurityService securityService;
    private PlaylistService playlistService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        try {
            if (ServletFileUpload.isMultipartContent(request)) {

                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<?> items = upload.parseRequest(request);
                for (Object o : items) {
                    FileItem item = (FileItem) o;

                    if ("file".equals(item.getFieldName()) && !StringUtils.isBlank(item.getName())) {
                        if (item.getSize() > MAX_PLAYLIST_SIZE_MB * 1024L * 1024L) {
                            throw new Exception("The playlist file is too large. Max file size is " + MAX_PLAYLIST_SIZE_MB + " MB.");
                        }
                        String playlistName = FilenameUtils.getBaseName(item.getName());
                        String fileName = FilenameUtils.getName(item.getName());
                        String format = StringUtils.lowerCase(FilenameUtils.getExtension(item.getName()));
                        String username = securityService.getCurrentUsername(request);
                        Playlist playlist = playlistService.importPlaylist(username, playlistName, fileName, format, item.getInputStream(), null);
                        map.put("playlist", playlist);
                    }
                }
            }
        } catch (Exception e) {
            map.put("error", e.getMessage());
        }

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
}
