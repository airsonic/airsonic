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
package org.airsonic.player.controller;

import junit.framework.TestCase;

import java.awt.*;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class StreamControllerTestCase extends TestCase {

    public void testGetRequestedVideoSize() {
        StreamController controller = new StreamController();

        // Valid spec.
        assertEquals("Wrong size.", new Dimension(123, 456), controller.getRequestedVideoSize("123x456"));
        assertEquals("Wrong size.", new Dimension(456, 123), controller.getRequestedVideoSize("456x123"));
        assertEquals("Wrong size.", new Dimension(1, 1), controller.getRequestedVideoSize("1x1"));

        // Missing spec.
        assertNull("Wrong size.", controller.getRequestedVideoSize(null));

        // Invalid spec.
        assertNull("Wrong size.", controller.getRequestedVideoSize("123"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("123x"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("x123"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("x"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("foo123x456bar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("foo123x456"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("123x456bar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("fooxbar"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("-1x1"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("1x-1"));

        // Too large.
        assertNull("Wrong size.", controller.getRequestedVideoSize("3000x100"));
        assertNull("Wrong size.", controller.getRequestedVideoSize("100x3000"));
    }

    public void testGetSuitableVideoSize() {

        // 4:3 aspect rate
        doTestGetSuitableVideoSize(1280, 960, 200, 400, 300);
        doTestGetSuitableVideoSize(1280, 960, 300, 400, 300);
        doTestGetSuitableVideoSize(1280, 960, 400, 480, 360);
        doTestGetSuitableVideoSize(1280, 960, 500, 480, 360);
        doTestGetSuitableVideoSize(1280, 960, 600, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 700, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 800, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 900, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 1000, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 1100, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 1200, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 1500, 640, 480);
        doTestGetSuitableVideoSize(1280, 960, 1800, 960, 720);
        doTestGetSuitableVideoSize(1280, 960, 2000, 960, 720);

        // 16:9 aspect rate
        doTestGetSuitableVideoSize(1280, 720, 200, 400, 226);
        doTestGetSuitableVideoSize(1280, 720, 300, 400, 226);
        doTestGetSuitableVideoSize(1280, 720, 400, 480, 270);
        doTestGetSuitableVideoSize(1280, 720, 500, 480, 270);
        doTestGetSuitableVideoSize(1280, 720, 600, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 700, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 800, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 900, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 1000, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 1100, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 1200, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 1500, 640, 360);
        doTestGetSuitableVideoSize(1280, 720, 1800, 960, 540);
        doTestGetSuitableVideoSize(1280, 720, 2000, 960, 540);

        // Small original size.
        doTestGetSuitableVideoSize(100, 100, 1000, 100, 100);
        doTestGetSuitableVideoSize(100, 1000, 1000, 100, 1000);
        doTestGetSuitableVideoSize(1000, 100, 100, 1000, 100);

        // Unknown original size.
        doTestGetSuitableVideoSize(720, null, 200, 400, 226);
        doTestGetSuitableVideoSize(null, 540, 300, 400, 226);
        doTestGetSuitableVideoSize(null, null, 400, 480, 270);
        doTestGetSuitableVideoSize(720, null, 500, 480, 270);
        doTestGetSuitableVideoSize(null, 540, 600, 640, 360);
        doTestGetSuitableVideoSize(null, null, 700, 640, 360);
        doTestGetSuitableVideoSize(720, null, 1200, 640, 360);
        doTestGetSuitableVideoSize(null, 540, 1500, 640, 360);
        doTestGetSuitableVideoSize(null, null, 2000, 960, 540);

        // Odd original size.
        doTestGetSuitableVideoSize(203, 101, 1500, 204, 102);
        doTestGetSuitableVideoSize(464, 853, 1500, 464, 854);
    }

    private void doTestGetSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate, int expectedWidth, int expectedHeight) {
        StreamController controller = new StreamController();
        Dimension dimension = controller.getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        assertEquals("Wrong width.", expectedWidth, dimension.width);
        assertEquals("Wrong height.", expectedHeight, dimension.height);
    }
}
