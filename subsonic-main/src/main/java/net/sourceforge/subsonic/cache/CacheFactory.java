/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.cache;


import java.io.File;

import org.springframework.beans.factory.InitializingBean;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * Initializes Ehcache and creates caches.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class CacheFactory implements InitializingBean {

    private static final Logger LOG = Logger.getLogger(CacheFactory.class);
    private CacheManager cacheManager;

    public void afterPropertiesSet() throws Exception {
        Configuration configuration = ConfigurationFactory.parseConfiguration();

        // Override configuration to make sure cache is stored in Subsonic home dir.
        File cacheDir = new File(SettingsService.getSubsonicHome(), "cache");
        configuration.getDiskStoreConfiguration().setPath(cacheDir.getPath());

        cacheManager = CacheManager.create(configuration);
    }

    public Ehcache getCache(String name) {
        return cacheManager.getCache(name);
    }
}
