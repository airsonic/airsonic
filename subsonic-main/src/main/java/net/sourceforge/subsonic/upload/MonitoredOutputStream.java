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
package net.sourceforge.subsonic.upload;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Extension of Commons FileUpload for monitoring the upload progress.
 *
 * @author Pierre-Alexandre Losson -- http://www.telio.be/blog -- plosson@users.sourceforge.net
 */
public class MonitoredOutputStream extends OutputStream {
    private OutputStream target;
    private UploadListener listener;

    public MonitoredOutputStream(OutputStream target, UploadListener listener) {
        this.target = target;
        this.listener = listener;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        target.write(b, off, len);
        listener.bytesRead(len);
    }

    public void write(byte[] b) throws IOException {
        target.write(b);
        listener.bytesRead(b.length);
    }

    public void write(int b) throws IOException {
        target.write(b);
        listener.bytesRead(1);
    }

    public void close() throws IOException {
        target.close();
    }

    public void flush() throws IOException {
        target.flush();
    }
}
