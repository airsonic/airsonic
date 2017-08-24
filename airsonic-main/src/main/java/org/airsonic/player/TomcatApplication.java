package org.airsonic.player;

import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

public class TomcatApplication {

    public static void configure(TomcatEmbeddedServletContainerFactory tomcatFactory) {

            tomcatFactory.addContextCustomizers((TomcatContextCustomizer) context -> {

                boolean development = (System.getProperty("airsonic.development") != null);

                // Increase the size and time before eviction of the Tomcat
                // cache so that resources aren't uncompressed too often.
                // See https://github.com/jhipster/generator-jhipster/issues/3995

                StandardRoot resources = new StandardRoot();
                if (development) {
                    resources.setCachingAllowed(false);
                } else {
                    resources.setCacheMaxSize(100000);
                    resources.setCacheObjectMaxSize(4000);
                    resources.setCacheTtl(24 * 3600 * 1000);  // 1 day, in milliseconds
                }
                context.setResources(resources);

                // Put Jasper in production mode so that JSP aren't recompiled
                // on each request.
                // See http://stackoverflow.com/questions/29653326/spring-boot-application-slow-because-of-jsp-compilation
                Container jsp = context.findChild("jsp");
                if (jsp instanceof Wrapper) {
                    ((Wrapper) jsp).addInitParameter("development", Boolean.toString(development));
                }
            });
    }
}
