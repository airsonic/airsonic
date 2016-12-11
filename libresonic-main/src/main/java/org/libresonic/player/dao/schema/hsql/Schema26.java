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

import org.libresonic.player.*;
import org.libresonic.player.dao.schema.Schema;
import org.libresonic.player.util.Util;
import org.springframework.jdbc.core.*;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 2.6.
 *
 * @author Sindre Mehus
 */
public class Schema26 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema26.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 2",Integer.class) == 0) {
            LOG.info("Updating database schema to version 2.");
            template.execute("insert into version values (2)");
        }

        if (!tableExists(template, "music_folder")) {
            LOG.info("Database table 'music_folder' not found.  Creating it.");
            template.execute("create table music_folder (" +
                             "id identity," +
                             "path varchar not null," +
                             "name varchar not null," +
                             "enabled boolean not null)");
            template.execute("insert into music_folder values (null, '" + Util.getDefaultMusicFolder() + "', 'Music', true)");
            LOG.info("Database table 'music_folder' was created successfully.");
        }

        if (!tableExists(template, "music_file_info")) {
            LOG.info("Database table 'music_file_info' not found.  Creating it.");
            template.execute("create cached table music_file_info (" +
                             "id identity," +
                             "path varchar not null," +
                             "rating int," +
                             "comment varchar," +
                             "play_count int," +
                             "last_played datetime)");
            template.execute("create index idx_music_file_info_path on music_file_info(path)");
            LOG.info("Database table 'music_file_info' was created successfully.");
        }

        if (!tableExists(template, "internet_radio")) {
            LOG.info("Database table 'internet_radio' not found.  Creating it.");
            template.execute("create table internet_radio (" +
                             "id identity," +
                             "name varchar not null," +
                             "stream_url varchar not null," +
                             "homepage_url varchar," +
                             "enabled boolean not null)");
            LOG.info("Database table 'internet_radio' was created successfully.");
        }

        if (!tableExists(template, "player")) {
            LOG.info("Database table 'player' not found.  Creating it.");
            template.execute("create table player (" +
                             "id int not null," +
                             "name varchar," +
                             "type varchar," +
                             "username varchar," +
                             "ip_address varchar," +
                             "auto_control_enabled boolean not null," +
                             "last_seen datetime," +
                             "cover_art_scheme varchar not null," +
                             "transcode_scheme varchar not null," +
                             "primary key (id))");
            LOG.info("Database table 'player' was created successfully.");
        }

        // 'dynamic_ip' was added in 2.6.beta2
        if (!columnExists(template, "dynamic_ip", "player")) {
            LOG.info("Database column 'player.dynamic_ip' not found.  Creating it.");
            template.execute("alter table player " +
                             "add dynamic_ip boolean default true not null");
            LOG.info("Database column 'player.dynamic_ip' was added successfully.");
        }

        if (template.queryForObject("select count(*) from role where id = 6",Integer.class) == 0) {
            LOG.info("Role 'comment' not found in database. Creating it.");
            template.execute("insert into role values (6, 'comment')");
            template.execute("insert into user_role " +
                             "select distinct u.username, 6 from user u, user_role ur " +
                             "where u.username = ur.username and ur.role_id in (1, 5)");
            LOG.info("Role 'comment' was created successfully.");
        }
    }

}
