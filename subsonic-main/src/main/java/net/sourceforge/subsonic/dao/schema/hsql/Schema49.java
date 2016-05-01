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
 * This class implements the database schema for Subsonic version 4.9.
 *
 * @author Sindre Mehus
 */
public class Schema49 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema49.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 21") == 0) {
            LOG.info("Updating database schema to version 21.");
            template.execute("insert into version values (21)");
        }

        if (!columnExists(template, "year", "album")) {
            LOG.info("Database column 'album.year' not found.  Creating it.");
            template.execute("alter table album add year int");
            LOG.info("Database column 'album.year' was added successfully.");
        }

        if (!columnExists(template, "genre", "album")) {
            LOG.info("Database column 'album.genre' not found.  Creating it.");
            template.execute("alter table album add genre varchar");
            LOG.info("Database column 'album.genre' was added successfully.");
        }

        if (!tableExists(template, "genre")) {
            LOG.info("Database table 'genre' not found.  Creating it.");
            template.execute("create table genre (" +
                    "name varchar not null," +
                    "song_count int not null)");

            LOG.info("Database table 'genre' was created successfully.");
        }

        if (!columnExists(template, "album_count", "genre")) {
            LOG.info("Database column 'genre.album_count' not found.  Creating it.");
            template.execute("alter table genre add album_count int default 0 not null");
            LOG.info("Database column 'genre.album_count' was added successfully.");
        }
    }
}