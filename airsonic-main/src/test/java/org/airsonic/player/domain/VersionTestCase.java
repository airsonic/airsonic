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
package org.airsonic.player.domain;

/**
 * Unit test of {@link Version}.
 * @author Sindre Mehus
 */

import junit.framework.TestCase;

public class VersionTestCase extends TestCase {

    /**
     * Tests that equals(), hashCode(), toString() and compareTo() works.
     */
    public void testVersion()  {
        doTestVersion("0.0", "0.1");
        doTestVersion("1.5", "2.3");
        doTestVersion("2.3", "2.34");

        doTestVersion("1.5", "1.5.1");
        doTestVersion("1.5.1", "1.5.2");
        doTestVersion("1.5.2", "1.5.11");

        doTestVersion("1.4", "1.5.beta1");
        doTestVersion("1.4.1", "1.5.beta1");
        doTestVersion("1.5.beta1", "1.5");
        doTestVersion("1.5.beta1", "1.5.1");
        doTestVersion("1.5.beta1", "1.6");
        doTestVersion("1.5.beta1", "1.5.beta2");
        doTestVersion("1.5.beta2", "1.5.beta11");

        doTestVersion("6.2-SNAPSHOT", "6.11-SNAPSHOT");
    }

    public void testIsPreview() {
        Version version = new Version("1.6.0-SNAPSHOT");
        assertTrue("Version should be snapshot", version.isPreview());

        version = new Version("1.6.0-beta2");
        assertTrue("Version should be snapshot", version.isPreview());

        version = new Version("1.6.0");
        assertFalse("Version should not be snapshot", version.isPreview());

        version = new Version("1.6.0-RELEASE");
        assertFalse("Version should not be snapshot", version.isPreview());
    }

    /**
     * Tests that equals(), hashCode(), toString() and compareTo() works.
     * @param v1 A lower version.
     * @param v2 A higher version.
     */
    private void doTestVersion(String v1, String v2) {
        Version ver1 = new Version(v1);
        Version ver2 = new Version(v2);

        assertEquals("Error in toString().", v1, ver1.toString());
        assertEquals("Error in toString().", v2, ver2.toString());

        assertEquals("Error in equals().", ver1, ver1);

        assertEquals("Error in compareTo().", 0, ver1.compareTo(ver1));
        assertEquals("Error in compareTo().", 0, ver2.compareTo(ver2));
        assertTrue("Error in compareTo().", ver1.compareTo(ver2) < 0);
        assertTrue("Error in compareTo().", ver2.compareTo(ver1) > 0);
    }
}