package org.libresonic.player.service;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.libresonic.player.TestCaseUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;

public class StartupTestCase extends TestCase {

    private static String baseResources = "/org/libresonic/player/service/mediaScannerServiceTestCase/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testStartup() throws Exception {
        String homeParent = TestCaseUtils.libresonicHomePathForTest();
        System.setProperty("libresonic.home", TestCaseUtils.libresonicHomePathForTest());
        TestCaseUtils.cleanLibresonicHomeForTest();
        File dbDirectory = new File(homeParent, "/db");
        FileUtils.forceMkdir(dbDirectory);
        org.libresonic.player.util.FileUtils.copyResourcesRecursively(getClass().getResource("/db/pre-liquibase/db"), new File(homeParent));

        // load spring context
        ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    }

}
