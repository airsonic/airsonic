package org.airsonic.player.spring;

import com.google.common.collect.Lists;
import org.airsonic.player.service.ApacheCommonsConfigurationService;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.util.List;

public class CustomPropertySourceConfigurer implements
        ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    public static final String DATASOURCE_CONFIG_TYPE = "DatabaseConfigType";

    public void initialize(ConfigurableWebApplicationContext ctx) {

        ApacheCommonsConfigurationService configurationService = new ApacheCommonsConfigurationService();
        ImmutableConfiguration snapshot = configurationService.getImmutableSnapshot();

        PropertySource ps = new CommonsConfigurationPropertySource("airsonic-pre-init-configs", snapshot);


        ctx.getEnvironment().getPropertySources().addLast(ps);

        addDataSourceProfile(ctx);
    }

    private void addDataSourceProfile(ConfigurableWebApplicationContext ctx) {
        DataSourceConfigType dataSourceConfigType;
        String rawType = ctx.getEnvironment().getProperty(DATASOURCE_CONFIG_TYPE);
        if(StringUtils.isNotBlank(rawType)) {
            dataSourceConfigType = DataSourceConfigType.valueOf(StringUtils.upperCase(rawType));
        } else {
            dataSourceConfigType = DataSourceConfigType.LEGACY;
        }
        String dataSourceTypeProfile = StringUtils.lowerCase(dataSourceConfigType.name());
        List<String> existingProfiles = Lists.newArrayList(ctx.getEnvironment().getActiveProfiles());
        existingProfiles.add(dataSourceTypeProfile);
        ctx.getEnvironment().setActiveProfiles(existingProfiles.toArray(new String[0]));
    }
}
