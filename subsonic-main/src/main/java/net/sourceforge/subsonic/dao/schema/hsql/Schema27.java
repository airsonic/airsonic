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

import net.sourceforge.subsonic.*;
import net.sourceforge.subsonic.dao.schema.Schema;

import org.springframework.jdbc.core.*;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Subsonic version 2.7.
 *
 * @author Sindre Mehus
 */
public class Schema27 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema27.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 3") == 0) {
            LOG.info("Updating database schema to version 3.");
            template.execute("insert into version values (3)");

            LOG.info("Converting database column 'music_file_info.path' to varchar_ignorecase.");
            template.execute("drop index idx_music_file_info_path");
            template.execute("alter table music_file_info alter column path varchar_ignorecase not null");
            template.execute("create index idx_music_file_info_path on music_file_info(path)");
            LOG.info("Database column 'music_file_info.path' was converted successfully.");
        }

        if (!columnExists(template, "bytes_streamed", "user")) {
            LOG.info("Database columns 'user.bytes_streamed/downloaded/uploaded' not found.  Creating them.");
            template.execute("alter table user add bytes_streamed bigint default 0 not null");
            template.execute("alter table user add bytes_downloaded bigint default 0 not null");
            template.execute("alter table user add bytes_uploaded bigint default 0 not null");
            LOG.info("Database columns 'user.bytes_streamed/downloaded/uploaded' were added successfully.");
        }
    }
}
