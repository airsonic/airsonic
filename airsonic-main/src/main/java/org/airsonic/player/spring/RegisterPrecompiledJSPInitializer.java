package org.airsonic.player.spring;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.webxmldomain.ServletDef;
import org.airsonic.player.spring.webxmldomain.ServletMappingDef;
import org.airsonic.player.spring.webxmldomain.WebApp;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;

@Component
public class RegisterPrecompiledJSPInitializer implements ServletContextInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterPrecompiledJSPInitializer.class);

    @Override
    public void onStartup(ServletContext servletContext) {
        if (SettingsService.isDevelopmentMode()) {
            LOG.debug("Not registering precompiled jsps");
        } else {
            LOG.debug("Registering precompiled jsps");
            registerPrecompiledJSPs(servletContext);
        }
    }

    private static void registerPrecompiledJSPs(ServletContext servletContext) {
        WebApp webApp = parseXmlFragment();
        for (ServletDef def :  webApp.getServletDefs()) {
            LOG.trace("Registering precompiled JSP: {} -> {}", def.getName(), def.getSclass());
            ServletRegistration.Dynamic reg = servletContext.addServlet(def.getName(), def.getSclass());
            // Need to set loadOnStartup somewhere between 0 and 128. 0 is highest priority. 99 should be fine
            reg.setLoadOnStartup(99);
        }

        for (ServletMappingDef mapping : webApp.getServletMappingDefs()) {
            LOG.trace("Mapping servlet: {} -> {}", mapping.getName(), mapping.getUrlPattern());
            servletContext.getServletRegistration(mapping.getName()).addMapping(mapping.getUrlPattern());
        }
    }

    private static WebApp parseXmlFragment() {
        InputStream precompiledJspWebXml = RegisterPrecompiledJSPInitializer.class.getResourceAsStream("/precompiled-jsp-web.xml");
        InputStream webXmlIS = new SequenceInputStream(
                new SequenceInputStream(
                        IOUtils.toInputStream("<web-app>", Charset.defaultCharset()),
                        precompiledJspWebXml),
                IOUtils.toInputStream("</web-app>", Charset.defaultCharset()));

        JAXBContext jaxbContext;
        try {
            jaxbContext = new JAXBDataBinding(WebApp.class).getContext();
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            WebApp webapp = (WebApp) unmarshaller.unmarshal(webXmlIS);
            try {
                webXmlIS.close();
            } catch (java.io.IOException e) {}
            return webapp;
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse precompiled-jsp-web.xml", e);
        }
    }

}
