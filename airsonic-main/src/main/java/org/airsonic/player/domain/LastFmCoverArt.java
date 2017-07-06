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
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class LastFmCoverArt {

    private final String imageUrl;
    private final String artist;
    private final String album;

    public LastFmCoverArt(String imageUrl, String artist, String album) {
        this.imageUrl = imageUrl;
        this.artist = artist;
        this.album = album;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }
}

