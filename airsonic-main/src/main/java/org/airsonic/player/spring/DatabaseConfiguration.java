package org.airsonic.player.spring;

import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.GenericDaoHelper;
import org.airsonic.player.dao.LegacyHsqlDaoHelper;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.Util;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Profile("legacy")
    @DependsOn("liquibase")
    public DaoHelper legacyDaoHelper(DataSource dataSource) {
        return new LegacyHsqlDaoHelper(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn("liquibase")
    public DaoHelper daoHelper(DataSource dataSource) {
        return new GenericDaoHelper(dataSource);
    }

    @Bean
    @Profile("legacy")
    public DataSource legacyDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        dataSource.setUrl(SettingsService.getDefaultJDBCUrl());
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    @Profile("embed")
    public DataSource embedDataSource(@Value("${DatabaseConfigEmbedDriver}") String driver,
                                           @Value("${DatabaseConfigEmbedUrl}") String url,
                                           @Value("${DatabaseConfigEmbedUsername}") String username,
                                           @Value("${DatabaseConfigEmbedPassword}") String password) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driver);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        return basicDataSource;
    }

    @Bean
    @Profile("jndi")
    public DataSource jndiDataSource(@Value("${DatabaseConfigJNDIName}") String jndiName) {
        JndiDataSourceLookup jndiLookup = new JndiDataSourceLookup();
        return jndiLookup.getDataSource(jndiName);
    }

    @Bean
    public File rollbackFile() {
        return new File(SettingsService.getAirsonicHome(), "rollback.sql");
    }

    @Bean
    public String userTableQuote(@Value("${DatabaseUsertableQuote:}") String value) {
        return value;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource,
                                     @Value("${DatabaseMysqlMaxlength:512}")
                                     String mysqlVarcharLimit,
                                     String userTableQuote) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog("classpath:liquibase/db-changelog.xml");
        springLiquibase.setRollbackFile(rollbackFile());
        Map<String, String> parameters = new HashMap<>();
        parameters.put("defaultMusicFolder", Util.getDefaultMusicFolder());
        parameters.put("mysqlVarcharLimit", mysqlVarcharLimit);
        parameters.put("userTableQuote", userTableQuote);
        springLiquibase.setChangeLogParameters(parameters);
        return springLiquibase;
    }

}
