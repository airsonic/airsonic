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

package org.airsonic.player.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class LoginFailureListener implements ApplicationListener {

    private static final Logger LOG = LoggerFactory.getLogger(LoginFailureListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AbstractAuthenticationFailureEvent) {
            if (event.getSource() instanceof AbstractAuthenticationToken) {
                AbstractAuthenticationToken token = (AbstractAuthenticationToken) event.getSource();
                Object details = token.getDetails();
                if (details instanceof WebAuthenticationDetails) {
                    LOG.info("Login failed from [" + ((WebAuthenticationDetails) details).getRemoteAddress() + "]");
                }
            }
        }

    }
}
