/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2018 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Database access for dealing with genres
 */
@Repository
public class GenreDao extends AbstractDao {

    private final RowMapper<Genre> genreMapper = new GenreMapper();

    public Genre getGenre(String name) {
        return queryOne("select id, name, song_count, album_count from genre where name = ?", genreMapper, name);
    }

    public Genre create(String name) {
        update("insert into genre(name, song_count, album_count) values(?, ?, ?)", name, 0, 0);
        return getGenre(name);
    }

    public void resetCounts() {
        update("delete from media_file_genre");
        update("delete from album_genre");
        update("update genre set song_count = 0, album_count = 0");
    }

    public void addGenre(MediaFile file, Genre genre) {
        if (addGenre("media_file", "song_count", file.getId(), genre.getId())) {
            genre.incrementSongCount();
        }
    }

    public void addGenre(Album album, Genre genre) {
        if (addGenre("album", "album_count", album.getId(), genre.getId())) {
            genre.incrementAlbumCount();
        }
    }

    public void clearGenres() {
        update("delete from media_file_genre");
        update("delete from album_genre");
        update("delete from genre");
    }

    private boolean addGenre(String type, String count_field, int type_id, int genre_id) {
        int count = queryForInt("select count(1) from " + type + "_genre where " + type + "_id = ? and genre_id = ?",
                0, type_id, genre_id);
        if (count == 0) {
            update("insert into " + type + "_genre(" + type + "_id, genre_id) values (?, ?)",
                    type_id, genre_id);
            update("update genre set " + count_field + " = " + count_field + " + 1 where id = ?", genre_id);
            return true;
        }
        else {
            return false;
        }
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        String sort_col = sortByAlbum ? "album_count" : "song_count";
        return query("select id, name, song_count, album_count from genre order by " + sort_col + " desc", genreMapper);
    }

    private static final class GenreMapper implements RowMapper<Genre> {
        @Override
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Genre(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getInt(3),
                    rs.getInt(4));
        }
    }
}
