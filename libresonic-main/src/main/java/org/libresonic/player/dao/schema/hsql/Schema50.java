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

import org.springframework.jdbc.core.JdbcTemplate;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Libresonic version 5.0.
 *
 * @author Sindre Mehus
 */
public class Schema50 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema50.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 22") == 0) {
            LOG.info("Updating database schema to version 22.");
            template.execute("insert into version values (22)");

            template.execute("insert into transcoding2(name, source_formats, target_format, step1, default_active) values('mkv video', " +
                    "'avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts', 'mkv', " +
                    "'ffmpeg -ss %o -i %s -c:v libx264 -preset superfast -b:v %bk -c:a libvorbis -f matroska -threads 0 -', 'true')");

            template.execute("insert into player_transcoding2(player_id, transcoding_id) " +
                    "select distinct p.id, t.id from player p, transcoding2 t where t.name='mkv video'");
            LOG.info("Added mkv transcoding.");
        }

        if (!columnExists(template, "song_notification", "user_settings")) {
            LOG.info("Database column 'user_settings.song_notification' not found.  Creating it.");
            template.execute("alter table user_settings add song_notification boolean default true not null");
            LOG.info("Database column 'user_settings.song_notification' was added successfully.");
        }

        // Added in 5.0.beta2
        if (template.queryForInt("select count(*) from version where version = 23") == 0) {
            LOG.info("Updating database schema to version 23.");
            template.execute("insert into version values (23)");
            template.execute("update transcoding2 set step1='ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -' where name='mp3 audio'");
        }
    }
}