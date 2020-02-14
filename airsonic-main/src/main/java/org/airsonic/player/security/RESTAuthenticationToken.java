package org.airsonic.player.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class RESTAuthenticationToken extends UsernamePasswordAuthenticationToken {
    public RESTAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public RESTAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
