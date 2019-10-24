package org.airsonic.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@SpringBootApplication(exclude = {
        JmxAutoConfiguration.class,
        MultipartAutoConfiguration.class // TODO: update to use spring boot builtin multipart support
})
public class Application extends SpringBootServletInitializer implements  WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

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
            Class<?> tomcatESCF = Class.forName("org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory");
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
            Class<?> jettyESCF = Class.forName("org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory");
            if (jettyESCF.isInstance(container)) {
                LOG.warn("Detected Jetty web server. Here there be dragons.");
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            LOG.debug("No jetty classes found");
        }
    }

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        doConfigure(builder).run(args);
    }

}
