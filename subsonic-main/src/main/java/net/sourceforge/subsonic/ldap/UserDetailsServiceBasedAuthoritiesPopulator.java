/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.ldap;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.ldap.LdapDataAccessException;
import org.acegisecurity.providers.ldap.LdapAuthoritiesPopulator;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;

/**
 * An {@link LdapAuthoritiesPopulator} that retrieves the roles from the
 * database using the {@link UserDetailsService} instead of retrieving the roles
 * from LDAP. An instance of this class can be configured for the
 * {@link org.acegisecurity.providers.ldap.LdapAuthenticationProvider} when
 * authentication should be done using LDAP and authorization using the
 * information stored in the database.
 *
 * @author Thomas M. Hofmann
 */
public class UserDetailsServiceBasedAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    private UserDetailsService userDetailsService;

    public GrantedAuthority[] getGrantedAuthorities(LdapUserDetails userDetails) throws LdapDataAccessException {
        UserDetails details = userDetailsService.loadUserByUsername(userDetails.getUsername());
        return details.getAuthorities();
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
}