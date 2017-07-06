/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

import java.util.Date;

/**
 * Represents the playback of a track, possibly remote (e.g., a cached song on a mobile phone).
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayStatus {

    private final MediaFile mediaFile;
    private final Player player;
    private final Date time;

    private final static long TTL_MILLIS = 6L * 60L * 60L * 1000L; // 6 hours

    public PlayStatus(MediaFile mediaFile, Player player, Date time) {
        this.mediaFile = mediaFile;
        this.player = player;
        this.time = time;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public Player getPlayer() {
        return player;
    }

    public Date getTime() {
        return time;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > time.getTime() + TTL_MILLIS;
    }

    public long getMinutesAgo() {
        return (System.currentTimeMillis() - time.getTime()) / 1000L / 60L;
    }
}
