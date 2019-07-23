
package org.airsonic.player.service.search;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.service.SearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;

/*
 * Tests to prove what kind of strings/chars can be used in the genre field.
 */
public class SearchServiceSpecialGenreTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath.apply("Search/SpecialGenre"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    /*
     * There are 19 files
     * in src/test/resources/MEDIAS/Search/SpecialGenre/ARTIST1/ALBUM_A.
     * In FILE01 to FILE16, Special strings for Lucene syntax are stored
     * as tag values ​​of Genre.
     * 
     * Legacy can not search all these genres.
     * (Strictly speaking, the genre field is not created at index creation.)
     */
    @Test
    public void testQueryEscapeRequires() {

        List<MusicFolder> folders = getMusicFolders();

        Function<String, RandomSearchCriteria> simpleStringCriteria = s ->
            new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    s, // genre,
                    null, // fromYear
                    null, // toYear
                    folders // musicFolders
            );

        List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("+"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("-"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("&&"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("+"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("||"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(" ("));// space & bracket
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(")"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("{}"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("[]"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("^"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\""));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("~"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("*"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("?"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(":"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\\"));
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("/"));
        Assert.assertEquals(0, songs.size());

    }

    /*
     * Jaudiotagger applies special treatment to bracket (FILE17).
     * 
     */
    @Test
    public void testBrackets() {

        List<MusicFolder> folders = getMusicFolders();

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

    /*
     * Jaudiotagger applies special treatment to numeric. (FILE18)
     */
    @Test
    public void testNumericMapping() {

        List<MusicFolder> folders = getMusicFolders();

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

    /*
     * Other special strings. (FILE19)
     * 
     * {'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's
     * 
     * Legacy stores with Analyze,
     * so searchable characters are different.
     * 
     */
    @Test
    public void testOthers() {

        List<MusicFolder> folders = getMusicFolders();

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
