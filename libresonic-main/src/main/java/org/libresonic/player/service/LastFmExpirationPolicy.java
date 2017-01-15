/**
 * This file is part of Libresonic.
 *
 *  Libresonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libresonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.libresonic.player.service;

import de.umass.lastfm.cache.ExpirationPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Artist and album info is cached permanently. Everything else is cached one year.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class LastFmExpirationPolicy implements ExpirationPolicy {

    private final static long ONE_YEAR = 12 * 30 * 24 * 3600 * 1000L;

    private final Map<String, Long> methodToExpirationTime = new LinkedHashMap<String, Long>() {{
        put("artist.getInfo", Long.MAX_VALUE);  // Cache forever
        put("album.getInfo", Long.MAX_VALUE);   // Cache forever
        put("album.search", -1L);               // Don't cache
    }};

    @Override
    public long getExpirationTime(String method, Map<String, String> params) {
        Long expirationTime = methodToExpirationTime.get(method);
        return expirationTime == null ? ONE_YEAR : expirationTime;
    }
}

