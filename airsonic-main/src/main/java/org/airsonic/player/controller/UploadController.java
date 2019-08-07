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

import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.upload.MonitoredDiskFileItemFactory;
import org.airsonic.player.upload.UploadListener;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping("/upload")
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;
    public static final String UPLOAD_STATUS = "uploadStatus";

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<>();
        List<File> uploadedFiles = new ArrayList<>();
        List<File> unzippedFiles = new ArrayList<>();
        TransferStatus status = null;

        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());

            request.getSession().setAttribute(UPLOAD_STATUS, status);

            // Check that we have a file upload request
            if (!ServletFileUpload.isMultipartContent(request)) {
                throw new Exception("Illegal request.");
            }

            File dir = null;
            boolean unzip = false;

            UploadListener listener = new UploadListenerImpl(status);

            FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
            ServletFileUpload upload = new ServletFileUpload(factory);

            List<?> items = upload.parseRequest(request);

            // First, look for "dir" and "unzip" parameters.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (item.isFormField() && "dir".equals(item.getFieldName())) {
                    dir = new File(item.getString());
                } else if (item.isFormField() && "unzip".equals(item.getFieldName())) {
                    unzip = true;
                }
            }

            if (dir == null) {
                throw new Exception("Missing 'dir' parameter.");
            }

            // Look for file items.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (!item.isFormField()) {
                    String fileName = item.getName();
                    if (!fileName.trim().isEmpty()) {

                        File targetFile = new File(dir, new File(fileName).getName());

                        if (!securityService.isUploadAllowed(targetFile)) {
                            throw new Exception("Permission denied: " + StringUtil.toHtml(targetFile.getPath()));
                        }

                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        item.write(targetFile);
                        uploadedFiles.add(targetFile);
                        LOG.info("Uploaded " + targetFile);

                        if (unzip && targetFile.getName().toLowerCase().endsWith(".zip")) {
                            unzip(targetFile, unzippedFiles);
                        }
                    }
                }
            }

        } catch (Exception x) {
            LOG.warn("Uploading failed.", x);
            map.put("exception", x);
        } finally {
            if (status != null) {
                statusService.removeUploadStatus(status);
                request.getSession().removeAttribute(UPLOAD_STATUS);
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransfered());
            }
        }

        map.put("uploadedFiles", uploadedFiles);
        map.put("unzippedFiles", unzippedFiles);

        return new ModelAndView("upload","model",map);
    }

    private void unzip(File file, List<File> unzippedFiles) throws Exception {
        LOG.info("Unzipping " + file);

        try (ZipFile zipFile = new ZipFile(file)) {

            Enumeration<?> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(file.getParentFile(), entry.getName());
                if (!entryFile.toPath().normalize().startsWith(file.getParentFile().toPath())) {
                    throw new Exception("Bad zip filename: " + StringUtil.toHtml(entryFile.getPath()));
                }

                if (!entry.isDirectory()) {

                    if (!securityService.isUploadAllowed(entryFile)) {
                        throw new Exception("Permission denied: " + StringUtil.toHtml(entryFile.getPath()));
                    }

                    entryFile.getParentFile().mkdirs();
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        inputStream = zipFile.getInputStream(entry);
                        outputStream = new FileOutputStream(entryFile);

                        byte[] buf = new byte[8192];
                        while (true) {
                            int n = inputStream.read(buf);
                            if (n == -1) {
                                break;
                            }
                            outputStream.write(buf, 0, n);
                        }

                        LOG.info("Unzipped " + entryFile);
                        unzippedFiles.add(entryFile);
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        IOUtils.closeQuietly(outputStream);
                    }
                }
            }

            zipFile.close();
            file.delete();

        }
    }





    /**
     * Receives callbacks as the file upload progresses.
     */
    private class UploadListenerImpl implements UploadListener {
        private TransferStatus status;
        private long start;

        private UploadListenerImpl(TransferStatus status) {
            this.status = status;
            start = System.currentTimeMillis();
        }

        @Override
        public void start(String fileName) {
            status.setFile(new File(fileName));
        }

        @Override
        public void bytesRead(long bytesRead) {

            // Throttle bitrate.

            long byteCount = status.getBytesTransfered() + bytesRead;
            long bitCount = byteCount * 8L;

            float elapsedMillis = Math.max(1, System.currentTimeMillis() - start);
            float elapsedSeconds = elapsedMillis / 1000.0F;
            long maxBitsPerSecond = getBitrateLimit();

            status.setBytesTransfered(byteCount);

            if (maxBitsPerSecond > 0) {
                float sleepMillis = 1000.0F * (bitCount / maxBitsPerSecond - elapsedSeconds);
                if (sleepMillis > 0) {
                    try {
                        Thread.sleep((long) sleepMillis);
                    } catch (InterruptedException x) {
                        LOG.warn("Failed to sleep.", x);
                    }
                }
            }
        }

        private long getBitrateLimit() {
            return 1024L * settingsService.getUploadBitrateLimit() / Math.max(1, statusService.getAllUploadStatuses().size());
        }
    }

}
