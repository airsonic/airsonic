package org.airsonic.player.command;

import org.airsonic.player.spring.DataSourceConfigType;

import javax.validation.constraints.NotNull;

public class DatabaseSettingsCommand {

    @NotNull
    private DataSourceConfigType configType;
    private String embedDriver;
    private String embedPassword;
    private String embedUrl;
    private String embedUsername;
    private String JNDIName;
    private int mysqlVarcharMaxlength;
    private String usertableQuote;

    public DataSourceConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(DataSourceConfigType configType) {
        this.configType = configType;
    }

    public String getEmbedDriver() {
        return embedDriver;
    }

    public void setEmbedDriver(String embedDriver) {
        this.embedDriver = embedDriver;
    }

    public String getEmbedPassword() {
        return embedPassword;
    }

    public void setEmbedPassword(String embedPassword) {
        this.embedPassword = embedPassword;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public String getEmbedUsername() {
        return embedUsername;
    }

    public void setEmbedUsername(String embedUsername) {
        this.embedUsername = embedUsername;
    }

    public String getJNDIName() {
        return JNDIName;
    }

    public void setJNDIName(String JNDIName) {
        this.JNDIName = JNDIName;
    }

    public int getMysqlVarcharMaxlength() {
        return mysqlVarcharMaxlength;
    }

    public void setMysqlVarcharMaxlength(int mysqlVarcharMaxlength) {
        this.mysqlVarcharMaxlength = mysqlVarcharMaxlength;
    }

    public String getUsertableQuote() {
        return usertableQuote;
    }

    public void setUsertableQuote(String usertableQuote) {
        this.usertableQuote = usertableQuote;
    }
}
