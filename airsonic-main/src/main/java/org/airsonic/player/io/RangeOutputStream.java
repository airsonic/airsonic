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
package org.airsonic.player.io;

import org.airsonic.player.util.HttpRange;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Special output stream for grabbing only part of a passed stream.
 *
 * @author Sindre Mehus
 */
public class RangeOutputStream extends FilterOutputStream {

    private final HttpRange range;

    /**
     * The current position.
     */
    protected long pos;

    public RangeOutputStream(OutputStream out, HttpRange range) {
        super(out);
        this.range = range;
    }

    /**
     * Wraps the given output stream in a RangeOutputStream, using the values
     * in the given range, unless the range is <code>null</code> in which case
     * the original OutputStream is returned.
     *
     * @param out   The output stream to wrap in a RangeOutputStream.
     * @param range The range, may be <code>null</code>.
     * @return The possibly wrapped output stream.
     */
    public static OutputStream wrap(OutputStream out, HttpRange range) {
        if (range == null) {
            return out;
        }
        return new RangeOutputStream(out, range);
    }

    /**
     * Writes the byte if it's within the range.
     *
     * @param b The byte to write.
     * @throws IOException Thrown if there was a problem writing to the stream.
     */
    @Override
    public void write(int b) throws IOException {
        if (range.contains(pos)) {
            super.write(b);
        }
        pos++;
    }

    /**
     * Writes the subset of the bytes that are within the range.
     *
     * @param b   The bytes to write.
     * @param off The offset to start at.
     * @param len The number of bytes to write.
     * @throws IOException Thrown if there was a problem writing to the stream.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        long start = range.getFirstBytePos();
        Long end = range.getLastBytePos();
        if (pos + len >= start && (end == null || pos <= end)) {
            long skipStart = Math.max(0, start - pos);
            long newOff = off + skipStart;
            long newLen = len - skipStart;
            if (end != null) {
                newLen = min(newLen, end - pos + 1, end - start + 1);
            }
            out.write(b, (int) newOff, (int) newLen);
        }
        pos += len;
    }

    private long min(long a, long b, long c) {
        return Math.min(a, Math.min(b, c));
    }
}

