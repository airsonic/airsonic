package org.airsonic.player.util;

import org.airsonic.player.TestCaseUtils;
import org.junit.rules.ExternalResource;

public class HomeRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        super.before();
        System.setProperty("airsonic.home", TestCaseUtils.airsonicHomePathForTest());

        TestCaseUtils.cleanAirsonicHomeForTest();
    }
}
