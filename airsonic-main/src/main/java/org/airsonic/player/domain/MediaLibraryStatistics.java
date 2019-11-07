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

import java.time.Instant;
import java.util.Objects;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains media libaray statistics, including the number of artists, albums and songs.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/11/17 18:29:03 $
 */
public class MediaLibraryStatistics {
    private Instant scanDate = Instant.now();
    private AtomicInteger artistCount = new AtomicInteger(0);
    private AtomicInteger albumCount = new AtomicInteger(0);
    private AtomicInteger songCount = new AtomicInteger(0);
    private AtomicLong totalLengthInBytes = new AtomicLong(0);
    private AtomicLong totalDurationInSeconds = new AtomicLong(0);

    public MediaLibraryStatistics() {}
    
    public void setScanDate(Instant scanDate) {
        this.scanDate = scanDate;
    }

    public void setArtistCount(int artistCount) {
        this.artistCount.set(artistCount);
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount.set(albumCount);
    }

    public void setSongCount(int songCount) {
        this.songCount.set(songCount);
    }

    public void setTotalLengthInBytes(long totalLengthInBytes) {
        this.totalLengthInBytes.set(totalLengthInBytes);
    }

    public void setTotalDurationInSeconds(long totalDurationInSeconds) {
        this.totalDurationInSeconds.set(totalDurationInSeconds);
    }
    
    public void reset() {
        artistCount.set(0);
        albumCount.set(0);
        songCount.set(0);
        totalLengthInBytes.set(0);
        totalDurationInSeconds.set(0);
        scanDate = Instant.now();
    }

    public void incrementArtists(int n) {
        artistCount.addAndGet(n);
    }

    public void incrementAlbums(int n) {
        albumCount.addAndGet(n);
    }

    public void incrementSongs(int n) {
        songCount.addAndGet(n);
    }

    public void incrementTotalLengthInBytes(long n) {
        totalLengthInBytes.addAndGet(n);
    }

    public void incrementTotalDurationInSeconds(long n) {
        totalDurationInSeconds.addAndGet(n);
    }

    public int getArtistCount() {
        return artistCount.get();
    }

    public int getAlbumCount() {
        return albumCount.get();
    }

    public int getSongCount() {
        return songCount.get();
    }

    public long getTotalLengthInBytes() {
        return totalLengthInBytes.get();
    }

    public long getTotalDurationInSeconds() {
        return totalDurationInSeconds.get();
    }

    public Instant getScanDate() {
        return scanDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaLibraryStatistics that = (MediaLibraryStatistics) o;
        return Objects.equals(artistCount.get(), that.artistCount.get()) &&
                Objects.equals(albumCount.get(), that.albumCount.get()) &&
                Objects.equals(songCount.get(), that.songCount.get()) &&
                Objects.equals(totalLengthInBytes.get(), that.totalLengthInBytes.get()) &&
                Objects.equals(totalDurationInSeconds.get(), that.totalDurationInSeconds.get()) &&
                Objects.equals(scanDate, that.scanDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistCount.get(), albumCount.get(), songCount.get(), totalLengthInBytes.get(), totalDurationInSeconds.get(), scanDate);
    }
}
