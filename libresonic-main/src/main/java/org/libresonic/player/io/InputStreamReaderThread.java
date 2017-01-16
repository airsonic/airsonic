/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.io;

import org.apache.commons.io.IOUtils;
import org.libresonic.player.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class which reads everything from an input stream and optionally logs it.
 *
 * @see TranscodeInputStream
 * @author Sindre Mehus
 */
public class InputStreamReaderThread extends Thread {

    private static final Logger LOG = Logger.getLogger(InputStreamReaderThread.class);

    private InputStream input;
    private String name;
    private boolean log;

    public InputStreamReaderThread(InputStream input, String name, boolean log) {
        super(name + " InputStreamLogger");
        this.input = input;
        this.name = name;
        this.log = log;
    }

    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (log) {
                    LOG.info('(' + name + ") " + line);
                }
            }
        } catch (IOException x) {
            // Intentionally ignored.
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(input);
        }
    }
}
