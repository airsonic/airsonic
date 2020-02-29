package org.airsonic.player.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JWTAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private String requestedPath;
    private final String username;

    public JWTAuthenticationToken(Collection<? extends GrantedAuthority> authorities, String token, String requestedPath, String username) {
        super(authorities);
        this.token = token;
        this.requestedPath = requestedPath;
        this.username = username;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public String getRequestedPath() {
        return requestedPath;
    }
}
