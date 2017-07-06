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
package org.airsonic.player.ajax;

/**
 * Contains info about a similar artist.
 *
 * @author Sindre Mehus
 */
public class SimilarArtist {

    private final int mediaFileId;
    private final String artistName;

    public SimilarArtist(int mediaFileId, String artistName) {
        this.mediaFileId = mediaFileId;
        this.artistName = artistName;
    }

    public int getMediaFileId() {
        return mediaFileId;
    }

    public String getArtistName() {
        return artistName;
    }
}