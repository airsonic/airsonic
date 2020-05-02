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
package org.airsonic.player.service;

import junit.framework.TestCase;

/**
 * Unit test of {@link SecurityService}.
 *
 * @author Sindre Mehus
 */
public class SecurityServiceTestCase extends TestCase {

    public void testIsFileInFolder() {
        SecurityService service = new SecurityService();

        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "/"));

        assertTrue(service.isFileInFolder("/music/foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("\\music\\foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music\\"));

        assertFalse(service.isFileInFolder("", "/tmp"));
        assertFalse(service.isFileInFolder("foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp/music"));

        // Test that references to the parent directory (..) is not allowed.
        assertTrue(service.isFileInFolder("/music/foo..mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo..", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.../", "/music"));
        assertFalse(service.isFileInFolder("/music/foo/..", "/music"));
        assertFalse(service.isFileInFolder("../music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../bar/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\foo\\..", "/music"));
        assertFalse(service.isFileInFolder("..\\music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/..\\bar/../foo", "/music"));
    }
}

