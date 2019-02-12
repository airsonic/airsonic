package org.airsonic.test.cucumber.steps.api;

import cucumber.api.java.Before;
import org.airsonic.test.SpringContext;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringContext.class)
public class SpringStepDef {

    public SpringStepDef(AirsonicServer server) {

    }

    @Before
    public void setup_cucumber_spring_context(){
        // Dummy method so cucumber will recognize this class as glue
        // and use its context configuration.
    }
}
