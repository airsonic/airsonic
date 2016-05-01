/*
 * This file is part of Subsonic.
 *
 *  Subsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Subsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package net.sourceforge.subsonic.security;

import net.sourceforge.subsonic.Logger;

/**
 * Logs login failures. Can be used by tools like fail2ban for blocking IP addresses.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class LoginFailureLogger {

    private static final Logger LOG = Logger.getLogger(LoginFailureLogger.class);

    public void log(String remoteAddress, String username) {
        LOG.info("Login failed for [" + username + "] from [" + remoteAddress + "]");
    }
}
