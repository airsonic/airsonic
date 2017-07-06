package org.airsonic.player.spring;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.springframework.core.env.PropertySource;

public class CommonsConfigurationPropertySource extends PropertySource {

    private final ImmutableConfiguration configuration;

    public CommonsConfigurationPropertySource(String name, ImmutableConfiguration configuration) {
        super(name);
        this.configuration = configuration;
    }

    @Override
    public Object getProperty(String s) {
        return configuration.getProperty(s);
    }
}
