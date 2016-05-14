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
 * This class implementes the database schema for Libresonic version 3.3.
 *
 * @author Sindre Mehus
 */
public class Schema33 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema33.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 9") == 0) {
            LOG.info("Updating database schema to version 9.");
            template.execute("insert into version values (9)");
        }

        if (!columnExists(template, "client_side_playlist", "player")) {
            LOG.info("Database column 'player.client_side_playlist' not found.  Creating it.");
            template.execute("alter table player add client_side_playlist boolean default false not null");
            LOG.info("Database column 'player.client_side_playlist' was added successfully.");
        }
    }
}
