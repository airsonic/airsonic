package org.libresonic.player.util;

import org.junit.rules.ExternalResource;
import org.libresonic.player.TestCaseUtils;

public class LibresonicHomeRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        super.before();
        System.setProperty("libresonic.home", TestCaseUtils.libresonicHomePathForTest());

        TestCaseUtils.cleanLibresonicHomeForTest();
    }
}
