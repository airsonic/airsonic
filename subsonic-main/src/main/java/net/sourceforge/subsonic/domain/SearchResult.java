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
package net.sourceforge.subsonic.domain;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.subsonic.service.MediaScannerService;
import net.sourceforge.subsonic.service.SearchService;

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