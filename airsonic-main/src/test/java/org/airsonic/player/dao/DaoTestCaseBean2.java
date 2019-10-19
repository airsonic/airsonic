package org.airsonic.player.dao;

import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.airsonic.player.util.MigrationConstantsRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "legacy" })
public class DaoTestCaseBean2 {
    @ClassRule
    public static TestRule rules = RuleChain.outerRule(new HomeRule()).around(new MigrationConstantsRule());

    @Autowired
    GenericDaoHelper genericDaoHelper;

    JdbcTemplate getJdbcTemplate() {
        return genericDaoHelper.getJdbcTemplate();
    }
}
