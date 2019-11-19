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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller which produces the HLS (Http Live Streaming) playlist.
 *
 * @author Sindre Mehus
 */
@Controller("hlsController")
@RequestMapping({"/hls/**", "/ext/hls/**"})
public class HLSController {

    private static final int SEGMENT_DURATION = 10;
    private static final Pattern BITRATE_PATTERN = Pattern.compile("(\\d+)(@(\\d+)x(\\d+))?");

    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private JWTSecurityService jwtSecurityService;

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setHeader("Access-Control-Allow-Origin", "*");

        int id = ServletRequestUtils.getIntParameter(request, "id", 0);
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        Player player = playerService.getPlayer(request, response);
        String username = player.getUsername();

        if (mediaFile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Media file not found: " + id);
            return;
        }

        if (username != null && !securityService.isFolderAccessAllowed(mediaFile, username)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Access to file " + mediaFile.getId() + " is forbidden for user " + username);
            return;
        }

        Integer duration = mediaFile.getDurationSeconds();
        if (duration == null || duration == 0) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown duration for media file: " + id);
            return;
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

        return;
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
            return Pair.of(kbps, null);
        } else {
            int width = Integer.parseInt(matcher.group(3));
            int height = Integer.parseInt(matcher.group(4));
            return Pair.of(kbps, new Dimension(width, height));
        }
    }

    private void generateVariantPlaylist(HttpServletRequest request, int id, Player player, List<Pair<Integer, Dimension>> bitRates, PrintWriter writer) {
        writer.println("#EXTM3U");
        writer.println("#EXT-X-VERSION:1");
//        writer.println("#EXT-X-TARGETDURATION:" + SEGMENT_DURATION);

        String contextPath = getContextPath(request);
        for (Pair<Integer, Dimension> bitRate : bitRates) {
            Integer kbps = bitRate.getLeft();
            writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=" + kbps * 1000L);
            UriComponentsBuilder url = (UriComponentsBuilder.fromUriString(contextPath + "ext/hls/hls.m3u8")
                    .queryParam("id", id)
                    .queryParam("player", player.getId())
                    .queryParam("bitRate", kbps));
            jwtSecurityService.addJWTToken(url);
            writer.print(url.toUriString());
            Dimension dimension = bitRate.getRight();
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getContextPath(request) + "ext/stream/stream.ts");
        builder.queryParam("id", id);
        builder.queryParam("hls", "true");
        builder.queryParam("timeOffset", offset);
        builder.queryParam("player", player.getId());
        builder.queryParam("duration", duration);
        if (bitRate != null) {
            builder.queryParam("maxBitRate", bitRate.getLeft());
            Dimension dimension = bitRate.getRight();
            if (dimension != null) {
                builder.queryParam("size", dimension.width);
                builder.queryParam("x", dimension.height);
            }
        }
        jwtSecurityService.addJWTToken(builder);
        return builder.toUriString();
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

}
