package org.libresonic.player.service;

import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.libresonic.player.TestCaseUtils;
import org.libresonic.player.util.LibresonicHomeRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;

@ContextConfiguration(locations = {
        "/applicationContext-service.xml",
        "/applicationContext-cache.xml",
        "/applicationContext-testdb.xml",
        "/applicationContext-mockSonos.xml"})
public class LegacyDatabaseStartupTestCase {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        LibresonicHomeRule libresonicRule = new LibresonicHomeRule() {
            @Override
            protected void before() throws Throwable {
                super.before();
                String homeParent = TestCaseUtils.libresonicHomePathForTest();
                System.setProperty("libresonic.home", TestCaseUtils.libresonicHomePathForTest());
                TestCaseUtils.cleanLibresonicHomeForTest();
                File dbDirectory = new File(homeParent, "/db");
                FileUtils.forceMkdir(dbDirectory);
                org.libresonic.player.util.FileUtils.copyResourcesRecursively(getClass().getResource("/db/pre-liquibase/db"), new File(homeParent));
            }
        };

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return libresonicRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void testStartup() throws Exception {
        System.out.println("Successful startup");
    }

}
