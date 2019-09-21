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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Locale;

/**
 * Unit test of {@link StringUtil}.
 *
 * @author Sindre Mehus
 */
public class StringUtilTestCase extends TestCase {

    public void testToHtml() {
        assertEquals(null, StringUtil.toHtml(null));
        assertEquals("", StringUtil.toHtml(""));
        assertEquals(" ", StringUtil.toHtml(" "));
        assertEquals("q &amp; a", StringUtil.toHtml("q & a"));
        assertEquals("q &amp; a &lt;&gt; b", StringUtil.toHtml("q & a <> b"));
    }

    public void testRemoveSuffix() {
        assertEquals("Error in removeSuffix().", "foo", StringUtil.removeSuffix("foo.mp3"));
        assertEquals("Error in removeSuffix().", "", StringUtil.removeSuffix(".mp3"));
        assertEquals("Error in removeSuffix().", "foo.bar", StringUtil.removeSuffix("foo.bar.mp3"));
        assertEquals("Error in removeSuffix().", "foo.", StringUtil.removeSuffix("foo..mp3"));
        assertEquals("Error in removeSuffix().", "foo", StringUtil.removeSuffix("foo"));
        assertEquals("Error in removeSuffix().", "", StringUtil.removeSuffix(""));
    }

    public void testGetMimeType() {
        assertEquals("Error in getMimeType().", "audio/mpeg", StringUtil.getMimeType("mp3"));
        assertEquals("Error in getMimeType().", "audio/mpeg", StringUtil.getMimeType(".mp3"));
        assertEquals("Error in getMimeType().", "audio/mpeg", StringUtil.getMimeType(".MP3"));
        assertEquals("Error in getMimeType().", "application/octet-stream", StringUtil.getMimeType("koko"));
        assertEquals("Error in getMimeType().", "application/octet-stream", StringUtil.getMimeType(""));
        assertEquals("Error in getMimeType().", "application/octet-stream", StringUtil.getMimeType(null));
    }

    public void testFormatBytes() {
        Locale locale = Locale.ENGLISH;
        assertEquals("Error in formatBytes().", "918 B", StringUtil.formatBytes(918, locale));
        assertEquals("Error in formatBytes().", "1023 B", StringUtil.formatBytes(1023, locale));
        assertEquals("Error in formatBytes().", "1 KB", StringUtil.formatBytes(1024, locale));
        assertEquals("Error in formatBytes().", "96 KB", StringUtil.formatBytes(98765, locale));
        assertEquals("Error in formatBytes().", "1024 KB", StringUtil.formatBytes(1048575, locale));
        assertEquals("Error in formatBytes().", "1.2 MB", StringUtil.formatBytes(1238476, locale));
        assertEquals("Error in formatBytes().", "3.50 GB", StringUtil.formatBytes(3758096384L, locale));
        assertEquals("Error in formatBytes().", "410.00 TB", StringUtil.formatBytes(450799767388160L, locale));
        assertEquals("Error in formatBytes().", "4413.43 TB", StringUtil.formatBytes(4852617603375432L, locale));

        locale = new Locale("no", "", "");
        assertEquals("Error in formatBytes().", "918 B", StringUtil.formatBytes(918, locale));
        assertEquals("Error in formatBytes().", "1023 B", StringUtil.formatBytes(1023, locale));
        assertEquals("Error in formatBytes().", "1 KB", StringUtil.formatBytes(1024, locale));
        assertEquals("Error in formatBytes().", "96 KB", StringUtil.formatBytes(98765, locale));
        assertEquals("Error in formatBytes().", "1024 KB", StringUtil.formatBytes(1048575, locale));
        assertEquals("Error in formatBytes().", "1,2 MB", StringUtil.formatBytes(1238476, locale));
        assertEquals("Error in formatBytes().", "3,50 GB", StringUtil.formatBytes(3758096384L, locale));
        assertEquals("Error in formatBytes().", "410,00 TB", StringUtil.formatBytes(450799767388160L, locale));
        assertEquals("Error in formatBytes().", "4413,43 TB", StringUtil.formatBytes(4852617603375432L, locale));
    }

    public void testFormatDuration() {
        assertEquals("Error in formatDuration().", "0:00", StringUtil.formatDuration(0));
        assertEquals("Error in formatDuration().", "0:05", StringUtil.formatDuration(5));
        assertEquals("Error in formatDuration().", "0:10", StringUtil.formatDuration(10));
        assertEquals("Error in formatDuration().", "0:59", StringUtil.formatDuration(59));
        assertEquals("Error in formatDuration().", "1:00", StringUtil.formatDuration(60));
        assertEquals("Error in formatDuration().", "1:01", StringUtil.formatDuration(61));
        assertEquals("Error in formatDuration().", "1:10", StringUtil.formatDuration(70));
        assertEquals("Error in formatDuration().", "10:00", StringUtil.formatDuration(600));
        assertEquals("Error in formatDuration().", "45:50", StringUtil.formatDuration(2750));
        assertEquals("Error in formatDuration().", "83:45", StringUtil.formatDuration(5025));
    }

