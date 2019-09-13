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

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.Objects;

/**
 * Contains media libaray statistics, including the number of artists, albums and songs.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/11/17 18:29:03 $
 */
public class MediaLibraryStatistics {

    @NotNull
    private Integer artistCount;
    @NotNull
    private Integer albumCount;
    @NotNull
    private Integer songCount;
    @NotNull
    private Long totalLengthInBytes;
    @NotNull
    private Long totalDurationInSeconds;
    @NotNull
    private Date scanDate;

    public MediaLibraryStatistics() {

    }

    public MediaLibraryStatistics(Date scanDate) {
        if (scanDate == null) {
            throw new IllegalArgumentException();
        }
        this.scanDate = scanDate;
        reset();
    }

    protected void reset() {
        artistCount = 0;
        albumCount = 0;
        songCount = 0;
        totalLengthInBytes = 0L;
        totalDurationInSeconds = 0L;
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

    public Integer getArtistCount() {
        return artistCount;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public Long getTotalLengthInBytes() {
        return totalLengthInBytes;
    }

    public Long getTotalDurationInSeconds() {
        return totalDurationInSeconds;
    }

    public Date getScanDate() {
        return scanDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaLibraryStatistics that = (MediaLibraryStatistics) o;
        return Objects.equals(artistCount, that.artistCount) &&
                Objects.equals(albumCount, that.albumCount) &&
                Objects.equals(songCount, that.songCount) &&
                Objects.equals(totalLengthInBytes, that.totalLengthInBytes) &&
                Objects.equals(totalDurationInSeconds, that.totalDurationInSeconds) &&
                Objects.equals(scanDate, that.scanDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistCount, albumCount, songCount, totalLengthInBytes, totalDurationInSeconds, scanDate);
    }
}
