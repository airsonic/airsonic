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
package org.airsonic.player.security;

import org.airsonic.player.controller.JAXBWriter;
import org.airsonic.player.controller.SubsonicRESTController;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.Version;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Performs authentication based on credentials being present in the HTTP request parameters. Also checks
 * API versions and license information.
 * <p/>
 * The username should be set in parameter "u", and the password should be set in parameter "p".
 * The REST protocol version should be set in parameter "v".
 * <p/>
 * The password can either be in plain text or be UTF-8 hexencoded preceded by "enc:".
 *
 * @author Sindre Mehus
 */
public class RESTRequestParameterProcessingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RESTRequestParameterProcessingFilter.class);

    private final JAXBWriter jaxbWriter = new JAXBWriter();
    private AuthenticationManager authenticationManager;
    private SecurityService securityService;
    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private ApplicationEventPublisher eventPublisher;

    private static RequestMatcher requiresAuthenticationRequestMatcher = new RegexRequestMatcher("/rest/.+",null);

    protected boolean requiresAuthentication(HttpServletRequest request,
                                             HttpServletResponse response) {
        return requiresAuthenticationRequestMatcher.matches(request);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("Can only process HttpServletRequest");
        }
        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("Can only process HttpServletResponse");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!requiresAuthentication(httpRequest, httpResponse)) {
            chain.doFilter(request, response);

            return;
        }


        String username = StringUtils.trimToNull(httpRequest.getParameter("u"));
        String password = decrypt(StringUtils.trimToNull(httpRequest.getParameter("p")));
        String salt = StringUtils.trimToNull(httpRequest.getParameter("s"));
        String token = StringUtils.trimToNull(httpRequest.getParameter("t"));
        String version = StringUtils.trimToNull(httpRequest.getParameter("v"));
        String client = StringUtils.trimToNull(httpRequest.getParameter("c"));

        SubsonicRESTController.ErrorCode errorCode = null;

        // The username and credentials parameters are not required if the user
        // was previously authenticated, for example using Basic Auth.
        boolean passwordOrTokenPresent = password != null || (salt != null && token != null);
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean missingCredentials = previousAuth == null && (username == null || !passwordOrTokenPresent);
        if (missingCredentials || version == null || client == null) {
            errorCode = SubsonicRESTController.ErrorCode.MISSING_PARAMETER;
        }

        if (errorCode == null) {
            errorCode = checkAPIVersion(version);
        }

        if (errorCode == null) {
            errorCode = authenticate(httpRequest, username, password, salt, token, previousAuth);
        }

        if (errorCode == null) {
            chain.doFilter(request, response);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
            sendErrorXml(httpRequest, httpResponse, errorCode);
        }
    }

    private SubsonicRESTController.ErrorCode checkAPIVersion(String version) {
        Version serverVersion = new Version(jaxbWriter.getRestProtocolVersion());
        Version clientVersion = new Version(version);

        if (serverVersion.getMajor() > clientVersion.getMajor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_CLIENT_TOO_OLD;
        } else if (serverVersion.getMajor() < clientVersion.getMajor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        } else if (serverVersion.getMinor() < clientVersion.getMinor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        }
        return null;
    }

    private SubsonicRESTController.ErrorCode authenticate(HttpServletRequest httpRequest, String username, String password, String salt, String token, Authentication previousAuth) {

        // Previously authenticated and username not overridden?
        if (username == null && previousAuth != null) {
            return null;
        }

        if (salt != null && token != null) {
            User user = securityService.getUserByName(username);
            if (user == null) {
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }
            String expectedToken = DigestUtils.md5Hex(user.getPassword() + salt);
            if (!expectedToken.equals(token)) {
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }

            password = user.getPassword();
        }

        if (password != null) {
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
            authRequest.setDetails(authenticationDetailsSource.buildDetails(httpRequest));
            try {
                Authentication authResult = authenticationManager.authenticate(authRequest);
                SecurityContextHolder.getContext().setAuthentication(authResult);
                return null;
            } catch (AuthenticationException x) {
                eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(authRequest, x));
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }
        }

        return SubsonicRESTController.ErrorCode.MISSING_PARAMETER;
    }

    public static String decrypt(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("enc:")) {
            return s;
        }
        try {
            return StringUtil.utf8HexDecode(s.substring(4));
        } catch (Exception e) {
            return s;
        }
    }

    private void sendErrorXml(HttpServletRequest request, HttpServletResponse response, SubsonicRESTController.ErrorCode errorCode) {
        try {
            jaxbWriter.writeErrorResponse(request, response, errorCode, errorCode.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to send error response.", e);
        }
    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }


    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public SecurityService getSecurityService() {
        return securityService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
