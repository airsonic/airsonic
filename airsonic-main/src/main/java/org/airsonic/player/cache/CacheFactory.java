/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.cache;

import com.google.common.io.Resources;

import org.ehcache.CacheManager;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.core.Ehcache;
import org.ehcache.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.URL;

/**
 * Initializes Ehcache and creates caches.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class CacheFactory implements InitializingBean, DisposableBean {

    private static final Logger LOG= LoggerFactory.getLogger(CacheFactory.class);
    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        URL myUrl = Resources.getResource("ehcache.xml");
        Configuration xmlConfig = new XmlConfiguration(myUrl);

        // Override configuration to make sure cache is stored in Airsonic home dir.
        //File cacheDir = new File(SettingsService.getAirsonicHome(), "cache");
        //configuration.getDiskStoreConfiguration().setPath(cacheDir.getPath());

        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
        cacheManager.init();
    }

    public <K, V> Ehcache<K, V> getCache(String name, Class<K> keyType, Class<V> valueType) {
        return (Ehcache<K, V>) cacheManager.getCache(name, keyType, valueType);
    }

    @Override
    public void destroy() throws Exception {
        if (cacheManager != null) {
            try {
                cacheManager.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
