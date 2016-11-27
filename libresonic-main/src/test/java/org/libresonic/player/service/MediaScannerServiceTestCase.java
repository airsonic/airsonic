package org.libresonic.player.service;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.libresonic.player.TestCaseUtils;
import org.libresonic.player.dao.*;
import org.libresonic.player.domain.Album;
import org.libresonic.player.domain.Artist;
import org.libresonic.player.domain.MediaFile;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A unit test class to test the MediaScannerService.
 *
 * This class uses the Spring application context configuration present in the
 * /org/libresonic/player/service/mediaScannerServiceTestCase/ directory.
 *
 * The media library is found in the /MEDIAS directory.
 * It is composed of 2 musicFolders (Music and Music2) and several little weight audio files.
 *
 * At runtime, the subsonic_home dir is set to target/test-classes/org/libresonic/player/service/mediaScannerServiceTestCase.
 * An empty database is created on the fly.
 *
 */
public class MediaScannerServiceTestCase extends TestCase {

  private static String baseResources = "/org/libresonic/player/service/mediaScannerServiceTestCase/";

  private final MetricRegistry metrics = new MetricRegistry();


  private MediaScannerService mediaScannerService = null;
  private MediaFileDao mediaFileDao = null;
  private MusicFolderDao musicFolderDao = null;
  private DaoHelper daoHelper = null;
  private MediaFileService mediaFileService = null;
  private ArtistDao artistDao = null;
  private AlbumDao albumDao = null;


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    System.setProperty("libresonic.home", TestCaseUtils.libresonicHomePathForTest());

    TestCaseUtils.cleanLibresonicHomeForTest();

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaScannerService = (MediaScannerService)context.getBean("mediaScannerService");
    mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");
    daoHelper = (DaoHelper) context.getBean("daoHelper");
    mediaFileService = (MediaFileService) context.getBean("mediaFileService");
    artistDao = (ArtistDao) context.getBean("artistDao");
    albumDao = (AlbumDao) context.getBean("albumDao");
  }


  /**
   * Tests the MediaScannerService by scanning the test media library into an empty database.
   */
  public void testScanLibrary() {

    Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTestCase.class, "Timer.global"));

    Timer.Context globalTimerContext =  globalTimer.time();
    TestCaseUtils.execScan(mediaScannerService);
    globalTimerContext.stop();

    System.out.println("--- Report of records count per table ---");
    Map<String,Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
    records.keySet().forEach(tableName -> System.out.println(tableName+" : "+records.get(tableName).toString() ));
    System.out.println("--- *********************** ---");


    // Music Folder Music must have 3 children
    List<MediaFile> listeMusicChildren = mediaFileDao.getChildrenOf(MusicFolderDaoMock.resolveMusicFolderPath());
    Assert.assertEquals(3,listeMusicChildren.size());
    // Music Folder Music2 must have 1 children
    List<MediaFile> listeMusic2Children = mediaFileDao.getChildrenOf(MusicFolderDaoMock.resolveMusic2FolderPath());
    Assert.assertEquals(1,listeMusic2Children.size());

    System.out.println("--- List of all artists ---");
    System.out.println("artistName#albumCount");
    List<Artist> allArtists = artistDao.getAlphabetialArtists(0,0,musicFolderDao.getAllMusicFolders());
    allArtists.forEach(artist -> System.out.println(artist.getName()+"#"+artist.getAlbumCount()));
    System.out.println("--- *********************** ---");

    System.out.println("--- List of all albums ---");
    System.out.println("name#artist");
    List<Album> allAlbums = albumDao.getAlphabetialAlbums(0,0,true,musicFolderDao.getAllMusicFolders());
    allAlbums.forEach(album -> System.out.println(album.getName()+"#"+album.getArtist()));
    Assert.assertEquals(5,allAlbums.size());
    System.out.println("--- *********************** ---");

    List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolderDao.getAllMusicFolders());
    Assert.assertEquals(2,listeSongs.size());

    // display out metrics report
    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    reporter.report();

    System.out.print("End");
  }

}
