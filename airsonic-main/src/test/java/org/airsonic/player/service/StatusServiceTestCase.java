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
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;

import java.util.Arrays;

/**
 * Unit test of {@link StatusService}.
 *
 * @author Sindre Mehus
 */
public class StatusServiceTestCase extends TestCase {

    private StatusService service;
    private Player player1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        service = new StatusService();
        player1 = new Player();
        player1.setId(1);
    }

    public void testSimpleAddRemove() {
        TransferStatus status = service.createStreamStatus(player1);
        assertTrue("Wrong status.", status.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), service.getStreamStatusesForPlayer(player1));

        service.removeStreamStatus(status);
        assertFalse("Wrong status.", status.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), service.getStreamStatusesForPlayer(player1));
    }

    public void testMultipleStreamsSamePlayer() {
        TransferStatus statusA = service.createStreamStatus(player1);
        TransferStatus statusB = service.createStreamStatus(player1);

        assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB), service.getStreamStatusesForPlayer(player1));

        // Stop stream A.
        service.removeStreamStatus(statusA);
        assertFalse("Wrong status.", statusA.isActive());
        assertTrue("Wrong status.", statusB.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), service.getStreamStatusesForPlayer(player1));

        // Stop stream B.
        service.removeStreamStatus(statusB);
        assertFalse("Wrong status.", statusB.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), service.getStreamStatusesForPlayer(player1));

        // Start stream C.
        TransferStatus statusC = service.createStreamStatus(player1);
        assertTrue("Wrong status.", statusC.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusC), service.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusC), service.getStreamStatusesForPlayer(player1));
    }
}
