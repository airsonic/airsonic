/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.domain;

import org.airsonic.player.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains media libaray statistics, including the number of artists, albums and songs.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/11/17 18:29:03 $
 */
public class MediaLibraryStatistics {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLibraryStatistics.class);

    private int artistCount;
    private int albumCount;
    private int songCount;
    private long totalLengthInBytes;
    private long totalDurationInSeconds;

    public MediaLibraryStatistics(int artistCount, int albumCount, int songCount, long totalLengthInBytes, long totalDurationInSeconds) {
        this.artistCount = artistCount;
        this.albumCount = albumCount;
        this.songCount = songCount;
        this.totalLengthInBytes = totalLengthInBytes;
        this.totalDurationInSeconds = totalDurationInSeconds;
    }

    public MediaLibraryStatistics() {
    }

    public void reset() {
        artistCount = 0;
        albumCount = 0;
        songCount = 0;
        totalLengthInBytes = 0;
        totalDurationInSeconds = 0;
    }

    public void incrementArtists(int n) {
        artistCount += n;
    }

    public void incrementAlbums(int n) {
        albumCount += n;
    }

    public void incrementSongs(int n) {
        songCount += n;
    }

    public void incrementTotalLengthInBytes(long n) {
        totalLengthInBytes += n;
    }

    public void incrementTotalDurationInSeconds(long n) {
        totalDurationInSeconds += n;
    }

    public int getArtistCount() {
        return artistCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public int getSongCount() {
        return songCount;
    }

    public long getTotalLengthInBytes() {
        return totalLengthInBytes;
    }

    public long getTotalDurationInSeconds() {
        return totalDurationInSeconds;
    }

    public String format() {
        return artistCount + " " + albumCount + " " + songCount + " " + totalLengthInBytes + " " + totalDurationInSeconds;
    }

    public static MediaLibraryStatistics parse(String s) {
        try {
            String[] strings = StringUtil.split(s);
            return new MediaLibraryStatistics(
                    Integer.parseInt(strings[0]),
                    Integer.parseInt(strings[1]),
                    Integer.parseInt(strings[2]),
                    Long.parseLong(strings[3]),
                    Long.parseLong(strings[4]));
        } catch (Exception e) {
            LOG.warn("Failed to parse media library statistics: " + s);
            return new MediaLibraryStatistics();
        }
    }
}
