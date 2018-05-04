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
package org.airsonic.site.filter;

import org.airsonic.player.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static org.airsonic.player.taglib.UrlTag.DEFAULT_ENCODING;

/**
 * Servlet filter which decodes HTTP request parameters.  If a parameter name ends with
 * "Utf8Hex" ({@link org.airsonic.player.taglib.UrlTag.DEFAULT_ENCODING}) , the corresponding parameter value is assumed to be the
 * hexadecimal representation of the UTF-8 bytes of the value.
 * <p/>
 * Used to support request parameter values of any character encoding.
 *
 * @author Sindre Mehus
 */
public class ParameterDecodingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ParameterDecodingFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Wrap request in decoder.
        ServletRequest decodedRequest = new DecodingServletRequestWrapper((HttpServletRequest) request);

        // Pass the request/response on
        chain.doFilter(decodedRequest, response);
    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }

    private static class DecodingServletRequestWrapper extends HttpServletRequestWrapper {

        public DecodingServletRequestWrapper(HttpServletRequest servletRequest) {
            super(servletRequest);
        }

        @Override
        public String getParameter(String name) {
            String[] values = getParameterValues(name);
            if (values == null || values.length == 0) {
                return null;
            }
            return values[0];
        }

        @Override
        public Map getParameterMap() {
            Map map = super.getParameterMap();
            Map<String, String[]> result = new HashMap<String, String[]>();

            for (Object o : map.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                String name = (String) entry.getKey();
                String[] values = (String[]) entry.getValue();

                if (name.endsWith(DEFAULT_ENCODING)) {
                    result.put(name.replace(DEFAULT_ENCODING, ""), decode(values));
                } else {
                    result.put(name, values);
                }
            }
            return result;
        }

        @Override
        public Enumeration getParameterNames() {
            Enumeration e = super.getParameterNames();
            Vector<String> v = new Vector<String>();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                if (name.endsWith(DEFAULT_ENCODING)) {
                    name = name.replace(DEFAULT_ENCODING, "");
                }
                v.add(name);
            }

            return v.elements();
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values != null) {
                return values;
            }

            values = super.getParameterValues(name + DEFAULT_ENCODING);
            if (values != null) {
                return decode(values);
            }

            return null;
        }

        private String[] decode(String[] values) {
            if (values == null) {
                return null;
            }

            String[] result = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    result[i] = StringUtil.utf8HexDecode(values[i]);
                } catch (Exception x) {
                    LOG.error("Failed to decode parameter value '" + values[i] + "'");
                    result[i] = values[i];
                }
            }

            return result;
        }

    }

}
