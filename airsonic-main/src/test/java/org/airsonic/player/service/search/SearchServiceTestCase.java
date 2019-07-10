
package org.airsonic.player.service.search;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.IndexType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.subsonic.restapi.ArtistID3;

public class SearchServiceTestCase extends AbstractAirsonicHomeTest {

    @Autowired
    private AlbumDao albumDao;

    private final MetricRegistry metrics = new MetricRegistry();

    @Autowired
    private MusicFolderDao musicFolderDao;

    @Autowired
    private SearchService searchService;

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testSearchTypical() {

        /*
         * A simple test that is expected to easily detect API syntax differences when updating lucene.
         * Complete route coverage and data coverage in this case alone are not conscious.
         */

        List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();
        Assert.assertEquals(3, allMusicFolders.size());
    
        // *** testSearch() ***

        String query = "Sarah Walker";
        final SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setQuery(query);
        searchCriteria.setCount(Integer.MAX_VALUE);
        searchCriteria.setOffset(0);

        /*
         * _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble
         * Should find any version of Lucene.
         */
        SearchResult result = searchService.search(searchCriteria, allMusicFolders,
                IndexType.ALBUM);
        Assert.assertEquals("(0) Specify '" + query + "' as query, total Hits is", 1,
                result.getTotalHits());
        Assert.assertEquals("(1) Specify artist '" + query + "' as query. Artist SIZE is", 0,
                result.getArtists().size());
        Assert.assertEquals("(2) Specify artist '" + query + "' as query. Album SIZE is", 0,
                result.getAlbums().size());
        Assert.assertEquals("(3) Specify artist '" + query + "' as query, MediaFile SIZE is", 1,
                result.getMediaFiles().size());
        Assert.assertEquals("(4) ", MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType());
        Assert.assertEquals(
                "(5) Specify artist '" + query + "' as query, and get a album. Name is ",
                "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble",
                result.getMediaFiles().get(0).getArtist());
        Assert.assertEquals(
                "(6) Specify artist '" + query + "' as query, and get a album. Name is ",
                "_ID3_ALBUM_ Ravel - Chamber Music With Voice",
                result.getMediaFiles().get(0).getAlbumName());

        /*
         * _ID3_ALBUM_ Ravel - Chamber Music With Voice
         * Should find any version of Lucene.
         */
        query = "music";
        searchCriteria.setQuery(query);
        result = searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM_ID3);
        Assert.assertEquals("Specify '" + query + "' as query, total Hits is", 1,
                result.getTotalHits());
        Assert.assertEquals("(7) Specify '" + query + "' as query, and get a song. Artist SIZE is ",
                0, result.getArtists().size());
        Assert.assertEquals("(8) Specify '" + query + "' as query, and get a song. Album SIZE is ",
                1, result.getAlbums().size());
        Assert.assertEquals(
                "(9) Specify '" + query + "' as query, and get a song. MediaFile SIZE is ", 0,
                result.getMediaFiles().size());
        Assert.assertEquals("(9) Specify '" + query + "' as query, and get a album. Name is ",
                "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble",
                result.getAlbums().get(0).getArtist());
        Assert.assertEquals("(10) Specify '" + query + "' as query, and get a album. Name is ",
                "_ID3_ALBUM_ Ravel - Chamber Music With Voice",
                result.getAlbums().get(0).getName());

