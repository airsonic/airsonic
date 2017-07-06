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
package org.airsonic.player.ajax;

/**
 * Contains lyrics info for a song.
 *
 * @author Sindre Mehus
 */
public class LyricsInfo {

    private final String lyrics;
    private final String artist;
    private final String title;
    private boolean tryLater;

    public LyricsInfo() {
        this(null, null, null);
    }

    public LyricsInfo(String lyrics, String artist, String title) {
        this.lyrics = lyrics;
        this.artist = artist;
        this.title = title;
    }

    public String getLyrics() {
        return lyrics;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTryLater(boolean tryLater) {
        this.tryLater = tryLater;
    }

    public boolean isTryLater() {
        return tryLater;
    }
}
