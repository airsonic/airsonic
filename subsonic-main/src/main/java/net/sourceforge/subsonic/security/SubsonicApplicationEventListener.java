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

import org.acegisecurity.event.authentication.AbstractAuthenticationFailureEvent;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.acegisecurity.ui.WebAuthenticationDetails;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class SubsonicApplicationEventListener implements ApplicationListener {

    private LoginFailureLogger loginFailureLogger;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AbstractAuthenticationFailureEvent) {
            if (event.getSource() instanceof AbstractAuthenticationToken) {
                AbstractAuthenticationToken token = (AbstractAuthenticationToken) event.getSource();
                Object details = token.getDetails();
                if (details instanceof WebAuthenticationDetails) {
                    loginFailureLogger.log(((WebAuthenticationDetails) details).getRemoteAddress(), String.valueOf(token.getPrincipal()));
                }
            }
        }

    }

    public void setLoginFailureLogger(LoginFailureLogger loginFailureLogger) {
        this.loginFailureLogger = loginFailureLogger;
    }
}
