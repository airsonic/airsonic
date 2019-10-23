package org.airsonic.player.service;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.*;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MusicFolderTestData;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A unit test class to test the MediaScannerService.
 * <p>
 * This class uses the Spring application context configuration present in the
 * /org/airsonic/player/service/mediaScannerServiceTestCase/ directory.
 * <p>
 * The media library is found in the /MEDIAS directory.
 * It is composed of 2 musicFolders (Music and Music2) and several little weight audio files.
 * <p>
 * At runtime, the subsonic_home dir is set to target/test-classes/org/airsonic/player/service/mediaScannerServiceTestCase.
 * An empty database is created on the fly.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MediaScannerServiceTestCase {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule airsonicRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return airsonicRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private final MetricRegistry metrics = new MetricRegistry();

    @Autowired
    private MediaScannerService mediaScannerService;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private MusicFolderDao musicFolderDao;

    @Autowired
    private DaoHelper daoHelper;

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    /**
     * Tests the MediaScannerService by scanning the test media library into an empty database.
     */
    @Test
    public void testScanLibrary() {
        musicFolderDao.getAllMusicFolders().forEach(musicFolder -> musicFolderDao.deleteMusicFolder(musicFolder.getId()));
        MusicFolderTestData.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
        settingsService.clearMusicFolderCache();

        Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTestCase.class, "Timer.global"));

        Timer.Context globalTimerContext = globalTimer.time();
        TestCaseUtils.execScan(mediaScannerService);
        globalTimerContext.stop();

        System.out.println("--- Report of records count per table ---");
        Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
        records.keySet().forEach(tableName -> System.out.println(tableName + " : " + records.get(tableName).toString()));
        System.out.println("--- *********************** ---");


        // Music Folder Music must have 3 children
        List<MediaFile> listeMusicChildren = mediaFileDao.getChildrenOf(new File(MusicFolderTestData.resolveMusicFolderPath()).getPath());
        Assert.assertEquals(3, listeMusicChildren.size());
        // Music Folder Music2 must have 1 children
        List<MediaFile> listeMusic2Children = mediaFileDao.getChildrenOf(new File(MusicFolderTestData.resolveMusic2FolderPath()).getPath());
        Assert.assertEquals(1, listeMusic2Children.size());

        System.out.println("--- List of all artists ---");
        System.out.println("artistName#albumCount");
        List<Artist> allArtists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolderDao.getAllMusicFolders());
        allArtists.forEach(artist -> System.out.println(artist.getName() + "#" + artist.getAlbumCount()));
        System.out.println("--- *********************** ---");

        System.out.println("--- List of all albums ---");
        System.out.println("name#artist");
        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, musicFolderDao.getAllMusicFolders());
        allAlbums.forEach(album -> System.out.println(album.getName() + "#" + album.getArtist()));
        Assert.assertEquals(5, allAlbums.size());
        System.out.println("--- *********************** ---");

        List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental", 0, 0, musicFolderDao.getAllMusicFolders());
        Assert.assertEquals(2, listeSongs.size());

        // display out metrics report
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();

        System.out.print("End");
    }

    @Test
    public void testSpecialCharactersInFilename() throws Exception {
        InputStream resource = MediaScannerServiceTestCase.class
                .getClassLoader()
                .getResourceAsStream("MEDIAS/piano.mp3");
        assert resource != null;
        String directoryName = "Muff1nman\u2019s \uFF0FMusic";
        String fileName = "Muff1nman\u2019s\uFF0FPiano.mp3";
        File artistDir = temporaryFolder.newFolder(directoryName);
        File musicFile = artistDir.toPath().resolve(fileName).toFile();
        IOUtils.copy(resource, new FileOutputStream(musicFile));

        MusicFolder musicFolder = new MusicFolder(1, temporaryFolder.getRoot(), "Music", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        settingsService.clearMusicFolderCache();
        TestCaseUtils.execScan(mediaScannerService);
        MediaFile mediaFile = mediaFileService.getMediaFile(musicFile);
        assertEquals(mediaFile.getFile().toString(), musicFile.toString());
        System.out.println(mediaFile.getFile().getPath());
        assertNotNull(mediaFile);
    }

    @Test
    public void testNeverScanned() {

        mediaScannerService.neverScanned();
    }

    @Test
    public void testMusicBrainzReleaseIdTag() {

        // Add the "Music3" folder to the database
        File musicFolderFile = new File(MusicFolderTestData.resolveMusic3FolderPath());
        MusicFolder musicFolder = new MusicFolder(1, musicFolderFile, "Music3", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        settingsService.clearMusicFolderCache();
        TestCaseUtils.execScan(mediaScannerService);

        // Retrieve the "Music3" folder from the database to make
        // sure that we don't accidentally operate on other folders
        // from previous tests.
        musicFolder = musicFolderDao.getMusicFolderForPath(musicFolder.getPath().getPath());
        List<MusicFolder> folders = new ArrayList<>();
        folders.add(musicFolder);

        // Test that the artist is correctly imported
        List<Artist> allArtists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        Assert.assertEquals(1, allArtists.size());
        Artist artist = allArtists.get(0);
        Assert.assertEquals("TestArtist", artist.getName());
        Assert.assertEquals(1, artist.getAlbumCount());

        // Test that the album is correctly imported, along with its MusicBrainz release ID
        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, folders);
        Assert.assertEquals(1, allAlbums.size());
        Album album = allAlbums.get(0);
        Assert.assertEquals("TestAlbum", album.getName());
        Assert.assertEquals("TestArtist", album.getArtist());
        Assert.assertEquals(1, album.getSongCount());
        Assert.assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", album.getMusicBrainzReleaseId());
        Assert.assertEquals(musicFolderFile.toPath().resolve("TestAlbum").toString(), album.getPath());

        // Test that the music file is correctly imported, along with its MusicBrainz release ID and recording ID
        List<MediaFile> albumFiles = mediaFileDao.getChildrenOf(allAlbums.get(0).getPath());
        Assert.assertEquals(1, albumFiles.size());
        MediaFile file = albumFiles.get(0);
        Assert.assertEquals("Aria", file.getTitle());
        Assert.assertEquals("flac", file.getFormat());
        Assert.assertEquals("TestAlbum", file.getAlbumName());
        Assert.assertEquals("TestArtist", file.getArtist());
        Assert.assertEquals("TestArtist", file.getAlbumArtist());
        Assert.assertEquals(1, (long)file.getTrackNumber());
        Assert.assertEquals(2001, (long)file.getYear());
        Assert.assertEquals(album.getPath(), file.getParentPath());
        Assert.assertEquals(new File(album.getPath()).toPath().resolve("01 - Aria.flac").toString(), file.getPath());
        Assert.assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", file.getMusicBrainzReleaseId());
        Assert.assertEquals("831586f4-56f9-4785-ac91-447ae20af633", file.getMusicBrainzRecordingId());
    }
}
