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
package net.sourceforge.subsonic.dao.schema.hsql;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.schema.Schema;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Subsonic version 3.4.
 *
 * @author Sindre Mehus
 */
public class Schema34 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema34.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 10") == 0) {
            LOG.info("Updating database schema to version 10.");
            template.execute("insert into version values (10)");
        }

        if (!columnExists(template, "ldap_authenticated", "user")) {
            LOG.info("Database column 'user.ldap_authenticated' not found.  Creating it.");
            template.execute("alter table user add ldap_authenticated boolean default false not null");
            LOG.info("Database column 'user.ldap_authenticated' was added successfully.");
        }

        if (!columnExists(template, "party_mode_enabled", "user_settings")) {
            LOG.info("Database column 'user_settings.party_mode_enabled' not found.  Creating it.");
            template.execute("alter table user_settings add party_mode_enabled boolean default false not null");
            LOG.info("Database column 'user_settings.party_mode_enabled' was added successfully.");
        }
    }
}