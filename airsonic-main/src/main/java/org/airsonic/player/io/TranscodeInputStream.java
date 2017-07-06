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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Subclass of {@link InputStream} which provides on-the-fly transcoding.
 * Instances of <code>TranscodeInputStream</code> can be chained together, for instance to convert
 * from OGG to WAV to MP3.
 *
 * @author Sindre Mehus
 */
public class TranscodeInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodeInputStream.class);

    private InputStream processInputStream;
    private OutputStream processOutputStream;
    private Process process;
    private final File tmpFile;

    /**
     * Creates a transcoded input stream by executing an external process. If <code>in</code> is not null,
     * data from it is copied to the command.
     *
     * @param processBuilder Used to create the external process.
     * @param in Data to feed to the process.  May be {@code null}.
     * @param tmpFile Temporary file to delete when this stream is closed.  May be {@code null}.
     * @throws IOException If an I/O error occurs.
     */
    public TranscodeInputStream(ProcessBuilder processBuilder, final InputStream in, File tmpFile) throws IOException {
        this.tmpFile = tmpFile;

        StringBuffer buf = new StringBuffer("Starting transcoder: ");
        for (String s : processBuilder.command()) {
            buf.append('[').append(s).append("] ");
        }
        LOG.info(buf.toString());

        process = processBuilder.start();
        processOutputStream = process.getOutputStream();
        processInputStream = process.getInputStream();

        // Must read stderr from the process, otherwise it may block.
        final String name = processBuilder.command().get(0);
        new InputStreamReaderThread(process.getErrorStream(), name, true).start();

        // Copy data in a separate thread
        if (in != null) {
            new Thread(name + " TranscodedInputStream copy thread") {
                public void run() {
                    try {
                        IOUtils.copy(in, processOutputStream);
                    } catch (IOException x) {
                        // Intentionally ignored. Will happen if the remote player closes the stream.
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(processOutputStream);
                    }
                }
            }.start();
        }
    }

    /**
     * @see InputStream#read()
     */
    public int read() throws IOException {
        return processInputStream.read();
    }

    /**
     * @see InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        return processInputStream.read(b);
    }

    /**
     * @see InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return processInputStream.read(b, off, len);
    }

    /**
     * @see InputStream#close()
     */
    public void close() throws IOException {
        IOUtils.closeQuietly(processInputStream);
        IOUtils.closeQuietly(processOutputStream);

        if (process != null) {
            process.destroy();
        }

        if (tmpFile != null) {
            if (!tmpFile.delete()) {
                LOG.warn("Failed to delete tmp file: " + tmpFile);
            }
        }
    }
}
