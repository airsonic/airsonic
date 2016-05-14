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
package org.libresonic.player.ajax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.util.StringUtil;

/**
 * Provides AJAX-enabled services for changing cover art images.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class CoverArtService {

    private static final Logger LOG = Logger.getLogger(CoverArtService.class);

    private SecurityService securityService;
    private MediaFileService mediaFileService;

    /**
     * Downloads and saves the cover art at the given URL.
     *
     * @param albumId ID of the album in question.
     * @param url  The image URL.
     * @return The error string if something goes wrong, <code>null</code> otherwise.
     */
    public String setCoverArtImage(int albumId, String url) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(albumId);
            saveCoverArt(mediaFile.getPath(), url);
            return null;
        } catch (Exception x) {
            LOG.warn("Failed to save cover art for album " + albumId, x);
            return x.toString();
        }
    }

    private void saveCoverArt(String path, String url) throws Exception {
        InputStream input = null;
        OutputStream output = null;
        HttpClient client = new DefaultHttpClient();

        try {
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 20 * 1000); // 20 seconds
            HttpConnectionParams.setSoTimeout(client.getParams(), 20 * 1000); // 20 seconds
            HttpGet method = new HttpGet(url);

            HttpResponse response = client.execute(method);
            input = response.getEntity().getContent();

            // Attempt to resolve proper suffix.
            String suffix = "jpg";
            if (url.toLowerCase().endsWith(".gif")) {
                suffix = "gif";
            } else if (url.toLowerCase().endsWith(".png")) {
                suffix = "png";
            }

            // Check permissions.
            File newCoverFile = new File(path, "cover." + suffix);
            if (!securityService.isWriteAllowed(newCoverFile)) {
                throw new Exception("Permission denied: " + StringUtil.toHtml(newCoverFile.getPath()));
            }

            // If file exists, create a backup.
            backup(newCoverFile, new File(path, "cover." + suffix + ".backup"));

            // Write file.
            output = new FileOutputStream(newCoverFile);
            IOUtils.copy(input, output);

            MediaFile dir = mediaFileService.getMediaFile(path);

            // Refresh database.
            mediaFileService.refreshMediaFile(dir);
            dir = mediaFileService.getMediaFile(dir.getId());

            // Rename existing cover files if new cover file is not the preferred.
            try {
                while (true) {
                    File coverFile = mediaFileService.getCoverArt(dir);
                    if (coverFile != null && !isMediaFile(coverFile) && !newCoverFile.equals(coverFile)) {
                        if (!coverFile.renameTo(new File(coverFile.getCanonicalPath() + ".old"))) {
                            LOG.warn("Unable to rename old image file " + coverFile);
                            break;
                        }
                        LOG.info("Renamed old image file " + coverFile);

                        // Must refresh again.
                        mediaFileService.refreshMediaFile(dir);
                        dir = mediaFileService.getMediaFile(dir.getId());
                    } else {
                        break;
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to rename existing cover file.", x);
            }

        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
            client.getConnectionManager().shutdown();
        }
    }

    private boolean isMediaFile(File file) {
        return !mediaFileService.filterMediaFiles(new File[]{file}).isEmpty();
    }

    private void backup(File newCoverFile, File backup) {
        if (newCoverFile.exists()) {
            if (backup.exists()) {
                backup.delete();
            }
            if (newCoverFile.renameTo(backup)) {
                LOG.info("Backed up old image file to " + backup);
            } else {
                LOG.warn("Failed to create image file backup " + backup);
            }
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}