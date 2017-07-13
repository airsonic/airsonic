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

import org.airsonic.player.domain.ArtistBio;

import java.util.List;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class ArtistInfo {

    private final List<SimilarArtist> similarArtists;
    private final ArtistBio artistBio;
    private final List<TopSong> topSongs;

    public ArtistInfo(List<SimilarArtist> similarArtists, ArtistBio artistBio, List<TopSong> topSongs) {
        this.similarArtists = similarArtists;
        this.artistBio = artistBio;
        this.topSongs = topSongs;
    }

    public List<SimilarArtist> getSimilarArtists() {
        return similarArtists;
    }

    public ArtistBio getArtistBio() {
        return artistBio;
    }

    public List<TopSong> getTopSongs() {
        return topSongs;
    }
}
