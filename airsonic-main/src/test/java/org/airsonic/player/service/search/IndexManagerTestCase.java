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
package org.airsonic.player.service.search;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.util.ObjectUtils.isEmpty;

public class IndexManagerTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexManager indexManager;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath.apply("Music"));
            musicFolders.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    ResourceLoader resourceLoader;

    @Test
    public void testExpunge() {

        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(0);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery("_DIR_ Ravel");

        SearchCriteria criteriaSong = new SearchCriteria();
        criteriaSong.setOffset(0);
        criteriaSong.setCount(Integer.MAX_VALUE);
        criteriaSong.setQuery("Gaspard");

        SearchCriteria criteriaAlbumId3 = new SearchCriteria();
        criteriaAlbumId3.setOffset(0);
        criteriaAlbumId3.setCount(Integer.MAX_VALUE);
        criteriaAlbumId3.setQuery("Complete Piano Works");

        /* Delete DB record. */

        // artist
        SearchResult result = searchService.search(criteria, musicFolders, IndexType.ARTIST);
        assertEquals(2, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel", result.getMediaFiles().get(0).getName());
        assertEquals("_DIR_ Sixteen Horsepower", result.getMediaFiles().get(1).getName());

        List<Integer> candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(2, candidates.size());

        // album
        result = searchService.search(criteria, musicFolders, IndexType.ALBUM);
        assertEquals(2, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel - Complete Piano Works", result.getMediaFiles().get(0).getName());
        assertEquals("_DIR_ Ravel - Chamber Music With Voice", result.getMediaFiles().get(1).getName());

        candidates = mediaFileDao.getAlbumExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getAlbumExpungeCandidates();
        assertEquals(2, candidates.size());

        // song
        result = searchService.search(criteriaSong, musicFolders, IndexType.SONG);
        assertEquals(2, result.getMediaFiles().size());
        assertEquals("01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(0).getName());
        assertEquals("02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getName());

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(2, candidates.size());

        // artistid3
        result = searchService.search(criteria, musicFolders, IndexType.ARTIST_ID3);
        assertEquals(1, result.getArtists().size());
        assertEquals("_DIR_ Ravel", result.getArtists().get(0).getName());

        candidates = artistDao.getExpungeCandidates();
        assertEquals(0, candidates.size());

        artistDao.markNonPresent(new Date());

        candidates = artistDao.getExpungeCandidates();
        assertEquals(4, candidates.size());

        // albumId3
        result = searchService.search(criteriaAlbumId3, musicFolders, IndexType.ALBUM_ID3);
        assertEquals(1, result.getAlbums().size());
        assertEquals("Complete Piano Works", result.getAlbums().get(0).getName());

        candidates = albumDao.getExpungeCandidates();
        assertEquals(0, candidates.size());

        albumDao.markNonPresent(new Date());

        candidates = albumDao.getExpungeCandidates();
        assertEquals(4, candidates.size());

        /* Does not scan, only expunges the index. */
        indexManager.startIndexing();
        indexManager.expunge();
        indexManager.stopIndexing(indexManager.getStatistics());

        /* 
         * Subsequent search results.
         * Results can also be confirmed with Luke.
         */

        result = searchService.search(criteria, musicFolders, IndexType.ARTIST);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteria, musicFolders, IndexType.ALBUM);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteriaSong, musicFolders, IndexType.SONG);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteria, musicFolders, IndexType.ARTIST_ID3);
        assertEquals(0, result.getArtists().size());

        result = searchService.search(criteriaAlbumId3, musicFolders, IndexType.ALBUM_ID3);
        assertEquals(0, result.getAlbums().size());

    }

}