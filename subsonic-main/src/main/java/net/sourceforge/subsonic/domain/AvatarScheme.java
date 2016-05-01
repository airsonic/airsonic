/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.domain;

/**
 * Enumeration of avatar schemes.
 *
 * @author Sindre Mehus
 */
public enum AvatarScheme {

    /**
     * No avatar should be displayed.
     */
    NONE(-1),

    /**
     * One of the system avatars should be displayed.
     */
    SYSTEM(0),

    /**
     * The custom avatar should be displayed.
     */
    CUSTOM(-2);

    private final int code;

    AvatarScheme(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}