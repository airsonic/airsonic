package org.airsonic.player.util;

import org.airsonic.player.Application;
import org.junit.rules.ExternalResource;

public class MigrationConstantsRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        super.before();
        Application.setMigrationConstants();
    }
}
