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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.airsonic.player.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;

/**
 * Initializes Ehcache and creates caches.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class CacheFactory implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(CacheFactory.class);
    private CacheManager cacheManager;

    public void afterPropertiesSet() throws Exception {
        Configuration configuration = ConfigurationFactory.parseConfiguration();

        // Override configuration to make sure cache is stored in Airsonic home dir.
        File cacheDir = new File(SettingsService.getAirsonicHome(), "cache");
        configuration.getDiskStoreConfiguration().setPath(cacheDir.getPath());

        cacheManager = CacheManager.create(configuration);
    }

    public Ehcache getCache(String name) {
        return cacheManager.getCache(name);
    }
}
