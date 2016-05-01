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
 * This class implements the database schema for Subsonic version 4.5.
 *
 * @author Sindre Mehus
 */
public class Schema45 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema45.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 18") == 0) {
            LOG.info("Updating database schema to version 18.");
            template.execute("insert into version values (18)");
        }

        if (template.queryForInt("select count(*) from role where id = 11") == 0) {
            LOG.info("Role 'share' not found in database. Creating it.");
            template.execute("insert into role values (11, 'share')");
            template.execute("insert into user_role " +
                             "select distinct u.username, 11 from user u, user_role ur " +
                             "where u.username = ur.username and ur.role_id = 1");
            LOG.info("Role 'share' was created successfully.");
        }

        if (!tableExists(template, "share")) {
            LOG.info("Table 'share' not found in database. Creating it.");
            template.execute("create cached table share (" +
                    "id identity," +
                    "name varchar not null," +
                    "description varchar," +
                    "username varchar not null," +
                    "created datetime not null," +
                    "expires datetime," +
                    "last_visited datetime," +
                    "visit_count int default 0 not null," +
                    "unique (name)," +
                    "foreign key (username) references user(username) on delete cascade)");
            template.execute("create index idx_share_name on share(name)");

            LOG.info("Table 'share' was created successfully.");
            LOG.info("Table 'share_file' not found in database. Creating it.");
            template.execute("create cached table share_file (" +
                    "id identity," +
                    "share_id int not null," +
                    "path varchar not null," +
                    "foreign key (share_id) references share(id) on delete cascade)");
            LOG.info("Table 'share_file' was created successfully.");
        }
    }
}