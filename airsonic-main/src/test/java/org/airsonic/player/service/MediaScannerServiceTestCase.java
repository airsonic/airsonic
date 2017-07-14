package org.airsonic.player.service;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.airsonic.player.dao.*;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.util.HomeRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.FileOutputStream;
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
@ContextConfiguration(locations = {
        "/applicationContext-service.xml",
        "/applicationContext-cache.xml",
        "/applicationContext-testdb.xml",
        "/applicationContext-mockSonos.xml"})
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

    @Autowired
    ResourceLoader resourceLoader;


    /**
     * Tests the MediaScannerService by scanning the test media library into an empty database.
     */
    @Test
    public void testScanLibrary() {
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
        List<MediaFile> listeMusicChildren = mediaFileDao.getChildrenOf(MusicFolderTestData.resolveMusicFolderPath());
        Assert.assertEquals(3, listeMusicChildren.size());
        // Music Folder Music2 must have 1 children
        List<MediaFile> listeMusic2Children = mediaFileDao.getChildrenOf(MusicFolderTestData.resolveMusic2FolderPath());
        Assert.assertEquals(1, listeMusic2Children.size());

        System.out.println("--- List of all artists ---");
        System.out.println("artistName#albumCount");
        List<Artist> allArtists = artistDao.getAlphabetialArtists(0, 0, musicFolderDao.getAllMusicFolders());
        allArtists.forEach(artist -> System.out.println(artist.getName() + "#" + artist.getAlbumCount()));
        System.out.println("--- *********************** ---");

        System.out.println("--- List of all albums ---");
        System.out.println("name#artist");
        List<Album> allAlbums = albumDao.getAlphabetialAlbums(0, 0, true, musicFolderDao.getAllMusicFolders());
        allAlbums.forEach(album -> System.out.println(album.getName() + "#" + album.getArtist()));
        Assert.assertEquals(5, allAlbums.size());
        System.out.println("--- *********************** ---");

        List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental", 0, 0, musicFolderDao.getAllMusicFolders());
        Assert.assertEquals(2, listeSongs.size());

        // display out metrics report
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();

        System.out.print("End");
    }

    @Test
    public void testSpecialCharactersInFilename() throws Exception {
        Resource resource = resourceLoader.getResource("MEDIAS/piano.mp3");
        String directoryName = "Muff1nman\u2019s \uFF0FMusic";
        String fileName = "Muff1nman\u2019s\uFF0FPiano.mp3";
        File artistDir = temporaryFolder.newFolder(directoryName);
        File musicFile = artistDir.toPath().resolve(fileName).toFile();
        IOUtils.copy(resource.getInputStream(), new FileOutputStream(musicFile));

        MusicFolder musicFolder = new MusicFolder(1, temporaryFolder.getRoot(), "Music", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        settingsService.clearMusicFolderCache();
        TestCaseUtils.execScan(mediaScannerService);
        MediaFile mediaFile = mediaFileService.getMediaFile(musicFile);
        assertEquals(mediaFile.getFile().toString(), musicFile.toString());
        System.out.println(mediaFile.getFile().getPath());
        assertNotNull(mediaFile);
    }
}
