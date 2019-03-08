package org.airsonic.player.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class TerminateBean {

    private static final Logger LOG = LoggerFactory.getLogger(TerminateBean.class);

    @Autowired
    private ApplicationContext context;

    @PreDestroy
    public void onDestroy() {
        Connection conn = null;
        try {
            // Connect to the database and retrieve the db name and version
            JdbcTemplate jdbcTemplate = new JdbcTemplate(this.context.getBean(DataSource.class));
            conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            DatabaseMetaData meta = conn.getMetaData();
            String productName = meta.getDatabaseProductName();
            int productVersion = meta.getDatabaseMajorVersion();

            // Properly shutdown HSQLDB databases
            if (productName.equals("HSQL Database Engine") && (productVersion == 1 || productVersion == 2)) {
                LOG.info("Database shutdown in progress...");
                conn.setAutoCommit(true);
                jdbcTemplate.execute("SHUTDOWN");
                LOG.info("Database shutdown complete.");
            }

        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            try {
                if(conn != null)
                    conn.close();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
