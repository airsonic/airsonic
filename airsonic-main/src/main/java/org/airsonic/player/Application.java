package org.airsonic.player;

import net.sf.ehcache.constructs.web.ShutdownListener;
import org.airsonic.player.filter.*;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.HsqlDatabase;
import org.airsonic.player.util.Util;
import org.directwebremoting.servlet.DwrServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import liquibase.database.DatabaseFactory;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;

import java.io.IOException;
import java.lang.reflect.Method;

@SpringBootApplication(exclude = {
        JmxAutoConfiguration.class,
        MultipartAutoConfiguration.class // TODO: update to use spring boot builtin multipart support
})
@Configuration
@ImportResource({
        "classpath:/applicationContext-cache.xml",
        "classpath:/applicationContext-sonos.xml"
})
public class Application extends SpringBootServletInitializer
        implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    /**
     * Registers the DWR servlet.
     *
     * @return a registration bean.
     */
    @Bean
    public ServletRegistrationBean<DwrServlet> dwrServletRegistrationBean() {
        ServletRegistrationBean<DwrServlet> servlet = new ServletRegistrationBean<>(new DwrServlet(), "/dwr/*");
        servlet.addInitParameter("crossDomainSessionSecurity","false");
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
    public FilterRegistrationBean<Filter> noCacheFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
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

    private static SpringApplicationBuilder doConfigure(SpringApplicationBuilder application) {
        // Customize the application or call application.sources(...) to add sources
        // Since our example is itself a @Configuration class (via @SpringBootApplication)
        // we actually don't need to override this method.
        return application.sources(Application.class).web(WebApplicationType.SERVLET);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return doConfigure(application);
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {
        LOG.trace("Servlet container is {}", container.getClass().getCanonicalName());
        // Yes, there is a good reason we do this.
        // We cannot count on the tomcat classes being on the classpath which will
        // happen if the war is deployed to another app server like Jetty. So, we
        // ensure this class does not have any direct dependencies on any Tomcat
        // specific classes.
        try {
            Class<?> tomcatESCF = Class
                    .forName("org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory");
            if (tomcatESCF.isInstance(container)) {
                LOG.info("Detected Tomcat web server");
                LOG.debug("Attempting to optimize tomcat");
                Object tomcatESCFInstance = tomcatESCF.cast(container);
                Class<?> tomcatApplicationClass = Class.forName("org.airsonic.player.TomcatApplication");
                Method configure = ReflectionUtils.findMethod(tomcatApplicationClass, "configure", tomcatESCF);
                configure.invoke(null, tomcatESCFInstance);
                LOG.debug("Tomcat optimizations complete");
            } else {
                LOG.debug("Skipping tomcat optimization as we are not running on tomcat");
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            LOG.debug("No tomcat classes found");
        } catch (Exception e) {
            LOG.warn("An error happened while trying to optimize tomcat", e);
        }

        try {
            Class<?> jettyESCF = Class
                    .forName("org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory");
            if (jettyESCF.isInstance(container)) {
                LOG.warn("Detected Jetty web server. Here there be dragons.");
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            LOG.debug("No jetty classes found");
        }
    }

    public static void main(String[] args) throws IOException {
        String airsonicProperties = SettingsService.getPropertyFile().getAbsolutePath();

        // set it to parse airsonic.properties
        System.setProperty(ConfigFileApplicationListener.CONFIG_ADDITIONAL_LOCATION_PROPERTY, airsonicProperties);

        // set active profile
        ConfigurableEnvironment env = new StandardServletEnvironment();
        new PropertiesPropertySourceLoader().load(airsonicProperties, new FileSystemResource(airsonicProperties))
                .forEach(x -> env.getPropertySources().addLast(x));

        String activeProfile = env.getProperty("DatabaseConfigType");
        if (activeProfile == null) {
            activeProfile = "legacy";
        }
        System.setProperty(ConfigFileApplicationListener.ACTIVE_PROFILES_PROPERTY, activeProfile);

        // set migration properties
        System.setProperty("migrationRollbackFile",
                SettingsService.getAirsonicHome().getAbsolutePath() + "/rollback.sql");
        System.setProperty("migrationDefaultMusicFolder", Util.getDefaultMusicFolder());
        // add support for our "special" ancient hqldb that doesn't support schemas
        DatabaseFactory.getInstance().register(new HsqlDatabase());

        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        doConfigure(builder).run(args);
    }

}
