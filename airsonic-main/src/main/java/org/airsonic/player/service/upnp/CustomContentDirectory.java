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
package org.airsonic.player.service.upnp;

import com.google.common.collect.Lists;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.seamless.util.MimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Sindre Mehus
 * @version $Id: TagBasedContentDirectory.java 3739 2013-12-03 11:55:01Z sindre_mehus $
 */
public abstract class CustomContentDirectory extends AbstractContentDirectoryService {

    protected static final String CONTAINER_ID_ROOT = "0";

    @Autowired
    protected SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    protected JWTSecurityService jwtSecurityService;

    public CustomContentDirectory() {
        super(Lists.newArrayList("*"), Lists.newArrayList());
    }

    protected Res createResourceForSong(MediaFile song) {
        Player player = playerService.getGuestPlayer(null);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/ext/stream")
                .queryParam("id", song.getId())
                .queryParam("player", player.getId());

        if (song.isVideo()) {
            builder.queryParam("format", TranscodingService.FORMAT_RAW);
        }

        jwtSecurityService.addJWTToken(builder);

        String url = builder.toUriString();

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
        String dlnaBaseLANURL = settingsService.getDlnaBaseLANURL();
        if(StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new RuntimeException("DLNA Base LAN URL is not set correctly");
        }
        return dlnaBaseLANURL;
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

    public void setJwtSecurityService(JWTSecurityService jwtSecurityService) {
        this.jwtSecurityService = jwtSecurityService;
    }
}
