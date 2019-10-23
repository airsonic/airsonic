package org.airsonic.player.spring;

import org.airsonic.player.controller.PodcastController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }
}
