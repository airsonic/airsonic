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

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 3.5.
 *
 * @author Sindre Mehus
 */
public class Schema35 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema35.class);

    private static final String[] AVATARS = {
            "Formal", "Engineer", "Footballer", "Green-Boy",

            "Linux-Zealot", "Mac-Zealot", "Windows-Zealot", "Army-Officer", "Beatnik",
            "All-Caps", "Clown", "Commie-Pinko", "Forum-Flirt", "Gamer", "Hopelessly-Addicted",
            "Jekyll-And-Hyde", "Joker", "Lurker", "Moderator", "Newbie", "No-Dissent",
            "Performer", "Push-My-Button", "Ray-Of-Sunshine", "Red-Hot-Chili-Peppers-1",
            "Red-Hot-Chili-Peppers-2", "Red-Hot-Chili-Peppers-3", "Red-Hot-Chili-Peppers-4",
            "Ringmaster", "Rumor-Junkie", "Sozzled-Surfer", "Statistician", "Tech-Support",
            "The-Guru", "The-Referee", "Troll", "Uptight",

            "Fire-Guitar", "Drum", "Headphones", "Mic", "Turntable", "Vinyl",

            "Cool", "Laugh", "Study"
    };

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 11",Integer.class) == 0) {
            LOG.info("Updating database schema to version 11.");
            template.execute("insert into version values (11)");
        }

        if (!columnExists(template, "now_playing_allowed", "user_settings")) {
            LOG.info("Database column 'user_settings.now_playing_allowed' not found.  Creating it.");
            template.execute("alter table user_settings add now_playing_allowed boolean default true not null");
            LOG.info("Database column 'user_settings.now_playing_allowed' was added successfully.");
        }

        if (!columnExists(template, "web_player_default", "user_settings")) {
            LOG.info("Database column 'user_settings.web_player_default' not found.  Creating it.");
            template.execute("alter table user_settings add web_player_default boolean default false not null");
            LOG.info("Database column 'user_settings.web_player_default' was added successfully.");
        }

        if (template.queryForObject("select count(*) from role where id = 8",Integer.class) == 0) {
            LOG.info("Role 'stream' not found in database. Creating it.");
            template.execute("insert into role values (8, 'stream')");
            template.execute("insert into user_role select distinct u.username, 8 from user u");
            LOG.info("Role 'stream' was created successfully.");
        }

        if (!tableExists(template, "system_avatar")) {
            LOG.info("Database table 'system_avatar' not found.  Creating it.");
            template.execute("create table system_avatar (" +
                             "id identity," +
                             "name varchar," +
                             "created_date datetime not null," +
                             "mime_type varchar not null," +
                             "width int not null," +
                             "height int not null," +
                             "data binary not null)");
            LOG.info("Database table 'system_avatar' was created successfully.");
        }

        for (String avatar : AVATARS) {
            createAvatar(template, avatar);
        }

        if (!tableExists(template, "custom_avatar")) {
            LOG.info("Database table 'custom_avatar' not found.  Creating it.");
            template.execute("create table custom_avatar (" +
                             "id identity," +
                             "name varchar," +
                             "created_date datetime not null," +
                             "mime_type varchar not null," +
                             "width int not null," +
                             "height int not null," +
                             "data binary not null," +
                             "username varchar not null," +
                             "foreign key (username) references user(username) on delete cascade)");
            LOG.info("Database table 'custom_avatar' was created successfully.");
        }

        if (!columnExists(template, "avatar_scheme", "user_settings")) {
            LOG.info("Database column 'user_settings.avatar_scheme' not found.  Creating it.");
            template.execute("alter table user_settings add avatar_scheme varchar default 'NONE' not null");
            LOG.info("Database column 'user_settings.avatar_scheme' was added successfully.");
        }

        if (!columnExists(template, "system_avatar_id", "user_settings")) {
            LOG.info("Database column 'user_settings.system_avatar_id' not found.  Creating it.");
            template.execute("alter table user_settings add system_avatar_id int");
            template.execute("alter table user_settings add foreign key (system_avatar_id) references system_avatar(id)");
            LOG.info("Database column 'user_settings.system_avatar_id' was added successfully.");
        }

        if (!columnExists(template, "jukebox", "player")) {
             LOG.info("Database column 'player.jukebox' not found.  Creating it.");
             template.execute("alter table player add jukebox boolean default false not null");
             LOG.info("Database column 'player.jukebox' was added successfully.");
         }
     }

    private void createAvatar(JdbcTemplate template, String avatar) {
        if (template.queryForObject("select count(*) from system_avatar where name = ?", new Object[]{avatar},Integer.class) == 0) {

            InputStream in = null;
            try {
                in = getClass().getResourceAsStream("/org/libresonic/player/dao/schema/" + avatar + ".png");
                byte[] imageData = IOUtils.toByteArray(in);
                template.update("insert into system_avatar values (null, ?, ?, ?, ?, ?, ?)",
                                new Object[]{avatar, new Date(), "image/png", 48, 48, imageData});
                LOG.info("Created avatar '" + avatar + "'.");
            } catch (IOException x) {
                LOG.error("Failed to create avatar '" + avatar + "'.", x);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
}
