package org.airsonic.player.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Special Dao Helper with additional features for managing the legacy embedded HSQL database.
 */
public class LegacyHsqlDaoHelper extends GenericDaoHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlDaoHelper.class);

    public LegacyHsqlDaoHelper(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void checkpoint() {
        // HSQLDB (at least version 1) does not handle automatic checkpoints very well by default.
        // This makes sure the temporary log is actually written to more persistent storage.
        Instant start = Instant.now();
        LOG.debug("Database checkpoint in progress...");
        getJdbcTemplate().execute("CHECKPOINT DEFRAG");
        LOG.debug("Database checkpoint complete in {}s.", ChronoUnit.SECONDS.between(start, Instant.now()));
    }

    /**
     * Shutdown the embedded HSQLDB database. After this has run, the database cannot be accessed again from the same DataSource.
     */
    private void shutdownHsqldbDatabase() {
        Instant start = Instant.now();
        try {
            LOG.debug("Database shutdown in progress...");
            JdbcTemplate jdbcTemplate = getJdbcTemplate();
            try (Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource())) {
                jdbcTemplate.execute("SHUTDOWN");
            }
            LOG.debug("Database shutdown complete in {}s.", ChronoUnit.SECONDS.between(start, Instant.now()));

        } catch (SQLException e) {
            LOG.error("Database shutdown failed", e);
        }
    }

    @PreDestroy
    public void onDestroy() {
        shutdownHsqldbDatabase();
    }
}
