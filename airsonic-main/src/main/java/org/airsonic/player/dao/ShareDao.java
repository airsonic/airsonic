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

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Share;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides database services for shared media.
 *
 * @author Sindre Mehus
 */
@Repository
public class ShareDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "name, description, username, created, expires, last_visited, visit_count";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private ShareRowMapper shareRowMapper = new ShareRowMapper();
    private ShareFileRowMapper shareFileRowMapper = new ShareFileRowMapper();

    /**
     * Creates a new share.
     *
     * @param share The share to create.  The ID of the share will be set by this method.
     */
    @Transactional
    public void createShare(Share share) {
        String sql = "insert into share (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")";
        update(sql, share.getName(), share.getDescription(), share.getUsername(), share.getCreated(),
                share.getExpires(), share.getLastVisited(), share.getVisitCount());

        int id = getJdbcTemplate().queryForObject("select max(id) from share", Integer.class);
        share.setId(id);
    }

    /**
     * Returns all shares.
     *
     * @return Possibly empty list of all shares.
     */
    public List<Share> getAllShares() {
        String sql = "select " + QUERY_COLUMNS + " from share";
        return query(sql, shareRowMapper);
    }

    public Share getShareByName(String shareName) {
        String sql = "select " + QUERY_COLUMNS + " from share where name=?";
        return queryOne(sql, shareRowMapper, shareName);
    }

    public Share getShareById(int id) {
        String sql = "select " + QUERY_COLUMNS + " from share where id=?";
        return queryOne(sql, shareRowMapper, id);
    }

    /**
     * Updates the given share.
     *
     * @param share The share to update.
     */
    public void updateShare(Share share) {
        String sql = "update share set name=?, description=?, username=?, created=?, expires=?, last_visited=?, visit_count=? where id=?";
        update(sql, share.getName(), share.getDescription(), share.getUsername(), share.getCreated(), share.getExpires(),
                share.getLastVisited(), share.getVisitCount(), share.getId());
    }

    /**
     * Creates shared files.
     *
     * @param shareId The share ID.
     * @param paths   Paths of the files to share.
     */
    public void createSharedFiles(int shareId, String... paths) {
        String sql = "insert into share_file (share_id, path) values (?, ?)";
        for (String path : paths) {
            update(sql, shareId, path);
        }
    }

    /**
     * Returns files for a share.
     *
     * @param shareId The ID of the share.
     * @return The paths of the shared files.
     */
    public List<String> getSharedFiles(final int shareId, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("shareId", shareId);
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQuery("select share_file.path from share_file, media_file where share_id = :shareId and " +
                          "share_file.path = media_file.path and media_file.present and media_file.folder in (:folders)",
                          shareFileRowMapper, args);
    }

    /**
     * Deletes the share with the given ID.
     *
     * @param id The ID of the share to delete.
     */
    public void deleteShare(Integer id) {
        update("delete from share where id=?", id);
    }

    private static class ShareRowMapper implements RowMapper<Share> {
        public Share mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Share(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5),
                    rs.getTimestamp(6), rs.getTimestamp(7), rs.getInt(8));
        }
    }

    private static class ShareFileRowMapper implements RowMapper<String> {
        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getString(1);
        }

    }
}
