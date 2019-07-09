
package org.airsonic.player.service.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void setup() throws Exception {
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
        Assert.assertEquals("Williams hit by \"will\" ", 1, result.getTotalHits());

        criteria.setQuery("the");
        result = searchService.search(criteria, folders, IndexType.SONG);
        Assert.assertEquals("Theater hit by \"the\" ", 1, result.getTotalHits());

    }

}
