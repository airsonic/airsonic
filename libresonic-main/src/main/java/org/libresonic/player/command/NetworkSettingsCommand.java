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
package org.libresonic.player.command;


/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class NetworkSettingsCommand {

    private boolean urlRedirectionEnabled;
    private String urlRedirectFrom;
    private String urlRedirectCustomUrl;
    private String urlRedirectType;
    private int port;
    private boolean toast;

    public boolean isUrlRedirectionEnabled() {
        return urlRedirectionEnabled;
    }

    public void setUrlRedirectionEnabled(boolean urlRedirectionEnabled) {
        this.urlRedirectionEnabled = urlRedirectionEnabled;
    }

    public String getUrlRedirectFrom() {
        return urlRedirectFrom;
    }

    public void setUrlRedirectFrom(String urlRedirectFrom) {
        this.urlRedirectFrom = urlRedirectFrom;
    }

    public String getUrlRedirectCustomUrl() {
        return urlRedirectCustomUrl;
    }

    public void setUrlRedirectCustomUrl(String urlRedirectCustomUrl) {
        this.urlRedirectCustomUrl = urlRedirectCustomUrl;
    }
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isToast() {
        return toast;
    }

    public void setToast(boolean toast) {
        this.toast = toast;
    }

    public String getUrlRedirectType() {
        return urlRedirectType;
    }

    public void setUrlRedirectType(String urlRedirectType) {
        this.urlRedirectType = urlRedirectType;
    }
}
