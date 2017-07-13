package org.airsonic.player.util;

import org.junit.rules.ExternalResource;
import org.airsonic.player.TestCaseUtils;

public class HomeRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        super.before();
        System.setProperty("airsonic.home", TestCaseUtils.airsonicHomePathForTest());

        TestCaseUtils.cleanAirsonicHomeForTest();
    }
}
