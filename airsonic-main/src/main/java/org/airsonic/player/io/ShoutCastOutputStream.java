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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Implements SHOUTcast support by decorating an existing output stream.
 * <p/>
 * Based on protocol description found on
 * <em>http://www.smackfu.com/stuff/programming/shoutcast.html</em>
 *
 * @author Sindre Mehus
 */
public class ShoutCastOutputStream extends OutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(ShoutCastOutputStream.class);

    /**
     * Number of bytes between each SHOUTcast metadata block.
     */
    public static final int META_DATA_INTERVAL = 20480;

    /**
     * The underlying output stream to decorate.
     */
    private OutputStream out;

    /**
     * What to write in the SHOUTcast metadata is fetched from the playlist.
     */
    final private PlayQueue playQueue;

    /**
     * Keeps track of the number of bytes written (excluding meta-data).  Between 0 and {@link #META_DATA_INTERVAL}.
     */
    private int byteCount;

    /**
     * The last stream title sent.
     */
    private String previousStreamTitle;

    private SettingsService settingsService;

    /**
     * Creates a new SHOUTcast-decorated stream for the given output stream.
     *
     * @param out      The output stream to decorate.
     * @param playQueue Meta-data is fetched from this playlist.
     */
    public ShoutCastOutputStream(OutputStream out, PlayQueue playQueue, SettingsService settingsService) {
        this.out = out;
        this.playQueue = playQueue;
        this.settingsService = settingsService;
    }

    /**
     * Writes the given byte array to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    public void write(byte[] b, int off, int len) throws IOException {

        int bytesWritten = 0;
        while (bytesWritten < len) {

            // 'n' is the number of bytes to write before the next potential meta-data block.
            int n = Math.min(len - bytesWritten, ShoutCastOutputStream.META_DATA_INTERVAL - byteCount);

            out.write(b, off + bytesWritten, n);
            bytesWritten += n;
            byteCount += n;

            // Reached meta-data block?
            if (byteCount % ShoutCastOutputStream.META_DATA_INTERVAL == 0) {
                writeMetaData();
                byteCount = 0;
            }
        }
    }

    /**
     * Writes the given byte array to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes the given byte to the underlying stream, adding SHOUTcast meta-data as necessary.
     */
    public void write(int b) throws IOException {
        byte[] buf = new byte[]{(byte) b};
        write(buf);
    }

    /**
     * Flushes the underlying stream.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes the underlying stream.
     */
    public void close() throws IOException {
        out.close();
    }

    private void writeMetaData() throws IOException {
        String streamTitle = StringUtils.trimToEmpty(settingsService.getWelcomeTitle());

        MediaFile result;
        synchronized (playQueue) {
            result = playQueue.getCurrentFile();
        }
        MediaFile mediaFile = result;
        if (mediaFile != null) {
            streamTitle = mediaFile.getArtist() + " - " + mediaFile.getTitle();
        }

        byte[] bytes;

        if (streamTitle.equals(previousStreamTitle)) {
            bytes = new byte[0];
        } else {
            previousStreamTitle = streamTitle;
            bytes = createStreamTitle(streamTitle);
        }

        // Length in groups of 16 bytes.
        int length = bytes.length / 16;
        if (bytes.length % 16 > 0) {
            length++;
        }

        // Write the length as a single byte.
        out.write(length);

        // Write the message.
        out.write(bytes);

        // Write padding zero bytes.
        int padding = length * 16 - bytes.length;
        for (int i = 0; i < padding; i++) {
            out.write(0);
        }
    }

    private byte[] createStreamTitle(String title) {
        // Remove any quotes from the title.
        title = title.replaceAll("'", "");

        // Convert non-ascii characters to similar ascii characters.
        for (char[] chars : ShoutCastOutputStream.CHAR_MAP) {
            title = title.replace(chars[0], chars[1]);
        }

        title = "StreamTitle='" + title + "';";
        return title.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Maps from miscellaneous accented characters to similar-looking ASCII characters.
     */
    private static final char[][] CHAR_MAP = {
            {'\u00C0', 'A'}, {'\u00C1', 'A'}, {'\u00C2', 'A'}, {'\u00C3', 'A'}, {'\u00C4', 'A'}, {'\u00C5', 'A'}, {'\u00C6', 'A'},
            {'\u00C8', 'E'}, {'\u00C9', 'E'}, {'\u00CA', 'E'}, {'\u00CB', 'E'}, {'\u00CC', 'I'}, {'\u00CD', 'I'}, {'\u00CE', 'I'},
            {'\u00CF', 'I'}, {'\u00D2', 'O'}, {'\u00D3', 'O'}, {'\u00D4', 'O'}, {'\u00D5', 'O'}, {'\u00D6', 'O'}, {'\u00D9', 'U'},
            {'\u00DA', 'U'}, {'\u00DB', 'U'}, {'\u00DC', 'U'}, {'\u00DF', 'B'}, {'\u00E0', 'a'}, {'\u00E1', 'a'}, {'\u00E2', 'a'},
            {'\u00E3', 'a'}, {'\u00E4', 'a'}, {'\u00E5', 'a'}, {'\u00E6', 'a'}, {'\u00E7', 'c'}, {'\u00E8', 'e'}, {'\u00E9', 'e'},
            {'\u00EA', 'e'}, {'\u00EB', 'e'}, {'\u00EC', 'i'}, {'\u00ED', 'i'}, {'\u00EE', 'i'}, {'\u00EF', 'i'}, {'\u00F1', 'n'},
            {'\u00F2', 'o'}, {'\u00F3', 'o'}, {'\u00F4', 'o'}, {'\u00F5', 'o'}, {'\u00F6', 'o'}, {'\u00F8', 'o'}, {'\u00F9', 'u'},
            {'\u00FA', 'u'}, {'\u00FB', 'u'}, {'\u00FC', 'u'}, {'\u2013', '-'}
    };
}
