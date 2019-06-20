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
import org.airsonic.player.io.PlayQueueInputStream;
import org.airsonic.player.io.RangeOutputStream;
import org.airsonic.player.io.ShoutCastOutputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.*;
import org.airsonic.player.service.sonos.SonosHelper;
import org.airsonic.player.util.HttpRange;
import org.airsonic.player.util.StringUtil;
import org.airsonic.player.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A controller which streams the content of a {@link PlayQueue} to a remote {@link Player}.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({"/stream/**", "/ext/stream/**"})
public class StreamController {

    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    @Autowired
    private StatusService statusService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private AudioScrobblerService audioScrobblerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SearchService searchService;

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        TransferStatus status = null;
        Player player = playerService.getPlayer(request, response, false, true);
        User user = securityService.getUserByName(player.getUsername());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {

            if (!(authentication instanceof JWTAuthenticationToken) && !user.isStreamRole()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Streaming is forbidden for user " + user.getUsername());
                return;
            }

            // If "playlist" request parameter is set, this is a Podcast request. In that case, create a separate
            // play queue (in order to support multiple parallel Podcast streams).
            Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");
            boolean isPodcast = playlistId != null;
            if (isPodcast) {
                PlayQueue playQueue = new PlayQueue();
                playQueue.addFiles(false, playlistService.getFilesInPlaylist(playlistId));
                player.setPlayQueue(playQueue);
                Util.setContentLength(response, playQueue.length());
                LOG.info("{}: Incoming Podcast request for playlist {}", request.getRemoteAddr(), playlistId);
            }

            response.setHeader("Access-Control-Allow-Origin", "*");

            String contentType = StringUtil.getMimeType(request.getParameter("suffix"));
            response.setContentType(contentType);

            String preferredTargetFormat = request.getParameter("format");
            Integer maxBitRate = ServletRequestUtils.getIntParameter(request, "maxBitRate");
            if (Integer.valueOf(0).equals(maxBitRate)) {
                maxBitRate = null;
            }

            VideoTranscodingSettings videoTranscodingSettings = null;

            // Is this a request for a single file (typically from the embedded Flash player)?
            // In that case, create a separate playlist (in order to support multiple parallel streams).
            // Also, enable partial download (HTTP byte range).
            MediaFile file = getSingleFile(request);
            boolean isSingleFile = file != null;
            HttpRange range = null;
            Long fileLengthExpected = null;

            if (isSingleFile) {

                if (!(authentication instanceof JWTAuthenticationToken) && !securityService.isFolderAccessAllowed(file,
                        user.getUsername())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Access to file " + file.getId() + " is forbidden for user " + user.getUsername());
                    return;
                }

                // Update the index of the currently playing media file. At
                // this point we haven't yet modified the play queue to support
                // multiple streams, so the current play queue is the real one.
                int currentIndex = player.getPlayQueue().getFiles().indexOf(file);
                player.getPlayQueue().setIndex(currentIndex);

                // Create a new, fake play queue that only contains the
                // currently playing media file, in case multiple streams want
                // to use the same player.
                PlayQueue playQueue = new PlayQueue();
                playQueue.addFiles(true, file);
                player.setPlayQueue(playQueue);

                TranscodingService.Parameters parameters = transcodingService.getParameters(file, player, maxBitRate,
                        preferredTargetFormat, null);
                boolean isHls = ServletRequestUtils.getBooleanParameter(request, "hls", false);
                fileLengthExpected = parameters.getExpectedLength();

                // Wrangle response length and ranges.
                //
                // Support ranges as long as we're not transcoding blindly; video is always assumed to transcode
                if (file.isVideo() || ! parameters.isRangeAllowed()) {
                    // Use chunked transfer; do not accept range requests
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setHeader("Accept-Ranges", "none");
                } else {
                    // Partial content permitted because either know or expect to be able to predict the final size
                    long contentLength;
                    // If range was requested, respond in kind
                    range = getRange(request, file.getDurationSeconds(), fileLengthExpected);
                    if (range != null) {
                        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                        response.setHeader("Accept-Ranges", "bytes");

                        // Both ends are inclusive
                        long startByte = range.getFirstBytePos();
                        long endByte = range.isClosed() ? range.getLastBytePos() : fileLengthExpected - 1;

                        response.setHeader("Content-Range",
                                String.format("bytes %d-%d/%d", startByte, endByte, fileLengthExpected));
                        contentLength = endByte + 1 - startByte;
                    } else {
                        // No range was requested, give back the whole file
                        response.setStatus(HttpServletResponse.SC_OK);
                        contentLength = fileLengthExpected;
                    }

                    response.setIntHeader("ETag", file.getId());
                    Util.setContentLength(response, contentLength);
                }

                // Set content type of response
                if (isHls) {
                    response.setContentType(StringUtil.getMimeType("ts")); // HLS is always MPEG TS.
                } else {
                    String transcodedSuffix = transcodingService.getSuffix(player, file, preferredTargetFormat);
                    boolean sonos = SonosHelper.AIRSONIC_CLIENT_ID.equals(player.getClientId());
                    response.setContentType(StringUtil.getMimeType(transcodedSuffix, sonos));
                    setContentDuration(response, file);
                }

                if (file.isVideo() || isHls) {
                    videoTranscodingSettings = createVideoTranscodingSettings(file, request);
                }
            }

            // All headers are set, stop if that's all the client requested.
            if (request.getMethod().equals("HEAD")) {
                return;
            }

            if (fileLengthExpected != null) {
                LOG.info("Streaming request for [{}] with range [{}]", file.getPath(), response.getHeader("Content-Range"));
            }

            // Terminate any other streams to this player.
            if (!isPodcast && !isSingleFile) {
                for (TransferStatus streamStatus : statusService.getStreamStatusesForPlayer(player)) {
                    if (streamStatus.isActive()) {
                        streamStatus.terminate();
                    }
                }
            }

            status = statusService.createStreamStatus(player);

            try (
                PlayQueueInputStream in = new PlayQueueInputStream(player, status, maxBitRate, preferredTargetFormat, videoTranscodingSettings,
                        transcodingService, audioScrobblerService, mediaFileService, searchService);
                OutputStream out = makeOutputStream(request, response, range, isSingleFile, player, settingsService)
            ) {
                final int BUFFER_SIZE = 2048;
                byte[] buf = new byte[BUFFER_SIZE];
                long bytesWritten = 0;

                while (!status.terminated()) {
                    if (player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
                        if (isPodcast || isSingleFile) {
                            break;
                        } else {
                            sendDummyDelayed(buf, out);
                        }
                    } else {

                        int n = in.read(buf);
                        if (n == -1) {
                            if (isPodcast || isSingleFile) {
                                // Pad the output if needed to avoid content length errors on transcodes
                                if (fileLengthExpected != null && bytesWritten < fileLengthExpected) {
                                    sendDummy(buf, out, fileLengthExpected - bytesWritten);
                                }
                                break;
                            } else {
                                sendDummyDelayed(buf, out);
                            }
                        } else {
                            if (fileLengthExpected != null && bytesWritten <= fileLengthExpected
                                && bytesWritten + n > fileLengthExpected) {
                                LOG.warn("Stream output exceeded expected length of {}. It is likely that "
                                    + "the transcoder is not adhering to the bitrate limit or the media "
                                    + "source is corrupted or has grown larger", fileLengthExpected);
                            }
                            out.write(buf, 0, n);
                            bytesWritten += n;
                        }
                    }
                }
            }
        } catch (IOException e) {

            // This happens often and outside of the control of the server, so
            // we catch Tomcat/Jetty "connection aborted by client" exceptions
            // and display a short error message.
            boolean shouldCatch = Util.isInstanceOfClassName(e, "org.apache.catalina.connector.ClientAbortException");
            if (shouldCatch) {
                LOG.info("{}: Client unexpectedly closed connection while loading {} ({})",
                        request.getRemoteAddr(),
                        Util.getAnonymizedURLForRequest(request),
                        e.getCause().toString());
                return;
            }

            // Rethrow the exception in all other cases
            throw e;

        } finally {
            if (status != null) {
                securityService.updateUserByteCounts(user, status.getBytesTransfered(), 0L, 0L);
                statusService.removeStreamStatus(status);
            }
        }
    }

    /**
     * Construct an appropriate output stream based on the request.
     * <p>
     * This is responsible for limiting the output to the given range (if not null) and injecting Shoutcast metadata
     * into the stream if requested.
     */
    private OutputStream makeOutputStream(HttpServletRequest request, HttpServletResponse response, HttpRange range,
                                          boolean isSingleFile, Player player, SettingsService settingsService)
            throws IOException {
        OutputStream out = RangeOutputStream.wrap(response.getOutputStream(), range);

        // Enabled SHOUTcast, if requested.
        boolean isShoutCastRequested = "1".equals(request.getHeader("icy-metadata"));
        if (isShoutCastRequested && !isSingleFile) {
            response.setHeader("icy-metaint", "" + ShoutCastOutputStream.META_DATA_INTERVAL);
            response.setHeader("icy-notice1", "This stream is served using Airsonic");
            response.setHeader("icy-notice2", "Airsonic - Free media streamer");
            response.setHeader("icy-name", "Airsonic");
            response.setHeader("icy-genre", "Mixed");
            response.setHeader("icy-url", "https://airsonic.github.io/");
            out = new ShoutCastOutputStream(out, player.getPlayQueue(), settingsService);
        }
        return out;
    }

    private void setContentDuration(HttpServletResponse response, MediaFile file) {
        if (file.getDurationSeconds() != null) {
            response.setHeader("X-Content-Duration", String.format("%.1f", file.getDurationSeconds().doubleValue()));
        }
    }

    private MediaFile getSingleFile(HttpServletRequest request) throws ServletRequestBindingException {
        String path = request.getParameter("path");
        if (path != null) {
            return mediaFileService.getMediaFile(path);
        }
        Integer id = ServletRequestUtils.getIntParameter(request, "id");
        if (id != null) {
            return mediaFileService.getMediaFile(id);
        }
        return null;
    }

    @Nullable
    private HttpRange getRange(HttpServletRequest request, Integer fileDuration, Long fileSize) {

        // First, look for "Range" HTTP header.
        HttpRange range = HttpRange.valueOf(request.getHeader("Range"));
        if (range != null) {
            return range;
        }

        // Second, look for "offsetSeconds" request parameter.
        String offsetSeconds = request.getParameter("offsetSeconds");
        range = parseAndConvertOffsetSeconds(offsetSeconds, fileDuration, fileSize);
        return range;

    }

    @Nullable
    private HttpRange parseAndConvertOffsetSeconds(String offsetSeconds, Integer fileDuration, Long fileSize) {
        if (offsetSeconds == null) {
            return null;
        }

        try {
            if (fileDuration == null || fileSize == null) {
                return null;
            }
            float offset = Float.parseFloat(offsetSeconds);

            // Convert from time offset to byte offset.
            long byteOffset = (long) (fileSize * (offset / fileDuration));
            return new HttpRange(byteOffset, null);

        } catch (Exception x) {
            LOG.error("Failed to parse and convert time offset: " + offsetSeconds, x);
            return null;
        }
    }

    private VideoTranscodingSettings createVideoTranscodingSettings(MediaFile file, HttpServletRequest request)
            throws ServletRequestBindingException {
        Integer existingWidth = file.getWidth();
        Integer existingHeight = file.getHeight();
        Integer maxBitRate = ServletRequestUtils.getIntParameter(request, "maxBitRate");
        int timeOffset = ServletRequestUtils.getIntParameter(request, "timeOffset", 0);
        int defaultDuration = file.getDurationSeconds() == null ? Integer.MAX_VALUE :
                file.getDurationSeconds() - timeOffset;
        int duration = ServletRequestUtils.getIntParameter(request, "duration", defaultDuration);
        boolean hls = ServletRequestUtils.getBooleanParameter(request, "hls", false);

        Dimension dim = getRequestedVideoSize(request.getParameter("size"));
        if (dim == null) {
            dim = getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        }

        return new VideoTranscodingSettings(dim.width, dim.height, timeOffset, duration, hls);
    }

    protected Dimension getRequestedVideoSize(String sizeSpec) {
        if (sizeSpec == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("^(\\d+)x(\\d+)$");
        Matcher matcher = pattern.matcher(sizeSpec);
        if (matcher.find()) {
            int w = Integer.parseInt(matcher.group(1));
            int h = Integer.parseInt(matcher.group(2));
            if (w >= 0 && h >= 0 && w <= 2000 && h <= 2000) {
                return new Dimension(w, h);
            }
        }
        return null;
    }

    protected Dimension getSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate) {
        if (maxBitRate == null) {
            return new Dimension(400, 224);
        }

        int w;
        if (maxBitRate < 400) {
            w = 400;
        } else if (maxBitRate < 600) {
            w = 480;
        } else if (maxBitRate < 1800) {
            w = 640;
        } else {
            w = 960;
        }
        int h = even(w * 9 / 16);

        if (existingWidth == null || existingHeight == null) {
            return new Dimension(w, h);
        }

        if (existingWidth < w || existingHeight < h) {
            return new Dimension(even(existingWidth), even(existingHeight));
        }

        double aspectRate = existingWidth.doubleValue() / existingHeight.doubleValue();
        h = (int) Math.round(w / aspectRate);

        return new Dimension(even(w), even(h));
    }

    // Make sure width and height are multiples of two, as some versions of ffmpeg require it.
    private int even(int size) {
        return size + (size % 2);
    }

    private void sendDummy(byte[] buf, OutputStream out, long len) throws IOException {
        long bytesWritten = 0;
        int n;

        Arrays.fill(buf, (byte) 0xFF);
        while (bytesWritten < len) {
            n = (int) Math.min(buf.length, len - bytesWritten);
            out.write(buf, 0, n);
            bytesWritten += n;
        }
    }

    /**
     * Feed the other end with some dummy data to keep it from reconnecting.
     */
    private void sendDummyDelayed(byte[] buf, OutputStream out) throws IOException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException x) {
            LOG.warn("Interrupted in sleep.", x);
        }
        sendDummy(buf, out, buf.length);
        out.flush();
    }
}
