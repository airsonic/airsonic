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

/**
 * Enumeration of player technologies.
 *
 * @author Sindre Mehus
 */
public enum PlayerTechnology {

    /**
     * Plays music directly in the web browser using the integrated Flash player.
     */
    WEB,

    /**
     * Plays music in an external player, such as WinAmp or Windows Media Player.
     */
    EXTERNAL,

    /**
     * Same as above, but the playlist is managed by the player, rather than the Airsonic server.
     * In this mode, skipping within songs is possible.
     */
    EXTERNAL_WITH_PLAYLIST,

    /**
     * Plays music directly on the audio device of the Airsonic server.
     */
    JUKEBOX,

    /**
     * Jukebox player that uses the Java Sound API.
     */
    JAVA_JUKEBOX

}