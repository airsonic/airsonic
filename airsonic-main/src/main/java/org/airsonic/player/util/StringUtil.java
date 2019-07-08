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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtil {

    public static final String ENCODING_UTF8 = "UTF-8";
    private static final DateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String[][] HTML_SUBSTITUTIONS = {
            {"&", "&amp;"},
            {"<", "&lt;"},
            {">", "&gt;"},
            {"'", "&#39;"},
            {"\"", "&#34;"},
    };

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
     * Returns the specified string converted to a format suitable for
     * HTML. All single-quote, double-quote, greater-than, less-than and
     * ampersand characters are replaces with their corresponding HTML
     * Character Entity code.
     *
     * @param s the string to convert
     * @return the converted string
     */
    public static String toHtml(String s) {
        if (s == null) {
            return null;
        }
        for (String[] substitution : HTML_SUBSTITUTIONS) {
            if (s.contains(substitution[0])) {
                s = s.replaceAll(substitution[0], substitution[1]);
            }
        }
        return s;
    }


    /**
     * Formats the given date to a ISO-8601 date/time format, and UTC timezone.
     * <p/>
     * The returned date uses the following format: 2007-12-17T14:57:17
     *
     * @param date The date to format
     * @return The corresponding ISO-8601 formatted string.
     */
    public static String toISO8601(Date date) {
        if (date == null) {
            return null;
        }

        synchronized (ISO_8601_DATE_FORMAT) {
            return ISO_8601_DATE_FORMAT.format(date);
        }
    }

    /**
     * Removes the suffix (the substring after the last dot) of the given string. The dot is
     * also removed.
     *
     * @param s The string in question, e.g., "foo.mp3".
     * @return The string without the suffix, e.g., "foo".
     */
    public static String removeSuffix(String s) {
        int index = s.lastIndexOf('.');
        return index == -1 ? s : s.substring(0, index);
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
            return teraByteFormat.format( byteCount / ((double) 1024 * 1024 * 1024 * 1024));
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
     * Formats a duration with minutes and seconds, e.g., "93:45"
     */
    public static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(6);
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
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

        Pattern pattern = Pattern.compile("\".*?\"|\\S+");
        Matcher matcher = pattern.matcher(input);

        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            String element = matcher.group();
            if (element.startsWith("\"") && element.endsWith("\"") && element.length() > 1) {
                element = element.substring(1, element.length() - 1);
            }
            result.add(element);
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
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(in));
            List<String> result = new ArrayList<String>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    result.add(line);
                }
            }
            return result.toArray(new String[result.size()]);

        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(reader);
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

        String[] strings = StringUtils.split(s);
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    /**
     * Determines whether a is equal to b, taking null into account.
     *
     * @return Whether a and b are equal, or both null.
     */
    public static boolean isEqual(Object a, Object b) {
        return Objects.equals(a, b);
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

        String[] elements = s.split("_");

        if (elements.length == 0) {
            return new Locale(s, "", "");
        }
        if (elements.length == 1) {
            return new Locale(elements[0], "", "");
        }
        if (elements.length == 2) {
            return new Locale(elements[0], elements[1], "");
        }
        return new Locale(elements[0], elements[1], elements[2]);
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
        try {
            utf8 = s.getBytes(ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
        return String.valueOf(Hex.encodeHex(utf8));
    }

    /**
     * Decodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to decode.
     * @return The decoded string.
     * @throws Exception If an error occurs.
     */
    public static String utf8HexDecode(String s) throws Exception {
        if (s == null) {
            return null;
        }
        return new String(Hex.decodeHex(s.toCharArray()), ENCODING_UTF8);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    public static String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return new String(Hex.encodeHex(md5.digest(s.getBytes(ENCODING_UTF8))));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
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
