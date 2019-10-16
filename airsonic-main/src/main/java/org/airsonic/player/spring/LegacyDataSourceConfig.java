package org.airsonic.player.spring;

import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@Profile("legacy")
public class LegacyDataSourceConfig {
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                // need this because the hsqldb driver doesn't support Connection.isValid for
                // pools (old driver)
                .type(DriverManagerDataSource.class).username("sa").password("")
                .url(SettingsService.getDefaultJDBCUrl()).driverClassName("org.hsqldb.jdbcDriver").build();
    }

    @Bean
    @Autowired
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
