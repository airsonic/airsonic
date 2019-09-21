
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.search.Query;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test case for QueryFactory.
 * These cases have the purpose of observing the current situation
 * and observing the impact of upgrading Lucene.
 */
@ContextConfiguration(
        locations = {
                "/applicationContext-service.xml",
                "/applicationContext-cache.xml",
                "/applicationContext-testdb.xml",
                "/applicationContext-mockSonos.xml" })
@DirtiesContext(
    classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryFactoryTestCase {

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
    private QueryFactory queryFactory;

    private static final String QUERY_ENG_ONLY = "ABC DEF";

    private static final String SEPA = System.getProperty("file.separator");

    private static final String PATH1 = SEPA + "var" + SEPA + "music1";
    private static final String PATH2 = SEPA + "var" + SEPA + "music2";

    private static final int FID1 = 10;
    private static final int FID2 = 20;

    private static final MusicFolder MUSIC_FOLDER1 =
            new MusicFolder(FID1, new File(PATH1), "music1", true, new java.util.Date());
    private static final MusicFolder MUSIC_FOLDER2 = 
            new MusicFolder(FID2, new File(PATH2), "music2", true, new java.util.Date());

    private static final List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    private static final List<MusicFolder> MULTI_FOLDERS  = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);


    /*
     * XXX 3.x -> 8.x :
     * It does not change the basic functional requirements for the query.
     * However, some minor improvements are included.
     * 
     *  - Use 'Or' instead of 'SpanOr'.
     *    This is suitable for 8.x document definition and query grammar.
     *    A more rigorous comparison.
     *
     *  - Removed comparison of input value and path from condition of search.
     *    It causes a false search that the user can not imagine.
     *    Originally unnecessary.
     *
     *  - mediaType and genre changed to raw string key comparison.
     *    Currently, these are "key" strings, both in the requirements and in the implementation.
     *    The legacy "normalize" is dirty code that compensates for the incomplete analytics implementation
     *    and is not necessary as long as proper key comparison can be done.
     *    
     *    => Treating these strictly as keys enables DB reference.
     *       For example, can support multi-genre by creating a new genre field that implements another Tokenizer.
     *
     *  - The method for comparing ranges of numbers has changed.
     *    This is suitable for 8.x.
     */

    @Test
    public void testSearchArtist() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals("SearchArtist",
                "+((artist:abc*) (artist:def*)) +(folder:" + PATH1 + ")",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals("SearchArtist", "+((artist:abc*) (artist:def*)) +(folder:" + PATH1
                + " folder:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testSearchAlbum() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals("SearchAlbum",
                "+((album:abc* artist:abc*) (album:def* artist:def*)) +(folder:" + PATH1
                        + ")",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals("SearchAlbum",
                "+((album:abc* artist:abc*) (album:def* artist:def*)) +(folder:" + PATH1
                        + " folder:" + PATH2 + ")",
                query.toString());
    }

    @Test
    public void testSearchSong() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.SONG);
        assertEquals("SearchSong",
                "+((title:abc* artist:abc*) (title:def* artist:def*)) +(folder:" + PATH1 + ")",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals("SearchSong", "+((title:abc* artist:abc*) (title:def* artist:def*)) +(folder:" + PATH1
                + " folder:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("SearchSong", "+((artist:abc*) (artist:def*)) +(folderId:"
                + FID1 + ")", query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("SearchSong",
                "+((artist:abc*) (artist:def*)) +(folderId:" + FID1
                        + " folderId:" + FID2 + ")",
                query.toString());
    }

    @Test
    public void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(
                "SearchAlbumId3", "+((album:abc* artist:abc*) (album:def* artist:def*)) "
                        + "+(folderId:" + FID1 + ")",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("SearchAlbumId3",
                "+((album:abc* artist:abc*) (album:def* artist:def*)) +(folderId:"
                        + FID1 + " folderId:"
                        + FID2 + ")",
                query.toString());
    }

    @Test
    public void testSearchByNameArtist() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_ENG_ONLY);
        assertEquals("SearchByNameArtist", "artist:abc artist:def*", query.toString());
    }

    @Test
    public void testSearchByNameAlbum() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_ENG_ONLY);
        assertEquals("SearchByNameAlbum", "album:abc album:def*", query.toString());
    }

    @Test
    public void testSearchByNameTitle() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.TITLE, QUERY_ENG_ONLY);
        assertEquals("SearchByNameTitle", "title:abc title:def*", query.toString());
    }

    @Test
    public void testGetRandomSongs() throws IOException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(50, "Classic Rock",
                1900, 2000, SINGLE_FOLDERS);

        Query query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:MUSIC +genre:Classic Rock +year:[1900 TO 2000] +(folder:" + PATH1 + ")",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", 1900,
                2000, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:MUSIC +genre:Classic Rock +year:[1900 TO 2000] +(folder:" + PATH1 + " folder:" + PATH2
                        + ")",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", null, null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:MUSIC +genre:Classic Rock +(folder:" + PATH1 + " folder:" + PATH2 + ")",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", 1900, null,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:MUSIC +genre:Classic Rock +year:[1900 TO 2147483647] +(folder:" + PATH1 + " folder:" + PATH2
                        + ")",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", null, 2000,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:MUSIC +genre:Classic Rock +year:[-2147483648 TO 2000] +(folder:" + PATH1 + " folder:" + PATH2
                        + ")",
                query.toString());
    }

    @Test
    public void testGetRandomAlbums() {
        Query query = queryFactory.getRandomAlbums(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS),
                "(folder:" + PATH1 + ")", query.toString());

        query = queryFactory.getRandomAlbums(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS),
                "(folder:" + PATH1 + " folder:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testGetRandomAlbumsId3() {
        Query query = queryFactory.getRandomAlbumsId3(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS),
                "(folderId:" + FID1 + ")", query.toString());

        query = queryFactory.getRandomAlbumsId3(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS),
                "(folderId:" + FID1 + " folderId:"
                        + FID2 + ")",
                query.toString());
    }

}
