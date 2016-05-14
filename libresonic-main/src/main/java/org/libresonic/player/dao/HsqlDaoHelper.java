/*
 * This file is part of Libresonic.
 *
 *  Libresonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libresonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package org.libresonic.player.dao;

import java.io.File;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;
import org.libresonic.player.dao.schema.hsql.Schema25;
import org.libresonic.player.dao.schema.hsql.Schema26;
import org.libresonic.player.dao.schema.hsql.Schema27;
import org.libresonic.player.dao.schema.hsql.Schema28;
import org.libresonic.player.dao.schema.hsql.Schema29;
import org.libresonic.player.dao.schema.hsql.Schema30;
import org.libresonic.player.dao.schema.hsql.Schema31;
import org.libresonic.player.dao.schema.hsql.Schema32;
import org.libresonic.player.dao.schema.hsql.Schema33;
import org.libresonic.player.dao.schema.hsql.Schema34;
import org.libresonic.player.dao.schema.hsql.Schema35;
import org.libresonic.player.dao.schema.hsql.Schema36;
import org.libresonic.player.dao.schema.hsql.Schema37;
import org.libresonic.player.dao.schema.hsql.Schema38;
import org.libresonic.player.dao.schema.hsql.Schema40;
import org.libresonic.player.dao.schema.hsql.Schema43;
import org.libresonic.player.dao.schema.hsql.Schema45;
import org.libresonic.player.dao.schema.hsql.Schema46;
import org.libresonic.player.dao.schema.hsql.Schema47;
import org.libresonic.player.dao.schema.hsql.Schema49;
import org.libresonic.player.dao.schema.hsql.Schema50;
import org.libresonic.player.dao.schema.hsql.Schema51;
import org.libresonic.player.dao.schema.hsql.Schema52;
import org.libresonic.player.dao.schema.hsql.Schema53;
import org.libresonic.player.service.SettingsService;

/**
 * DAO helper class which creates the data source, and updates the database schema.
 *
 * @author Sindre Mehus
 */
public class HsqlDaoHelper implements DaoHelper {

    private static final Logger LOG = Logger.getLogger(HsqlDaoHelper.class);

    private Schema[] schemas = {new Schema25(), new Schema26(), new Schema27(), new Schema28(), new Schema29(),
                                new Schema30(), new Schema31(), new Schema32(), new Schema33(), new Schema34(),
                                new Schema35(), new Schema36(), new Schema37(), new Schema38(), new Schema40(),
                                new Schema43(), new Schema45(), new Schema46(), new Schema47(), new Schema49(),
                                new Schema50(), new Schema51(), new Schema52(), new Schema53()};
    private DataSource dataSource;
    private static boolean shutdownHookAdded;

    public HsqlDaoHelper() {
        dataSource = createDataSource();
        checkDatabase();
        addShutdownHook();
    }

    private void addShutdownHook() {
        if (shutdownHookAdded) {
            return;
        }
        shutdownHookAdded = true;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Shutting down database...");
                getJdbcTemplate().execute("shutdown");
                System.err.println("Shutting down database - Done!");
            }
        });
    }

    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    private DataSource createDataSource() {
        File libresonicHome = SettingsService.getLibresonicHome();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:file:" + libresonicHome.getPath() + "/db/libresonic");
        ds.setUsername("sa");
        ds.setPassword("");

        return ds;
    }

    private void checkDatabase() {
        LOG.info("Checking database schema.");
        try {
            for (Schema schema : schemas) {
                schema.execute(getJdbcTemplate());
            }
            LOG.info("Done checking database schema.");
        } catch (Exception x) {
            LOG.error("Failed to initialize database.", x);
        }
    }
}
