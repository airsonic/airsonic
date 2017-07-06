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
package org.airsonic.player.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a list of genres.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.2 $ $Date: 2005/12/25 13:48:46 $
 */
public class Genres {

    private final Map<String, Genre> genres = new HashMap<String, Genre>();

    public void incrementAlbumCount(String genreName) {
        Genre genre = getOrCreateGenre(genreName);
        genre.incrementAlbumCount();
    }

    public void incrementSongCount(String genreName) {
        Genre genre = getOrCreateGenre(genreName);
        genre.incrementSongCount();
    }

    private Genre getOrCreateGenre(String genreName) {
        Genre genre = genres.get(genreName);
        if (genre == null) {
            genre = new Genre(genreName);
            genres.put(genreName, genre);
        }
        return genre;
    }

    public List<Genre> getGenres() {
        return new ArrayList<Genre>(genres.values());
    }
}