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

import java.awt.Dimension;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.util.Pair;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Controller which produces the HLS (Http Live Streaming) playlist.
 *
 * @author Sindre Mehus
 */
public class HLSController implements Controller {

    private static final int SEGMENT_DURATION = 10;
    private static final Pattern BITRATE_PATTERN = Pattern.compile("(\\d+)(@(\\d+)x(\\d+))?");

    private PlayerService playerService;
    private MediaFileService mediaFileService;
    private SecurityService securityService;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setHeader("Access-Control-Allow-Origin", "*");

        int id = ServletRequestUtils.getIntParameter(request, "id");
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        Player player = playerService.getPlayer(request, response);
        String username = player.getUsername();

        if (mediaFile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Media file not found: " + id);
            return null;
        }

        if (username != null && !securityService.isFolderAccessAllowed(mediaFile, username)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                               "Access to file " + mediaFile.getId() + " is forbidden for user " + username);
            return null;
        }

        Integer duration = mediaFile.getDurationSeconds();
        if (duration == null || duration == 0) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown duration for media file: " + id);
            return null;
        }

        response.setContentType("application/vnd.apple.mpegurl");
        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);
        List<Pair<Integer, Dimension>> bitRates = parseBitRates(request);
        PrintWriter writer = response.getWriter();
        if (bitRates.size() > 1) {
            generateVariantPlaylist(request, id, player, bitRates, writer);
        } else {
            generateNormalPlaylist(request, id, player, bitRates.size() == 1 ? bitRates.get(0) : null, duration, writer);
        }

        return null;
    }

    private List<Pair<Integer, Dimension>> parseBitRates(HttpServletRequest request) throws IllegalArgumentException {
        List<Pair<Integer, Dimension>> result = new ArrayList<Pair<Integer, Dimension>>();
        String[] bitRates = request.getParameterValues("bitRate");
        if (bitRates != null) {
            for (String bitRate : bitRates) {
                result.add(parseBitRate(bitRate));
            }
        }
        return result;
    }

    /**
     * Parses a string containing the bitrate and an optional width/height, e.g., 1200@640x480
     */
    protected Pair<Integer, Dimension> parseBitRate(String bitRate) throws IllegalArgumentException {

        Matcher matcher = BITRATE_PATTERN.matcher(bitRate);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid bitrate specification: " + bitRate);
        }
        int kbps = Integer.parseInt(matcher.group(1));
        if (matcher.group(3) == null) {
            return new Pair<Integer, Dimension>(kbps, null);
        } else {
            int width = Integer.parseInt(matcher.group(3));
            int height = Integer.parseInt(matcher.group(4));
            return new Pair<Integer, Dimension>(kbps, new Dimension(width, height));
        }
    }

    private void generateVariantPlaylist(HttpServletRequest request, int id, Player player, List<Pair<Integer, Dimension>> bitRates, PrintWriter writer) {
        writer.println("#EXTM3U");
        writer.println("#EXT-X-VERSION:1");
//        writer.println("#EXT-X-TARGETDURATION:" + SEGMENT_DURATION);

        String contextPath = getContextPath(request);
        for (Pair<Integer, Dimension> bitRate : bitRates) {
            Integer kbps = bitRate.getFirst();
            writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=" + kbps * 1000L);
            writer.print(contextPath + "hls/hls.m3u8?id=" + id + "&player=" + player.getId() + "&bitRate=" + kbps);
            Dimension dimension = bitRate.getSecond();
            if (dimension != null) {
                writer.print("@" + dimension.width + "x" + dimension.height);
            }
            writer.println();
        }
//        writer.println("#EXT-X-ENDLIST");
    }

    private void generateNormalPlaylist(HttpServletRequest request, int id, Player player, Pair<Integer, Dimension> bitRate, int totalDuration, PrintWriter writer) {
        writer.println("#EXTM3U");
        writer.println("#EXT-X-VERSION:1");
        writer.println("#EXT-X-TARGETDURATION:" + SEGMENT_DURATION);

        for (int i = 0; i < totalDuration / SEGMENT_DURATION; i++) {
            int offset = i * SEGMENT_DURATION;
            writer.println("#EXTINF:" + SEGMENT_DURATION + ",");
            writer.println(createStreamUrl(request, player, id, offset, SEGMENT_DURATION, bitRate));
        }

        int remainder = totalDuration % SEGMENT_DURATION;
        if (remainder > 0) {
            writer.println("#EXTINF:" + remainder + ",");
            int offset = totalDuration - remainder;
            writer.println(createStreamUrl(request, player, id, offset, remainder, bitRate));
        }
        writer.println("#EXT-X-ENDLIST");
    }

    private String createStreamUrl(HttpServletRequest request, Player player, int id, int offset, int duration, Pair<Integer, Dimension> bitRate) {
        StringBuilder builder = new StringBuilder();
        builder.append(getContextPath(request)).append("stream/stream.ts?id=").append(id).append("&hls=true&timeOffset=").append(offset)
                .append("&player=").append(player.getId()).append("&duration=").append(duration);
        if (bitRate != null) {
            builder.append("&maxBitRate=").append(bitRate.getFirst());
            Dimension dimension = bitRate.getSecond();
            if (dimension != null) {
                builder.append("&size=").append(dimension.width).append("x").append(dimension.height);
            }
        }
        return builder.toString();
    }

    private String getContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "/";
        } else {
            contextPath += "/";
        }
        return contextPath;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
