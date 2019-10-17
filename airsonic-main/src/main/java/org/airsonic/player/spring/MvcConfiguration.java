package org.airsonic.player.spring;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.theme.CustomThemeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.util.Properties;

@Configuration
@EnableWebMvc
public class MvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private SettingsService settingsService;

    @Bean
    public ViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/jsp/");
        bean.setSuffix(".jsp");
        return bean;
    }

    @Bean(UiApplicationContextUtils.THEME_SOURCE_BEAN_NAME)
    public ThemeSource themeSource() {
        CustomThemeSource themeSource = new org.airsonic.player.theme.CustomThemeSource();
        themeSource.setBasenamePrefix("org.airsonic.player.theme.");
        themeSource.setSettingsService(settingsService);
        return themeSource;
    }

    @Bean
    public SimpleUrlHandlerMapping podcastServletMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        // Default is intmax, so need to set a higher priority than
        // ResourceHttpRequestHandler below (which is intmax-1). Otherwise, that will
        // intercept every request before it gets here
        mapping.setOrder(Integer.MAX_VALUE - 2);

        Properties urlProperties = new Properties();
        urlProperties.put("/podcast/**", "podcastController");
        mapping.setMappings(urlProperties);

        mapping.setAlwaysUseFullPath(true);

        return mapping;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
    }
}
