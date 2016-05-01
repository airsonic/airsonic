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
package net.sourceforge.subsonic.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.InternetRadio;

/**
 * Provides database services for internet radio.
 *
 * @author Sindre Mehus
 */
public class InternetRadioDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(InternetRadioDao.class);
    private static final String COLUMNS = "id, name, stream_url, homepage_url, enabled, changed";
    private final InternetRadioRowMapper rowMapper = new InternetRadioRowMapper();

    /**
     * Returns all internet radio stations.
     *
     * @return Possibly empty list of all internet radio stations.
     */
    public List<InternetRadio> getAllInternetRadios() {
        String sql = "select " + COLUMNS + " from internet_radio";
        return query(sql, rowMapper);
    }

    /**
     * Creates a new internet radio station.
     *
     * @param radio The internet radio station to create.
     */
    public void createInternetRadio(InternetRadio radio) {
        String sql = "insert into internet_radio (" + COLUMNS + ") values (null, ?, ?, ?, ?, ?)";
        update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(), radio.getChanged());
        LOG.info("Created internet radio station " + radio.getName());
    }

    /**
     * Deletes the internet radio station with the given ID.
     *
     * @param id The internet radio station ID.
     */
    public void deleteInternetRadio(Integer id) {
        String sql = "delete from internet_radio where id=?";
        update(sql, id);
        LOG.info("Deleted internet radio station with ID " + id);
    }

    /**
     * Updates the given internet radio station.
     *
     * @param radio The internet radio station to update.
     */
    public void updateInternetRadio(InternetRadio radio) {
        String sql = "update internet_radio set name=?, stream_url=?, homepage_url=?, enabled=?, changed=? where id=?";
        update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(), radio.getChanged(), radio.getId());
    }

    private static class InternetRadioRowMapper implements ParameterizedRowMapper<InternetRadio> {
        public InternetRadio mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InternetRadio(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5), rs.getTimestamp(6));
        }
    }

}
