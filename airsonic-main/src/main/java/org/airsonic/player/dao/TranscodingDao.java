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

import org.airsonic.player.domain.Transcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides database services for transcoding configurations.
 *
 * @author Sindre Mehus
 */
@Repository
public class TranscodingDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodingDao.class);
    private static final String INSERT_COLUMNS = "name, source_formats, target_format, step1, step2, step3, default_active";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private TranscodingRowMapper rowMapper = new TranscodingRowMapper();

    /**
     * Returns all transcodings.
     *
     * @return Possibly empty list of all transcodings.
     */
    public List<Transcoding> getAllTranscodings() {
        String sql = "select " + QUERY_COLUMNS + " from transcoding2";
        return query(sql, rowMapper);
    }

    /**
     * Returns all active transcodings for the given player.
     *
     * @param playerId The player ID.
     * @return All active transcodings for the player.
     */
    public List<Transcoding> getTranscodingsForPlayer(Integer playerId) {
        String sql = "select " + QUERY_COLUMNS + " from transcoding2, player_transcoding2 " +
                     "where player_transcoding2.player_id = ? " +
                     "and   player_transcoding2.transcoding_id = transcoding2.id";
        return query(sql, rowMapper, playerId);
    }

    /**
     * Sets the list of active transcodings for the given player.
     *
     * @param playerId       The player ID.
     * @param transcodingIds ID's of the active transcodings.
     */
    public void setTranscodingsForPlayer(Integer playerId, int[] transcodingIds) {
        update("delete from player_transcoding2 where player_id = ?", playerId);
        String sql = "insert into player_transcoding2(player_id, transcoding_id) values (?, ?)";
        for (int transcodingId : transcodingIds) {
            update(sql, playerId, transcodingId);
        }
    }

    /**
     * Creates a new transcoding.
     *
     * @param transcoding The transcoding to create.
     */
    @Transactional
    public void createTranscoding(Transcoding transcoding) {
        Integer existingMax = getJdbcTemplate().queryForObject("select max(id) from transcoding2", Integer.class);
        if(existingMax == null) {
            existingMax = 0;
        }
        transcoding.setId(existingMax + 1);
        String sql = "insert into transcoding2 (" + QUERY_COLUMNS + ") values (" + questionMarks(QUERY_COLUMNS) + ")";
        update(sql, transcoding.getId(), transcoding.getName(), transcoding.getSourceFormats(),
                transcoding.getTargetFormat(), transcoding.getStep1(),
                transcoding.getStep2(), transcoding.getStep3(), transcoding.isDefaultActive());
        LOG.info("Created transcoding " + transcoding.getName());
    }

    /**
     * Deletes the transcoding with the given ID.
     *
     * @param id The transcoding ID.
     */
    public void deleteTranscoding(Integer id) {
        String sql = "delete from transcoding2 where id=?";
        update(sql, id);
        LOG.info("Deleted transcoding with ID " + id);
    }

    /**
     * Updates the given transcoding.
     *
     * @param transcoding The transcoding to update.
     */
    public void updateTranscoding(Transcoding transcoding) {
        String sql = "update transcoding2 set name=?, source_formats=?, target_format=?, " +
                "step1=?, step2=?, step3=?, default_active=? where id=?";
        update(sql, transcoding.getName(), transcoding.getSourceFormats(),
                transcoding.getTargetFormat(), transcoding.getStep1(), transcoding.getStep2(),
                transcoding.getStep3(), transcoding.isDefaultActive(), transcoding.getId());
    }

    private static class TranscodingRowMapper implements RowMapper<Transcoding> {
        public Transcoding mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Transcoding(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7), rs.getBoolean(8));
        }
    }
}
