/*
 * This file is part of Subsonic.
 *
 *  Subsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Subsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package net.sourceforge.subsonic.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class URLBuilder {

    public static String HTTP = "http";
    public static String HTTPS = "https";

    private String protocol;
    private String host;
    private int port;
    private String file;

    public URLBuilder(URL url) {
        this.protocol = url.getProtocol();
        this.host = url.getHost();
        this.port = url.getPort();
        this.file = url.getFile();
    }

    public URLBuilder(String url) throws MalformedURLException {
        this(new URL(url));
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFile() {
        return file;
    }

    public URL getURL() {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getURLAsString() {
        return getURL().toString();
    }
}
