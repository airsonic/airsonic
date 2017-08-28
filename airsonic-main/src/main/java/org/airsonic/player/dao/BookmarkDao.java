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

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.airsonic.player.domain.Bookmark;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides database services for media file bookmarks.
 *
 * @author Sindre Mehus
 */
@Repository
public class BookmarkDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "media_file_id, position_millis, username, comment, created, changed";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private BookmarkRowMapper bookmarkRowMapper = new BookmarkRowMapper();

    /**
     * Returns all bookmarks.
     *
     * @return Possibly empty list of all bookmarks.
     */
    public List<Bookmark> getBookmarks() {
        String sql = "select " + QUERY_COLUMNS + " from bookmark";
        return query(sql, bookmarkRowMapper);
    }

    /**
     * Returns all bookmarks for a given user.
     *
     * @return Possibly empty list of all bookmarks for the user.
     */
    public List<Bookmark> getBookmarks(String username) {
        String sql = "select " + QUERY_COLUMNS + " from bookmark where username=?";
        return query(sql, bookmarkRowMapper, username);
    }

    /**
     * Creates or updates a bookmark.  If created, the ID of the bookmark will be set by this method.
     */
    @Transactional
    public void createOrUpdateBookmark(Bookmark bookmark) {
        int n = update("update bookmark set position_millis=?, comment=?, changed=? where media_file_id=? and username=?",
                bookmark.getPositionMillis(), bookmark.getComment(), bookmark.getChanged(), bookmark.getMediaFileId(), bookmark.getUsername());

        if (n == 0) {
            update("insert into bookmark (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                   bookmark.getMediaFileId(), bookmark.getPositionMillis(), bookmark.getUsername(), bookmark.getComment(),
                   bookmark.getCreated(), bookmark.getChanged());
            int id = queryForInt("select id from bookmark where media_file_id=? and username=?", 0, bookmark.getMediaFileId(), bookmark.getUsername());
            bookmark.setId(id);
        }
    }

    /**
     * Deletes the bookmark for the given username and media file.
     */
    @Transactional
    public void deleteBookmark(String username, int mediaFileId) {
        update("delete from bookmark where username=? and media_file_id=?", username, mediaFileId);
    }

    private static class BookmarkRowMapper implements RowMapper<Bookmark> {
        public Bookmark mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Bookmark(rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4),
                    rs.getString(5), rs.getTimestamp(6), rs.getTimestamp(7));
        }
    }
}
