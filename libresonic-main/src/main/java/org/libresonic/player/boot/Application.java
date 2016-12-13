package org.libresonic.player.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@Configuration
@ImportResource(value = {"classpath:/applicationContext-service.xml",
        "classpath:/applicationContext-cache.xml",
        "classpath:/applicationContext-sonos.xml",
        "classpath:/applicationContext-security.xml",
        "classpath:/libresonic-servlet.xml"})
public class Application extends SpringBootServletInitializer {

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