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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Provides database services for playlists.
 *
 * @author Sindre Mehus
 */
@Repository
public class PlaylistDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistDao.class);
    private static final String INSERT_COLUMNS = "username, is_public, name, comment, file_count, duration_seconds, " +
                                                "created, changed, imported_from";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private final RowMapper rowMapper = new PlaylistMapper();

    public List<Playlist> getReadablePlaylistsForUser(String username) {

        List<Playlist> result1 = getWritablePlaylistsForUser(username);
        List<Playlist> result2 = query("select " + QUERY_COLUMNS + " from playlist where is_public", rowMapper);
        List<Playlist> result3 = query("select " + prefix(QUERY_COLUMNS, "playlist") + " from playlist, playlist_user where " +
                                       "playlist.id = playlist_user.playlist_id and " +
                                       "playlist.username != ? and " +
                                       "playlist_user.username = ?", rowMapper, username, username);

        // Put in sorted map to avoid duplicates.
        SortedMap<Integer, Playlist> map = new TreeMap<Integer, Playlist>();
        for (Playlist playlist : result1) {
            map.put(playlist.getId(), playlist);
        }
        for (Playlist playlist : result2) {
            map.put(playlist.getId(), playlist);
        }
        for (Playlist playlist : result3) {
            map.put(playlist.getId(), playlist);
        }
        return new ArrayList<Playlist>(map.values());
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {
        return query("select " + QUERY_COLUMNS + " from playlist where username=?", rowMapper, username);
    }

    public Playlist getPlaylist(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from playlist where id=?", rowMapper, id);
    }

    public List<Playlist> getAllPlaylists() {
        return query("select " + QUERY_COLUMNS + " from playlist", rowMapper);
    }

    @Transactional
    public void createPlaylist(Playlist playlist) {
        update("insert into playlist(" + INSERT_COLUMNS + ") values(" + questionMarks(INSERT_COLUMNS) + ")",
                playlist.getUsername(), playlist.isShared(), playlist.getName(), playlist.getComment(),
                0, 0, playlist.getCreated(), playlist.getChanged(), playlist.getImportedFrom());

        int id = queryForInt("select max(id) from playlist", 0);
        playlist.setId(id);
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        update("delete from playlist_file where playlist_id=?", id);
        int duration = 0;
        for (MediaFile file : files) {
            update("insert into playlist_file (playlist_id, media_file_id) values (?, ?)", id, file.getId());
            if (file.getDurationSeconds() != null) {
                duration += file.getDurationSeconds();
            }
        }
        update("update playlist set file_count=?, duration_seconds=?, changed=? where id=?", files.size(), duration, new Date(), id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return queryForStrings("select username from playlist_user where playlist_id=?", playlistId);
    }

    public void addPlaylistUser(int playlistId, String username) {
        if (!getPlaylistUsers(playlistId).contains(username)) {
            update("insert into playlist_user(playlist_id,username) values (?,?)", playlistId, username);
        }
    }

    public void deletePlaylistUser(int playlistId, String username) {
        update("delete from playlist_user where playlist_id=? and username=?", playlistId, username);
    }

    @Transactional
    public void deletePlaylist(int id) {
        update("delete from playlist where id=?", id);
    }

    public void updatePlaylist(Playlist playlist) {
        update("update playlist set username=?, is_public=?, name=?, comment=?, changed=?, imported_from=? where id=?",
                playlist.getUsername(), playlist.isShared(), playlist.getName(), playlist.getComment(),
                new Date(), playlist.getImportedFrom(), playlist.getId());
    }

    private static class PlaylistMapper implements RowMapper<Playlist> {
        public Playlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Playlist(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getBoolean(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getTimestamp(8),
                    rs.getTimestamp(9),
                    rs.getString(10));
        }
    }
}
