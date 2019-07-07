
package org.airsonic.player.service.search;

import com.google.common.base.Function;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.dao.MusicFolderTestData;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import static org.springframework.util.ObjectUtils.isEmpty;

@ContextConfiguration(
        locations = {
                "/applicationContext-service.xml",
                "/applicationContext-cache.xml",
                "/applicationContext-testdb.xml",
                "/applicationContext-mockSonos.xml" })
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
/*
 * Test cases related to #1139.
 * Confirming whether shuffle search can be performed correctly in MusicFolder containing special strings.
 * 
 * (Since the query of getRandomAlbums consists of folder paths only,
 * this verification is easy to perform.)
 * 
 * This test case is a FalsePattern for search,
 * but there may be problems with the data flow prior to creating the search index.
 */
public class SearchServiceSpecialPathTestCase {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MediaScannerService mediaScannerService;

    @Autowired
    private MusicFolderDao musicFolderDao;

    @Autowired
    private DaoHelper daoHelper;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    ResourceLoader resourceLoader;

    @Before
    public void setup() throws Exception {
        populateDatabase();
    }

    private static boolean dataBasePopulated;

    private static Function<String, String> resolvePath = (childPath) ->{
        return MusicFolderTestData.resolveBaseMediaPath() + childPath;
    };
    
    private List<MusicFolder> musicFolders;
    
    private List<MusicFolder> getTestMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();

            File musicDir = new File(resolvePath.apply("Search/SpecialPath/accessible"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));

            File music2Dir = new File(resolvePath.apply("Search/SpecialPath/accessible's"));
            musicFolders.add(new MusicFolder(2, music2Dir, "accessible's", true, new Date()));

            File music3Dir = new File(resolvePath.apply("Search/SpecialPath/accessible+s"));
            musicFolders.add(new MusicFolder(3, music3Dir, "accessible+s", true, new Date()));
        }
        return musicFolders;
    }

    private synchronized void populateDatabase() {

        if (!dataBasePopulated) {
            getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();
            TestCaseUtils.execScan(mediaScannerService);
            System.out.println("--- Report of records count per table ---");
            Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
            records.keySet().stream().filter(s -> s.equals("MEDIA_FILE") // 20
                    | s.equals("ARTIST") // 5
                    | s.equals("MUSIC_FOLDER")// 3
                    | s.equals("ALBUM"))// 5
                    .forEach(tableName -> System.out
                            .println("\t" + tableName + " : " + records.get(tableName).toString()));
            System.out.println("--- *********************** ---");
            dataBasePopulated = true;
        }
    }

    @Test
    public void testSpecialCharactersInDirName() {

        List<MusicFolder> folders = getTestMusicFolders();

        // ALL Songs
        List<MediaFile> randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folders);
        Assert.assertEquals("ALL Albums ", 3, randomAlbums.size());

        // dir - accessible
        List<MusicFolder> folder01 = folders.stream()
                .filter(m -> "accessible".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder01);
        Assert.assertEquals("Albums in \"accessible\" ", 3, randomAlbums.size());

        // dir - accessible's
        List<MusicFolder> folder02 = folders.stream()
                .filter(m -> "accessible's".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder02);
        Assert.assertEquals("Albums in \"accessible's\" ", 0, randomAlbums.size());

        // dir - accessible+s
        List<MusicFolder> folder03 = folders.stream()
                .filter(m -> "accessible+s".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder03);
        Assert.assertEquals("Albums in \"accessible+s\" ", 1, folder03.size());

    }

}