        /*
         * _ID3_ALBUM_ Ravel - Chamber Music With Voice
         * Should find any version of Lucene.
         */
        query = "Ravel - Chamber Music";
        searchCriteria.setQuery(query);
        result = searchService.search(searchCriteria, allMusicFolders, IndexType.SONG);
        Assert.assertEquals("(11) Specify album '" + query + "' as query, total Hits is", 2,
                result.getTotalHits());
        Assert.assertEquals("(12) Specify album '" + query + "', and get a song. Artist SIZE is", 0,
                result.getArtists().size());
        Assert.assertEquals("(13) Specify album '" + query + "', and get a song. Album SIZE is", 0,
                result.getAlbums().size());
        Assert.assertEquals("(14) Specify album '" + query + "', and get a song. MediaFile SIZE is",
                2, result.getMediaFiles().size());
        Assert.assertEquals("(15) Specify album '" + query + "', and get songs. The first song is ",
                "01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(0).getTitle());
        Assert.assertEquals(
                "(16) Specify album '" + query + "', and get songs. The second song is ",
                "02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getTitle());

        // *** testSearchByName() ***

        /*
         * _ID3_ALBUM_ Sackcloth 'n' Ashes
         * Should be 1 in Lucene 3.0(Because Single quate is not a delimiter).
         */
        query = "Sackcloth 'n' Ashes";
        ParamSearchResult<Album> albumResult = searchService.searchByName(query, 0,
                Integer.MAX_VALUE, allMusicFolders, Album.class);
        Assert.assertEquals(
                "(17) Specify album name '" + query + "' as the name, and get an album.", 1,
                albumResult.getItems().size());
        Assert.assertEquals("(18) Specify '" + query + "' as the name, The album name is ",
                "_ID3_ALBUM_ Sackcloth 'n' Ashes", albumResult.getItems().get(0).getName());
        Assert.assertEquals(
                "(19) Whether the acquired album contains data of the specified album name", 1L,
                albumResult.getItems().stream()
                        .filter(r -> "_ID3_ALBUM_ Sackcloth \'n\' Ashes".equals(r.getName()))
                        .count());

        /*
         * Should be 0 in Lucene 3.0(Since the slash is not a delimiter).
         */
        query = "lker/Nash";
        ParamSearchResult<ArtistID3> artistId3Result = searchService.searchByName(query, 0,
                Integer.MAX_VALUE, allMusicFolders, ArtistID3.class);
        Assert.assertEquals("(20) Specify '" + query + "' as the name, and get an artist.", 0,
                artistId3Result.getItems().size());
        ParamSearchResult<Artist> artistResult = searchService.searchByName(query, 0,
                Integer.MAX_VALUE, allMusicFolders, Artist.class);
        Assert.assertEquals("(21) Specify '" + query + "' as the name, and get an artist.", 0,
                artistResult.getItems().size());

        // *** testGetRandomSongs() ***

        /*
         * Regardless of the Lucene version,
         * RandomSearchCriteria can specify null and means the maximum range.
         * 11 should be obtainable.
         */
        RandomSearchCriteria randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                null, // genre,
                null, // fromYear
                null, // toYear
                allMusicFolders // musicFolders
        );
        List<MediaFile> allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
        Assert.assertEquals(
                "(22) Specify MAX_VALUE as the upper limit, and randomly acquire songs.", 11,
                allRandomSongs.size());

        /*
         * Regardless of the Lucene version,
         * 7 should be obtainable.
         */
        randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                null, // genre,
                1900, // fromYear
                null, // toYear
                allMusicFolders // musicFolders
        );
        allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
        Assert.assertEquals("(23) Specify 1900 as 'fromYear', and randomly acquire songs.", 7,
                allRandomSongs.size());

