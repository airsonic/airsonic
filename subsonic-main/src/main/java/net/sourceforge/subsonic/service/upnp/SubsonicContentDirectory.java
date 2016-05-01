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
package net.sourceforge.subsonic.service.upnp;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.seamless.util.MimeType;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * @author Sindre Mehus
 * @version $Id: TagBasedContentDirectory.java 3739 2013-12-03 11:55:01Z sindre_mehus $
 */
public abstract class SubsonicContentDirectory extends AbstractContentDirectoryService {

    protected static final String CONTAINER_ID_ROOT = "0";

    protected SettingsService settingsService;
    private PlayerService playerService;
    private TranscodingService transcodingService;

    protected Res createResourceForSong(MediaFile song) {
        Player player = playerService.getGuestPlayer(null);
        String url = getBaseUrl() + "stream?id=" + song.getId() + "&player=" + player.getId();
        if (song.isVideo()) {
            url += "&format=" + TranscodingService.FORMAT_RAW;
        }

        String suffix = song.isVideo() ? FilenameUtils.getExtension(song.getPath()) : transcodingService.getSuffix(player, song, null);
        String mimeTypeString = StringUtil.getMimeType(suffix);
        MimeType mimeType = mimeTypeString == null ? null : MimeType.valueOf(mimeTypeString);

        Res res = new Res(mimeType, null, url);
        res.setDuration(formatDuration(song.getDurationSeconds()));
        return res;
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(8);

        int hours = seconds / 3600;
        seconds -= hours * 3600;

        int minutes = seconds / 60;
        seconds -= minutes * 60;

        result.append(hours).append(':');
        if (minutes < 10) {
            result.append('0');
        }
        result.append(minutes).append(':');
        if (seconds < 10) {
            result.append('0');
        }
        result.append(seconds);
        result.append(".0");

        return result.toString();
    }

    protected String getBaseUrl() {
        int port = settingsService.getPort();
        String contextPath = settingsService.getUrlRedirectContextPath();

        // Note: Serving media and cover art with http (as opposed to https) works when using jetty and SubsonicDeployer.
        StringBuilder url = new StringBuilder("http://")
                .append(settingsService.getLocalIpAddress())
                .append(":")
                .append(port)
                .append("/");

        if (StringUtils.isNotEmpty(contextPath)) {
            url.append(contextPath).append("/");
        }
        return url.toString();
    }

    protected BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws Exception {
        return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
    }

    @Override
    public BrowseResult search(String containerId,
                               String searchCriteria, String filter,
                               long firstResult, long maxResults,
                               SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
