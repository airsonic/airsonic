package org.airsonic.player.service;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LegacyDatabaseStartupTestCase {

    @ClassRule
    public static final HomeRule airsonicRule = new HomeRule();
    
    @BeforeClass
    public static void setupOnce() throws IOException {
        String homeParent = TestCaseUtils.airsonicHomePathForTest();
        File dbDirectory = new File(homeParent, "/db");
        FileUtils.forceMkdir(dbDirectory);
        org.airsonic.player.util.FileUtils.copyResourcesRecursively(LegacyDatabaseStartupTestCase.class.getResource("/db/pre-liquibase/db"), new File(homeParent));
    }
    
    @Test
    public void testStartup() {
        System.out.println("Successful startup");
    }

}
