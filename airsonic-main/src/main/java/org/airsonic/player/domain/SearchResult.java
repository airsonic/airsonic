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

import org.airsonic.player.service.SearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of a search.
 *
 * @author Sindre Mehus
 * @see SearchService#search
 */
public class SearchResult {

    private final List<MediaFile> mediaFiles = new ArrayList<MediaFile>();
    private final List<Artist> artists = new ArrayList<Artist>();
    private final List<Album> albums = new ArrayList<Album>();

    private int offset;
    private int totalHits;

    public List<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

      public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }
}