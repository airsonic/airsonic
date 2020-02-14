package org.airsonic.player.security;

import org.airsonic.player.service.SecurityService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

public class RESTAuthenticationProvider extends DaoAuthenticationProvider {

    public RESTAuthenticationProvider() {
        super();
        setPasswordEncoder(NoOpPasswordEncoder.getInstance());
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (!SecurityService.UserDetail.class.isAssignableFrom(userDetails.getClass())) {
            throw new InternalAuthenticationServiceException("Invalid user details class");
        }
        String restToken = ((SecurityService.UserDetail)userDetails).getRestToken();
        if (restToken == null) {
            throw new BadCredentialsException("Empty REST token");
        }
        User restUser = new org.springframework.security.core.userdetails.User(
                userDetails.getUsername(),
                restToken,
                userDetails.isEnabled(),
                userDetails.isAccountNonExpired(),
                userDetails.isCredentialsNonExpired(),
                userDetails.isAccountNonLocked(),
                userDetails.getAuthorities());
        super.additionalAuthenticationChecks(restUser, authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RESTAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
