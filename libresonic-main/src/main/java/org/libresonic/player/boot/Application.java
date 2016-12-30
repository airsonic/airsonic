package org.libresonic.player.boot;

import org.directwebremoting.servlet.DwrServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@Configuration
@ImportResource(value = {"classpath:/applicationContext-service.xml",
        "classpath:/applicationContext-cache.xml",
        "classpath:/applicationContext-sonos.xml",
        "classpath:/libresonic-servlet.xml"})
public class Application extends SpringBootServletInitializer {

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


    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Customize the application or call application.sources(...) to add sources
        // Since our example is itself a @Configuration class (via @SpringBootApplication)
        // we actually don't need to override this method.
        return application;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}