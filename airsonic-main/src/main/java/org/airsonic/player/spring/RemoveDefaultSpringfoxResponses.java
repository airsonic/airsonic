package org.airsonic.player.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.plugin.core.PluginRegistry;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spring.web.readers.operation.ResponseMessagesReader;

import java.util.Iterator;

public class RemoveDefaultSpringfoxResponses implements InitializingBean
//        implements ApplicationListener<ApplicationPreparedEvent>
{
    private static final Logger logger = LoggerFactory.getLogger(RemoveDefaultSpringfoxResponses.class);

    @Autowired
    @Qualifier("operationBuilderPluginRegistry")
    private PluginRegistry<OperationBuilderPlugin, DocumentationType> operationBuilderPlugins;

//    @Override
//    public void onApplicationEvent(ApplicationPreparedEvent event) {
//        ConfigurableListableBeanFactory beanFactory = event.getApplicationContext().getBeanFactory();
//        Object bean = beanFactory.getBean(ResponseMessagesReader.class);
//        event.getApplicationContext().getBeanFactory().destroyBean(bean);
//    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Iterator<OperationBuilderPlugin> iterator = operationBuilderPlugins.iterator();
        while(iterator.hasNext()) {
            OperationBuilderPlugin next = iterator.next();
            if(next instanceof ResponseMessagesReader) {
//                logger.warn("Removing ResponseMessagesReader");
//                iterator.remove();
                break;
            }
        }
    }
}
