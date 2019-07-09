package org.airsonic.player.service.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.MusicFolderTestData;

/**
 * Test case interface for scanning MusicFolder.
 */
public interface AirsonicHomeTest {

    /**
     * MusicFolder used by test class.
     * 
     * @return MusicFolder used by test class
     */
    default List<MusicFolder> getMusicFolders() {
        return MusicFolderTestData.getTestMusicFolders();
    };

    /**
     * Whether the data input has been completed.
     * 
     * @return Static AtomicBoolean indicating whether the data injection has been
     *         completed
     */
    abstract AtomicBoolean dataBasePopulated();

    /**
     * Whether the data input has been completed.
     * 
     * @return Static AtomicBoolean indicating whether the data injection has been
     *         completed
     */
    abstract AtomicBoolean dataBaseReady();
    
    /**
     * Populate the database only once.
     * It is called in the @Before granted method.
     */
    void populateDatabaseOnlyOnce();

}
