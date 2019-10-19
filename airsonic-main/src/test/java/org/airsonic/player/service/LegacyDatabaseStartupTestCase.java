package org.airsonic.player.service;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MigrationConstantsRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import javax.sql.DataSource;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "legacy" })
public class LegacyDatabaseStartupTestCase {
    @ClassRule
    public static TestRule rules = RuleChain.outerRule(new HomeRule()).around(new MigrationConstantsRule());

    @BeforeClass
    public static void setupOnce() throws Exception {
        String homeParent = TestCaseUtils.airsonicHomePathForTest();
        File dbDirectory = new File(homeParent, "/db");
        FileUtils.forceMkdir(dbDirectory);
        org.airsonic.player.util.FileUtils.copyResourcesRecursively(
                LegacyDatabaseStartupTestCase.class.getResource("/db/pre-liquibase/db"), new File(homeParent));

        // have to change the url here because old db files are libresonic
        System.setProperty("DatabaseConfigEmbedUrl",
                SettingsService.getDefaultJDBCUrl().replaceAll("airsonic$", "libresonic"));
    }

    @Autowired
    private DataSource datasource;

    @Test
    public void testStartup() {
        assertThat(datasource).isNotNull();
    }

}
