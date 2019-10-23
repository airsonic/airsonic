package org.airsonic.player.spring;

import org.airsonic.player.controller.PodcastController;
import org.airsonic.player.filter.BootstrapVerificationFilter;
import org.airsonic.player.filter.MetricsFilter;
import org.airsonic.player.filter.ParameterDecodingFilter;
import org.airsonic.player.filter.RESTFilter;
import org.airsonic.player.filter.RequestEncodingFilter;
import org.airsonic.player.filter.ResponseHeaderFilter;
import org.directwebremoting.servlet.DwrServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.servlet.Filter;

import java.util.Properties;

@Configuration
@EnableWebMvc
public class ServletConfiguration extends WebMvcConfigurerAdapter {
    /**
     * Registers the DWR servlet.
     *
     * @return a registration bean.
     */
    @Bean
    public ServletRegistrationBean dwrServletRegistrationBean() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(new DwrServlet(), "/dwr/*");
        servlet.addInitParameter("crossDomainSessionSecurity","false");
        return servlet;
    }

    @Bean
    public ServletRegistrationBean cxfServletBean() {
        return new ServletRegistrationBean(new org.apache.cxf.transport.servlet.CXFServlet(), "/ws/*");
    }

    @Bean
    public FilterRegistrationBean bootstrapVerificationFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(bootstrapVerificationFiler());
        registration.addUrlPatterns("/*");
        registration.setName("BootstrapVerificationFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public Filter bootstrapVerificationFiler() {
        return new BootstrapVerificationFilter();
    }

    @Bean
    public FilterRegistrationBean parameterDecodingFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(parameterDecodingFilter());
        registration.addUrlPatterns("/*");
        registration.setName("ParameterDecodingFilter");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public Filter parameterDecodingFilter() {
        return new ParameterDecodingFilter();
    }

    @Bean
    public FilterRegistrationBean restFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(restFilter());
        registration.addUrlPatterns("/rest/*");
        registration.setName("RESTFilter");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    public Filter restFilter() {
        return new RESTFilter();
    }

    @Bean
    public FilterRegistrationBean requestEncodingFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(requestEncodingFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("encoding", "UTF-8");
        registration.setName("RequestEncodingFilter");
        registration.setOrder(4);
        return registration;
    }

    @Bean
    public Filter requestEncodingFilter() {
        return new RequestEncodingFilter();
    }

    @Bean
    public FilterRegistrationBean cacheFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(cacheFilter());
        registration.addUrlPatterns("/icons/*", "/style/*", "/script/*", "/dwr/*", "/icons/*", "/coverArt.view", "/avatar.view");
        registration.addInitParameter("Cache-Control", "max-age=36000");
        registration.setName("CacheFilter");
        registration.setOrder(5);
        return registration;
    }

    @Bean
    public Filter cacheFilter() {
        return new ResponseHeaderFilter();
    }

    @Bean
    public FilterRegistrationBean noCacheFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(noCacheFilter());
        registration.addUrlPatterns("/statusChart.view", "/userChart.view", "/playQueue.view", "/podcastChannels.view", "/podcastChannel.view", "/help.view", "/top.view", "/home.view");
        registration.addInitParameter("Cache-Control", "no-cache, post-check=0, pre-check=0");
        registration.addInitParameter("Pragma", "no-cache");
        registration.addInitParameter("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        registration.setName("NoCacheFilter");
        registration.setOrder(6);
        return registration;
    }

    @Bean
    public Filter metricsFilter() {
        return new MetricsFilter();
    }

    @Bean
    public FilterRegistrationBean metricsFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(metricsFilter());
        registration.setOrder(7);
        return registration;
    }


    @Bean
    public Filter noCacheFilter() {
        return new ResponseHeaderFilter();
    }
    
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
        // ResourceHttpRequestHandler below (which is intmax-1). Otherwise, that will
        // intercept every request before it gets here
        mapping.setOrder(Integer.MAX_VALUE - 2);

        mapping.setAlwaysUseFullPath(true);

        Properties properties = new Properties();
        properties.put("/podcast/**", podcastController);
        mapping.setMappings(properties);

        return mapping;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
    }
}
