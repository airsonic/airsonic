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
package org.airsonic.player.util;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class HttpRange {

    private static final Pattern PATTERN = Pattern.compile("bytes=(\\d+)-(\\d*)");
    private final Long firstBytePos;
    private final Long lastBytePos;

    /**
     * Parses the given string as a HTTP header byte range.  See chapter 14.36.1 in RFC 2068
     * for details.
     * <p/>
     * Only a subset of the allowed syntaxes are supported. Only ranges which specify first-byte-pos
     * are supported. The last-byte-pos is optional.
     *
     * @param range The range from the HTTP header, for instance "bytes=0-499" or "bytes=500-"
     * @return A range object (using inclusive values). If the last-byte-pos is not given, the end of
     *         the returned range is {@code null}. The method returns <code>null</code> if the syntax
     *         of the given range is not supported.
     */
    public static HttpRange valueOf(String range) {
        if (range == null) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(range);
        if (matcher.matches()) {
            String firstString = matcher.group(1);
            String lastString = StringUtils.trimToNull(matcher.group(2));

            long first = Long.parseLong(firstString);
            Long last = lastString == null ? null : Long.parseLong(lastString);

            if (last != null && first > last) {
                return null;
            }
            return new HttpRange(first, last);
        }
        return null;
    }

    public HttpRange(long firstBytePos, Long lastBytePos) {
        this.firstBytePos = firstBytePos;
        this.lastBytePos = lastBytePos;
    }

    /**
     * @return The first byte position (inclusive) in the range. Never {@code null}.
     */
    public Long getFirstBytePos() {
        return firstBytePos;
    }

    /**
     * @return The last byte position (inclusive) in the range. Can be {@code null}.
     */
    public Long getLastBytePos() {
        return lastBytePos;
    }

    /**
     * @return Whether this is a closed range (both first and last byte position specified).
     */
    public boolean isClosed() {
        return firstBytePos != null && lastBytePos != null;
    }

    /**
     * @return The size in bytes if the range is closed, -1 otherwise.
     */
    public long size() {
        return isClosed() ? (lastBytePos - firstBytePos + 1) : -1;
    }

    /**
     * @return Returns whether the given byte position is within this range.
     */
    public boolean contains(long pos) {
        if (pos < firstBytePos) {
            return false;
        }
        return lastBytePos == null || pos <= lastBytePos;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(firstBytePos).append('-');
        if (lastBytePos != null) {
            builder.append(lastBytePos);
        }
        return builder.toString();
    }
}
