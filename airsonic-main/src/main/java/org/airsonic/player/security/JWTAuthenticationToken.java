package org.airsonic.player.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JWTAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private String requestedPath;

    public static final String USERNAME_ANONYMOUS = "anonymous";

    public JWTAuthenticationToken(Collection<? extends GrantedAuthority> authorities, String token, String requestedPath) {
        super(authorities);
        this.token = token;
        this.requestedPath = requestedPath;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return USERNAME_ANONYMOUS;
    }

    public String getRequestedPath() {
        return requestedPath;
    }
}
