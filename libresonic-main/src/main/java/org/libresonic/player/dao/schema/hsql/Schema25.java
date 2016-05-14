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

import org.springframework.jdbc.core.JdbcTemplate;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 2.5.
 *
 * @author Sindre Mehus
 */
public class Schema25 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema25.class);

    public void execute(JdbcTemplate template) {
        if (!tableExists(template, "version")) {

            // Increase data file limit. See http://www.hsqldb.org/doc/guide/ch04.html
            template.execute("set property \"hsqldb.cache_file_scale\" 8");

            LOG.info("Database table 'version' not found.  Creating it.");
            template.execute("create table version (version int not null)");
            template.execute("insert into version values (1)");
            LOG.info("Database table 'version' was created successfully.");
        }

        if (!tableExists(template, "role")) {
            LOG.info("Database table 'role' not found.  Creating it.");
            template.execute("create table role (" +
                             "id int not null," +
                             "name varchar not null," +
                             "primary key (id))");
            template.execute("insert into role values (1, 'admin')");
            template.execute("insert into role values (2, 'download')");
            template.execute("insert into role values (3, 'upload')");
            template.execute("insert into role values (4, 'playlist')");
            template.execute("insert into role values (5, 'coverart')");
            LOG.info("Database table 'role' was created successfully.");
        }

        if (!tableExists(template, "user")) {
            LOG.info("Database table 'user' not found.  Creating it.");
            template.execute("create table user (" +
                             "username varchar not null," +
                             "password varchar not null," +
                             "primary key (username))");
            template.execute("insert into user values ('admin', 'admin')");
            LOG.info("Database table 'user' was created successfully.");
        }

        if (!tableExists(template, "user_role")) {
            LOG.info("Database table 'user_role' not found.  Creating it.");
            template.execute("create table user_role (" +
                             "username varchar not null," +
                             "role_id int not null," +
                             "primary key (username, role_id)," +
                             "foreign key (username) references user(username)," +
                             "foreign key (role_id) references role(id))");
            template.execute("insert into user_role values ('admin', 1)");
            template.execute("insert into user_role values ('admin', 2)");
            template.execute("insert into user_role values ('admin', 3)");
            template.execute("insert into user_role values ('admin', 4)");
            template.execute("insert into user_role values ('admin', 5)");
            LOG.info("Database table 'user_role' was created successfully.");
        }
    }
}
