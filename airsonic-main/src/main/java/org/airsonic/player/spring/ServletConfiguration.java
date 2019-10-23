package org.airsonic.player.spring;

import org.airsonic.player.controller.PodcastController;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.theme.CustomThemeResolver;
import org.airsonic.player.theme.CustomThemeSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.util.Properties;

@Configuration
public class ServletConfiguration {

    @Bean
    public SimpleUrlHandlerMapping podcastUrlMapping(PodcastController podcastController) {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setAlwaysUseFullPath(true);
        Properties properties = new Properties();
        properties.put("/podcast/**", podcastController);
        handlerMapping.setMappings(properties);
        return handlerMapping;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource resourceMessageSource = new ResourceBundleMessageSource();
        resourceMessageSource.setBasename("org.airsonic.player.i18n.ResourceBundle");
        return resourceMessageSource;
    }

    @Bean
    public CustomThemeSource themeSource(SettingsService settingsService) {
        CustomThemeSource customThemeSource = new CustomThemeSource();
        customThemeSource.setBasenamePrefix("org.airsonic.player.theme.");
        customThemeSource.setSettingsService(settingsService);
        return customThemeSource;
    }

    @Bean
    public ThemeResolver themeResolver(SecurityService securityService, SettingsService settingsService) {
        CustomThemeResolver customThemeResolver = new CustomThemeResolver();
        customThemeResolver.setSecurityService(securityService);
        customThemeResolver.setSettingsService(settingsService);
        return customThemeResolver;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }
}
