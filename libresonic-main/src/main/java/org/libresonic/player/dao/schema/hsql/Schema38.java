/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Libresonic version 3.8.
 *
 * @author Sindre Mehus
 */
public class Schema38 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema38.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 14",Integer.class) == 0) {
            LOG.info("Updating database schema to version 14.");
            template.execute("insert into version values (14)");
        }

        if (!columnExists(template, "client_id", "player")) {
            LOG.info("Database column 'player.client_id' not found.  Creating it.");
            template.execute("alter table player add client_id varchar");
            LOG.info("Database column 'player.client_id' was added successfully.");
        }

        if (!columnExists(template, "show_chat", "user_settings")) {
            LOG.info("Database column 'user_settings.show_chat' not found.  Creating it.");
            template.execute("alter table user_settings add show_chat boolean default true not null");
            LOG.info("Database column 'user_settings.show_chat' was added successfully.");
        }
    }
}