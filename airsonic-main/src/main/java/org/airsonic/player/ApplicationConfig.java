package org.airsonic.player;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;

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
import org.springframework.context.annotation.ImportResource;

import net.sf.ehcache.constructs.web.ShutdownListener;

@Configuration
@ImportResource({ "classpath:/applicationContext-cache.xml", "classpath:/applicationContext-sonos.xml" })
public class ApplicationConfig {

    /**
     * Registers the DWR servlet.
     *
     * @return a registration bean.
     */
    @Bean
    public ServletRegistrationBean<DwrServlet> dwrServletRegistrationBean() {
        ServletRegistrationBean<DwrServlet> servlet = new ServletRegistrationBean<>(new DwrServlet(), "/dwr/*");
        servlet.addInitParameter("crossDomainSessionSecurity", "false");
        return servlet;
    }

    @Bean
    public ServletRegistrationBean<org.apache.cxf.transport.servlet.CXFServlet> cxfServletBean() {
        return new ServletRegistrationBean<>(new org.apache.cxf.transport.servlet.CXFServlet(), "/ws/*");
    }

    @Bean
    public ServletContextListener ehCacheShutdownListener() {
        return new ShutdownListener();
    }

    @Bean
    public FilterRegistrationBean<Filter> bootstrapVerificationFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(bootstrapVerificationFilter());
        registration.addUrlPatterns("/*");
        registration.setName("BootstrapVerificationFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public Filter bootstrapVerificationFilter() {
        return new BootstrapVerificationFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> parameterDecodingFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
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
    public FilterRegistrationBean<Filter> restFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
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
    public FilterRegistrationBean<Filter> requestEncodingFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
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
    public FilterRegistrationBean<Filter> cacheFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(cacheFilter());
        registration.addUrlPatterns("/icons/*", "/style/*", "/script/*", "/dwr/*", "/icons/*", "/coverArt.view",
                "/avatar.view");
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
    public FilterRegistrationBean<Filter> noCacheFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(noCacheFilter());
        registration.addUrlPatterns("/statusChart.view", "/userChart.view", "/playQueue.view", "/podcastChannels.view",
                "/podcastChannel.view", "/help.view", "/top.view", "/home.view");
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
    public FilterRegistrationBean<Filter> metricsFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(metricsFilter());
        registration.setOrder(7);
        return registration;
    }

    @Bean
    public Filter noCacheFilter() {
        return new ResponseHeaderFilter();
    }
}
