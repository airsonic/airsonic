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

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public enum AlbumListType {

    RANDOM("random", "Random"),
    NEWEST("newest", "Recently Added"),
    STARRED("starred", "Starred"),
    HIGHEST("highest", "Top Rated"),
    FREQUENT("frequent", "Most Played"),
    RECENT("recent", "Recently Played"),
    DECADE("decade", "By Decade"),
    GENRE("genre", "By Genre"),
    ALPHABETICAL("alphabetical", "All");

    private final String id;
    private final String description;

    AlbumListType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static AlbumListType fromId(String id) {
        for (AlbumListType albumListType : values()) {
            if (albumListType.id.equals(id)) {
                return albumListType;
            }
        }
        return null;
    }
}
