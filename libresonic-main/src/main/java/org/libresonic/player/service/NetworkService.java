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
package org.libresonic.player.service;

import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class NetworkService {

    public static String getBaseUrl(HttpServletRequest request) {
        try {
            UrlPathHelper urlPathHelper = new UrlPathHelper();
            URL url = new URL(request.getRequestURL().toString());
            String host = url.getHost();
            String userInfo = url.getUserInfo();
            String scheme = url.getProtocol();
            int port = url.getPort();

            URI uri = new URI(scheme, userInfo, host, port, urlPathHelper.getContextPath(request), null, null);
            return uri.toString() + "/";
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Could not calculate base url", e);
        }
    }

}
