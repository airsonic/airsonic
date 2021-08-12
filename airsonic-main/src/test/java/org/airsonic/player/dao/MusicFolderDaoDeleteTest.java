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
package org.airsonic.player.dao;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.util.ObjectUtils.isEmpty;

/*
 * Cleanup database doesn't clear files after removing music folder #1425
 */
public class MusicFolderDaoDeleteTest extends AbstractAirsonicHomeTest {

    private MusicFolder musicFolder;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private MusicFolderDao musicFolderDao;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    ResourceLoader resourceLoader;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolder)) {
            File musicDir = new File(resolveBaseMediaPath.apply("Music"));
            musicFolder = new MusicFolder(1, musicDir, "Music", true, new Date());
        }
        return Arrays.asList(musicFolder);
    }

    @Before
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testDeleteMusicFolder() {

        assertEquals(3, mediaFileDao.getChildrenOf(musicFolder.getPath().getPath()).size());
        assertEquals(4, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(musicFolder)).size());
        assertEquals(4, albumDao.getAlbumCount(Arrays.asList(musicFolder)));

        // Empty run.
        mediaFileDao.expunge();

        // Results should not change.
        assertEquals(3, mediaFileDao.getChildrenOf(musicFolder.getPath().getPath()).size());
        assertEquals(4, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(musicFolder)).size());
        assertEquals(4, albumDao.getAlbumCount(Arrays.asList(musicFolder)));

        // Delete musicFolder and run expunge of each dao.
        musicFolderDao.deleteMusicFolder(musicFolder.getId());
        mediaFileDao.expunge();
        artistDao.expunge();
        albumDao.expunge();

        // All results should be 0.
        assertEquals(0, mediaFileDao.getChildrenOf(musicFolder.getPath().getPath()).size());
        assertEquals(0, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(musicFolder)).size());
        assertEquals(0, albumDao.getAlbumCount(Arrays.asList(musicFolder)));

    }

}