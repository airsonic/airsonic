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
 * This class implements the database schema for Subsonic version 4.7.
 *
 * @author Sindre Mehus
 */
public class Schema47 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema47.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 20") == 0) {
            LOG.info("Updating database schema to version 20.");
            template.execute("insert into version values (20)");
        }

        if (!tableExists(template, "media_file")) {
            LOG.info("Database table 'media_file' not found.  Creating it.");
            template.execute("create cached table media_file (" +
                    "id identity," +
                    "path varchar not null," +
                    "folder varchar," +
                    "type varchar not null," +
                    "format varchar," +
                    "title varchar," +
                    "album varchar," +
                    "artist varchar," +
                    "album_artist varchar," +
                    "disc_number int," +
                    "track_number int," +
                    "year int," +
                    "genre varchar," +
                    "bit_rate int," +
                    "variable_bit_rate boolean not null," +
                    "duration_seconds int," +
                    "file_size bigint," +
                    "width int," +
                    "height int," +
                    "cover_art_path varchar," +
                    "parent_path varchar," +
                    "play_count int not null," +
                    "last_played datetime," +
                    "comment varchar," +
                    "created datetime not null," +
                    "changed datetime not null," +
                    "last_scanned datetime not null," +
                    "children_last_updated datetime not null," +
                    "present boolean not null," +
                    "version int not null," +
                    "unique (path))");

            template.execute("create index idx_media_file_path on media_file(path)");
            template.execute("create index idx_media_file_parent_path on media_file(parent_path)");
            template.execute("create index idx_media_file_type on media_file(type)");
            template.execute("create index idx_media_file_album on media_file(album)");
            template.execute("create index idx_media_file_artist on media_file(artist)");
            template.execute("create index idx_media_file_album_artist on media_file(album_artist)");
            template.execute("create index idx_media_file_present on media_file(present)");
            template.execute("create index idx_media_file_genre on media_file(genre)");
            template.execute("create index idx_media_file_play_count on media_file(play_count)");
            template.execute("create index idx_media_file_created on media_file(created)");
            template.execute("create index idx_media_file_last_played on media_file(last_played)");

            LOG.info("Database table 'media_file' was created successfully.");
        }

        if (!tableExists(template, "artist")) {
            LOG.info("Database table 'artist' not found.  Creating it.");
            template.execute("create cached table artist (" +
                    "id identity," +
                    "name varchar not null," +
                    "cover_art_path varchar," +
                    "album_count int default 0 not null," +
                    "last_scanned datetime not null," +
                    "present boolean not null," +
                    "unique (name))");

            template.execute("create index idx_artist_name on artist(name)");
            template.execute("create index idx_artist_present on artist(present)");

            LOG.info("Database table 'artist' was created successfully.");
        }

        if (!tableExists(template, "album")) {
            LOG.info("Database table 'album' not found.  Creating it.");
            template.execute("create cached table album (" +
                    "id identity," +
                    "path varchar not null," +
                    "name varchar not null," +
                    "artist varchar not null," +
                    "song_count int default 0 not null," +
                    "duration_seconds int default 0 not null," +
                    "cover_art_path varchar," +
                    "play_count int default 0 not null," +
                    "last_played datetime," +
                    "comment varchar," +
                    "created datetime not null," +
                    "last_scanned datetime not null," +
                    "present boolean not null," +
                    "unique (artist, name))");

            template.execute("create index idx_album_artist_name on album(artist, name)");
            template.execute("create index idx_album_play_count on album(play_count)");
            template.execute("create index idx_album_last_played on album(last_played)");
            template.execute("create index idx_album_present on album(present)");

            LOG.info("Database table 'album' was created successfully.");
        }

        // Added in 4.7.beta3
        if (!rowExists(template, "table_name='ALBUM' and column_name='NAME' and ordinal_position=1",
                "information_schema.system_indexinfo")) {
            template.execute("create index idx_album_name on album(name)");
        }

        if (!tableExists(template, "starred_media_file")) {
            LOG.info("Database table 'starred_media_file' not found.  Creating it.");
            template.execute("create table starred_media_file (" +
                    "id identity," +
                    "media_file_id int not null," +
                    "username varchar not null," +
                    "created datetime not null," +
                    "foreign key (media_file_id) references media_file(id) on delete cascade,"+
                    "foreign key (username) references user(username) on delete cascade," +
                    "unique (media_file_id, username))");

            template.execute("create index idx_starred_media_file_media_file_id on starred_media_file(media_file_id)");
            template.execute("create index idx_starred_media_file_username on starred_media_file(username)");

            LOG.info("Database table 'starred_media_file' was created successfully.");
        }

        if (!tableExists(template, "starred_album")) {
            LOG.info("Database table 'starred_album' not found.  Creating it.");
            template.execute("create table starred_album (" +
                    "id identity," +
                    "album_id int not null," +
                    "username varchar not null," +
                    "created datetime not null," +
                    "foreign key (album_id) references album(id) on delete cascade," +
                    "foreign key (username) references user(username) on delete cascade," +
                    "unique (album_id, username))");

            template.execute("create index idx_starred_album_album_id on starred_album(album_id)");
            template.execute("create index idx_starred_album_username on starred_album(username)");

            LOG.info("Database table 'starred_album' was created successfully.");
        }

        if (!tableExists(template, "starred_artist")) {
            LOG.info("Database table 'starred_artist' not found.  Creating it.");
            template.execute("create table starred_artist (" +
                    "id identity," +
                    "artist_id int not null," +
                    "username varchar not null," +
                    "created datetime not null," +
                    "foreign key (artist_id) references artist(id) on delete cascade,"+
                    "foreign key (username) references user(username) on delete cascade," +
                    "unique (artist_id, username))");

            template.execute("create index idx_starred_artist_artist_id on starred_artist(artist_id)");
            template.execute("create index idx_starred_artist_username on starred_artist(username)");

            LOG.info("Database table 'starred_artist' was created successfully.");
        }

        if (!tableExists(template, "playlist")) {
            LOG.info("Database table 'playlist' not found.  Creating it.");
            template.execute("create table playlist (" +
                    "id identity," +
                    "username varchar not null," +
                    "is_public boolean not null," +
                    "name varchar not null," +
                    "comment varchar," +
                    "file_count int default 0 not null," +
                    "duration_seconds int default 0 not null," +
                    "created datetime not null," +
                    "changed datetime not null," +
                    "foreign key (username) references user(username) on delete cascade)");

            LOG.info("Database table 'playlist' was created successfully.");
        }

        if (!columnExists(template, "imported_from", "playlist")) {
            LOG.info("Database column 'playlist.imported_from' not found.  Creating it.");
            template.execute("alter table playlist add imported_from varchar");
            LOG.info("Database column 'playlist.imported_from' was added successfully.");
        }

        if (!tableExists(template, "playlist_file")) {
            LOG.info("Database table 'playlist_file' not found.  Creating it.");
            template.execute("create cached table playlist_file (" +
                    "id identity," +
                    "playlist_id int not null," +
                    "media_file_id int not null," +
                    "foreign key (playlist_id) references playlist(id) on delete cascade," +
                    "foreign key (media_file_id) references media_file(id) on delete cascade)");

            LOG.info("Database table 'playlist_file' was created successfully.");
        }

        if (!tableExists(template, "playlist_user")) {
            LOG.info("Database table 'playlist_user' not found.  Creating it.");
            template.execute("create table playlist_user (" +
                    "id identity," +
                    "playlist_id int not null," +
                    "username varchar not null," +
                    "unique(playlist_id, username)," +
                    "foreign key (playlist_id) references playlist(id) on delete cascade," +
                    "foreign key (username) references user(username) on delete cascade)");

            LOG.info("Database table 'playlist_user' was created successfully.");
        }

        if (!tableExists(template, "bookmark")) {
            LOG.info("Database table 'bookmark' not found.  Creating it.");
            template.execute("create table bookmark (" +
                    "id identity," +
                    "media_file_id int not null," +
                    "position_millis bigint not null," +
                    "username varchar not null," +
                    "comment varchar," +
                    "created datetime not null," +
                    "changed datetime not null," +
                    "foreign key (media_file_id) references media_file(id) on delete cascade,"+
                    "foreign key (username) references user(username) on delete cascade," +
                    "unique (media_file_id, username))");

            template.execute("create index idx_bookmark_media_file_id on bookmark(media_file_id)");
            template.execute("create index idx_bookmark_username on bookmark(username)");

            LOG.info("Database table 'bookmark' was created successfully.");
        }
    }
}