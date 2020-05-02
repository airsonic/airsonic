
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
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
 * Test cases related to #1142.
 * The filter is not properly applied when analyzing the query,
 *
 * In the process of hardening the Analyzer implementation,
 * this problem is solved side by side.
 */
public class SearchServiceStartWithStopwardsTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath.apply("Search/StartWithStopwards"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testStartWithStopwards() {

        List<MusicFolder> folders = getMusicFolders();

        final SearchCriteria criteria = new SearchCriteria();
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setOffset(0);

        criteria.setQuery("will");
        SearchResult result = searchService.search(criteria, folders, IndexType.ARTIST_ID3);
        // Will hit because Airsonic's stopword is defined(#1235)
        Assert.assertEquals("Williams hit by \"will\" ", 1, result.getTotalHits());

        criteria.setQuery("the");
        result = searchService.search(criteria, folders, IndexType.SONG);
        // XXX 3.x -> 8.x : The filter is properly applied to the input(Stopward)
        Assert.assertEquals("Theater hit by \"the\" ", 0, result.getTotalHits());

        criteria.setQuery("willi");
        result = searchService.search(criteria, folders, IndexType.ARTIST_ID3);
        // XXX 3.x -> 8.x : Normal forward matching
        Assert.assertEquals("Williams hit by \"Williams\" ", 1, result.getTotalHits());

        criteria.setQuery("thea");
        result = searchService.search(criteria, folders, IndexType.SONG);
        // XXX 3.x -> 8.x : Normal forward matching
        Assert.assertEquals("Theater hit by \"thea\" ", 1, result.getTotalHits());

    }

}
