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
import org.airsonic.player.util.Pair;

import java.awt.*;

/**
 * @author Sindre Mehus
 * @version $Id: StreamControllerTestCase.java 3307 2013-01-04 13:48:49Z sindre_mehus $
 */
public class HLSControllerTestCase extends TestCase {

    public void testParseBitRate() {
        HLSController controller = new HLSController();

        Pair<Integer,Dimension> pair = controller.parseBitRate("1000");
        assertEquals(1000, pair.getFirst().intValue());
        assertNull(pair.getSecond());

        pair = controller.parseBitRate("1000@400x300");
        assertEquals(1000, pair.getFirst().intValue());
        assertEquals(400, pair.getSecond().width);
        assertEquals(300, pair.getSecond().height);

        try {
            controller.parseBitRate("asdfl");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            controller.parseBitRate("1000@300");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            controller.parseBitRate("1000@300x400ZZ");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}
