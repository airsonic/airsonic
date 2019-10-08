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

package org.airsonic.player.service.search;

/**
 * Enum that symbolizes the field name used for lucene index.
 * This class is a division of what was once part of SearchService and added functionality.
 */
class FieldNames {

    private FieldNames() {
    }

    /**
     * A field same to a legacy server, id field.
     * 
     * @since legacy
     **/
    public static final String ID = "id";

    /**
     * A field same to a legacy server, id field.
     * 
     * @since legacy
     **/
    public static final String FOLDER_ID = "folderId";

    /**
     * A field same to a legacy server, numeric field.
     * 
     * @since legacy
     **/
    public static final String YEAR = "year";

    /**
     * A field same to a legacy server, key field.
     * 
     * @since legacy
     **/
    public static final String GENRE = "genre";

    /**
     * A field same to a legacy server, key field.
     * 
     * @since legacy
     **/
    public static final String MEDIA_TYPE = "mediaType";

    /**
     * A field same to a legacy server, key field.
     * 
     * @since legacy
     **/
    public static final String FOLDER = "folder";

    /**
     * A field same to a legacy server, usually with common word parsing.
     * 
     * @since legacy
     **/
    public static final String ARTIST = "artist";

    /**
     * A field same to a legacy server, usually with common word parsing.
     * 
     * @since legacy
     **/
    public static final String ALBUM = "album";

    /**
     * A field same to a legacy server, usually with common word parsing.
     * 
     * @since legacy
     **/
    public static final String TITLE = "title";

}
