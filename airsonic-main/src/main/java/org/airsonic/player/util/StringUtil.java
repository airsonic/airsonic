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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtil {

    public static final String ENCODING_UTF8 = "UTF-8";

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private static final String[][] MIME_TYPES = {
            {"mp3", "audio/mpeg"},
            {"ogg", "audio/ogg"},
            {"oga", "audio/ogg"},
            {"opus", "audio/ogg"},
            {"ogx", "application/ogg"},
            {"aac", "audio/mp4"},
            {"m4a", "audio/mp4"},
            {"m4b", "audio/mp4"},
            {"flac", "audio/flac"},
            {"wav", "audio/x-wav"},
            {"wma", "audio/x-ms-wma"},
            {"ape", "audio/x-monkeys-audio"},
            {"mpc", "audio/x-musepack"},
            {"shn", "audio/x-shn"},

            {"flv", "video/x-flv"},
            {"avi", "video/avi"},
            {"mpg", "video/mpeg"},
            {"mpeg", "video/mpeg"},
            {"mp4", "video/mp4"},
            {"m4v", "video/x-m4v"},
            {"mkv", "video/x-matroska"},
            {"mov", "video/quicktime"},
            {"wmv", "video/x-ms-wmv"},
            {"ogv", "video/ogg"},
            {"divx", "video/divx"},
            {"m2ts", "video/MP2T"},
            {"ts", "video/MP2T"},
            {"webm", "video/webm"},

            {"gif", "image/gif"},
            {"jpg", "image/jpeg"},
            {"jpeg", "image/jpeg"},
            {"png", "image/png"},
            {"bmp", "image/bmp"},
    };

    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":", "\"", "?", "*", "|"};

    /**
     * Disallow external instantiation.
     */
    private StringUtil() {
    }

    /**
     * Returns the proper MIME type for the given suffix.
     *
     * @param suffix The suffix, e.g., "mp3" or ".mp3".
     * @return The corresponding MIME type, e.g., "audio/mpeg". If no MIME type is found,
     *         <code>application/octet-stream</code> is returned.
     */
    public static String getMimeType(String suffix) {
        for (String[] map : MIME_TYPES) {
            if (map[0].equalsIgnoreCase(suffix) || ('.' + map[0]).equalsIgnoreCase(suffix)) {
                return map[1];
            }
        }
        return "application/octet-stream";
    }

    public static String getMimeType(String suffix, boolean sonos) {
        String result = getMimeType(suffix);

        // Sonos doesn't work with "audio/mp4" but needs "audio/aac" for ALAC and AAC (in MP4 container)
        return sonos && "audio/mp4".equals(result) ? "audio/aac" : result;
    }

    public static String getSuffix(String mimeType) {
        for (String[] map : MIME_TYPES) {
            if (map[1].equalsIgnoreCase(mimeType)) {
                return map[0];
            }
        }
        return null;
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     *
     * @param byteCount The number of bytes.
     * @param locale    The locale used for formatting.
     * @return The formatted string.
     */
    public static synchronized String formatBytes(long byteCount, Locale locale) {

        // More than 1 TB?
        if (byteCount >= 1024L * 1024 * 1024 * 1024) {
            NumberFormat teraByteFormat = new DecimalFormat("0.00 TB", new DecimalFormatSymbols(locale));
            return teraByteFormat.format(byteCount / ((double) 1024 * 1024 * 1024 * 1024));
        }
     
        // More than 1 GB?
        if (byteCount >= 1024L * 1024 * 1024) {
            NumberFormat gigaByteFormat = new DecimalFormat("0.00 GB", new DecimalFormatSymbols(locale));
            return gigaByteFormat.format(byteCount / ((double) 1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024L * 1024) {
            NumberFormat megaByteFormat = new DecimalFormat("0.0 MB", new DecimalFormatSymbols(locale));
            return megaByteFormat.format(byteCount / ((double) 1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024L) {
            NumberFormat kiloByteFormat = new DecimalFormat("0 KB", new DecimalFormatSymbols(locale));
            return kiloByteFormat.format((double) byteCount / 1024);
        }

        return byteCount + " B";
    }

    /**
     * Formats a duration with minutes and seconds, e.g., "4:34" or "93:45"
     */
    public static String formatDurationMSS(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds must be >= 0");
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Formats a duration with H:MM:SS, e.g., "1:33:45"
     */
    public static String formatDurationHMMSS(int seconds) {
        int hours = seconds / 3600;
        seconds -= hours * 3600;

        return String.format("%d:%s%s", hours, seconds < 600 ? "0" : "", formatDurationMSS(seconds));
    }

    /**
     * Formats a duration to M:SS or H:MM:SS
     */
    public static String formatDuration(int seconds) {
        if (seconds >= 3600) {
            return formatDurationHMMSS(seconds);
        }
        return formatDurationMSS(seconds);
    }

    /**
     * Splits the input string. White space is interpreted as separator token. Double quotes
     * are interpreted as grouping operator. <br/>
     * For instance, the input <code>"u2 rem "greatest hits""</code> will return an array with
     * three elements: <code>{"u2", "rem", "greatest hits"}</code>
     *
     * @param input The input string.
     * @return Array of elements.
     */
    public static String[] split(String input) {
        if (input == null) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        Matcher m = SPLIT_PATTERN.matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                result.add(m.group(1)); // quoted string
            } else {
                result.add(m.group(2)); // unquoted string
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Reads lines from the given input stream. All lines are trimmed. Empty lines and lines starting
     * with "#" are skipped. The input stream is always closed by this method.
     *
     * @param in The input stream to read from.
     * @return Array of lines.
     * @throws IOException If an I/O error occurs.
     */
    public static String[] readLines(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            List<String> result = new ArrayList<String>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    result.add(line);
                }
            }
            return result.toArray(new String[result.size()]);

        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    /**
     * Converts the given string of whitespace-separated integers to an <code>int</code> array.
     *
     * @param s String consisting of integers separated by whitespace.
     * @return The corresponding array of ints.
     * @throws NumberFormatException If string contains non-parseable text.
     */
    public static int[] parseInts(String s) {
        if (s == null) {
            return new int[0];
        }

        return Stream.of(StringUtils.split(s))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    /**
     * Parses a locale from the given string.
     *
     * @param s The locale string. Should be formatted as per the documentation in {@link Locale#toString()}.
     * @return The locale.
     */
    public static Locale parseLocale(String s) {
        if (s == null) {
            return null;
        }

        List<String> elements = new ArrayList<>(Arrays.asList(s.split("_", 3)));
        while (elements.size() < 3) {
            elements.add("");
        }
        return new Locale(elements.get(0), elements.get(1), elements.get(2));
    }

    /**
     * URL-encodes the input value using UTF-8.
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StringUtil.ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * URL-decodes the input value using UTF-8.
     */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StringUtil.ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    /**
    * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
    *
    * @param s The string to encode.
    * @return The encoded string.
    */
    public static String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        utf8 = s.getBytes(StandardCharsets.UTF_8);
        return String.valueOf(Hex.encodeHex(utf8));
    }

    /**
     * Decodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to decode.
     * @return The decoded string.
     * @throws DecoderException If an error occurs.
     */
    public static String utf8HexDecode(String s) throws DecoderException {
        if (s == null) {
            return null;
        }
        return new String(Hex.decodeHex(s.toCharArray()), StandardCharsets.UTF_8);
    }

    /**
     * Returns the file part of an URL. For instance:
     * <p/>
     * <code>
     * getUrlFile("http://archive.ncsa.uiuc.edu:80/SDG/Software/Mosaic/Demo/url-primer.html")
     * </code>
     * <p/>
     * will return "url-primer.html".
     *
     * @param url The URL in question.
     * @return The file part, or <code>null</code> if no file can be resolved.
     */
    public static String getUrlFile(String url) {
        try {
            String path = new URL(url).getPath();
            if (StringUtils.isBlank(path) || path.endsWith("/")) {
                return null;
            }

            File file = new File(path);
            String filename = file.getName();
            if (StringUtils.isBlank(filename)) {
                return null;
            }
            return filename;

        } catch (MalformedURLException x) {
            return null;
        }
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes ("/" and "\")
     * with dashes ("-").
     *
     * @param filename The filename in question.
     * @return The filename with special characters replaced by underscores.
     */
    public static String fileSystemSafe(String filename) {
        for (String s : FILE_SYSTEM_UNSAFE) {
            filename = filename.replace(s, "-");
        }
        return filename;
    }

    public static String removeMarkup(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("<.*?>", "");
    }
}
