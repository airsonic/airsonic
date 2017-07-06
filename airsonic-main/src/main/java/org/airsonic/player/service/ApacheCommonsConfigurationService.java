package org.airsonic.player.service;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ApacheCommonsConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheCommonsConfigurationService.class);

    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    private final Configuration config;

    public static final String HEADER_COMMENT = "Airsonic preferences.  NOTE: This file is automatically generated."
                           + " Do not modify while application is running";

    public ApacheCommonsConfigurationService() {
        File propertyFile = SettingsService.getPropertyFile();
        if(!propertyFile.exists()) {
            try {
                FileUtils.touch(propertyFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not create new property file", e);
            }
        }
        Parameters params = new Parameters();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        layout.setHeaderComment(HEADER_COMMENT);
        layout.setGlobalSeparator("=");
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(
                params.properties()
                      .setFile(propertyFile)
                      .setSynchronizer(new ReadWriteSynchronizer())
                      .setLayout(layout));
        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Could not load property file at " + propertyFile, e);
        }
    }

    public void save() {
        try {
            builder.save();
        } catch (ConfigurationException e) {
            LOG.error("Unable to write to property file.", e);
        }
    }

    public Object getProperty(String key) {
        return config.getProperty(key);
    }

    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    public void clearProperty(String key) {
        config.clearProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }

    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    public int getInteger(String key, int defaultValue) {
        return config.getInteger(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    public ImmutableConfiguration getImmutableSnapshot() {
        MapConfiguration mapConfiguration = new MapConfiguration(new HashMap<>());
        mapConfiguration.copy(config);
        return mapConfiguration;
    }
}
