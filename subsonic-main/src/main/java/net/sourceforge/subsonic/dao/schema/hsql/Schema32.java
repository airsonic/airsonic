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
 * This class implementes the database schema for Subsonic version 3.2.
 *
 * @author Sindre Mehus
 */
public class Schema32 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema32.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 8") == 0) {
            LOG.info("Updating database schema to version 8.");
            template.execute("insert into version values (8)");
        }

        if (!columnExists(template, "show_now_playing", "user_settings")) {
            LOG.info("Database column 'user_settings.show_now_playing' not found.  Creating it.");
            template.execute("alter table user_settings add show_now_playing boolean default true not null");
            LOG.info("Database column 'user_settings.show_now_playing' was added successfully.");
        }

        if (!columnExists(template, "selected_music_folder_id", "user_settings")) {
            LOG.info("Database column 'user_settings.selected_music_folder_id' not found.  Creating it.");
            template.execute("alter table user_settings add selected_music_folder_id int default -1 not null");
            LOG.info("Database column 'user_settings.selected_music_folder_id' was added successfully.");
        }

        if (!tableExists(template, "podcast_channel")) {
            LOG.info("Database table 'podcast_channel' not found.  Creating it.");
            template.execute("create table podcast_channel (" +
                             "id identity," +
                             "url varchar not null," +
                             "title varchar," +
                             "description varchar," +
                             "status varchar not null," +
                             "error_message varchar)");
            LOG.info("Database table 'podcast_channel' was created successfully.");
        }

        if (!tableExists(template, "podcast_episode")) {
            LOG.info("Database table 'podcast_episode' not found.  Creating it.");
            template.execute("create table podcast_episode (" +
                             "id identity," +
                             "channel_id int not null," +
                             "url varchar not null," +
                             "path varchar," +
                             "title varchar," +
                             "description varchar," +
                             "publish_date datetime," +
                             "duration varchar," +
                             "bytes_total bigint," +
                             "bytes_downloaded bigint," +
                             "status varchar not null," +
                             "error_message varchar," +
                             "foreign key (channel_id) references podcast_channel(id) on delete cascade)");
            LOG.info("Database table 'podcast_episode' was created successfully.");
        }

        if (template.queryForInt("select count(*) from role where id = 7") == 0) {
            LOG.info("Role 'podcast' not found in database. Creating it.");
            template.execute("insert into role values (7, 'podcast')");
            template.execute("insert into user_role " +
                             "select distinct u.username, 7 from user u, user_role ur " +
                             "where u.username = ur.username and ur.role_id = 1");
            LOG.info("Role 'podcast' was created successfully.");
        }

    }
}
