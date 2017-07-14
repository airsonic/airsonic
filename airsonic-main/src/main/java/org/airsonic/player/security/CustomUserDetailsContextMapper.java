/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.airsonic.player.security;

import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class CustomUserDetailsContextMapper implements UserDetailsContextMapper {
    // ~ Instance fields
    // ================================================================================================

    private final Logger logger = LoggerFactory.getLogger(CustomUserDetailsContextMapper.class);
    private String passwordAttributeName = "userPassword";

    @Autowired
    SecurityService securityService;

    @Autowired
    SettingsService settingsService;

    // ~ Methods
    // ========================================================================================================

    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        String dn = ctx.getNameInNamespace();

        logger.debug("Mapping user details from context with DN: " + dn);

        // User must be defined in Airsonic, unless auto-shadowing is enabled.
        User user = securityService.getUserByName(username, false);
        if (user == null && !settingsService.isLdapAutoShadowing()) {
            throw new BadCredentialsException("User does not exist.");
        }

        if (user == null) {
            User newUser = new User(username, "", null, true, 0L, 0L, 0L);
            newUser.setStreamRole(true);
            newUser.setSettingsRole(true);
            securityService.createUser(newUser);
            logger.info("Created local user '" + username + "' for DN " + dn);
            user = securityService.getUserByName(username, false);
        }

        // LDAP authentication must be enabled for the given user.
        if (!user.isLdapAuthenticated()) {
            throw new BadCredentialsException("LDAP authentication disabled for user.");
        }

        LdapUserDetailsImpl.Essence essence = new LdapUserDetailsImpl.Essence();
        essence.setDn(dn);

        Object passwordValue = ctx.getObjectAttribute(passwordAttributeName);

        if (passwordValue != null) {
            essence.setPassword(mapPassword(passwordValue));
        }

        essence.setUsername(user.getUsername());

        // Add the supplied authorities
        for (GrantedAuthority authority : securityService.getGrantedAuthorities(user.getUsername())) {
            essence.addAuthority(authority);
        }

        // Check for PPolicy data

        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ctx
                .getObjectAttribute(PasswordPolicyControl.OID);

        if (ppolicy != null) {
            essence.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
            essence.setGraceLoginsRemaining(ppolicy.getGraceLoginsRemaining());
        }

        return essence.createUserDetails();

    }

    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException(
                "LdapUserDetailsMapper only supports reading from a context. Please"
                        + "use a subclass if mapUserToContext() is required.");
    }

    /**
     * Extension point to allow customized creation of the user's password from the
     * attribute stored in the directory.
     *
     * @param passwordValue the value of the password attribute
     * @return a String representation of the password.
     */
    protected String mapPassword(Object passwordValue) {

        if (!(passwordValue instanceof String)) {
            // Assume it's binary
            passwordValue = new String((byte[]) passwordValue);
        }

        return (String) passwordValue;

    }
}
