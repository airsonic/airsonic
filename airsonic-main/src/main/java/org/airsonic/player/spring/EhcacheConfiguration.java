package org.airsonic.player.spring;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.web.ShutdownListener;
import org.airsonic.player.cache.CacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContextListener;

@Configuration
public class EhcacheConfiguration {

    @Bean
    public ServletContextListener ehCacheShutdownListener() {
        return new ShutdownListener();
    }

    @Bean
    public Ehcache userCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("userCache");
    }

    @Bean
    public Ehcache mediaFileMemoryCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("mediaFileMemoryCache");
    }

    @Bean
    public CacheFactory cacheFactory() {
        return new CacheFactory();
    }
}
