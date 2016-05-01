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

import org.springframework.jdbc.core.JdbcTemplate;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.schema.Schema;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Subsonic version 4.6.
 *
 * @author Sindre Mehus
 */
public class Schema46 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema46.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 19") == 0) {
            LOG.info("Updating database schema to version 19.");
            template.execute("insert into version values (19)");
        }

        if (!tableExists(template, "transcoding2")) {
            LOG.info("Database table 'transcoding2' not found.  Creating it.");
            template.execute("create table transcoding2 (" +
                             "id identity," +
                             "name varchar not null," +
                             "source_formats varchar not null," +
                             "target_format varchar not null," +
                             "step1 varchar not null," +
                             "step2 varchar," +
                             "step3 varchar)");

            template.execute("insert into transcoding2(name, source_formats, target_format, step1) values('mp3 audio'," +
                    "'ogg oga aac m4a flac wav wma aif aiff ape mpc shn', 'mp3', " +
                    "'ffmpeg -i %s -ab %bk -v 0 -f mp3 -')");

            template.execute("insert into transcoding2(name, source_formats, target_format, step1) values('flv/h264 video', " +
                    "'avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts', 'flv', " +
                    "'ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -')");

            LOG.info("Database table 'transcoding2' was created successfully.");
        }

        if (!tableExists(template, "player_transcoding2")) {
            LOG.info("Database table 'player_transcoding2' not found.  Creating it.");
            template.execute("create table player_transcoding2 (" +
                             "player_id int not null," +
                             "transcoding_id int not null," +
                             "primary key (player_id, transcoding_id)," +
                             "foreign key (player_id) references player(id) on delete cascade," +
                             "foreign key (transcoding_id) references transcoding2(id) on delete cascade)");

            template.execute("insert into player_transcoding2(player_id, transcoding_id) " +
                    "select distinct p.id, t.id from player p, transcoding2 t");

            LOG.info("Database table 'player_transcoding2' was created successfully.");
        }

        if (!columnExists(template, "default_active", "transcoding2")) {
            LOG.info("Database column 'transcoding2.default_active' not found.  Creating it.");
            template.execute("alter table transcoding2 add default_active boolean default true not null");
            LOG.info("Database column 'transcoding2.default_active' was added successfully.");
        }
    }

}