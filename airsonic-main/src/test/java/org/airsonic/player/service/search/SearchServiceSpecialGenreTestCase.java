
package org.airsonic.player.service.search;

import com.google.common.base.Function;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.service.SearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

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
    public void setup() {
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
     *
     * // XXX 3.x -> 8.x : Do the process more strictly.
     * 
     *  - Values ​​that can be cross-referenced with DB are stored in the index.
     *  - Search is also possible with user's readable value (file tag value).
     *  - However, there is an exception in parentheses.
     */
    @Test
    public void testQueryEscapeRequires() {

        Function<String, RandomSearchCriteria> simpleStringCriteria = s ->
            new RandomSearchCriteria(Integer.MAX_VALUE, // count
                s, // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("+"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("+", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 1", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("-"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("-", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 2", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("&&"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("&&", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 3", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("||"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("||", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 4", songs.get(0).getTitle());

        /*
         * // XXX 3.x -> 8.x : Brackets ()
         * 
         * Lucene can handle these.
         * However, brackets are specially parsed before the index creation process.
         * 
         * This string is never stored in the index.
         * This is the only exception.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply(" ("));// space & bracket
        Assert.assertEquals(0, songs.size());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(")"));
        Assert.assertEquals(0, songs.size());

        /*
         * // XXX 3.x -> 8.x : Brackets {}[]
         * 
         * Lucene can handle these.
         * However, brackets are specially parsed before the index creation process.
         * 
         * This can be done with a filter that performs the reverse process
         * on the input values ​​when searching.
         * As a result, the values ​​stored in the file can be retrieved by search.
         * 
         * @see AnalyzerFactory
         * 
         * >>>>>
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("{}"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        /*
         * This is the result of the tag parser and domain value.
         * It is different from the tag value in file.
         */
        Assert.assertEquals("{ }", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 7", songs.get(0).getTitle());
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("{ }"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("Query Escape Requires 7", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("[]"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        /*
         * This is the result of the tag parser and domain value.
         * It is different from the tag value in file.
         */
        Assert.assertEquals("[ ]", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("[ ]"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
        // <<<<<

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("^"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("^", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 9", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\""));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("\"", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 10", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("~"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("~", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 11", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("*"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("*", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 12", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("?"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("?", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 13", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply(":"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals(":", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 14", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("\\"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("\\", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 15", songs.get(0).getTitle());

        songs = searchService.getRandomSongs(simpleStringCriteria.apply("/"));
        Assert.assertEquals(1, songs.size());// XXX 3.x -> 8.x : Searchable
        Assert.assertEquals("/", songs.get(0).getGenre());
        Assert.assertEquals("Query Escape Requires 16", songs.get(0).getTitle());

    }

    /*
     * Jaudiotagger applies special treatment to bracket (FILE17).
     * XXX 3.x -> 8.x : Specification of genre search became more natural.
     */
    @Test
    public void testBrackets() {

        Function<String, RandomSearchCriteria> simpleStringCriteria = s ->
            new RandomSearchCriteria(Integer.MAX_VALUE, // count
                s, // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        // -(GENRE)- is registered as genre of FILE17.

        /*
         * Search by genre string registered in file.
         * 
         * The value stored in the index is different from legacy.
         * Domain value is kept as it is.
         */
        List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("-(GENRE)-"));
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("-GENRE -", songs.get(0).getGenre());
        Assert.assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

        /*
         * Search by Domain value.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("-GENRE -"));
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("-GENRE -", songs.get(0).getGenre());
        Assert.assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

        /*
         * Legacy genre search
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply(" genre"));
        // Strong unique parsing rules have been removed.
        Assert.assertEquals(0, songs.size());

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

        Function<String, RandomSearchCriteria> simpleStringCriteria = s ->
            new RandomSearchCriteria(Integer.MAX_VALUE, // count
                s, // genre,
                null, // fromYear
                null, // toYear
                getMusicFolders() // musicFolders
        );

        // XXX 3.x -> 8.x : Do the process more strictly.
        List<MediaFile> songs = searchService
                .getRandomSongs(simpleStringCriteria.apply("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's"));
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals(1, songs.size());
        Assert.assertEquals("Other special strings 1", songs.get(0).getTitle());
        Assert.assertEquals("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's", songs.get(0).getGenre());

        /*
         * Legacy kept "widthcaesar" using their own rules.
         * The previous rule has been discarded.
         */
        songs = searchService.getRandomSongs(simpleStringCriteria.apply("widthcaesar"));
        Assert.assertEquals(0, songs.size());

    }
}
