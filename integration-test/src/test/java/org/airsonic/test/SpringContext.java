package org.airsonic.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("org.airsonic.test")
@PropertySource("classpath:application.properties")
public class SpringContext {

}
