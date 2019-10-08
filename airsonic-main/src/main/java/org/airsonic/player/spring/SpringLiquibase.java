package org.airsonic.player.spring;

import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;
import liquibase.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

public class SpringLiquibase extends liquibase.integration.spring.SpringLiquibase {
    private static final Logger logger = LoggerFactory.getLogger(SpringLiquibase.class);

    @Override
    public void afterPropertiesSet() throws LiquibaseException {
        logger.trace("Starting Liquibase Update");
        try {
            super.afterPropertiesSet();
        } catch (Exception e) {
            logger.error("===============================================");
            logger.error("An exception occurred during database migration");
            logger.error("A rollback file has been generated at " + rollbackFile);
            logger.error("Execute it within your database to rollback any changes");
            logger.error("The exception is as follows\n", e);
            logger.error("===============================================");
            throw(e);
        }
    }

    @Override
    protected Database createDatabase(
            Connection c, ResourceAccessor resourceAccessor
    ) throws DatabaseException {
        DatabaseConnection liquibaseConnection;
        if (c == null) {
            log.warning("Null connection returned by liquibase datasource. Using offline unknown database");
            liquibaseConnection = new OfflineConnection("offline:unknown", resourceAccessor);

        } else {
            liquibaseConnection = new JdbcConnection(c);
        }
        DatabaseFactory factory = DatabaseFactory.getInstance();
        overrideHsqlDbImplementation(factory);
        Database database = factory.findCorrectDatabaseImplementation(liquibaseConnection);
        if (StringUtils.trimToNull(this.defaultSchema) != null) {
            database.setDefaultSchemaName(this.defaultSchema);
        }
        return database;
    }

    private void overrideHsqlDbImplementation(DatabaseFactory factory) {
        List<Database> implementedDatabases = factory.getImplementedDatabases();
        factory.clearRegistry();
        removeCurrentHsqlDb(implementedDatabases);
        implementedDatabases.forEach(factory::register);
        factory.register(new HsqlDatabase());
    }

    private void removeCurrentHsqlDb(List<Database> implementedDatabases) {
        implementedDatabases.removeIf(db -> db instanceof liquibase.database.core.HsqlDatabase);
    }
}
