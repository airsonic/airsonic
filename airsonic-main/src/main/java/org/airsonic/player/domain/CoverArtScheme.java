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
 * Enumeration of cover art schemes. Each value contains a size, which indicates how big the
 * scaled covert art images should be.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.3 $ $Date: 2005/06/15 18:10:40 $
 */
public enum CoverArtScheme {

    OFF(0),
    SMALL(110),
    MEDIUM(160),
    LARGE(300);

    private int size;

    CoverArtScheme(int size) {
        this.size = size;
    }

    /**
     * Returns the covert art size for this scheme.
     *
     * @return the covert art size for this scheme.
     */
    public int getSize() {
        return size;
    }

}