    public void testSplit() {
        doTestSplit("u2 rem \"greatest hits\"", "u2", "rem", "greatest hits");
        doTestSplit("u2", "u2");
        doTestSplit("u2 rem", "u2", "rem");
        doTestSplit(" u2  \t rem ", "u2", "rem");
        doTestSplit("u2 \"rem\"", "u2", "rem");
        doTestSplit("u2 \"rem", "u2", "\"rem");
        doTestSplit("\"", "\"");

        assertEquals(0, StringUtil.split("").length);
        assertEquals(0, StringUtil.split(" ").length);
        assertEquals(0, StringUtil.split(null).length);
    }

    private void doTestSplit(String input, String... expected) {
        String[] actual = StringUtil.split(input);
        assertEquals("Wrong number of elements.", expected.length, actual.length);

        for (int i = 0; i < expected.length; i++) {
            assertEquals("Wrong criteria.", expected[i], actual[i]);
        }
    }

    public void testParseInts() {
        doTestParseInts("123", 123);
        doTestParseInts("1 2 3", 1, 2, 3);
        doTestParseInts("10  20 \t\n 30", 10, 20, 30);

        assertTrue("Error in parseInts().", StringUtil.parseInts(null).length == 0);
        assertTrue("Error in parseInts().", StringUtil.parseInts("").length == 0);
        assertTrue("Error in parseInts().", StringUtil.parseInts(" ").length == 0);
        assertTrue("Error in parseInts().", StringUtil.parseInts("  ").length == 0);
    }

    private void doTestParseInts(String s, int... expected) {
        assertEquals("Error in parseInts().", Arrays.toString(expected), Arrays.toString(StringUtil.parseInts(s)));
    }

    public void testParseLocale() {
        assertEquals("Error in parseLocale().", null, null);
        assertEquals("Error in parseLocale().", new Locale("en"), StringUtil.parseLocale("en"));
        assertEquals("Error in parseLocale().", new Locale("en"), StringUtil.parseLocale("en_"));
        assertEquals("Error in parseLocale().", new Locale("en"), StringUtil.parseLocale("en__"));
        assertEquals("Error in parseLocale().", new Locale("en", "US"), StringUtil.parseLocale("en_US"));
        assertEquals("Error in parseLocale().", new Locale("en", "US", "WIN"), StringUtil.parseLocale("en_US_WIN"));
        assertEquals("Error in parseLocale().", new Locale("en", "", "WIN"), StringUtil.parseLocale("en__WIN"));
    }

    public void testUtf8Hex() throws Exception {
        doTestUtf8Hex(null);
        doTestUtf8Hex("");
        doTestUtf8Hex("a");
        doTestUtf8Hex("abcdefg");
        doTestUtf8Hex("abc������");
        doTestUtf8Hex("NRK P3 � FK Fotball");
    }

    private void doTestUtf8Hex(String s) throws Exception {
        assertEquals("Error in utf8hex.", s, StringUtil.utf8HexDecode(StringUtil.utf8HexEncode(s)));
    }

    public void testMd5Hex() {
        assertNull("Error in md5Hex().", StringUtil.md5Hex(null));
        assertEquals("Error in md5Hex().", "d41d8cd98f00b204e9800998ecf8427e", StringUtil.md5Hex(""));
        assertEquals("Error in md5Hex().", "308ed0af23d48f6d2fd4717e77a23e0c", StringUtil.md5Hex("sindre@activeobjects.no"));
    }

    public void testGetUrlFile() {
        assertEquals("Error in getUrlFile().", "foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/foo.mp3"));
        assertEquals("Error in getUrlFile().", "foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/bar/foo.mp3"));
        assertEquals("Error in getUrlFile().", "foo", StringUtil.getUrlFile("http://www.asdf.com/bar/foo"));
        assertEquals("Error in getUrlFile().", "foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/bar/foo.mp3?a=1&b=2"));
        assertNull("Error in getUrlFile().", StringUtil.getUrlFile("not a url"));
        assertNull("Error in getUrlFile().", StringUtil.getUrlFile("http://www.asdf.com"));
        assertNull("Error in getUrlFile().", StringUtil.getUrlFile("http://www.asdf.com/"));
        assertNull("Error in getUrlFile().", StringUtil.getUrlFile("http://www.asdf.com/foo/"));
    }

    public void testFileSystemSafe() {
        assertEquals("Error in fileSystemSafe().", "foo", StringUtil.fileSystemSafe("foo"));
        assertEquals("Error in fileSystemSafe().", "foo.mp3", StringUtil.fileSystemSafe("foo.mp3"));
        assertEquals("Error in fileSystemSafe().", "foo-bar", StringUtil.fileSystemSafe("foo/bar"));
        assertEquals("Error in fileSystemSafe().", "foo-bar", StringUtil.fileSystemSafe("foo\\bar"));
        assertEquals("Error in fileSystemSafe().", "foo-bar", StringUtil.fileSystemSafe("foo:bar"));
    }

    public void testRemoveMarkup() {
        assertEquals("Error in removeMarkup()", "foo", StringUtil.removeMarkup("<b>foo</b>"));
        assertEquals("Error in removeMarkup()", "foobar", StringUtil.removeMarkup("<b>foo</b>bar"));
        assertEquals("Error in removeMarkup()", "foo", StringUtil.removeMarkup("foo"));
        assertEquals("Error in removeMarkup()", "foo", StringUtil.removeMarkup("<b>foo"));
        assertEquals("Error in removeMarkup()", null, StringUtil.removeMarkup(null));
    }

}
