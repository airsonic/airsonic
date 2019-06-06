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

import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/importPlaylist")
public class ImportPlaylistController {

    private static final long MAX_PLAYLIST_SIZE_MB = 5L;

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlaylistService playlistService;

    @PostMapping
    protected String handlePost(RedirectAttributes redirectAttributes,
                                           HttpServletRequest request
                                           ) throws Exception {
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
                        Playlist playlist = playlistService.importPlaylist(username, playlistName, fileName,
                                                                           item.getInputStream(), null);
                        map.put("playlist", playlist);
                    }
                }
            }
        } catch (Exception e) {
            map.put("error", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("model", map);
        return "redirect:importPlaylist";
    }

    @GetMapping
    public String handleGet() {
        return "importPlaylist";
    }

}
