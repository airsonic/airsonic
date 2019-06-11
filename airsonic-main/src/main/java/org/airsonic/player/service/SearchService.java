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

package org.airsonic.player.service;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.search.IndexType;

import java.util.List;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
public interface SearchService {

    void startIndexing();

    void index(MediaFile mediaFile);

    void index(Artist artist, MusicFolder musicFolder);

    void index(Album album);

    void stopIndexing();

    SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders,
            IndexType indexType);

    /**
     * Returns a number of random songs.
     *
     * @param criteria Search criteria.
     * @return List of random songs.
     */
    List<MediaFile> getRandomSongs(RandomSearchCriteria criteria);

    /**
     * Returns a number of random albums.
     *
     * @param count Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders);

    /**
     * Returns a number of random albums, using ID3 tag.
     *
     * @param count Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders);

    <T> ParamSearchResult<T> searchByName(
            String name, int offset, int count, List<MusicFolder> folderList, Class<T> clazz);

}
