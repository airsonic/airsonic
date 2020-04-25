package org.airsonic.player.spring;

import org.airsonic.player.controller.PodcastController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.util.Properties;

@Configuration
public class ServletConfiguration implements WebMvcConfigurer {

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

    // Both setUseSuffixPatternMatch and favorPathExtension calls allow URLs
    // with extensions to be resolved to the same mapping as without extension.
    //
    // In Airsonic's case, this is necessary, because a lot of our mappings assume
    // this behavior (for example "/home.view" URL to a "/home" mapping, or the
    // entire Subsonic REST API controller).
    //
    // Starting from Spring Boot 2.0, this feature is not enabled by default anymore,
    // so we must enable it manually.
    //
    // See: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide#spring-mvc-path-matching-default-behavior-change

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(true);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true);
    }
}
