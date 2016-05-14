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
package org.libresonic.player.ajax;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.AvatarScheme;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.PlayStatus;
import org.libresonic.player.domain.Player;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.MediaScannerService;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.service.StatusService;
import org.libresonic.player.util.StringUtil;

/**
 * Provides AJAX-enabled services for retrieving the currently playing file and directory.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class NowPlayingService {

    private static final Logger LOG = Logger.getLogger(NowPlayingService.class);

    private PlayerService playerService;
    private StatusService statusService;
    private SettingsService settingsService;
    private MediaScannerService mediaScannerService;

    /**
     * Returns details about what the current player is playing.
     *
     * @return Details about what the current player is playing, or <code>null</code> if not playing anything.
     */
    public NowPlayingInfo getNowPlayingForCurrentPlayer() throws Exception {
        WebContext webContext = WebContextFactory.get();
        Player player = playerService.getPlayer(webContext.getHttpServletRequest(), webContext.getHttpServletResponse());

        for (NowPlayingInfo info : getNowPlaying()) {
            if (player.getId().equals(info.getPlayerId())) {
                return info;
            }
        }
        return null;
    }

    /**
     * Returns details about what all users are currently playing.
     *
     * @return Details about what all users are currently playing.
     */
    public List<NowPlayingInfo> getNowPlaying() throws Exception {
        try {
            return convert(statusService.getPlayStatuses());
        } catch (Throwable x) {
            LOG.error("Unexpected error in getNowPlaying: " + x, x);
            return Collections.emptyList();
        }
    }

    /**
     * Returns media folder scanning status.
     */
    public ScanInfo getScanningStatus() {
        return new ScanInfo(mediaScannerService.isScanning(), mediaScannerService.getScanCount());
    }

    private List<NowPlayingInfo> convert(List<PlayStatus> playStatuses) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String url = request.getRequestURL().toString();
        List<NowPlayingInfo> result = new ArrayList<NowPlayingInfo>();
        for (PlayStatus status : playStatuses) {

            Player player = status.getPlayer();
            MediaFile mediaFile = status.getMediaFile();
            String username = player.getUsername();
            if (username == null) {
                continue;
            }
            UserSettings userSettings = settingsService.getUserSettings(username);
            if (!userSettings.isNowPlayingAllowed()) {
                continue;
            }

            String artist = mediaFile.getArtist();
            String title = mediaFile.getTitle();
            String streamUrl = url.replaceFirst("/dwr/.*", "/stream?player=" + player.getId() + "&id=" + mediaFile.getId());
            String albumUrl = url.replaceFirst("/dwr/.*", "/main.view?id=" + mediaFile.getId());
            String lyricsUrl = null;
            if (!mediaFile.isVideo()) {
                lyricsUrl = url.replaceFirst("/dwr/.*", "/lyrics.view?artistUtf8Hex=" + StringUtil.utf8HexEncode(artist) +
                                                        "&songUtf8Hex=" + StringUtil.utf8HexEncode(title));
            }
            String coverArtUrl = url.replaceFirst("/dwr/.*", "/coverArt.view?size=60&id=" + mediaFile.getId());

            String avatarUrl = null;
            if (userSettings.getAvatarScheme() == AvatarScheme.SYSTEM) {
                avatarUrl = url.replaceFirst("/dwr/.*", "/avatar.view?id=" + userSettings.getSystemAvatarId());
            } else if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM && settingsService.getCustomAvatar(username) != null) {
                avatarUrl = url.replaceFirst("/dwr/.*", "/avatar.view?usernameUtf8Hex=" + StringUtil.utf8HexEncode(username));
            }

            // Rewrite URLs in case we're behind a proxy.
            if (settingsService.isRewriteUrlEnabled()) {
                String referer = request.getHeader("referer");
                streamUrl = StringUtil.rewriteUrl(streamUrl, referer);
                albumUrl = StringUtil.rewriteUrl(albumUrl, referer);
                lyricsUrl = StringUtil.rewriteUrl(lyricsUrl, referer);
                coverArtUrl = StringUtil.rewriteUrl(coverArtUrl, referer);
                avatarUrl = StringUtil.rewriteUrl(avatarUrl, referer);
            }

            String tooltip = StringUtil.toHtml(artist) + " &ndash; " + StringUtil.toHtml(title);

            if (StringUtils.isNotBlank(player.getName())) {
                username += "@" + player.getName();
            }
            artist = StringUtil.toHtml(StringUtils.abbreviate(artist, 25));
            title = StringUtil.toHtml(StringUtils.abbreviate(title, 25));
            username = StringUtil.toHtml(StringUtils.abbreviate(username, 25));

            long minutesAgo = status.getMinutesAgo();

            if (minutesAgo < 60) {
                result.add(new NowPlayingInfo(player.getId(),username, artist, title, tooltip, streamUrl, albumUrl, lyricsUrl,
                                              coverArtUrl, avatarUrl, (int) minutesAgo));
            }
        }
        return result;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaScannerService(MediaScannerService mediaScannerService) {
        this.mediaScannerService = mediaScannerService;
    }
}
