package org.airsonic.player.spring;

import liquibase.exception.DatabaseException;

public class HsqlDatabase extends liquibase.database.core.HsqlDatabase {

    @Override
    public boolean supportsSchemas() {
        try {
            if (getDatabaseMajorVersion() < 2) {
                return false;
            } else {
                return super.supportsSchemas();
            }
        } catch (DatabaseException e) {
            return false;
        }
    }
}
