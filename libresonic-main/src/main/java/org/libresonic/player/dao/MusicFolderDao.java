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
package org.libresonic.player.dao;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.MusicFolder;
import org.springframework.jdbc.core.RowMapper;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides database services for music folders.
 *
 * @author Sindre Mehus
 */
public class MusicFolderDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(MusicFolderDao.class);
    private static final String COLUMNS = "id, path, name, enabled, changed";
    private final MusicFolderRowMapper rowMapper = new MusicFolderRowMapper();

    /**
     * Returns all music folders.
     *
     * @return Possibly empty list of all music folders.
     */
    public List<MusicFolder> getAllMusicFolders() {
        String sql = "select " + COLUMNS + " from music_folder";
        return query(sql, rowMapper);
    }

    /**
     * Creates a new music folder.
     *
     * @param musicFolder The music folder to create.
     */
    public void createMusicFolder(MusicFolder musicFolder) {
        String sql = "insert into music_folder (" + COLUMNS + ") values (null, ?, ?, ?, ?)";
        update(sql, musicFolder.getPath(), musicFolder.getName(), musicFolder.isEnabled(), musicFolder.getChanged());

        Integer id = queryForInt("select max(id) from music_folder", 0);
        update("insert into music_folder_user (music_folder_id, username) select ?, username from user", id);
        LOG.info("Created music folder " + musicFolder.getPath());
    }

    /**
     * Deletes the music folder with the given ID.
     *
     * @param id The music folder ID.
     */
    public void deleteMusicFolder(Integer id) {
        String sql = "delete from music_folder where id=?";
        update(sql, id);
        LOG.info("Deleted music folder with ID " + id);
    }

    /**
     * Updates the given music folder.
     *
     * @param musicFolder The music folder to update.
     */
    public void updateMusicFolder(MusicFolder musicFolder) {
        String sql = "update music_folder set path=?, name=?, enabled=?, changed=? where id=?";
        update(sql, musicFolder.getPath().getPath(), musicFolder.getName(),
               musicFolder.isEnabled(), musicFolder.getChanged(), musicFolder.getId());
    }

    public List<MusicFolder> getMusicFoldersForUser(String username) {
        String sql = "select " + prefix(COLUMNS, "music_folder") + " from music_folder, music_folder_user " +
                     "where music_folder.id = music_folder_user.music_folder_id and music_folder_user.username = ?";
        return query(sql, rowMapper, username);
    }

    public void setMusicFoldersForUser(String username, List<Integer> musicFolderIds) {
        update("delete from music_folder_user where username = ?", username);
        for (Integer musicFolderId : musicFolderIds) {
            update("insert into music_folder_user(music_folder_id, username) values (?, ?)", musicFolderId, username);
        }
    }

    private static class MusicFolderRowMapper implements RowMapper<MusicFolder> {
        public MusicFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MusicFolder(rs.getInt(1), new File(rs.getString(2)), rs.getString(3), rs.getBoolean(4), rs.getTimestamp(5));
        }
    }

}
