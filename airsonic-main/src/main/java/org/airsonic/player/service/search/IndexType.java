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

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum that symbolizes the each lucene index entity.
 * This class is a division of what was once part of SearchService and added functionality.
 * @since legacy
 */
public enum IndexType {

    /*
     * Boosts is a factor for search scores, which is 1 by default.
     */
    SONG(
        fieldNames(
            FieldNames.TITLE,
            FieldNames.ARTIST),
        boosts(
            entry(FieldNames.TITLE, 2F))),

    ALBUM(
        fieldNames(
            FieldNames.ALBUM,
            FieldNames.ARTIST,
            FieldNames.FOLDER),
        boosts(
            entry(FieldNames.ALBUM, 2F))),

    ALBUM_ID3(
        fieldNames(
            FieldNames.ALBUM,
            FieldNames.ARTIST,
            FieldNames.FOLDER_ID),
        boosts(
            entry(FieldNames.ALBUM, 2F))),

    ARTIST(
        fieldNames(
            FieldNames.ARTIST,
            FieldNames.FOLDER),
        boosts(
            entry(FieldNames.ARTIST, 1F))),

    ARTIST_ID3(
        fieldNames(
            FieldNames.ARTIST),
        boosts(
            entry(FieldNames.ARTIST, 2F))),

    ;

    @SafeVarargs
    private static final Map<String, Float> boosts(SimpleEntry<String, Float>... entry) {
        Map<String, Float> m = new HashMap<>();
        Arrays.stream(entry).forEach(kv -> m.put(kv.getKey(), kv.getValue()));
        return Collections.unmodifiableMap(m);
    }

    /*
     * The current state is implemented to set the same value as legacy.
     * However unlike legacy, it has been changed
     * so that different values ​​can be set for each field.
     * When setting two or more boost values,
     * it is desirable to differentiate the values.
     */
    private static final SimpleEntry<String, Float> entry(String k, float v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private static final String[] fieldNames(String... names) {
        return Arrays.stream(names).toArray(String[]::new);
    }

    private final Map<String, Float> boosts;

    private final String[] fields;

    private IndexType(String[] fieldNames, Map<String, Float> boosts) {
        this.fields = fieldNames;
        this.boosts = boosts;
    }

    /**
     * Returns a map of fields and boost values.
     * 
     * @return map of fields and boost values
     * @since legacy
     */
    /*
     * See the lucene documentation for boost specifications.
     */
    public Map<String, Float> getBoosts() {
        return boosts;
    }

    /**
     * Return some of the fields defined in the index.
     * 
     * @return Fields mainly used in multi-field search
     * @since legacy
     */
    /*
     * It maintains a fairly early implementation
     * and can be considered as an argument of MultiFieldQueryParser.
     * In fact, the fields and boosts used in the search are difficult topics
     * that can be determined by the search requirements.
     */
    public String[] getFields() {
        return fields;
    }

}
