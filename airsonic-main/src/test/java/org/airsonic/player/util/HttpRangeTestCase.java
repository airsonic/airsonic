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

/**
 * Unit test of {@link HttpRange}.
 *
 * @author Sindre Mehus
 */
public class HttpRangeTestCase extends TestCase {

    public void testSize() {
        assertEquals(1, new HttpRange(0L, 0L).size());
        assertEquals(2, new HttpRange(0L, 1L).size());
        assertEquals(1, new HttpRange(66L, 66L).size());
        assertEquals(2, new HttpRange(66L, 67L).size());
        assertEquals(10, new HttpRange(66L, 75L).size());
        assertEquals(-1, new HttpRange(66L, null).size());
    }

    public void testContains() {
        assertFalse(new HttpRange(0L, 0L).contains(-1));
        assertTrue(new HttpRange(0L, 0L).contains(0));
        assertFalse(new HttpRange(0L, 0L).contains(1));

        assertFalse(new HttpRange(5L, 10L).contains(-1));
        assertFalse(new HttpRange(5L, 10L).contains(4));
        assertTrue(new HttpRange(5L, 10L).contains(5));
        assertTrue(new HttpRange(5L, 10L).contains(9));
        assertTrue(new HttpRange(5L, 10L).contains(10));
        assertFalse(new HttpRange(5L, 10L).contains(11));
        assertFalse(new HttpRange(5L, 10L).contains(66));

        assertFalse(new HttpRange(5L, null).contains(-1));
        assertFalse(new HttpRange(5L, null).contains(4));
        assertTrue(new HttpRange(5L, null).contains(5));
        assertTrue(new HttpRange(5L, null).contains(6));
        assertTrue(new HttpRange(5L, null).contains(66));
    }

    public void testParseRange() {
        doTestParseRange(0L, 0L, "bytes=0-0");
        doTestParseRange(0L, 1L, "bytes=0-1");
        doTestParseRange(100L, 100L, "bytes=100-100");
        doTestParseRange(0L, 499L, "bytes=0-499");
        doTestParseRange(500L, 999L, "bytes=500-999");
        doTestParseRange(9500L, null, "bytes=9500-");

        assertNull("Error in parseRange().", HttpRange.valueOf(null));
        assertNull("Error in parseRange().", HttpRange.valueOf(""));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes"));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes=a-b"));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes=-100-500"));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes=-500"));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes=500-600,601-999"));
        assertNull("Error in parseRange().", HttpRange.valueOf("bytes=200-100"));
    }

    private void doTestParseRange(Long expectedFrom, Long expectedTo, String range) {
        HttpRange actual = HttpRange.valueOf(range);
        assertEquals("Error in parseRange().", expectedFrom, actual.getFirstBytePos());
        assertEquals("Error in parseRange().", expectedTo, actual.getLastBytePos());
    }
}
