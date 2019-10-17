package org.airsonic.player.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("#{ systemProperties['DatabaseConfigEmbedUrl'] ?: T(org.airsonic.player.service.SettingsService).defaultJDBCUrl }")
    private String url;
    @Value("${DatabaseConfigEmbedUsername:sa}")
    private String user;
    @Value("${DatabaseConfigEmbedPassword:}")
    private String password;
    @Value("${DatabaseConfigEmbedDriver:org.hsqldb.jdbcDriver}")
    private String driver;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                // need this because the hsqldb driver doesn't support Connection.isValid for
                // pools (old driver)
                .type(DriverManagerDataSource.class).username(user).password(password).url(url).driverClassName(driver)
                .build();
    }

    @Bean
    @Autowired
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
