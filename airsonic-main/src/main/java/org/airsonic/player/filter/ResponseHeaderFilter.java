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
package org.airsonic.player.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Configurable filter for setting HTTP response headers. Can be used, for instance, to
 * set cache control directives for certain resources.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/08/14 13:14:47 $
 */
public class ResponseHeaderFilter implements Filter {
    private FilterConfig filterConfig;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        // Sets the provided HTTP response parameters
        for (Enumeration e = filterConfig.getInitParameterNames(); e.hasMoreElements();) {
            String headerName = (String) e.nextElement();
            response.setHeader(headerName, filterConfig.getInitParameter(headerName));
        }

        // pass the request/response on
        chain.doFilter(req, response);
    }

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
        this.filterConfig = null;
    }
}