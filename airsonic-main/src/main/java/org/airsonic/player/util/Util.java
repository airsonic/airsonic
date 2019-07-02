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
package org.airsonic.player.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Miscellaneous general utility methods.
 *
 * @author Sindre Mehus
 */
public final class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final String URL_SENSITIVE_REPLACEMENT_STRING = "<hidden>";

    /**
     * Disallow external instantiation.
     */
    private Util() {
    }

    public static String getDefaultMusicFolder() {
        String def = isWindows() ? "c:\\music" : "/var/music";
        return System.getProperty("airsonic.defaultMusicFolder", def);
    }

    public static String getDefaultPodcastFolder() {
        String def = isWindows() ? "c:\\music\\Podcast" : "/var/music/Podcast";
        return System.getProperty("airsonic.defaultPodcastFolder", def);
    }

    public static String getDefaultPlaylistFolder() {
        String def = isWindows() ? "c:\\playlists" : "/var/playlists";
        return System.getProperty("airsonic.defaultPlaylistFolder", def);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "Windows").toLowerCase().startsWith("windows");
    }

    /**
     * Similar to {@link ServletResponse#setContentLength(int)}, but this
     * method supports lengths bigger than 2GB.
     * <p/>
     * See http://blogger.ziesemer.com/2008/03/suns-version-of-640k-2gb.html
     *
     * @param response The HTTP response.
     * @param length   The content length.
     */
    public static void setContentLength(HttpServletResponse response, long length) {
        if (length <= Integer.MAX_VALUE) {
            response.setContentLength((int) length);
        } else {
            response.setHeader("Content-Length", String.valueOf(length));
        }
    }

    public static <T> List<T> subList(List<T> list, long offset, long max) {
        return list.subList((int) offset, Math.min(list.size(), (int) (offset + max)));
    }

    public static List<Integer> toIntegerList(int[] values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<Integer>(values.length);
        for (int value : values) {
            result.add(value);
        }
        return result;
    }

    public static int[] toIntArray(List<Integer> values) {
        if (values == null) {
            return new int[0];
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    static ObjectMapper objectMapper = new ObjectMapper();
    public static String debugObject(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.warn("Cant output debug object", e);
            return "";
        }
    }

    /**
     * Return a complete URL for the given HTTP request,
     * including the query string.
     *
     * @param request An HTTP request instance
     * @return The associated URL
     */
    public static String getURLForRequest(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) url += "?" + queryString;
        return url;
    }

    /**
     * Return an URL for the given HTTP request, with anonymized sensitive parameters.
     *
     * @param request An HTTP request instance
     * @return The associated anonymized URL
     */
    public static String getAnonymizedURLForRequest(HttpServletRequest request) {

        String url = getURLForRequest(request);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        MultiValueMap<String, String> components = builder.build().getQueryParams();

        // Subsonic REST API authentication (see RESTRequestParameterProcessingFilter)
        if (components.containsKey("p")) builder.replaceQueryParam("p", URL_SENSITIVE_REPLACEMENT_STRING);  // Cleartext password
        if (components.containsKey("t")) builder.replaceQueryParam("t", URL_SENSITIVE_REPLACEMENT_STRING);  // Token
        if (components.containsKey("s")) builder.replaceQueryParam("s", URL_SENSITIVE_REPLACEMENT_STRING);  // Salt
        if (components.containsKey("u")) builder.replaceQueryParam("u", URL_SENSITIVE_REPLACEMENT_STRING);  // Username

        return builder.build().toUriString();
    }

    /**
     * Return true if the given object is an instance of the class name in argument.
     * If the class doesn't exist, returns false.
     */
    public static boolean isInstanceOfClassName(Object o, String className) {
        try {
            return Class.forName(className).isInstance(o);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
