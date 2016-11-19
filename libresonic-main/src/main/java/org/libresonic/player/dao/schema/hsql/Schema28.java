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

import org.springframework.jdbc.core.*;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 2.8.
 *
 * @author Sindre Mehus
 */
public class Schema28 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema28.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 4") == 0) {
            LOG.info("Updating database schema to version 4.");
            template.execute("insert into version values (4)");
        }

        if (!tableExists(template, "user_settings")) {
            LOG.info("Database table 'user_settings' not found.  Creating it.");
            template.execute("create table user_settings (" +
                             "username varchar not null," +
                             "locale varchar," +
                             "theme_id varchar," +
                             "final_version_notification boolean default true not null," +
                             "beta_version_notification boolean default false not null," +
                             "main_caption_cutoff int default 35 not null," +
                             "main_track_number boolean default true not null," +
                             "main_artist boolean default true not null," +
                             "main_album boolean default false not null," +
                             "main_genre boolean default false not null," +
                             "main_year boolean default false not null," +
                             "main_bit_rate boolean default false not null," +
                             "main_duration boolean default true not null," +
                             "main_format boolean default false not null," +
                             "main_file_size boolean default false not null," +
                             "playlist_caption_cutoff int default 35 not null," +
                             "playlist_track_number boolean default false not null," +
                             "playlist_artist boolean default true not null," +
                             "playlist_album boolean default true not null," +
                             "playlist_genre boolean default false not null," +
                             "playlist_year boolean default true not null," +
                             "playlist_bit_rate boolean default false not null," +
                             "playlist_duration boolean default true not null," +
                             "playlist_format boolean default true not null," +
                             "playlist_file_size boolean default true not null," +
                             "primary key (username)," +
                             "foreign key (username) references user(username) on delete cascade)");
            LOG.info("Database table 'user_settings' was created successfully.");
        }

        if (!tableExists(template, "transcoding")) {
            LOG.info("Database table 'transcoding' not found.  Creating it.");
            template.execute("create table transcoding (" +
                             "id identity," +
                             "name varchar not null," +
                             "source_format varchar not null," +
                             "target_format varchar not null," +
                             "step1 varchar not null," +
                             "step2 varchar," +
                             "step3 varchar," +
                             "enabled boolean not null)");

            template.execute("insert into transcoding values(null,'wav > mp3', 'wav', 'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'flac > mp3','flac','mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'ogg > mp3' ,'ogg' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'wma > mp3' ,'wma' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'m4a > mp3' ,'m4a' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,false)");
            template.execute("insert into transcoding values(null,'aac > mp3' ,'aac' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,false)");
            template.execute("insert into transcoding values(null,'ape > mp3' ,'ape' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'mpc > mp3' ,'mpc' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'mv > mp3'  ,'mv'  ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");
            template.execute("insert into transcoding values(null,'shn > mp3' ,'shn' ,'mp3','ffmpeg -i %s -v 0 -f wav -','lame -b %b --tt %t --ta %a --tl %l -S --resample 44.1 - -',null,true)");

            LOG.info("Database table 'transcoding' was created successfully.");
        }

        if (!tableExists(template, "player_transcoding")) {
            LOG.info("Database table 'player_transcoding' not found.  Creating it.");
            template.execute("create table player_transcoding (" +
                             "player_id int not null," +
                             "transcoding_id int not null," +
                             "primary key (player_id, transcoding_id)," +
                             "foreign key (player_id) references player(id) on delete cascade," +
                             "foreign key (transcoding_id) references transcoding(id) on delete cascade)");
            LOG.info("Database table 'player_transcoding' was created successfully.");
        }
    }
}
