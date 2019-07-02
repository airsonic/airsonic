
package org.airsonic.player.service.search;

import static org.junit.Assert.assertEquals;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;
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
            new MusicFolder(Integer.valueOf(FID1), new File(PATH1), "music1", true, new java.util.Date());
    private static final MusicFolder MUSIC_FOLDER2 = 
            new MusicFolder(Integer.valueOf(FID2), new File(PATH2), "music2", true, new java.util.Date());

    private static final List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    private static final List<MusicFolder> MULTI_FOLDERS  = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    @Test
    public void testSearchArtist() throws ParseException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals("SearchArtist",
                "+((artist:abc* folder:abc*) (artist:def* folder:def*)) +spanOr([folder:" + PATH1 + "])",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals("SearchArtist", "+((artist:abc* folder:abc*) (artist:def* folder:def*)) +spanOr([folder:" + PATH1
                + ", folder:" + PATH2 + "])", query.toString());
    }

    @Test
    public void testSearchAlbum() throws ParseException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals("SearchAlbum",
                "+((album:abc* artist:abc* folder:abc*) (album:def* artist:def* folder:def*)) +spanOr([folder:" + PATH1
                        + "])",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals("SearchAlbum",
                "+((album:abc* artist:abc* folder:abc*) (album:def* artist:def* folder:def*)) +spanOr([folder:" + PATH1
                        + ", folder:" + PATH2 + "])",
                query.toString());
    }

    @Test
    public void testSearchSong() throws ParseException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.SONG);
        assertEquals("SearchSong",
                "+((title:abc* artist:abc*) (title:def* artist:def*)) +spanOr([folder:" + PATH1 + "])",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals("SearchSong", "+((title:abc* artist:abc*) (title:def* artist:def*)) +spanOr([folder:" + PATH1
                + ", folder:" + PATH2 + "])", query.toString());
    }

    @Test
    public void testSearchArtistId3() throws ParseException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("SearchSong", "+((artist:abc*) (artist:def*)) +spanOr([folderId:"
                + NumericUtils.intToPrefixCoded(FID1) + "])", query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("SearchSong",
                "+((artist:abc*) (artist:def*)) +spanOr([folderId:" + NumericUtils.intToPrefixCoded(FID1)
                        + ", folderId:" + NumericUtils.intToPrefixCoded(FID2) + "])",
                query.toString());
    }

    @Test
    public void testSearchAlbumId3() throws ParseException, IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_ENG_ONLY);

        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(
                "SearchAlbumId3", "+((album:abc* artist:abc* folderId:abc*) (album:def* artist:def* folderId:def*)) "
                        + "+spanOr([folderId:" + NumericUtils.intToPrefixCoded(FID1) + "])",
                query.toString());

        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("SearchAlbumId3",
                "+((album:abc* artist:abc* folderId:abc*) (album:def* artist:def* folderId:def*)) +spanOr([folderId:"
                        + NumericUtils.intToPrefixCoded(FID1) + ", folderId:"
                        + NumericUtils.intToPrefixCoded(FID2) + "])",
                query.toString());
    }

    @Test
    public void testSearchByNameArtist() throws ParseException {
        Query query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_ENG_ONLY);
        assertEquals("SearchByNameArtist", "artist:abc artist:def*", query.toString());
    }

    @Test
    public void testSearchByNameAlbum() throws ParseException {
        Query query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_ENG_ONLY);
        assertEquals("SearchByNameAlbum", "album:abc album:def*", query.toString());
    }

    @Test
    public void testSearchByNameTitle() throws ParseException {
        Query query = queryFactory.searchByName(FieldNames.TITLE, QUERY_ENG_ONLY);
        assertEquals("SearchByNameTitle", "title:abc title:def*", query.toString());
    }

    @Test
    public void testGetRandomSongs() {
        RandomSearchCriteria criteria = new RandomSearchCriteria(50, "Classic Rock",
                Integer.valueOf(1900), Integer.valueOf(2000), SINGLE_FOLDERS);

        Query query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:music +genre:classicrock +year:[1900 TO 2000] +spanOr([folder:" + PATH1 + "])",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", Integer.valueOf(1900),
                Integer.valueOf(2000), MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:music +genre:classicrock +year:[1900 TO 2000] +spanOr([folder:" + PATH1 + ", folder:" + PATH2
                        + "])",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", null, null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:music +genre:classicrock +spanOr([folder:" + PATH1 + ", folder:" + PATH2 + "])",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", Integer.valueOf(1900), null,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:music +genre:classicrock +year:[1900 TO *] +spanOr([folder:" + PATH1 + ", folder:" + PATH2
                        + "])",
                query.toString());

        criteria = new RandomSearchCriteria(50, "Classic Rock", null, Integer.valueOf(2000),
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria),
                "+mediaType:music +genre:classicrock +year:[* TO 2000] +spanOr([folder:" + PATH1 + ", folder:" + PATH2
                        + "])",
                query.toString());
    }

    @Test
    public void testGetRandomAlbums() {
        Query query = queryFactory.getRandomAlbums(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS),
                "spanOr([folder:" + PATH1 + "])", query.toString());

        query = queryFactory.getRandomAlbums(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS),
                "spanOr([folder:" + PATH1 + ", folder:" + PATH2 + "])", query.toString());
    }

    @Test
    public void testGetRandomAlbumsId3() {
        Query query = queryFactory.getRandomAlbumsId3(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS),
                "spanOr([folderId:" + NumericUtils.intToPrefixCoded(FID1) + "])", query.toString());

        query = queryFactory.getRandomAlbumsId3(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS),
                "spanOr([folderId:" + NumericUtils.intToPrefixCoded(FID1) + ", folderId:"
                        + NumericUtils.intToPrefixCoded(FID2) + "])",
                query.toString());
    }

}
