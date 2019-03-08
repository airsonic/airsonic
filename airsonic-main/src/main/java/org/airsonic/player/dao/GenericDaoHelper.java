package org.airsonic.player.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class GenericDaoHelper implements DaoHelper {

    private static final Logger LOG = LoggerFactory.getLogger(GenericDaoHelper.class);

    final JdbcTemplate jdbcTemplate;

    final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    final DataSource dataSource;

    public GenericDaoHelper(
            DataSource dataSource
    ) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean checkpoint() {

        try {
            Connection conn = DataSourceUtils.getConnection(getJdbcTemplate().getDataSource());
            DatabaseMetaData meta = conn.getMetaData();
            String productName = meta.getDatabaseProductName();
            int productVersion = meta.getDatabaseMajorVersion();

            // HSQLDB (at least version 1) does not handle automatic checkpoints very well by default.
            // This makes sure the temporary log is actually written to more persistent storage.
            if (productName.equals("HSQL Database Engine") && (productVersion == 1 || productVersion == 2)) {
                LOG.info("Performing database checkpoint");
                getJdbcTemplate().execute("CHECKPOINT DEFRAG");
                return true;
            } else {
                LOG.debug("Database checkpoint not implemented for '" + productName + "'");
                return false;
            }
        }

        // Since this method is a best-effort operation, we don't want to show
        // a message if the checkpoint failed ; just assume the operation is
        // unsupported.
        catch(java.sql.SQLException e) {
            LOG.debug("An exception occurred during database checkpoint: " + e.toString());
        }

        return false;
    }
}
