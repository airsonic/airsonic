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
package net.sourceforge.subsonic.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * Configurable filter for setting the character encoding to use for the HTTP request.
 * Typically used to set UTF-8 encoding when reading request parameters with non-Latin
 * content.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2006/03/01 16:58:08 $
 */
public class RequestEncodingFilter implements Filter {

    private String encoding;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        request.setCharacterEncoding(encoding);

        // Pass the request/response on
        chain.doFilter(req, res);
    }

    public void init(FilterConfig filterConfig) {
        encoding = filterConfig.getInitParameter("encoding");
    }

    public void destroy() {
        encoding = null;
    }

}
