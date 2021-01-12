package org.airsonic.player.dao;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AlbumDaoTest extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private AlbumDao albumDao;

    private List<MusicFolder> sameName;
    private List<MusicFolder> noTagFirst;
    private List<MusicFolder> nameInReverse;

    private final int offset = 0;
    private final int count = Integer.MAX_VALUE;

    @Before
    public void setup() {
        if (ObjectUtils.isEmpty(musicFolders)) {
            sameName = Arrays.asList(new MusicFolder(1, new File(resolveBaseMediaPath.apply("Scan/ID3Album/sameName")), "1", true, new Date()));
            noTagFirst = Arrays.asList(new MusicFolder(2, new File(resolveBaseMediaPath.apply("Scan/ID3Album/noTagFirst")), "2", true, new Date()));
            nameInReverse = Arrays.asList(new MusicFolder(3, new File(resolveBaseMediaPath.apply("Scan/ID3Album/nameInReverse")), "3", true, new Date()));
            musicFolders = new ArrayList<>();
            musicFolders.addAll(sameName);
            musicFolders.addAll(noTagFirst);
            musicFolders.addAll(nameInReverse);
        }
        populateDatabaseOnlyOnce();
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Test
    public void testUpdateAlbum() {

        // File structure
        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(offset, count, false, sameName);
        assertEquals(1, albums.size());
        MediaFile album = albums.get(0);
        assertEquals("ALBUM1", album.getName());
        assertEquals("albumArtistA", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertEquals("genreA", album.getGenre());
        assertEquals(Integer.valueOf(2001), album.getYear());

        // ID3 tags
        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(offset, count, false, false, sameName);
        assertEquals(1, albumId3s.size());
        Album albumId3 = albumId3s.get(0);

        // [albumA albumArtistA genreB 2002]
        // This combination is "a combination that should not exist.
        assertEquals("albumA", albumId3.getName());
        assertEquals("albumArtistA", albumId3.getArtist());
        assertEquals("genreB", albumId3.getGenre()); // XXX Unintentional overwrite
        assertEquals(Integer.valueOf(2002), albumId3.getYear()); // XXX Unintentional overwrite

        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());
    }

    @Test
    public void testNoTagFirst() {

        // File structure
        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(offset, count, false, noTagFirst);
        assertEquals(1, albums.size());
        MediaFile album = albums.get(0);
        assertEquals("ALBUM2", album.getName());
        assertEquals("ARTIST2", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertNull(album.getGenre());
        assertNull(album.getYear());
        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        // ID3 tags
        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(offset, count, false, false, noTagFirst);
        assertEquals(2, albumId3s.size());

        Album albumId3 = albumId3s.get(0);
        assertEquals("ALBUM2", albumId3.getName());
        assertEquals("ARTIST2", albumId3.getArtist());
        assertNull(albumId3.getGenre());
        assertNull(albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

        albumId3 = albumId3s.get(1);
        assertEquals("albumC", albumId3.getName());
        assertEquals("albumArtistC", albumId3.getArtist());
        assertEquals("genreC", albumId3.getGenre());
        assertEquals(Integer.valueOf(2003), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());
    }

    @Test
    public void testNameInReverse() {

        // File structure
        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(offset, count, false, nameInReverse);
        assertEquals(1, albums.size());

        MediaFile album = albums.get(0);
        assertEquals("ALBUM3", album.getName());
        assertEquals("albumArtistE", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertEquals("genreE", album.getGenre());
        assertEquals(Integer.valueOf(2005), album.getYear());
        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        // ID3 tags
        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(offset, count, false, false, nameInReverse);
        assertEquals(2, albumId3s.size());

        Album albumId3 = albumId3s.get(0);
        assertEquals("albumD", albumId3.getName());
        assertEquals("albumArtistD", albumId3.getArtist());
        assertEquals("genreD", albumId3.getGenre());
        assertEquals(Integer.valueOf(2004), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

        albumId3 = albumId3s.get(1);
        assertEquals("albumE", albumId3.getName());
        assertEquals("albumArtistE", albumId3.getArtist());
        assertEquals("genreE", albumId3.getGenre());
        assertEquals(Integer.valueOf(2005), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());
    }

}
