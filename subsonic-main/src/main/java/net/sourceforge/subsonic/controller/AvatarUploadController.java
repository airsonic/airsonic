/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.Avatar;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.StringUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class AvatarUploadController extends ParameterizableViewController {

    private static final Logger LOG = Logger.getLogger(AvatarUploadController.class);
    private static final int MAX_AVATAR_SIZE = 64;

    private SettingsService settingsService;
    private SecurityService securityService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String username = securityService.getCurrentUsername(request);

        // Check that we have a file upload request.
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new Exception("Illegal request.");
        }

        Map<String, Object> map = new HashMap<String, Object>();
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<?> items = upload.parseRequest(request);

        // Look for file items.
        for (Object o : items) {
            FileItem item = (FileItem) o;

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
        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private void createAvatar(String fileName, byte[] data, String username, Map<String, Object> map) throws IOException {

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new Exception("Failed to decode incoming image: " + fileName + " (" + data.length + " bytes).");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            String mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(fileName));

            // Scale down image if necessary.
            if (width > MAX_AVATAR_SIZE || height > MAX_AVATAR_SIZE) {
                double scaleFactor = (double) MAX_AVATAR_SIZE / (double) Math.max(width, height);
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

        } catch (Exception x) {
            LOG.warn("Failed to upload personal image: " + x, x);
            map.put("error", x);
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
