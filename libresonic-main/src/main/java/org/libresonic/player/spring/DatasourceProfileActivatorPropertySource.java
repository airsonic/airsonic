package org.libresonic.player.spring;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertySource;

public class DatasourceProfileActivatorPropertySource extends PropertySource {
    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    public static final String DATASOURCE_CONFIG_TYPE = "database.config.type";
    final PropertySource parent;

    public DatasourceProfileActivatorPropertySource(PropertySource parent) {
        super(parent.getName());
        this.parent = parent;
    }

    @Override
    public Object getProperty(String name) {
        if(StringUtils.equalsIgnoreCase(name, SPRING_PROFILES_ACTIVE)) {
            String appendTo = "";
            Object existing = parent.getProperty(SPRING_PROFILES_ACTIVE);
            if(existing != null && existing instanceof String) {
                appendTo += (String) existing;
            }
            DataSourceConfigType dataSourceConfigType;
            Object rawType = parent.getProperty(DATASOURCE_CONFIG_TYPE);
            if(rawType != null && rawType instanceof String) {
                dataSourceConfigType = DataSourceConfigType.valueOf(StringUtils.upperCase((String) rawType));
            } else {
                dataSourceConfigType = DataSourceConfigType.LEGACY;
            }
            if(StringUtils.isNotBlank(appendTo)) {
                appendTo += ",";
            }
            appendTo += StringUtils.lowerCase(dataSourceConfigType.name());
            return appendTo;
        } else {
            return parent.getProperty(name);
        }
    }
}