        /*
         * Regardless of the Lucene version,
         * It should be 0 because it is a non-existent genre.
         */
        randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "Chamber Music", // genre,
                null, // fromYear
                null, // toYear
                allMusicFolders // musicFolders
        );
        allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
        Assert.assertEquals("(24) Specify music as 'genre', and randomly acquire songs.", 0,
                allRandomSongs.size());

        /*
         * Genre including blank.
         * Regardless of the Lucene version, It should be 2.
         */
        randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                "Baroque Instrumental", // genre,
                null, // fromYear
                null, // toYear
                allMusicFolders // musicFolders
        );
        allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
        Assert.assertEquals("(25) Search by specifying genres including spaces and hyphens.", 2,
                allRandomSongs.size());

        // *** testGetRandomAlbums() ***

        /*
         * Acquisition of maximum number(5).
         */
        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, 0, true, true, allMusicFolders);
        Assert.assertEquals("(26) Get all albums with Dao.", 5, allAlbums.size());
        List<MediaFile> allRandomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE,
                allMusicFolders);
        Assert.assertEquals("(27) Specify Integer.MAX_VALUE as the upper limit,"
                + "and randomly acquire albums(file struct).", 5, allRandomAlbums.size());

        /*
         * Acquisition of maximum number(5).
         */
        List<Album> allRandomAlbumsId3 = searchService.getRandomAlbumsId3(Integer.MAX_VALUE,
                allMusicFolders);
        Assert.assertEquals(
                "(28) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire albums(ID3).",
                5, allRandomAlbumsId3.size());

        /*
         * Total is 4.
         */
        query = "ID 3 ARTIST";
        searchCriteria.setQuery(query);
        result = searchService.search(searchCriteria, allMusicFolders, IndexType.ARTIST_ID3);
        Assert.assertEquals("(29) Specify '" + query + "', total Hits is", 4,
                result.getTotalHits());
        Assert.assertEquals("(30) Specify '" + query + "', and get an artists. Artist SIZE is ", 4,
                result.getArtists().size());
        Assert.assertEquals("(31) Specify '" + query + "', and get a artists. Album SIZE is ", 0,
                result.getAlbums().size());
        Assert.assertEquals("(32) Specify '" + query + "', and get a artists. MediaFile SIZE is ",
                0, result.getMediaFiles().size());

        /*
         * Three hits to the artist.
         * ALBUMARTIST is not registered with these.
         * Therefore, the registered value of ARTIST is substituted in ALBUMARTIST.
         */
        long count = result.getArtists().stream()
                .filter(a -> a.getName().startsWith("_ID3_ARTIST_")).count();
        Assert.assertEquals("(33) Artist whose name contains \\\"_ID3_ARTIST_\\\" is 3 records.",
                3L, count);

        /*
         * The structure of "01 - Sonata Violin & Cello I. Allegro.ogg"
         * ARTIST -> _ID3_ARTIST_ Sarah Walker/Nash Ensemble
         * ALBUMARTIST -> _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble
         * (The result must not contain duplicates. And ALBUMARTIST must be returned correctly.)
         */
        count = result.getArtists().stream()
                .filter(a -> a.getName().startsWith("_ID3_ALBUMARTIST_")).count();
        Assert.assertEquals("(34) Artist whose name is \"_ID3_ARTIST_\" is 1 records.", 1L, count);

        /*
         * Below is a simple loop test.
         * How long is the total time?
         */
        int countForEachMethod = 500;
        String[] randomWords4Search = createRandomWords(countForEachMethod);
        String[] randomWords4SearchByName = createRandomWords(countForEachMethod);

        Timer globalTimer = metrics
                .timer(MetricRegistry.name(SearchServiceTestCase.class, "Timer.global"));
        final Timer.Context globalTimerContext = globalTimer.time();

        System.out.println("--- Random search (" + countForEachMethod * 5 + " times) ---");

        // testSearch()
        Arrays.stream(randomWords4Search).forEach(w -> {
            searchCriteria.setQuery(w);
            searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM);
        });

        // testSearchByName()
        Arrays.stream(randomWords4SearchByName).forEach(w -> {
            searchService.searchByName(w, 0, Integer.MAX_VALUE, allMusicFolders, Artist.class);
        });

        // testGetRandomSongs()
        RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                null, // genre,
                null, // fromYear
                null, // toYear
                allMusicFolders // musicFolders
        );
        for (int i = 0; i < countForEachMethod; i++) {
            searchService.getRandomSongs(criteria);
        }

        // testGetRandomAlbums()
        for (int i = 0; i < countForEachMethod; i++) {
            searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
        }

        // testGetRandomAlbumsId3()
        for (int i = 0; i < countForEachMethod; i++) {
            searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
        }

        globalTimerContext.stop();

        /*
         * Whether or not IndexReader is exhausted.
         */
        query = "Sarah Walker";
        searchCriteria.setQuery(query);
        result = searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM);
        Assert.assertEquals("(35) Can the normal case be implemented.", 0,
                result.getArtists().size());
        Assert.assertEquals("(36) Can the normal case be implemented.", 0,
                result.getAlbums().size());
        Assert.assertEquals("(37) Can the normal case be implemented.", 1,
                result.getMediaFiles().size());
        Assert.assertEquals("(38) Can the normal case be implemented.", MediaType.ALBUM,
                result.getMediaFiles().get(0).getMediaType());
        Assert.assertEquals("(39) Can the normal case be implemented.",
                "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble",
                result.getMediaFiles().get(0).getArtist());

        System.out.println("--- SUCCESS ---");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        reporter.report();

        System.out.println("End. ");
    }

    private static String[] createRandomWords(int count) {
        String[] randomStrings = new String[count];
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            char[] word = new char[random.nextInt(8) + 3];
            for (int j = 0; j < word.length; j++) {
                word[j] = (char) ('a' + random.nextInt(26));
            }
            randomStrings[i] = new String(word);
        }
        return randomStrings;
    }

}
