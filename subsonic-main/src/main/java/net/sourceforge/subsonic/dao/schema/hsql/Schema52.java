/*
 * This file is part of Subsonic.
 *
 *  Subsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Subsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.dao.schema.hsql;

import org.springframework.jdbc.core.JdbcTemplate;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.schema.Schema;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Subsonic version 5.2.
 *
 * @author Sindre Mehus
 */
public class Schema52 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema52.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 24") == 0) {
            LOG.info("Updating database schema to version 24.");
            template.execute("insert into version values (24)");
        }

        if (!tableExists(template, "music_folder_user")) {
            LOG.info("Database table 'music_folder_user' not found.  Creating it.");
            template.execute("create table music_folder_user (" +
                             "music_folder_id int not null," +
                             "username varchar not null, " +
                             "foreign key (username) references user(username) on delete cascade, " +
                             "foreign key (music_folder_id) references music_folder(id) on delete cascade)");
            template.execute("create index idx_music_folder_user_username on music_folder_user(username)");
            template.execute("insert into music_folder_user select music_folder.id, user.username from music_folder, user");
            LOG.info("Database table 'music_folder_user' was created successfully.");
        }

        if (!columnExists(template, "folder_id", "album")) {
            LOG.info("Database column 'album.folder_id' not found.  Creating it.");
            template.execute("alter table album add folder_id int");
            LOG.info("Database column 'album.folder_id' was added successfully.");
        }

        if (!tableExists(template, "play_queue")) {
            LOG.info("Database table 'play_queue' not found.  Creating it.");
            template.execute("create table play_queue (" +
                             "id identity," +
                             "username varchar not null," +
                             "current int," +
                             "position_millis bigint," +
                             "changed datetime not null," +
                             "changed_by varchar not null," +
                             "foreign key (username) references user(username) on delete cascade)");
            LOG.info("Database table 'play_queue' was created successfully.");
        }

        if (!tableExists(template, "play_queue_file")) {
            LOG.info("Database table 'play_queue_file' not found.  Creating it.");
            template.execute("create cached table play_queue_file (" +
                             "id identity," +
                             "play_queue_id int not null," +
                             "media_file_id int not null," +
                             "foreign key (play_queue_id) references play_queue(id) on delete cascade," +
                             "foreign key (media_file_id) references media_file(id) on delete cascade)");

            LOG.info("Database table 'play_queue_file' was created successfully.");
        }
    }
}