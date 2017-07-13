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

package org.airsonic.player.service.sonos;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class SonosSoapFault extends RuntimeException {

    private final String faultCode;

    // Must match values in strings.xml
    private final int sonosError;

    protected SonosSoapFault(String faultCode, int sonosError) {
        this.faultCode = faultCode;
        this.sonosError = sonosError;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public int getSonosError() {
        return sonosError;
    }

    public static class LoginInvalid extends SonosSoapFault {

        public LoginInvalid() {
            super("Client.LoginInvalid", 0);
        }
    }

    public static class LoginUnauthorized extends SonosSoapFault {

        public LoginUnauthorized() {
            super("Client.LoginUnauthorized", 1);
        }
    }
}
