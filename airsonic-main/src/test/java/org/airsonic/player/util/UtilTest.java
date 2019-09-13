package org.airsonic.player.util;

import org.airsonic.player.domain.MediaLibraryStatistics;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void objectToStringMapNull() {
        MediaLibraryStatistics statistics = null;
        Map<String, String> stringStringMap = Util.objectToStringMap(statistics);
        assertNull(stringStringMap);
    }

    @Test
    public void objectToStringMap() {
        Date date = new Date(1568350960725L);
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(date);
        statistics.incrementAlbums(5);
        statistics.incrementSongs(4);
        statistics.incrementArtists(910823);
        statistics.incrementTotalDurationInSeconds(30);
        statistics.incrementTotalLengthInBytes(2930491082L);
        Map<String, String> stringStringMap = Util.objectToStringMap(statistics);
        assertEquals("5", stringStringMap.get("albumCount"));
        assertEquals("4", stringStringMap.get("songCount"));
        assertEquals("910823", stringStringMap.get("artistCount"));
        assertEquals("30", stringStringMap.get("totalDurationInSeconds"));
        assertEquals("2930491082", stringStringMap.get("totalLengthInBytes"));
        assertEquals("1568350960725", stringStringMap.get("scanDate"));
    }

    @Test
    public void stringMapToObject() {
        Map<String, String> stringStringMap = new HashMap<>();
        stringStringMap.put("albumCount", "5");
        stringStringMap.put("songCount", "4");
        stringStringMap.put("artistCount", "910823");
        stringStringMap.put("totalDurationInSeconds", "30");
        stringStringMap.put("totalLengthInBytes", "2930491082");
        stringStringMap.put("scanDate", "1568350960725");
        MediaLibraryStatistics statistics = Util.stringMapToObject(MediaLibraryStatistics.class, stringStringMap);
        assertEquals(new Integer(5), statistics.getAlbumCount());
        assertEquals(new Integer(4), statistics.getSongCount());
        assertEquals(new Integer(910823), statistics.getArtistCount());
        assertEquals(new Long(30L), statistics.getTotalDurationInSeconds());
        assertEquals(new Long(2930491082L), statistics.getTotalLengthInBytes());
        assertEquals(new Date(1568350960725L), statistics.getScanDate());
    }

    @Test
    public void stringMapToObjectWithExtraneousData() {
        Map<String, String> stringStringMap = new HashMap<>();
        stringStringMap.put("albumCount", "5");
        stringStringMap.put("songCount", "4");
        stringStringMap.put("artistCount", "910823");
        stringStringMap.put("totalDurationInSeconds", "30");
        stringStringMap.put("totalLengthInBytes", "2930491082");
        stringStringMap.put("scanDate", "1568350960725");
        stringStringMap.put("extraneousData", "nothingHereToLookAt");
        MediaLibraryStatistics statistics = Util.stringMapToObject(MediaLibraryStatistics.class, stringStringMap);
        assertEquals(new Integer(5), statistics.getAlbumCount());
        assertEquals(new Integer(4), statistics.getSongCount());
        assertEquals(new Integer(910823), statistics.getArtistCount());
        assertEquals(new Long(30L), statistics.getTotalDurationInSeconds());
        assertEquals(new Long(2930491082L), statistics.getTotalLengthInBytes());
        assertEquals(new Date(1568350960725L), statistics.getScanDate());
    }

    public void stringMapToObjectWithNoData() {
        Map<String, String> stringStringMap = new HashMap<>();
        MediaLibraryStatistics statistics = Util.stringMapToObject(MediaLibraryStatistics.class, stringStringMap);
        assertNotNull(statistics);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringMapToValidObjectWithNoData() {
        Map<String, String> stringStringMap = new HashMap<>();
        MediaLibraryStatistics statistics = Util.stringMapToValidObject(MediaLibraryStatistics.class, stringStringMap);
    }

}