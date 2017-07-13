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
package org.airsonic.player.ajax;

/**
 * Details about what a user is currently listening to.
 *
 * @author Sindre Mehus
 */
public class NowPlayingInfo {

    private final String playerId;
    private final String username;
    private final String artist;
    private final String title;
    private final String tooltip;
    private final String streamUrl;
    private final String albumUrl;
    private final String lyricsUrl;
    private final String coverArtUrl;
    private final String avatarUrl;
    private final int minutesAgo;

    public NowPlayingInfo(String playerId, String user, String artist, String title, String tooltip, String streamUrl, String albumUrl,
                          String lyricsUrl, String coverArtUrl, String avatarUrl, int minutesAgo) {
        this.playerId = playerId;
        this.username = user;
        this.artist = artist;
        this.title = title;
        this.tooltip = tooltip;
        this.streamUrl = streamUrl;
        this.albumUrl = albumUrl;
        this.lyricsUrl = lyricsUrl;
        this.coverArtUrl = coverArtUrl;
        this.avatarUrl = avatarUrl;
        this.minutesAgo = minutesAgo;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public String getLyricsUrl() {
        return lyricsUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getMinutesAgo() {
        return minutesAgo;
    }
}
