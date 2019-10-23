package org.airsonic.player.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.annotation.PreDestroy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Special Dao Helper with additional features for managing the legacy embedded HSQL database.
 */
@Configuration("daoHelper")
@Profile("legacy")
public class LegacyHsqlDaoHelper extends GenericDaoHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlDaoHelper.class);

    @Override
    public void checkpoint() {
        // HSQLDB (at least version 1) does not handle automatic checkpoints very well by default.
        // This makes sure the temporary log is actually written to more persistent storage.
        LOG.debug("Database checkpoint in progress...");
        getJdbcTemplate().execute("CHECKPOINT DEFRAG");
        LOG.debug("Database checkpoint complete.");
    }

    @PreDestroy
    public void onDestroy() {
        Connection conn = null;
        try {
            // Properly shutdown the embedded HSQLDB database.
            LOG.debug("Database shutdown in progress...");
            JdbcTemplate jdbcTemplate = getJdbcTemplate();
            conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            conn.setAutoCommit(true);
            jdbcTemplate.execute("SHUTDOWN");
            LOG.debug("Database shutdown complete.");

        } catch (SQLException e) {
            LOG.error("Database shutdown failed: " + e);
            e.printStackTrace();

        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
