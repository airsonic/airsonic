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

import org.airsonic.player.domain.Avatar;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller which receives uploaded avatar images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/avatarUpload")
public class AvatarUploadController  {

    private static final Logger LOG = LoggerFactory.getLogger(AvatarUploadController.class);
    private static final int MAX_AVATAR_SIZE = 64;

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws Exception {

        String username = securityService.getCurrentUsername(request);

        // Check that we have a file upload request.
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new Exception("Illegal request.");
        }

        Map<String, Object> map = new HashMap<String, Object>();
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = upload.parseRequest(request);

        // Look for file items.
        for (FileItem item : items) {
            if (!item.isFormField()) {
                String fileName = item.getName();
                byte[] data = item.get();

                if (StringUtils.isNotBlank(fileName) && data.length > 0) {
                    createAvatar(fileName, data, username, map);
                } else {
                    map.put("error", new Exception("Missing file."));
                    LOG.warn("Failed to upload personal image. No file specified.");
                }
                break;
            }
        }

        map.put("username", username);
        map.put("avatar", settingsService.getCustomAvatar(username));
        return new ModelAndView("avatarUploadResult","model",map);
    }

    private void createAvatar(String fileName, byte[] data, String username, Map<String, Object> map) {

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IOException("Failed to decode incoming image: " + fileName + " (" + data.length + " bytes).");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            String mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(fileName));

            // Scale down image if necessary.
            if (width > MAX_AVATAR_SIZE || height > MAX_AVATAR_SIZE) {
                double scaleFactor = MAX_AVATAR_SIZE / (double)Math.max(width, height);
                height = (int) (height * scaleFactor);
                width = (int) (width * scaleFactor);
                image = CoverArtController.scale(image, width, height);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "jpeg", out);
                data = out.toByteArray();
                mimeType = StringUtil.getMimeType("jpeg");
                map.put("resized", true);
            }
            Avatar avatar = new Avatar(0, fileName, new Date(), mimeType, width, height, data);
            settingsService.setCustomAvatar(avatar, username);
            LOG.info("Created avatar '" + fileName + "' (" + data.length + " bytes) for user " + username);

        } catch (IOException x) {
            LOG.warn("Failed to upload personal image: " + x, x);
            map.put("error", x);
        }
    }


}
