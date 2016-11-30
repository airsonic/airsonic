package org.libresonic.player.spring;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.libresonic.player.service.ApacheCommonsConfigurationService;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class AdditionalPropertySourceConfigurer implements
        ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    public void initialize(ConfigurableWebApplicationContext ctx) {

        ApacheCommonsConfigurationService configurationService = new ApacheCommonsConfigurationService();
        ImmutableConfiguration snapshot = configurationService.getImmutableSnapshot();

        PropertySource ps = new DatasourceProfileActivatorPropertySource(new CommonsConfigurationPropertySource(
                "libresonic-pre-init-configs",
                snapshot));
        ctx.getEnvironment().getPropertySources().addLast(ps);
    }
}
