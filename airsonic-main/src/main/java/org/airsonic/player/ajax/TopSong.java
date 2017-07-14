/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package org.airsonic.player.ajax;

/**
 * See {@link ArtistInfo}.
 *
 * @author Sindre Mehus
 */
public class TopSong {

    private final int id;
    private final String title;
    private final String artist;
    private final String album;
    private final String durationAsString;
    private final boolean starred;

    public TopSong(int id, String title, String artist, String album, String durationAsString, boolean starred) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationAsString = durationAsString;
        this.starred = starred;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getDurationAsString() {
        return durationAsString;
    }

    public boolean isStarred() {
        return starred;
    }
}
