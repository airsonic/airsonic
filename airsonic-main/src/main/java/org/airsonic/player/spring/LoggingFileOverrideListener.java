package org.airsonic.player.spring;

import org.airsonic.player.service.SettingsService;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Collections;


public class LoggingFileOverrideListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        PropertySource ps = new MapPropertySource("LogFileLocationPS",
                Collections.singletonMap(LogFile.FILE_PROPERTY, SettingsService.getLogFile().getAbsolutePath()));
        event.getEnvironment().getPropertySources().addLast(ps);
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER - 1;
    }

}
