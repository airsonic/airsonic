package org.airsonic.player.service.search;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MusicFolderTestData;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@ContextConfiguration(locations = {
        "/applicationContext-service.xml",
        "/applicationContext-cache.xml",
        "/applicationContext-testdb.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Component
/**
 * Abstract class for scanning MusicFolder.
 */
public abstract class AbstractAirsonicHomeTest implements AirsonicHomeTest {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    /*
     * Currently, Maven is executing test classes in series,
     * so this class can hold the state.
     * When executing in parallel, subclasses should override this.
     */
    private static AtomicBoolean dataBasePopulated = new AtomicBoolean();

    // Above.
    private static AtomicBoolean dataBaseReady = new AtomicBoolean();

    protected final static Function<String, String> resolveBaseMediaPath = (childPath) -> {
        return MusicFolderTestData.resolveBaseMediaPath().concat(childPath);
    };

    @Autowired
    protected DaoHelper daoHelper;

    @Autowired
    protected MediaScannerService mediaScannerService;

    @Autowired
    protected MusicFolderDao musicFolderDao;

    @Autowired
    protected SettingsService settingsService;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    public AtomicBoolean dataBasePopulated() {
        return dataBasePopulated;
    }

    @Override
    public AtomicBoolean dataBaseReady() {
        return dataBaseReady;
    }
    
    @Override
    public final void populateDatabaseOnlyOnce() {
        if (!dataBasePopulated().get()) {
            dataBasePopulated().set(true);
            getMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();
            try {
                // Await time to avoid scan failure.
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TestCaseUtils.execScan(mediaScannerService);
            System.out.println("--- Report of records count per table ---");
            Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
            records.keySet().stream().filter(s ->
                    s.equals("MEDIA_FILE")
                    | s.equals("ARTIST")
                    | s.equals("MUSIC_FOLDER")
                    | s.equals("ALBUM"))
                    .forEach(tableName ->
                        System.out.println("\t" + tableName + " : " + records.get(tableName).toString()));
            System.out.println("--- *********************** ---");
            try {
                // Await for Lucene to finish writing(asynchronous).
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dataBaseReady().set(true);
        } else {
            while (!dataBaseReady().get()) {
                try {
                    // The subsequent test method waits while reading DB data.
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}