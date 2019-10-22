package org.airsonic.player.spring;

import org.airsonic.player.service.SonosService;
import org.airsonic.player.service.sonos.SonosFaultInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.xml.ws.Endpoint;

import java.util.Collections;

@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class SonosConfiguration {

    @Bean
    public Endpoint sonosEndpoint(SonosService sonosService, SonosFaultInterceptor sonosFaultInterceptor) {
        EndpointImpl endpoint = new EndpointImpl(sonosService);
        endpoint.publish("/Sonos");
        endpoint.setOutFaultInterceptors(Collections.singletonList(sonosFaultInterceptor));
        return endpoint;
    }

}
