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
package org.airsonic.player.service.metadata;

import junit.framework.TestCase;
import org.airsonic.player.domain.MediaFile;

/**
 * Unit test of {@link MediaFile}.
 *
 * @author Sindre Mehus
 */
public class MediaFileTestCase extends TestCase {

    public void testGetDurationAsString() throws Exception {
        doTestGetDurationAsString(0, "0:00");
        doTestGetDurationAsString(1, "0:01");
        doTestGetDurationAsString(10, "0:10");
        doTestGetDurationAsString(33, "0:33");
        doTestGetDurationAsString(59, "0:59");
        doTestGetDurationAsString(60, "1:00");
        doTestGetDurationAsString(61, "1:01");
        doTestGetDurationAsString(70, "1:10");
        doTestGetDurationAsString(119, "1:59");
        doTestGetDurationAsString(120, "2:00");
        doTestGetDurationAsString(1200, "20:00");
        doTestGetDurationAsString(1201, "20:01");
        doTestGetDurationAsString(3599, "59:59");
        doTestGetDurationAsString(3600, "1:00:00");
        doTestGetDurationAsString(3601, "1:00:01");
        doTestGetDurationAsString(3661, "1:01:01");
        doTestGetDurationAsString(4200, "1:10:00");
        doTestGetDurationAsString(4201, "1:10:01");
        doTestGetDurationAsString(4210, "1:10:10");
        doTestGetDurationAsString(36000, "10:00:00");
        doTestGetDurationAsString(360000, "100:00:00");
    }

    private void doTestGetDurationAsString(int seconds, String expected) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setDurationSeconds(seconds);
        assertEquals("Error in getDurationString().", expected, mediaFile.getDurationString());
    }
}