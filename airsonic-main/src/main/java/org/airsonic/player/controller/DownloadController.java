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

import org.airsonic.player.domain.*;
import org.airsonic.player.io.RangeOutputStream;
import org.airsonic.player.service.*;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.HttpRange;
import org.airsonic.player.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A controller used for downloading files to a remote client. If the requested path refers to a file, the
 * given file is downloaded.  If the requested path refers to a directory, the entire directory (including
 * sub-directories) are downloaded as an uncompressed zip-file.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/download")
public class DownloadController implements LastModified {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileService mediaFileService;

    public long getLastModified(HttpServletRequest request) {
        try {
            MediaFile mediaFile = getMediaFile(request);
            if (mediaFile == null || mediaFile.isDirectory() || mediaFile.getChanged() == null) {
                return -1;
            }
            return mediaFile.getChanged().getTime();
        } catch (ServletRequestBindingException e) {
            return -1;
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        TransferStatus status = null;
        try {

            status = statusService.createDownloadStatus(playerService.getPlayer(request, response, false, false));

            MediaFile mediaFile = getMediaFile(request);

            Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");
            Integer playerId = ServletRequestUtils.getIntParameter(request, "player");
            int[] indexes = request.getParameter("i") == null ? null : ServletRequestUtils.getIntParameters(request, "i");

            if (mediaFile != null) {
                response.setIntHeader("ETag", mediaFile.getId());
                response.setHeader("Accept-Ranges", "bytes");
            }

            HttpRange range = HttpRange.valueOf(request.getHeader("Range"));
            if (range != null) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                LOG.info("Got HTTP range: " + range);
            }

            if (mediaFile != null) {
                if (!securityService.isFolderAccessAllowed(mediaFile, user.getUsername())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Access to file " + mediaFile.getId() + " is forbidden for user " + user.getUsername());
                    return;
                }

                if (mediaFile.isFile()) {
                    downloadFile(response, status, mediaFile.getFile(), range);
                } else {
                    List<MediaFile> children = mediaFileService.getChildrenOf(mediaFile, true, false, true);
                    String zipFileName = FilenameUtils.getBaseName(mediaFile.getPath()) + ".zip";
                    File coverArtFile = indexes == null ? mediaFile.getCoverArtFile() : null;
                    downloadFiles(response, status, children, indexes, coverArtFile, range, zipFileName);
                }

            } else if (playlistId != null) {
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                Playlist playlist = playlistService.getPlaylist(playlistId);
                downloadFiles(response, status, songs, null, null, range, playlist.getName() + ".zip");

            } else if (playerId != null) {
                Player player = playerService.getPlayerById(playerId);
                PlayQueue playQueue = player.getPlayQueue();
                playQueue.setName("Playlist");
                downloadFiles(response, status, playQueue.getFiles(), indexes, null, range, "download.zip");
            }

        } finally {
            if (status != null) {
                statusService.removeDownloadStatus(status);
                securityService.updateUserByteCounts(user, 0L, status.getBytesTransfered(), 0L);
            }
        }
    }

    private MediaFile getMediaFile(HttpServletRequest request) throws ServletRequestBindingException {
        Integer id = ServletRequestUtils.getIntParameter(request, "id");
        return id == null ? null : mediaFileService.getMediaFile(id);
    }

    /**
     * Downloads a single file.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param file     The file to download.
     * @param range    The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFile(HttpServletResponse response, TransferStatus status, File file, HttpRange range) throws IOException {
        LOG.info("Starting to download '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
        status.setFile(file);

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeAsRFC5987(file.getName()));
        if (range == null) {
            Util.setContentLength(response, file.length());
        }

        copyFileToStream(file, RangeOutputStream.wrap(response.getOutputStream(), range), status, range);
        LOG.info("Downloaded '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
    }

    private String encodeAsRFC5987(String string) throws UnsupportedEncodingException {
        byte[] stringAsByteArray = string.getBytes("UTF-8");
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] attrChar = {'!', '#', '$', '&', '+', '-', '.', '^', '_', '`', '|', '~', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        StringBuilder sb = new StringBuilder();
        for (byte b : stringAsByteArray) {
            if (Arrays.binarySearch(attrChar, b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(digits[0x0f & (b >>> 4)]);
                sb.append(digits[b & 0x0f]);
            }
        }
        return sb.toString();
    }

    /**
     * Downloads the given files.  The files are packed together in an
     * uncompressed zip-file.
     *
     * @param response     The HTTP response.
     * @param status       The download status.
     * @param files        The files to download.
     * @param indexes      Only download songs at these indexes. May be <code>null</code>.
     * @param coverArtFile The cover art file to include, may be {@code null}.
     * @param range        The byte range, may be <code>null</code>.
     * @param zipFileName  The name of the resulting zip file.   @throws IOException If an I/O error occurs.
     */
    private void downloadFiles(HttpServletResponse response, TransferStatus status, List<MediaFile> files, int[] indexes, File coverArtFile, HttpRange range, String zipFileName) throws IOException {
        boolean cover_embedded = false;

        if (indexes != null && indexes.length == 1) {
            downloadFile(response, status, files.get(indexes[0]).getFile(), range);
            return;
        }

        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeAsRFC5987(zipFileName));

