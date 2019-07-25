package org.airsonic.player.dao;

import org.airsonic.player.domain.SonosLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
@Transactional
public class SonosLinkDao extends AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(SonosLinkDao.class);
    private static final String COLUMNS = "username, householdid, linkcode";

    private SonosLinkRowMapper rowMapper = new SonosLinkRowMapper();

    @Transactional(propagation = Propagation.SUPPORTS)
    public SonosLink findByLinkcode(String linkcode) {
        String sql = "select " + COLUMNS + " from sonoslink where linkcode=?";
        return queryOne(sql, rowMapper, linkcode);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void create(SonosLink sonosLink) {
        String sql = "insert into sonoslink (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ')';
        update(sql, sonosLink.getUsername(), sonosLink.getHouseholdid(), sonosLink.getLinkcode());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void removeAll(){
        String sql = "delete from sonoslink;";
        update(sql);
    }

    private static class SonosLinkRowMapper implements RowMapper<SonosLink>{
        public SonosLink mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SonosLink(rs.getString(1),
                    rs.getString(2),
                    rs.getString(3));
        }
    }
}
