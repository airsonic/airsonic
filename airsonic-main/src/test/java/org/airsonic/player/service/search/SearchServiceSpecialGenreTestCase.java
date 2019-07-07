
package org.airsonic.player.service.search;

import com.google.common.base.Function;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.dao.MusicFolderTestData;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
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
 * Tests to prove what kind of strings/chars can be used in the genre field.
 */
public class SearchServiceSpecialGenreTestCase {

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

            File musicDir = new File(resolvePath.apply("Search/SpecialGenre"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));

        }
        return musicFolders;
    }

    private int count = 1;

    private synchronized void populateDatabase() {

        try {
            Thread.sleep(300 * count++);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
    public void testQueryEscapeRequires() {
    
        /*
         * Legacy is not searchable. (FILE01 - FILE16)
         */

    }

    @Test
    public void testBrackets() {
        
        /*
         * Jaudiotagger applies special treatment to bracket (FILE17).
         * 
         */

        List<MusicFolder> folders = getTestMusicFolders();

        RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "-(GENRE)-", // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(criteria);
        Assert.assertEquals(0, songs.size());

        criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                " genre", // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        songs = searchService.getRandomSongs(criteria);
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());
        Assert.assertEquals("-GENRE -", songs.get(0).getGenre());

    }

    @Test
    public void testNumericMapping() {

        /*
         * Jaudiotagger applies special treatment to numeric. (FILE18)
         */
        List<MusicFolder> folders = getTestMusicFolders();

        RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "Rock", // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(criteria);
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("Numeric mapping specification of genre 1", songs.get(0).getTitle());

        // The value registered in the file is 17
        Assert.assertEquals("Rock", songs.get(0).getGenre());

    }

    @Test
    public void testOthers() {

        /*
         * Other special strings. (FILE19)
         * 
         * {'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's
         * 
         * Legacy stores with Analyze,
         * so searchable characters are different.
         * 
         */
        List<MusicFolder> folders = getTestMusicFolders();

        RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's", // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(criteria);
        Assert.assertEquals(0, songs.size());

        criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "widthcaesar", // genre,
                null, // fromYear
                null, // toYear
                folders // musicFolders
        );

        songs = searchService.getRandomSongs(criteria);
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("Other special strings 1", songs.get(0).getTitle());
        Assert.assertEquals("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's", songs.get(0).getGenre());

    }
}
