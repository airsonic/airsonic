/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.service;

import junit.framework.TestCase;

public class SonosServiceTest extends TestCase {

    public void testParsePlaylistIndices() {
        SonosService sonosService = new SonosService();
        assertEquals("[]", sonosService.parsePlaylistIndices("").toString());
        assertEquals("[999]", sonosService.parsePlaylistIndices("999").toString());
        assertEquals("[1, 2, 3]", sonosService.parsePlaylistIndices("1,2,3").toString());
        assertEquals("[1, 2, 3]", sonosService.parsePlaylistIndices("2,1,3").toString());
        assertEquals("[1, 2, 4, 5, 6, 7]", sonosService.parsePlaylistIndices("1,2,4-7").toString());
        assertEquals("[11, 12, 15, 20, 21, 22]", sonosService.parsePlaylistIndices("11-12,15,20-22").toString());
    }
}