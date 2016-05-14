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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 3.1.
 *
 * @author Sindre Mehus
 */
public class Schema31 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema31.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 7") == 0) {
            LOG.info("Updating database schema to version 7.");
            template.execute("insert into version values (7)");
        }

        if (!columnExists(template, "enabled", "music_file_info")) {
            LOG.info("Database column 'music_file_info.enabled' not found.  Creating it.");
            template.execute("alter table music_file_info add enabled boolean default true not null");
            LOG.info("Database column 'music_file_info.enabled' was added successfully.");
        }

        if (!columnExists(template, "default_active", "transcoding")) {
            LOG.info("Database column 'transcoding.default_active' not found.  Creating it.");
            template.execute("alter table transcoding add default_active boolean default true not null");
            LOG.info("Database column 'transcoding.default_active' was added successfully.");
        }
    }
}
