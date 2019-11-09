package org.airsonic.player.spring;

import org.airsonic.player.controller.PodcastController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.util.Properties;

@Configuration
public class ServletConfiguration extends WebMvcConfigurerAdapter {
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean
    public SimpleUrlHandlerMapping urlMapping(PodcastController podcastController) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();

        // Default is intmax, so need to set a higher priority than
        // ResourceHttpRequestHandler/ResourceHandlerRegistry (which is intmax-1). Otherwise, that will
        // intercept every request before it gets here
        mapping.setOrder(Integer.MAX_VALUE - 2);

        mapping.setAlwaysUseFullPath(true);

        Properties properties = new Properties();
        properties.put("/podcast/**", podcastController);
        mapping.setMappings(properties);

        return mapping;
    }
}
