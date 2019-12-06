
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;

/*
 * Test cases related to #1139.
 * Confirming whether shuffle search can be performed correctly in MusicFolder containing special strings.
 *
 * (Since the query of getRandomAlbums consists of folder paths only,
 * this verification is easy to perform.)
 *
 * This test case is a FalsePattern for search,
 * but there may be problems with the data flow prior to creating the search index.
 */
public class SearchServiceSpecialPathTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();

            File musicDir = new File(resolveBaseMediaPath.apply("Search/SpecialPath/accessible"));
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));

            File music2Dir = new File(resolveBaseMediaPath.apply("Search/SpecialPath/accessible's"));
            musicFolders.add(new MusicFolder(2, music2Dir, "accessible's", true, new Date()));

            File music3Dir = new File(resolveBaseMediaPath.apply("Search/SpecialPath/accessible+s"));
            musicFolders.add(new MusicFolder(3, music3Dir, "accessible+s", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testSpecialCharactersInDirName() {

        List<MusicFolder> folders = getMusicFolders();

        // ALL Songs
        List<MediaFile> randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folders);
        Assert.assertEquals("ALL Albums ", 3, randomAlbums.size());

        // dir - accessible
        List<MusicFolder> folder01 = folders.stream()
                .filter(m -> "accessible".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder01);
        Assert.assertEquals("Albums in \"accessible\" ", 1, randomAlbums.size());

        // dir - accessible's
        List<MusicFolder> folder02 = folders.stream()
                .filter(m -> "accessible's".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder02);
        Assert.assertEquals("Albums in \"accessible's\" ", 1, randomAlbums.size());

        // dir - accessible+s
        List<MusicFolder> folder03 = folders.stream()
                .filter(m -> "accessible+s".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder03);
        Assert.assertEquals("Albums in \"accessible+s\" ", 1, folder03.size());

    }

}