        ZipOutputStream out = new ZipOutputStream(RangeOutputStream.wrap(response.getOutputStream(), range));
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        Set<MediaFile> filesToDownload = new HashSet<>();
        if (indexes == null) {
            filesToDownload.addAll(files);
        } else {
            for (int index : indexes) {
                try {
                    filesToDownload.add(files.get(index));
                } catch (IndexOutOfBoundsException x) { /* Ignored */}
            }
        }

        for (MediaFile mediaFile : filesToDownload) {
            zip(out, mediaFile.getParentFile(), mediaFile.getFile(), status, range);
            if (coverArtFile != null && coverArtFile.exists()) {
                if (mediaFile.getFile().getCanonicalPath() == coverArtFile.getCanonicalPath()) {
                    cover_embedded = true;
                }
            }
        }
        if (coverArtFile != null && coverArtFile.exists() && cover_embedded == false) {
            zip(out, coverArtFile.getParentFile(), coverArtFile, status, range);
        }


        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Utility method for writing the content of a given file to a given output stream.
     *
     * @param file   The file to copy.
     * @param out    The output stream to write to.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void copyFileToStream(File file, OutputStream out, TransferStatus status, HttpRange range) throws IOException {
        LOG.info("Downloading '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());

        final int bufferSize = 16 * 1024; // 16 Kbit
        InputStream in = new BufferedInputStream(new FileInputStream(file), bufferSize);

        try {
            byte[] buf = new byte[bufferSize];
            long bitrateLimit = 0;
            long lastLimitCheck = 0;

            while (true) {
                long before = System.currentTimeMillis();
                int n = in.read(buf);
                if (n == -1) {
                    break;
                }
                out.write(buf, 0, n);

                // Don't sleep if outside range.
                if (range != null && !range.contains(status.getBytesSkipped() + status.getBytesTransfered())) {
                    status.addBytesSkipped(n);
                    continue;
                }

                status.addBytesTransfered(n);
                long after = System.currentTimeMillis();

                // Calculate bitrate limit every 5 seconds.
                if (after - lastLimitCheck > 5000) {
                    bitrateLimit = 1024L * settingsService.getDownloadBitrateLimit() /
                            Math.max(1, statusService.getAllDownloadStatuses().size());
                    lastLimitCheck = after;
                }

                // Sleep for a while to throttle bitrate.
                if (bitrateLimit != 0) {
                    long sleepTime = 8L * 1000 * bufferSize / bitrateLimit - (after - before);
                    if (sleepTime > 0L) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (Exception x) {
                            LOG.warn("Failed to sleep.", x);
                        }
                    }
                }
            }
        } finally {
            out.flush();
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Writes a file or a directory structure to a zip output stream. File entries in the zip file are relative
     * to the given root.
     *
     * @param out    The zip output stream.
     * @param root   The root of the directory structure.  Used to create path information in the zip file.
     * @param file   The file or directory to zip.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void zip(ZipOutputStream out, File root, File file, TransferStatus status, HttpRange range) throws IOException {

        // Exclude all hidden files starting with a "."
        if (file.getName().startsWith(".")) {
            return;
        }

        String zipName = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);

        if (file.isFile()) {
            status.setFile(file);

            ZipEntry zipEntry = new ZipEntry(zipName);
            zipEntry.setSize(file.length());
            zipEntry.setCompressedSize(file.length());
            zipEntry.setCrc(computeCrc(file));

            out.putNextEntry(zipEntry);
            copyFileToStream(file, out, status, range);
            out.closeEntry();

        } else {
            ZipEntry zipEntry = new ZipEntry(zipName + '/');
            zipEntry.setSize(0);
            zipEntry.setCompressedSize(0);
            zipEntry.setCrc(0);

            out.putNextEntry(zipEntry);
            out.closeEntry();

            File[] children = FileUtil.listFiles(file);
            for (File child : children) {
                zip(out, root, child, status, range);
            }
        }
    }

    /**
     * Computes the CRC checksum for the given file.
     *
     * @param file The file to compute checksum for.
     * @return A CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    private long computeCrc(File file) throws IOException {
        CRC32 crc = new CRC32();
        InputStream in = new FileInputStream(file);

        try {

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n != -1) {
                crc.update(buf, 0, n);
                n = in.read(buf);
            }

        } finally {
            in.close();
        }

        return crc.getValue();
    }

}
